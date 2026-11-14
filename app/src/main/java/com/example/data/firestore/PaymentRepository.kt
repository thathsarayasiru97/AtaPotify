package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.model.ADMIN_EMAIL
import com.example.data.model.DEFAULT_MONTHLY_LKR
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.example.data.model.PaymentStatus
import com.example.data.model.PlanConfig
import com.example.data.model.ReceiptSlip
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.WhitelistMember
import com.example.data.model.emailToMemberKey
import com.example.data.model.formatMonthLabel
import com.example.data.model.normalizeEmail
import com.example.data.model.sanitizeWhatsAppPhone
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PaymentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        uid: String,
        email: String,
        displayName: String,
        isAdmin: Boolean
    ): Result<Unit> = runCatching {
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val cleanName = displayName.trim().ifBlank { email.substringBefore("@") }.take(60)
            val payload = mapOf(
                "uid" to uid,
                "email" to normalizeEmail(email),
                "displayName" to cleanName,
                "role" to if (isAdmin) UserRole.ADMIN.value else UserRole.MEMBER.value,
                "phone" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.WRITE, "users/$uid", auth)
    }

    suspend fun updateUserProfile(
        uid: String,
        email: String,
        displayName: String,
        phone: String,
        isAdmin: Boolean,
        activeMonthId: String?
    ): Result<Unit> = runCatching {
        requireUserId()
        val cleanName = displayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Display name cannot be empty" }
        val cleanPhone = sanitizeWhatsAppPhone(phone).take(24)
        val normalizedEmail = normalizeEmail(email)
        val memberKey = emailToMemberKey(normalizedEmail)

        val userRef = db.collection("users").document(uid)
        val userSnapshot = userRef.get().await()

        val whitelistRef = db.collection("whitelist").document(memberKey)
        val whitelistExists = runCatching { whitelistRef.get().await().exists() }.getOrDefault(false)

        val monthMemberRef = if (!activeMonthId.isNullOrBlank()) {
            db.collection("payments").document(activeMonthId).collection("members").document(memberKey)
        } else {
            null
        }
        val monthMemberExists = if (monthMemberRef != null) {
            runCatching { monthMemberRef.get().await().exists() }.getOrDefault(false)
        } else {
            false
        }

        val batch = db.batch()
        if (!userSnapshot.exists()) {
            batch.set(
                userRef,
                mapOf(
                    "uid" to uid,
                    "email" to normalizedEmail,
                    "displayName" to cleanName,
                    "role" to if (isAdmin) UserRole.ADMIN.value else UserRole.MEMBER.value,
                    "phone" to cleanPhone,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.update(
                userRef,
                mapOf(
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        if (whitelistExists) {
            batch.update(
                whitelistRef,
                mapOf(
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        if (monthMemberRef != null && monthMemberExists) {
            batch.update(
                monthMemberRef,
                mapOf(
                    "displayName" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "users/$uid", auth)
    }

    fun observeWhitelistEntryByEmail(email: String): Flow<WhitelistMember?> = flow {
        val normalized = normalizeEmail(email)
        val path = "whitelist"
        emitAll(
            db.collection("whitelist")
                .whereEqualTo("email", normalized)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WhitelistMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).firstOrNull()
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeAllWhitelist(): Flow<List<WhitelistMember>> = flow {
        val path = "whitelist"
        emitAll(
            db.collection("whitelist")
                .orderBy("displayName", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WhitelistMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observePlanConfig(): Flow<PlanConfig> = flow {
        val path = "plan_config/settings"
        emitAll(
            db.collection("plan_config").document("settings")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlanConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: PlanConfig()
                    } else {
                        PlanConfig()
                    }
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // Before Admin seeds plan_config/settings, return sensible default
                        emit(PlanConfig())
                        return@catch
                    }
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun updateMonthlyRate(
        amountLkr: Int,
        planName: String = "Spotify Family",
        applyToMonthId: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        require(amountLkr in 1..100000) { "Amount must be between 1 and 100000 LKR" }
        val activeWhitelist = db.collection("whitelist").get().await()
            .toObjects(WhitelistMember::class.java)
            .filter { it.active }
        val allowedEmails = buildAllowedEmails(activeWhitelist)

        val configRef = db.collection("plan_config").document("settings")
        val existingConfig = configRef.get().await()
        val batch = db.batch()
        val configPayload = mapOf(
            "planName" to planName.trim().ifBlank { "Spotify Family" }.take(60),
            "monthlyAmountLkr" to amountLkr.toLong(),
            "currency" to "LKR",
            "allowedEmails" to allowedEmails,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (existingConfig.exists()) {
            batch.update(configRef, configPayload)
        } else {
            batch.set(configRef, configPayload)
        }

        if (!applyToMonthId.isNullOrBlank()) {
            val monthRef = db.collection("payments").document(applyToMonthId)
            val monthSnap = monthRef.get().await()
            if (monthSnap.exists()) {
                batch.update(
                    monthRef,
                    mapOf(
                        "amountLkr" to amountLkr.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val memberDocs = monthRef.collection("members").get().await()
                for (doc in memberDocs.documents) {
                    if (doc.getString("status") != PaymentStatus.PAID.value) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "amountLkr" to amountLkr.toLong(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
        }

        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.WRITE, "plan_config/settings", auth)
    }

    suspend fun upsertWhitelistMember(
        displayName: String,
        email: String,
        phone: String,
        active: Boolean = true,
        activeMonthId: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val normalizedEmail = normalizeEmail(email)
        require(normalizedEmail.contains("@") && normalizedEmail.length >= 3) { "Enter a valid Google email" }
        val cleanName = displayName.trim().ifBlank { normalizedEmail.substringBefore("@") }.take(60)
        val cleanPhone = sanitizeWhatsAppPhone(phone).take(24)
        val memberKey = emailToMemberKey(normalizedEmail)

        val currentMembers = db.collection("whitelist").get().await()
            .toObjects(WhitelistMember::class.java)
            .filter { it.memberKey != memberKey }
        val updatedActiveMembers = if (active) {
            currentMembers.filter { it.active } + WhitelistMember(
                memberKey = memberKey,
                email = normalizedEmail,
                displayName = cleanName,
                phone = cleanPhone,
                active = true
            )
        } else {
            currentMembers.filter { it.active }
        }
        val allowedEmails = buildAllowedEmails(updatedActiveMembers)

        val memberRef = db.collection("whitelist").document(memberKey)
        val existingMember = memberRef.get().await()

        val configRef = db.collection("plan_config").document("settings")
        val existingConfig = configRef.get().await()
        val currentAmount = (existingConfig.getLong("monthlyAmountLkr") ?: DEFAULT_MONTHLY_LKR.toLong())
        val currentPlanName = existingConfig.getString("planName") ?: "Spotify Family"

        val batch = db.batch()
        if (existingMember.exists()) {
            batch.update(
                memberRef,
                mapOf(
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                memberRef,
                mapOf(
                    "memberKey" to memberKey,
                    "email" to normalizedEmail,
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "role" to UserRole.MEMBER.value,
                    "active" to active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        if (existingConfig.exists()) {
            batch.update(
                configRef,
                mapOf(
                    "allowedEmails" to allowedEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                configRef,
                mapOf(
                    "planName" to currentPlanName,
                    "monthlyAmountLkr" to currentAmount,
                    "currency" to "LKR",
                    "allowedEmails" to allowedEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Sync active month board permissions and optionally add newly active member
        if (!activeMonthId.isNullOrBlank()) {
            val monthRef = db.collection("payments").document(activeMonthId)
            val monthSnap = monthRef.get().await()
            if (monthSnap.exists()) {
                val monthAmount = monthSnap.getLong("amountLkr") ?: currentAmount
                batch.update(
                    monthRef,
                    mapOf(
                        "allowedEmails" to allowedEmails,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val existingMonthMembers = monthRef.collection("members").get().await()
                var memberFoundInMonth = false
                for (doc in existingMonthMembers.documents) {
                    val isTarget = doc.id == memberKey
                    if (isTarget) memberFoundInMonth = true
                    val updateMap = mutableMapOf<String, Any>(
                        "allowedEmails" to allowedEmails,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (isTarget) {
                        updateMap["displayName"] = cleanName
                    }
                    batch.update(doc.reference, updateMap)
                }
                if (active && !memberFoundInMonth) {
                    batch.set(
                        monthRef.collection("members").document(memberKey),
                        mapOf(
                            "memberKey" to memberKey,
                            "memberEmail" to normalizedEmail,
                            "displayName" to cleanName,
                            "status" to PaymentStatus.PENDING.value,
                            "amountLkr" to monthAmount,
                            "hasReceipt" to false,
                            "allowedEmails" to allowedEmails,
                            "reminderCount" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.WRITE, "whitelist", auth)
    }

    suspend fun deleteWhitelistMember(
        memberKey: String,
        activeMonthId: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val remainingActive = db.collection("whitelist").get().await()
            .toObjects(WhitelistMember::class.java)
            .filter { it.memberKey != memberKey && it.active }
        val allowedEmails = buildAllowedEmails(remainingActive)

        val batch = db.batch()
        batch.delete(db.collection("whitelist").document(memberKey))

        val configRef = db.collection("plan_config").document("settings")
        if (configRef.get().await().exists()) {
            batch.update(
                configRef,
                mapOf(
                    "allowedEmails" to allowedEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        if (!activeMonthId.isNullOrBlank()) {
            val monthRef = db.collection("payments").document(activeMonthId)
            if (monthRef.get().await().exists()) {
                batch.update(
                    monthRef,
                    mapOf(
                        "allowedEmails" to allowedEmails,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val membersSnap = monthRef.collection("members").get().await()
                for (doc in membersSnap.documents) {
                    if (doc.id != memberKey) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "allowedEmails" to allowedEmails,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
        }

        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.DELETE, "whitelist/$memberKey", auth)
    }

    fun observeMonths(userEmail: String, isAdmin: Boolean): Flow<List<PaymentMonth>> = flow {
        val path = "payments"
        val query = if (isAdmin) {
            db.collection(path).orderBy("monthId", Query.Direction.DESCENDING)
        } else {
            db.collection(path)
                .whereArrayContains("allowedEmails", normalizeEmail(userEmail))
                .orderBy("monthId", Query.Direction.DESCENDING)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentMonth::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.monthId }
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeMonthMembers(
        monthId: String,
        userEmail: String,
        isAdmin: Boolean
    ): Flow<List<MemberPaymentStatus>> = flow {
        val path = "payments/$monthId/members"
        val baseCollection = db.collection("payments").document(monthId).collection("members")
        val query = if (isAdmin) {
            baseCollection
        } else {
            baseCollection.whereArrayContains("allowedEmails", normalizeEmail(userEmail))
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MemberPaymentStatus::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<MemberPaymentStatus>(
                            {
                                when (it.paymentStatus) {
                                    PaymentStatus.VERIFYING -> 0
                                    PaymentStatus.PENDING -> 1
                                    PaymentStatus.PAID -> 2
                                }
                            },
                            { it.displayName.lowercase() }
                        )
                    )
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun startNewMonth(monthId: String, amountLkr: Int): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(monthId.matches(Regex("^[0-9]{4}-[0-9]{2}$"))) {
            "Month ID must be formatted as YYYY-MM"
        }
        require(amountLkr in 1..100000) { "Amount must be between 1 and 100000 LKR" }

        val activeWhitelist = db.collection("whitelist").get().await()
            .toObjects(WhitelistMember::class.java)
            .filter { it.active }
        val allowedEmails = buildAllowedEmails(activeWhitelist)

        val monthRef = db.collection("payments").document(monthId)
        val existingMonth = monthRef.get().await()
        val configRef = db.collection("plan_config").document("settings")
        val existingConfig = configRef.get().await()

        val batch = db.batch()
        if (existingConfig.exists()) {
            batch.update(
                configRef,
                mapOf(
                    "monthlyAmountLkr" to amountLkr.toLong(),
                    "allowedEmails" to allowedEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                configRef,
                mapOf(
                    "planName" to "Spotify Family",
                    "monthlyAmountLkr" to amountLkr.toLong(),
                    "currency" to "LKR",
                    "allowedEmails" to allowedEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        val label = formatMonthLabel(monthId)
        if (existingMonth.exists()) {
            batch.update(
                monthRef,
                mapOf(
                    "displayLabel" to label,
                    "amountLkr" to amountLkr.toLong(),
                    "allowedEmails" to allowedEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                monthRef,
                mapOf(
                    "monthId" to monthId,
                    "displayLabel" to label,
                    "amountLkr" to amountLkr.toLong(),
                    "allowedEmails" to allowedEmails,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        for (member in activeWhitelist) {
            val memberDocRef = monthRef.collection("members").document(member.memberKey)
            val existingMemberDoc = memberDocRef.get().await()
            if (existingMemberDoc.exists()) {
                batch.update(
                    memberDocRef,
                    mapOf(
                        "displayName" to member.displayName,
                        "status" to PaymentStatus.PENDING.value,
                        "amountLkr" to amountLkr.toLong(),
                        "hasReceipt" to false,
                        "allowedEmails" to allowedEmails,
                        "reminderCount" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    memberDocRef,
                    mapOf(
                        "memberKey" to member.memberKey,
                        "memberEmail" to normalizeEmail(member.email),
                        "displayName" to member.displayName,
                        "status" to PaymentStatus.PENDING.value,
                        "amountLkr" to amountLkr.toLong(),
                        "hasReceipt" to false,
                        "allowedEmails" to allowedEmails,
                        "reminderCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.WRITE, "payments/$monthId", auth)
    }

    suspend fun submitReceiptSlip(
        monthId: String,
        memberKey: String,
        memberEmail: String,
        mimeType: String,
        imageBase64: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val normalizedEmail = normalizeEmail(memberEmail)
        val memberRef = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        val slipRef = memberRef.collection("private").document("slip")

        val batch = db.batch()
        batch.set(
            slipRef,
            mapOf(
                "memberEmail" to normalizedEmail,
                "uploadedByUid" to uid,
                "mimeType" to mimeType,
                "imageBase64" to imageBase64,
                "uploadedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
        batch.update(
            memberRef,
            mapOf(
                "status" to PaymentStatus.VERIFYING.value,
                "hasReceipt" to true,
                "submittedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.WRITE, "payments/$monthId/members/$memberKey", auth)
    }

    suspend fun getReceiptSlip(monthId: String, memberKey: String): Result<ReceiptSlip?> = runCatching {
        requireUserId()
        val snapshot = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
            .collection("private").document("slip")
            .get()
            .await()
        if (snapshot.exists()) {
            snapshot.toObject(
                ReceiptSlip::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } else {
            null
        }
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.GET, "payments/$monthId/members/$memberKey/private/slip", auth)
    }

    suspend fun confirmPayment(monthId: String, memberKey: String): Result<Unit> = runCatching {
        requireUserId()
        val memberRef = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        memberRef.update(
            mapOf(
                "status" to PaymentStatus.PAID.value,
                "confirmedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "payments/$monthId/members/$memberKey", auth)
    }

    suspend fun rejectReceiptToPending(monthId: String, memberKey: String): Result<Unit> = runCatching {
        requireUserId()
        val memberRef = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        memberRef.update(
            mapOf(
                "status" to PaymentStatus.PENDING.value,
                "hasReceipt" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "payments/$monthId/members/$memberKey", auth)
    }

    suspend fun recordWhatsAppReminder(
        monthId: String,
        memberKey: String,
        currentCount: Long
    ): Result<Unit> = runCatching {
        requireUserId()
        val memberRef = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        memberRef.update(
            mapOf(
                "lastRemindedAt" to FieldValue.serverTimestamp(),
                "reminderCount" to (currentCount + 1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.UPDATE, "payments/$monthId/members/$memberKey", auth)
    }

    private fun buildAllowedEmails(activeMembers: List<WhitelistMember>): List<String> {
        return (activeMembers.map { normalizeEmail(it.email) } + normalizeEmail(ADMIN_EMAIL))
            .distinct()
            .take(25)
    }
}
