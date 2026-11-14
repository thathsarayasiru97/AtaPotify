package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.error.unwrapFirestoreException
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.ListeningStat
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.PlanConfig
import com.example.data.model.ReceiptSlip
import com.example.data.model.WhitelistMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PaymentRepository(
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

    fun observePlanConfig(): Flow<PlanConfig> {
        val path = "plan_config/${AtaPotifyConstants.PLAN_CONFIG_DOC_ID}"
        return db.collection("plan_config")
            .document(AtaPotifyConstants.PLAN_CONFIG_DOC_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(PlanConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: PlanConfig()
            }
            .catch { error ->
                val firestoreError = error.unwrapFirestoreException()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path, auth)
                }
                throw error
            }
    }

    suspend fun ensureDefaultPlanConfigIfAdmin(): Result<Unit> {
        requireUserId()
        val ref = db.collection("plan_config").document(AtaPotifyConstants.PLAN_CONFIG_DOC_ID)
        return try {
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "planName" to AtaPotifyConstants.DEFAULT_PLAN_NAME,
                    "monthlyAmountLkr" to AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
                    "currency" to AtaPotifyConstants.CURRENCY_LKR,
                    "allowedEmails" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
            Result.failure(e)
        }
    }

    fun observeWhitelist(): Flow<List<WhitelistMember>> {
        val path = "whitelist"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WhitelistMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayName.lowercase() }
            }
            .catch { error ->
                val firestoreError = error.unwrapFirestoreException()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    fun observePaymentMonths(userEmailLower: String, isAdmin: Boolean): Flow<List<PaymentMonth>> {
        val path = "payments"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereArrayContains("allowedEmails", userEmailLower)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentMonth::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.monthId }
            }
            .catch { error ->
                val firestoreError = error.unwrapFirestoreException()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    fun observeMonthMembers(
        monthId: String,
        userEmailLower: String,
        isAdmin: Boolean,
    ): Flow<List<MemberPaymentStatus>> {
        val path = "payments/$monthId/members"
        val baseCollection = db.collection("payments").document(monthId).collection("members")
        val query = if (isAdmin) {
            baseCollection
        } else {
            baseCollection.whereArrayContains("allowedEmails", userEmailLower)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(MemberPaymentStatus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<MemberPaymentStatus> {
                        when (it.status) {
                            AtaPotifyConstants.STATUS_PENDING -> 0
                            AtaPotifyConstants.STATUS_VERIFYING -> 1
                            else -> 2
                        }
                    }.thenBy { it.displayName.lowercase() })
            }
            .catch { error ->
                val firestoreError = error.unwrapFirestoreException()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    fun observeMonthStats(
        monthId: String,
        userEmailLower: String,
        isAdmin: Boolean,
    ): Flow<List<ListeningStat>> {
        val path = "payments/$monthId/stats"
        val baseCollection = db.collection("payments").document(monthId).collection("stats")
        val query = if (isAdmin) {
            baseCollection
        } else {
            baseCollection.whereArrayContains("allowedEmails", userEmailLower)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(ListeningStat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.minutesListened }
            }
            .catch { error ->
                val firestoreError = error.unwrapFirestoreException()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    suspend fun uploadReceiptSlip(
        monthId: String,
        memberKey: String,
        memberEmail: String,
        mimeType: String,
        imageBase64: String,
    ): Result<Unit> {
        val uid = requireUserId()
        val emailLower = AtaPotifyConstants.normalizeEmail(memberEmail)
        val memberRef = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        val slipRef = memberRef.collection("private").document(AtaPotifyConstants.SLIP_DOC_ID)

        val batch = db.batch()
        batch.set(
            slipRef,
            mapOf(
                "memberEmail" to emailLower,
                "uploadedByUid" to uid,
                "mimeType" to mimeType,
                "imageBase64" to imageBase64,
                "uploadedAt" to FieldValue.serverTimestamp(),
            )
        )
        batch.update(
            memberRef,
            mapOf(
                "status" to AtaPotifyConstants.STATUS_VERIFYING,
                "hasReceipt" to true,
                "submittedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, slipRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun fetchReceiptSlip(monthId: String, memberKey: String): Result<ReceiptSlip?> {
        requireUserId()
        val slipRef = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
            .collection("private").document(AtaPotifyConstants.SLIP_DOC_ID)
        return try {
            val snap = slipRef.get().await()
            val slip = snap.toObject(ReceiptSlip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(slip)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, slipRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun confirmPayment(monthId: String, memberKey: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        return try {
            ref.update(
                mapOf(
                    "status" to AtaPotifyConstants.STATUS_PAID,
                    "confirmedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            Result.failure(e)
        }
    }

    suspend fun rejectPaymentToPending(monthId: String, memberKey: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        return try {
            ref.update(
                mapOf(
                    "status" to AtaPotifyConstants.STATUS_PENDING,
                    "hasReceipt" to false,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            Result.failure(e)
        }
    }

    suspend fun recordWhatsAppReminder(
        monthId: String,
        memberKey: String,
        currentCount: Long,
    ): Result<Unit> {
        requireUserId()
        val ref = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        return try {
            ref.update(
                mapOf(
                    "lastRemindedAt" to FieldValue.serverTimestamp(),
                    "reminderCount" to (currentCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            Result.failure(e)
        }
    }

    suspend fun startNewMonth(
        monthId: String,
        displayLabel: String,
        amountLkr: Long,
        activeMembers: List<WhitelistMember>,
    ): Result<Unit> {
        val uid = requireUserId()
        require(monthId.matches(Regex("^[0-9]{4}-[0-9]{2}$"))) { "Invalid month ID format (YYYY-MM)" }
        require(amountLkr in 1..100_000) { "Amount must be between 1 and 100,000 LKR" }

        val allowedEmails = activeMembers.map { AtaPotifyConstants.normalizeEmail(it.email) }.distinct().take(25)
        val monthRef = db.collection("payments").document(monthId)
        val batch = db.batch()
        batch.set(
            monthRef,
            mapOf(
                "monthId" to monthId,
                "displayLabel" to displayLabel.take(40),
                "amountLkr" to amountLkr,
                "allowedEmails" to allowedEmails,
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )

        for (member in activeMembers.take(25)) {
            val memberRef = monthRef.collection("members").document(member.memberKey)
            batch.set(
                memberRef,
                mapOf(
                    "memberKey" to member.memberKey,
                    "memberEmail" to AtaPotifyConstants.normalizeEmail(member.email),
                    "displayName" to member.displayName.take(60),
                    "status" to AtaPotifyConstants.STATUS_PENDING,
                    "amountLkr" to amountLkr,
                    "hasReceipt" to false,
                    "allowedEmails" to allowedEmails,
                    "reminderCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        }

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, monthRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateMonthlyAmountLkr(
        newAmountLkr: Long,
        alsoUpdateMonthId: String?,
        existingAllowedEmails: List<String>,
        monthMemberKeys: List<String>,
    ): Result<Unit> {
        requireUserId()
        require(newAmountLkr in 1..100_000) { "Amount must be between 1 and 100,000 LKR" }

        val configRef = db.collection("plan_config").document(AtaPotifyConstants.PLAN_CONFIG_DOC_ID)
        return try {
            val snap = configRef.get().await()
            val batch = db.batch()
            if (snap.exists()) {
                batch.update(
                    configRef,
                    mapOf(
                        "monthlyAmountLkr" to newAmountLkr,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                batch.set(
                    configRef,
                    mapOf(
                        "planName" to AtaPotifyConstants.DEFAULT_PLAN_NAME,
                        "monthlyAmountLkr" to newAmountLkr,
                        "currency" to AtaPotifyConstants.CURRENCY_LKR,
                        "allowedEmails" to existingAllowedEmails.take(25),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            if (!alsoUpdateMonthId.isNullOrBlank()) {
                val monthRef = db.collection("payments").document(alsoUpdateMonthId)
                batch.update(
                    monthRef,
                    mapOf(
                        "amountLkr" to newAmountLkr,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                for (key in monthMemberKeys) {
                    batch.update(
                        monthRef.collection("members").document(key),
                        mapOf(
                            "amountLkr" to newAmountLkr,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, configRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun upsertWhitelistMember(
        email: String,
        displayName: String,
        phone: String,
        active: Boolean,
        allCurrentWhitelist: List<WhitelistMember>,
        activeMonth: PaymentMonth?,
        activeMonthMembers: List<MemberPaymentStatus>,
    ): Result<Unit> {
        requireUserId()
        val emailLower = AtaPotifyConstants.normalizeEmail(email)
        require(emailLower.contains("@") && emailLower.length in 3..160) { "Enter a valid Google email" }
        val cleanName = displayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Name cannot be empty" }
        val cleanPhone = AtaPotifyConstants.sanitizeWhatsAppDigits(phone).take(24)
        val memberKey = AtaPotifyConstants.memberKeyForEmail(emailLower)

        val whitelistRef = db.collection("whitelist").document(memberKey)
        val existingDoc = try {
            whitelistRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, whitelistRef.path, auth)
            null
        }

        val updatedActiveEmails = (allCurrentWhitelist
            .filter { it.active && it.memberKey != memberKey }
            .map { AtaPotifyConstants.normalizeEmail(it.email) } +
            if (active) listOf(emailLower) else emptyList())
            .distinct()
            .take(25)

        val batch = db.batch()
        if (existingDoc != null && existingDoc.exists()) {
            batch.update(
                whitelistRef,
                mapOf(
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        } else {
            batch.set(
                whitelistRef,
                mapOf(
                    "memberKey" to memberKey,
                    "email" to emailLower,
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "role" to AtaPotifyConstants.ROLE_MEMBER,
                    "active" to active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        }

        val configRef = db.collection("plan_config").document(AtaPotifyConstants.PLAN_CONFIG_DOC_ID)
        val configExists = try {
            configRef.get().await().exists()
        } catch (e: Exception) {
            false
        }
        if (configExists) {
            batch.update(
                configRef,
                mapOf(
                    "allowedEmails" to updatedActiveEmails,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        } else {
            batch.set(
                configRef,
                mapOf(
                    "planName" to AtaPotifyConstants.DEFAULT_PLAN_NAME,
                    "monthlyAmountLkr" to AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
                    "currency" to AtaPotifyConstants.CURRENCY_LKR,
                    "allowedEmails" to updatedActiveEmails,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        }

        if (activeMonth != null) {
            val monthRef = db.collection("payments").document(activeMonth.monthId)
            val mergedMonthEmails = (activeMonth.allowedEmails + updatedActiveEmails).distinct().take(25)
            batch.update(
                monthRef,
                mapOf(
                    "allowedEmails" to mergedMonthEmails,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            val existingMemberEntry = activeMonthMembers.firstOrNull { it.memberKey == memberKey }
            if (existingMemberEntry != null) {
                batch.update(
                    monthRef.collection("members").document(memberKey),
                    mapOf(
                        "displayName" to cleanName,
                        "allowedEmails" to mergedMonthEmails,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else if (active) {
                batch.set(
                    monthRef.collection("members").document(memberKey),
                    mapOf(
                        "memberKey" to memberKey,
                        "memberEmail" to emailLower,
                        "displayName" to cleanName,
                        "status" to AtaPotifyConstants.STATUS_PENDING,
                        "amountLkr" to activeMonth.amountLkr,
                        "hasReceipt" to false,
                        "allowedEmails" to mergedMonthEmails,
                        "reminderCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            for (other in activeMonthMembers) {
                if (other.memberKey != memberKey) {
                    batch.update(
                        monthRef.collection("members").document(other.memberKey),
                        mapOf(
                            "allowedEmails" to mergedMonthEmails,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }
        }

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, whitelistRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun removeWhitelistMember(
        memberKey: String,
        allCurrentWhitelist: List<WhitelistMember>,
    ): Result<Unit> {
        requireUserId()
        val whitelistRef = db.collection("whitelist").document(memberKey)
        val remainingEmails = allCurrentWhitelist
            .filter { it.active && it.memberKey != memberKey }
            .map { AtaPotifyConstants.normalizeEmail(it.email) }
            .distinct()
            .take(25)

        val configRef = db.collection("plan_config").document(AtaPotifyConstants.PLAN_CONFIG_DOC_ID)
        val batch = db.batch()
        batch.delete(whitelistRef)
        val configExists = try {
            configRef.get().await().exists()
        } catch (e: Exception) {
            false
        }
        if (configExists) {
            batch.update(
                configRef,
                mapOf(
                    "allowedEmails" to remainingEmails,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        }

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, whitelistRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun syncListeningStats(
        monthId: String,
        memberKey: String,
        memberEmail: String,
        displayName: String,
        spotifyDisplayName: String,
        minutesListened: Long,
        tracksCounted: Long,
        topArtist: String,
        topTrack: String,
        allowedEmails: List<String>,
    ): Result<Unit> {
        requireUserId()
        val statRef = db.collection("payments").document(monthId)
            .collection("stats").document(memberKey)
        val payload = mapOf(
            "memberKey" to memberKey,
            "memberEmail" to AtaPotifyConstants.normalizeEmail(memberEmail),
            "displayName" to displayName.take(60),
            "spotifyDisplayName" to spotifyDisplayName.take(80),
            "minutesListened" to minutesListened.coerceIn(0L, 50_000L),
            "tracksCounted" to tracksCounted.coerceIn(0L, 20_000L),
            "topArtist" to topArtist.take(100),
            "topTrack" to topTrack.take(120),
            "allowedEmails" to allowedEmails.take(25),
            "syncedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            statRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, statRef.path, auth)
            Result.failure(e)
        }
    }
}
