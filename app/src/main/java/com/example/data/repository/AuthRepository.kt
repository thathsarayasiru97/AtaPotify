package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.error.unwrapFirestoreException
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.UserProfile
import com.example.data.model.WhitelistMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

sealed interface AccessResolution {
    data class Authorized(
        val uid: String,
        val email: String,
        val isAdmin: Boolean,
        val memberKey: String,
        val displayName: String,
        val phone: String,
    ) : AccessResolution

    data class Denied(val email: String) : AccessResolution
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class AuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = FirebaseAuth.getInstance(),
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    suspend fun resolveAccessAndSyncProfile(user: FirebaseUser): Result<AccessResolution> {
        val rawEmail = user.email.orEmpty()
        val emailLower = AtaPotifyConstants.normalizeEmail(rawEmail)
        if (emailLower.isBlank() || !user.isEmailVerified) {
            return Result.success(AccessResolution.Denied(rawEmail.ifBlank { "Unknown account" }))
        }

        val isAdmin = emailLower == AtaPotifyConstants.ADMIN_EMAIL
        val memberKey = AtaPotifyConstants.memberKeyForEmail(emailLower)
        val fallbackName = user.displayName?.takeIf { it.isNotBlank() }
            ?: emailLower.substringBefore("@").replaceFirstChar { it.uppercase() }

        return try {
            var whitelistEntry: WhitelistMember? = null
            if (!isAdmin) {
                val whitelistPath = "whitelist"
                val snap = try {
                    db.collection(whitelistPath)
                        .whereEqualTo("email", emailLower)
                        .get()
                        .await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.LIST, whitelistPath, auth)
                    throw e
                }
                whitelistEntry = snap.documents
                    .mapNotNull { it.toObject(WhitelistMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .firstOrNull { it.active }
                if (whitelistEntry == null) {
                    return Result.success(AccessResolution.Denied(emailLower))
                }
            }

            val userPath = "users/${user.uid}"
            val userDocRef = db.collection("users").document(user.uid)
            val existingUser = try {
                userDocRef.get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, userPath, auth)
                throw e
            }

            val resolvedName = existingUser?.displayName?.takeIf { it.isNotBlank() }
                ?: whitelistEntry?.displayName?.takeIf { it.isNotBlank() }
                ?: fallbackName
            val resolvedPhone = existingUser?.phone?.takeIf { it.isNotBlank() }
                ?: whitelistEntry?.phone.orEmpty()
            val resolvedRole = if (isAdmin) AtaPotifyConstants.ROLE_ADMIN else AtaPotifyConstants.ROLE_MEMBER

            if (existingUser == null) {
                val payload = mapOf(
                    "uid" to user.uid,
                    "email" to emailLower,
                    "displayName" to resolvedName.take(60),
                    "role" to resolvedRole,
                    "phone" to resolvedPhone.take(24),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                try {
                    userDocRef.set(payload).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, userPath, auth)
                    throw e
                }
            }

            Result.success(
                AccessResolution.Authorized(
                    uid = user.uid,
                    email = emailLower,
                    isAdmin = isAdmin,
                    memberKey = memberKey,
                    displayName = resolvedName,
                    phone = resolvedPhone,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        val docPath = "users/$uid"
        return db.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = error.unwrapFirestoreException()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docPath, auth)
                }
                throw error
            }
    }

    suspend fun updateOwnProfile(
        emailLower: String,
        isAdmin: Boolean,
        newDisplayName: String,
        newPhone: String,
        monthIdsToSync: List<String>,
    ): Result<Unit> {
        val uid = requireUserId()
        val trimmedName = newDisplayName.trim().take(60)
        val cleanPhone = AtaPotifyConstants.sanitizeWhatsAppDigits(newPhone).take(24)
        require(trimmedName.isNotEmpty()) { "Display name cannot be blank" }

        val userPath = "users/$uid"
        val memberKey = AtaPotifyConstants.memberKeyForEmail(emailLower)
        val batch = db.batch()
        val userRef = db.collection("users").document(uid)
        batch.update(
            userRef,
            mapOf(
                "displayName" to trimmedName,
                "phone" to cleanPhone,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )

        if (!isAdmin) {
            val whitelistRef = db.collection("whitelist").document(memberKey)
            val whitelistExists = try {
                whitelistRef.get().await().exists()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, whitelistRef.path, auth)
                false
            }
            if (whitelistExists) {
                batch.update(
                    whitelistRef,
                    mapOf(
                        "displayName" to trimmedName,
                        "phone" to cleanPhone,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
        }

        for (monthId in monthIdsToSync.take(5)) {
            val memberDocRef = db.collection("payments").document(monthId)
                .collection("members").document(memberKey)
            val memberDocExists = try {
                memberDocRef.get().await().exists()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, memberDocRef.path, auth)
                false
            }
            if (memberDocExists) {
                batch.update(
                    memberDocRef,
                    mapOf(
                        "displayName" to trimmedName,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
        }

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userPath, auth)
            Result.failure(e)
        }
    }
}
