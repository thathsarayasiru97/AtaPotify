package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.error.unwrapFirestoreException
import com.example.data.model.AccessRequest
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.FamilyGroup
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

    fun observeFamilyGroups(): Flow<List<FamilyGroup>> {
        val path = "family_groups"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FamilyGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<FamilyGroup> { it.sortOrder }.thenBy { it.name.lowercase() })
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

    suspend fun ensureDefaultFamilyGroupsIfAdmin(
        defaultAmountLkr: Long = AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
    ): Result<Unit> {
        requireUserId()
        val colRef = db.collection("family_groups")
        return try {
            val snap = colRef.get().await()
            if (snap.isEmpty) {
                val defaultStart = "${AtaPotifyConstants.currentMonthId()}-01"
                val safeRate = defaultAmountLkr.coerceIn(1L, 100_000L)
                val batch = db.batch()
                batch.set(
                    colRef.document(AtaPotifyConstants.GROUP_1),
                    mapOf(
                        "groupId" to AtaPotifyConstants.GROUP_1,
                        "name" to "Family 1",
                        "startDate" to defaultStart,
                        "amountLkr" to safeRate,
                        "colorHex" to AtaPotifyConstants.DEFAULT_GROUP_1_COLOR_HEX,
                        "maxSlots" to AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS.toLong(),
                        "sortOrder" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                batch.set(
                    colRef.document(AtaPotifyConstants.GROUP_2),
                    mapOf(
                        "groupId" to AtaPotifyConstants.GROUP_2,
                        "name" to "Family 2",
                        "startDate" to defaultStart,
                        "amountLkr" to safeRate,
                        "colorHex" to AtaPotifyConstants.DEFAULT_GROUP_2_COLOR_HEX,
                        "maxSlots" to AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS.toLong(),
                        "sortOrder" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun saveFamilyGroup(
        groupId: String?,
        name: String,
        startDate: String,
        amountLkr: Long,
        colorHex: String,
        maxSlots: Long,
        existingGroups: List<FamilyGroup>,
        alsoUpdateActiveMonthMembers: Boolean,
        activeMonth: PaymentMonth?,
        activeMonthMembers: List<MemberPaymentStatus>,
        address: String = "",
        managerName: String = "",
        managerEmail: String = "",
        inviteLink: String = "",
        notes: String = "",
    ): Result<String> {
        requireUserId()
        val cleanName = name.trim().take(40)
        require(cleanName.isNotEmpty()) { "Group name cannot be empty" }
        val cleanStartDate = startDate.trim()
        require(cleanStartDate.isEmpty() || cleanStartDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
            "Start date must be YYYY-MM-DD"
        }
        require(amountLkr in 1..100_000) { "Amount must be between 1 and 100,000 LKR" }
        val cleanColor = AtaPotifyConstants.normalizeHexColor(colorHex)
        val safeSlots = maxSlots.coerceIn(1L, 20L)
        val cleanAddress = address.trim().take(240)
        val cleanManagerName = managerName.trim().take(80)
        val cleanManagerEmail = managerEmail.trim().take(160)
        val cleanInviteLink = inviteLink.trim().take(300)
        val cleanNotes = notes.trim().take(320)

        val targetId = if (!groupId.isNullOrBlank()) {
            AtaPotifyConstants.normalizeFamilyGroup(groupId)
        } else {
            AtaPotifyConstants.generateGroupId(
                cleanName,
                existingGroups.map { it.effectiveId }.toSet(),
            )
        }

        val groupRef = db.collection("family_groups").document(targetId)
        val existingDoc = try {
            groupRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, groupRef.path, auth)
            null
        }

        val batch = db.batch()
        if (existingDoc != null && existingDoc.exists()) {
            batch.update(
                groupRef,
                mapOf(
                    "name" to cleanName,
                    "startDate" to cleanStartDate,
                    "amountLkr" to amountLkr,
                    "colorHex" to cleanColor,
                    "maxSlots" to safeSlots,
                    "address" to cleanAddress,
                    "managerName" to cleanManagerName,
                    "managerEmail" to cleanManagerEmail,
                    "inviteLink" to cleanInviteLink,
                    "notes" to cleanNotes,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        } else {
            val nextSort = (existingGroups.maxOfOrNull { it.sortOrder }?.plus(1L))
                ?: existingGroups.size.toLong()
            batch.set(
                groupRef,
                mapOf(
                    "groupId" to targetId,
                    "name" to cleanName,
                    "startDate" to cleanStartDate,
                    "amountLkr" to amountLkr,
                    "colorHex" to cleanColor,
                    "maxSlots" to safeSlots,
                    "sortOrder" to nextSort.coerceIn(0L, 1000L),
                    "address" to cleanAddress,
                    "managerName" to cleanManagerName,
                    "managerEmail" to cleanManagerEmail,
                    "inviteLink" to cleanInviteLink,
                    "notes" to cleanNotes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        }

        if (alsoUpdateActiveMonthMembers && activeMonth != null) {
            val monthMembersRef = db.collection("payments").document(activeMonth.monthId).collection("members")
            for (member in activeMonthMembers) {
                if (member.normalizedGroup == targetId) {
                    batch.update(
                        monthMembersRef.document(member.memberKey),
                        mapOf(
                            "amountLkr" to amountLkr,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }
        }

        return try {
            batch.commit().await()
            Result.success(targetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, groupRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteFamilyGroup(
        groupId: String,
        reassignToGroupId: String?,
        whitelist: List<WhitelistMember>,
        activeMonth: PaymentMonth?,
        activeMonthMembers: List<MemberPaymentStatus>,
    ): Result<Unit> {
        requireUserId()
        val normalizedTarget = AtaPotifyConstants.normalizeFamilyGroup(groupId)
        val groupRef = db.collection("family_groups").document(normalizedTarget)
        val batch = db.batch()
        batch.delete(groupRef)

        if (!reassignToGroupId.isNullOrBlank()) {
            val fallbackGroup = AtaPotifyConstants.normalizeFamilyGroup(reassignToGroupId)
            if (fallbackGroup != normalizedTarget) {
                for (member in whitelist) {
                    if (member.normalizedGroup == normalizedTarget) {
                        batch.update(
                            db.collection("whitelist").document(member.memberKey),
                            mapOf(
                                "familyGroup" to fallbackGroup,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    }
                }
                if (activeMonth != null) {
                    val monthMembersRef = db.collection("payments").document(activeMonth.monthId).collection("members")
                    for (member in activeMonthMembers) {
                        if (member.normalizedGroup == normalizedTarget) {
                            batch.update(
                                monthMembersRef.document(member.memberKey),
                                mapOf(
                                    "familyGroup" to fallbackGroup,
                                    "updatedAt" to FieldValue.serverTimestamp(),
                                )
                            )
                        }
                    }
                }
            }
        }

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, groupRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun transferMemberToGroup(
        memberKey: String,
        targetGroupId: String,
        targetGroupAmountLkr: Long? = null,
        updateActiveMonthAmount: Boolean = false,
        activeMonth: PaymentMonth?,
        activeMonthMembers: List<MemberPaymentStatus>,
    ): Result<Unit> {
        requireUserId()
        val normalizedTarget = AtaPotifyConstants.normalizeFamilyGroup(targetGroupId)
        val whitelistRef = db.collection("whitelist").document(memberKey)
        val wlExists = try {
            whitelistRef.get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }

        val batch = db.batch()
        if (wlExists) {
            batch.update(
                whitelistRef,
                mapOf(
                    "familyGroup" to normalizedTarget,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        }

        if (activeMonth != null && activeMonthMembers.any { it.memberKey == memberKey }) {
            val memberRef = db.collection("payments").document(activeMonth.monthId)
                .collection("members").document(memberKey)
            val updates = mutableMapOf<String, Any>(
                "familyGroup" to normalizedTarget,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (updateActiveMonthAmount && targetGroupAmountLkr != null && targetGroupAmountLkr in 1..100_000) {
                updates["amountLkr"] = targetGroupAmountLkr
            }
            batch.update(memberRef, updates)
        }

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, whitelistRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeWhitelist(
        userEmailLower: String = "",
        isAdmin: Boolean = true,
    ): Flow<List<WhitelistMember>> {
        val path = "whitelist"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("email", AtaPotifyConstants.normalizeEmail(userEmailLower))
        }
        return query
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

    fun observePendingAccessRequests(): Flow<List<AccessRequest>> {
        val path = "access_requests"
        return db.collection(path)
            .whereEqualTo("status", AtaPotifyConstants.STATUS_PENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AccessRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
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
            val snap = ref.get().await()
            val totalLkr = (snap.getLong("amountLkr") ?: 0L).coerceAtLeast(0L)
            val method = if (snap.getBoolean("hasReceipt") == true) {
                AtaPotifyConstants.PAYMENT_METHOD_BANK_SLIP
            } else {
                AtaPotifyConstants.PAYMENT_METHOD_DIRECT
            }
            val updates = mutableMapOf<String, Any>(
                "status" to AtaPotifyConstants.STATUS_PAID,
                "paymentMethod" to method,
                "confirmedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (totalLkr > 0L) {
                updates["paidAmountLkr"] = totalLkr
            }
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
            Result.failure(e)
        }
    }

    suspend fun markPaidInCash(
        monthId: String,
        member: MemberPaymentStatus,
    ): Result<Unit> {
        requireUserId()
        val ref = db.collection("payments").document(monthId)
            .collection("members").document(member.memberKey)
        val totalLkr = member.amountLkr.coerceAtLeast(1L)
        return try {
            ref.update(
                mapOf(
                    "status" to AtaPotifyConstants.STATUS_PAID,
                    "paidAmountLkr" to totalLkr,
                    "paymentMethod" to AtaPotifyConstants.PAYMENT_METHOD_CASH,
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

    suspend fun recordPartialPayment(
        monthId: String,
        member: MemberPaymentStatus,
        additionalAmountLkr: Long,
        isCash: Boolean,
    ): Result<Boolean> {
        requireUserId()
        require(additionalAmountLkr > 0L) { "Partial payment amount must be positive" }
        val total = member.amountLkr.coerceAtLeast(1L)
        val newPaid = (member.effectivePaidLkr + additionalAmountLkr).coerceIn(1L, total)
        val isNowSettled = newPaid >= total
        val method = if (isCash) {
            AtaPotifyConstants.PAYMENT_METHOD_CASH
        } else {
            AtaPotifyConstants.PAYMENT_METHOD_BANK_SLIP
        }
        val ref = db.collection("payments").document(monthId)
            .collection("members").document(member.memberKey)
        val updates = mutableMapOf<String, Any>(
            "paidAmountLkr" to newPaid,
            "paymentMethod" to method,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (isNowSettled) {
            updates["status"] = AtaPotifyConstants.STATUS_PAID
            updates["confirmedAt"] = FieldValue.serverTimestamp()
        }
        return try {
            ref.update(updates).await()
            Result.success(isNowSettled)
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
                    "paidAmountLkr" to 0L,
                    "paymentMethod" to FieldValue.delete(),
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

    suspend fun rewindPayment(
        monthId: String,
        memberKey: String,
        targetStatus: String,
    ): Result<Unit> {
        requireUserId()
        val safeStatus = when (targetStatus) {
            AtaPotifyConstants.STATUS_VERIFYING -> AtaPotifyConstants.STATUS_VERIFYING
            else -> AtaPotifyConstants.STATUS_PENDING
        }
        val ref = db.collection("payments").document(monthId)
            .collection("members").document(memberKey)
        return try {
            ref.update(
                mapOf(
                    "status" to safeStatus,
                    "paidAmountLkr" to 0L,
                    "paymentMethod" to FieldValue.delete(),
                    "confirmedAt" to FieldValue.delete(),
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

    suspend fun editMemberPayment(
        monthId: String,
        member: MemberPaymentStatus,
        newDisplayName: String,
        newStatus: String,
        newAmountLkr: Long,
        clearReceipt: Boolean,
        resetReminders: Boolean,
        familyGroup: String? = null,
        newPaidAmountLkr: Long = member.paidAmountLkr,
        newPaymentMethod: String = member.paymentMethod,
    ): Result<Unit> {
        requireUserId()
        val cleanName = newDisplayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Display name cannot be blank" }
        require(newAmountLkr in 1..100_000) { "Amount must be between 1 and 100,000 LKR" }
        require(
            newStatus in listOf(
                AtaPotifyConstants.STATUS_PENDING,
                AtaPotifyConstants.STATUS_VERIFYING,
                AtaPotifyConstants.STATUS_PAID,
            )
        ) { "Invalid payment status" }
        val normalizedGroup = familyGroup?.let { AtaPotifyConstants.normalizeFamilyGroup(it) }
        val safeMethod = when (newPaymentMethod) {
            AtaPotifyConstants.PAYMENT_METHOD_CASH,
            AtaPotifyConstants.PAYMENT_METHOD_BANK_SLIP,
            AtaPotifyConstants.PAYMENT_METHOD_DIRECT -> newPaymentMethod
            else -> ""
        }

        val memberRef = db.collection("payments").document(monthId)
            .collection("members").document(member.memberKey)
        val slipRef = memberRef.collection("private").document(AtaPotifyConstants.SLIP_DOC_ID)

        val updates = mutableMapOf<String, Any>(
            "displayName" to cleanName,
            "status" to newStatus,
            "amountLkr" to newAmountLkr,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (normalizedGroup != null) {
            updates["familyGroup"] = normalizedGroup
        }

        if (clearReceipt) {
            updates["hasReceipt"] = false
        }
        if (resetReminders) {
            updates["reminderCount"] = 0L
            updates["lastRemindedAt"] = FieldValue.delete()
        }
        if (newStatus == AtaPotifyConstants.STATUS_PAID) {
            updates["paidAmountLkr"] = newAmountLkr
            updates["paymentMethod"] = safeMethod.ifBlank {
                if (!clearReceipt && member.hasReceipt) {
                    AtaPotifyConstants.PAYMENT_METHOD_BANK_SLIP
                } else {
                    AtaPotifyConstants.PAYMENT_METHOD_DIRECT
                }
            }
            if (member.confirmedAt == null) {
                updates["confirmedAt"] = FieldValue.serverTimestamp()
            }
        } else {
            val maxPartial = (newAmountLkr - 1L).coerceAtLeast(0L)
            val clampedPartial = newPaidAmountLkr.coerceIn(0L, maxPartial)
            updates["paidAmountLkr"] = clampedPartial
            if (clampedPartial > 0L && safeMethod.isNotBlank()) {
                updates["paymentMethod"] = safeMethod
            } else if (clampedPartial == 0L) {
                updates["paymentMethod"] = FieldValue.delete()
            }
            if (member.confirmedAt != null || member.status == AtaPotifyConstants.STATUS_PAID) {
                updates["confirmedAt"] = FieldValue.delete()
            }
        }

        val batch = db.batch()
        batch.update(memberRef, updates)
        if (clearReceipt && member.hasReceipt) {
            batch.delete(slipRef)
        }
        if (normalizedGroup != null) {
            val whitelistRef = db.collection("whitelist").document(member.memberKey)
            val wlExists = try {
                whitelistRef.get().await().exists()
            } catch (_: Exception) {
                false
            }
            if (wlExists) {
                batch.update(
                    whitelistRef,
                    mapOf(
                        "familyGroup" to normalizedGroup,
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
            handleFirestoreError(e, OperationType.UPDATE, memberRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteMemberPayment(
        monthId: String,
        member: MemberPaymentStatus,
        allMonthMembers: List<MemberPaymentStatus>,
    ): Result<Unit> {
        requireUserId()
        val remaining = allMonthMembers.filter { it.memberKey != member.memberKey }
        val updatedAllowedEmails = (remaining.map { AtaPotifyConstants.normalizeEmail(it.memberEmail) } +
            AtaPotifyConstants.ADMIN_EMAIL).distinct().take(25)

        val monthRef = db.collection("payments").document(monthId)
        val targetRef = monthRef.collection("members").document(member.memberKey)
        val slipRef = targetRef.collection("private").document(AtaPotifyConstants.SLIP_DOC_ID)

        val batch = db.batch()
        batch.delete(slipRef)
        batch.delete(targetRef)
        batch.update(
            monthRef,
            mapOf(
                "allowedEmails" to updatedAllowedEmails,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
        for (other in remaining) {
            batch.update(
                monthRef.collection("members").document(other.memberKey),
                mapOf(
                    "allowedEmails" to updatedAllowedEmails,
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
            handleFirestoreError(e, OperationType.DELETE, targetRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun addMemberToMonth(
        month: PaymentMonth,
        existingMembers: List<MemberPaymentStatus>,
        email: String,
        displayName: String,
        amountLkr: Long,
        familyGroup: String = AtaPotifyConstants.GROUP_1,
    ): Result<Unit> {
        requireUserId()
        val emailLower = AtaPotifyConstants.normalizeEmail(email)
        require(emailLower.contains("@") && emailLower.length in 3..160) { "Invalid email" }
        val cleanName = displayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Name cannot be blank" }
        require(amountLkr in 1..100_000) { "Amount must be between 1 and 100,000 LKR" }
        val normalizedGroup = AtaPotifyConstants.normalizeFamilyGroup(familyGroup)

        val memberKey = AtaPotifyConstants.memberKeyForEmail(emailLower)
        val mergedEmails = (month.allowedEmails + existingMembers.map { AtaPotifyConstants.normalizeEmail(it.memberEmail) } +
            emailLower + AtaPotifyConstants.ADMIN_EMAIL).distinct().take(25)

        val monthRef = db.collection("payments").document(month.monthId)
        val memberRef = monthRef.collection("members").document(memberKey)
        val batch = db.batch()

        batch.update(
            monthRef,
            mapOf(
                "allowedEmails" to mergedEmails,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
        for (other in existingMembers) {
            if (other.memberKey != memberKey) {
                batch.update(
                    monthRef.collection("members").document(other.memberKey),
                    mapOf(
                        "allowedEmails" to mergedEmails,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
        }
        val existingEntry = existingMembers.firstOrNull { it.memberKey == memberKey }
        if (existingEntry != null) {
            batch.update(
                memberRef,
                mapOf(
                    "displayName" to cleanName,
                    "amountLkr" to amountLkr,
                    "familyGroup" to normalizedGroup,
                    "allowedEmails" to mergedEmails,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        } else {
            batch.set(
                memberRef,
                mapOf(
                    "memberKey" to memberKey,
                    "memberEmail" to emailLower,
                    "displayName" to cleanName,
                    "status" to AtaPotifyConstants.STATUS_PENDING,
                    "amountLkr" to amountLkr,
                    "hasReceipt" to false,
                    "allowedEmails" to mergedEmails,
                    "reminderCount" to 0L,
                    "familyGroup" to normalizedGroup,
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
            handleFirestoreError(e, OperationType.WRITE, memberRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteBillingMonth(
        monthId: String,
        memberKeys: List<String>,
        statKeys: List<String>,
    ): Result<Unit> {
        requireUserId()
        val monthRef = db.collection("payments").document(monthId)
        val batch = db.batch()
        for (key in memberKeys) {
            val memberRef = monthRef.collection("members").document(key)
            batch.delete(memberRef.collection("private").document(AtaPotifyConstants.SLIP_DOC_ID))
            batch.delete(memberRef)
        }
        for (key in statKeys) {
            batch.delete(monthRef.collection("stats").document(key))
        }
        batch.delete(monthRef)

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, monthRef.path, auth)
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
        familyGroups: List<FamilyGroup> = emptyList(),
    ): Result<Unit> {
        val uid = requireUserId()
        require(monthId.matches(Regex("^[0-9]{4}-[0-9]{2}$"))) { "Invalid month ID format (YYYY-MM)" }
        require(amountLkr in 1..100_000) { "Amount must be between 1 and 100,000 LKR" }

        val rateByGroup = familyGroups.associate { it.effectiveId to it.amountLkr }
        val allowedEmails = (activeMembers.map { AtaPotifyConstants.normalizeEmail(it.email) } +
            AtaPotifyConstants.ADMIN_EMAIL).distinct().take(25)
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
            val memberAmount = rateByGroup[member.normalizedGroup]?.takeIf { it in 1..100_000 } ?: amountLkr
            batch.set(
                memberRef,
                mapOf(
                    "memberKey" to member.memberKey,
                    "memberEmail" to AtaPotifyConstants.normalizeEmail(member.email),
                    "displayName" to member.displayName.take(60),
                    "status" to AtaPotifyConstants.STATUS_PENDING,
                    "amountLkr" to memberAmount,
                    "hasReceipt" to false,
                    "allowedEmails" to allowedEmails,
                    "reminderCount" to 0L,
                    "familyGroup" to member.normalizedGroup,
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

    suspend fun updateBankDetails(
        bankName: String,
        bankAccountNumber: String,
        bankAccountHolder: String,
        bankBranch: String,
        existingAllowedEmails: List<String>,
    ): Result<Unit> {
        requireUserId()
        val cleanBankName = bankName.trim().take(80)
        val cleanAccNo = bankAccountNumber.trim().take(60)
        val cleanHolder = bankAccountHolder.trim().take(80)
        val cleanBranch = bankBranch.trim().take(80)

        val configRef = db.collection("plan_config").document(AtaPotifyConstants.PLAN_CONFIG_DOC_ID)
        return try {
            val snap = configRef.get().await()
            if (snap.exists()) {
                configRef.update(
                    mapOf(
                        "bankName" to cleanBankName,
                        "bankAccountNumber" to cleanAccNo,
                        "bankAccountHolder" to cleanHolder,
                        "bankBranch" to cleanBranch,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                configRef.set(
                    mapOf(
                        "planName" to AtaPotifyConstants.DEFAULT_PLAN_NAME,
                        "monthlyAmountLkr" to AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
                        "currency" to AtaPotifyConstants.CURRENCY_LKR,
                        "allowedEmails" to existingAllowedEmails.take(25),
                        "bankName" to cleanBankName,
                        "bankAccountNumber" to cleanAccNo,
                        "bankAccountHolder" to cleanHolder,
                        "bankBranch" to cleanBranch,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, configRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updatePinnedAnnouncement(
        announcement: String,
        existingAllowedEmails: List<String>,
    ): Result<Unit> {
        val clean = announcement.trim().take(280)
        return updatePinnedAnnouncements(
            announcements = if (clean.isNotEmpty()) listOf(clean) else emptyList(),
            existingAllowedEmails = existingAllowedEmails,
        )
    }

    suspend fun updatePinnedAnnouncements(
        announcements: List<String>,
        existingAllowedEmails: List<String>,
    ): Result<Unit> {
        requireUserId()
        val cleanList = announcements
            .map { it.trim().take(280) }
            .filter { it.isNotEmpty() }
            .take(AtaPotifyConstants.MAX_PINNED_ANNOUNCEMENTS)
        val primaryAnnouncement = cleanList.firstOrNull().orEmpty()
        val configRef = db.collection("plan_config").document(AtaPotifyConstants.PLAN_CONFIG_DOC_ID)
        return try {
            val snap = configRef.get().await()
            if (snap.exists()) {
                configRef.update(
                    mapOf(
                        "pinnedAnnouncement" to primaryAnnouncement,
                        "pinnedAnnouncements" to cleanList,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                configRef.set(
                    mapOf(
                        "planName" to AtaPotifyConstants.DEFAULT_PLAN_NAME,
                        "monthlyAmountLkr" to AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
                        "currency" to AtaPotifyConstants.CURRENCY_LKR,
                        "allowedEmails" to existingAllowedEmails.take(25),
                        "pinnedAnnouncement" to primaryAnnouncement,
                        "pinnedAnnouncements" to cleanList,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, configRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun confirmAdvancePaymentMonths(
        startMonth: PaymentMonth,
        member: MemberPaymentStatus,
        monthsCount: Int,
        activeWhitelist: List<WhitelistMember>,
    ): Result<List<String>> {
        val uid = requireUserId()
        val safeMonthsCount = monthsCount.coerceIn(1, 12)
        val targetMonthIds = AtaPotifyConstants.consecutiveMonthIds(startMonth.monthId, safeMonthsCount)
        val memberEmailLower = AtaPotifyConstants.normalizeEmail(member.memberEmail)
        val baseAllowedEmails = (
            startMonth.allowedEmails +
                activeWhitelist.filter { it.active }.map { AtaPotifyConstants.normalizeEmail(it.email) } +
                memberEmailLower +
                AtaPotifyConstants.ADMIN_EMAIL
            ).distinct().take(25)

        return try {
            val batch = db.batch()
            for (monthId in targetMonthIds) {
                val monthRef = db.collection("payments").document(monthId)
                val monthSnap = monthRef.get().await()
                val memberRef = monthRef.collection("members").document(member.memberKey)

                if (monthSnap.exists()) {
                    val memberSnap = memberRef.get().await()
                    if (memberSnap.exists()) {
                        batch.update(
                            memberRef,
                            mapOf(
                                "status" to AtaPotifyConstants.STATUS_PAID,
                                "paidAmountLkr" to member.amountLkr,
                                "paymentMethod" to AtaPotifyConstants.PAYMENT_METHOD_DIRECT,
                                "confirmedAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    } else {
                        batch.set(
                            memberRef,
                            mapOf(
                                "memberKey" to member.memberKey,
                                "memberEmail" to memberEmailLower,
                                "displayName" to member.displayName.take(60),
                                "status" to AtaPotifyConstants.STATUS_PAID,
                                "amountLkr" to member.amountLkr,
                                "paidAmountLkr" to member.amountLkr,
                                "paymentMethod" to AtaPotifyConstants.PAYMENT_METHOD_DIRECT,
                                "hasReceipt" to false,
                                "allowedEmails" to baseAllowedEmails,
                                "reminderCount" to 0L,
                                "familyGroup" to member.normalizedGroup,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "confirmedAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    }
                } else {
                    val label = AtaPotifyConstants.monthLabelFromId(monthId).take(40)
                    batch.set(
                        monthRef,
                        mapOf(
                            "monthId" to monthId,
                            "displayLabel" to label,
                            "amountLkr" to startMonth.amountLkr,
                            "allowedEmails" to baseAllowedEmails,
                            "createdByUid" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                    val rosterKeys = mutableSetOf<String>()
                    for (wl in activeWhitelist.filter { it.active }.take(25)) {
                        rosterKeys.add(wl.memberKey)
                        val isTarget = wl.memberKey == member.memberKey
                        val wlRef = monthRef.collection("members").document(wl.memberKey)
                        val payload = mutableMapOf<String, Any>(
                            "memberKey" to wl.memberKey,
                            "memberEmail" to AtaPotifyConstants.normalizeEmail(wl.email),
                            "displayName" to wl.displayName.take(60),
                            "status" to if (isTarget) AtaPotifyConstants.STATUS_PAID else AtaPotifyConstants.STATUS_PENDING,
                            "amountLkr" to if (isTarget) member.amountLkr else startMonth.amountLkr,
                            "hasReceipt" to false,
                            "allowedEmails" to baseAllowedEmails,
                            "reminderCount" to 0L,
                            "familyGroup" to wl.normalizedGroup,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                        if (isTarget) {
                            payload["paidAmountLkr"] = member.amountLkr
                            payload["paymentMethod"] = AtaPotifyConstants.PAYMENT_METHOD_DIRECT
                            payload["confirmedAt"] = FieldValue.serverTimestamp()
                        }
                        batch.set(wlRef, payload)
                    }
                    if (member.memberKey !in rosterKeys) {
                        batch.set(
                            memberRef,
                            mapOf(
                                "memberKey" to member.memberKey,
                                "memberEmail" to memberEmailLower,
                                "displayName" to member.displayName.take(60),
                                "status" to AtaPotifyConstants.STATUS_PAID,
                                "amountLkr" to member.amountLkr,
                                "paidAmountLkr" to member.amountLkr,
                                "paymentMethod" to AtaPotifyConstants.PAYMENT_METHOD_DIRECT,
                                "hasReceipt" to false,
                                "allowedEmails" to baseAllowedEmails,
                                "reminderCount" to 0L,
                                "familyGroup" to member.normalizedGroup,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "confirmedAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    }
                }
            }
            batch.commit().await()
            Result.success(targetMonthIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "payments/${startMonth.monthId}", auth)
            Result.failure(e)
        }
    }

    suspend fun fetchAllMonthsStats(
        months: List<PaymentMonth>,
        userEmailLower: String,
        isAdmin: Boolean,
    ): Map<String, List<ListeningStat>> {
        if (auth.currentUser == null || months.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, List<ListeningStat>>()
        for (month in months.take(12)) {
            val baseCol = db.collection("payments").document(month.monthId).collection("stats")
            val query = if (isAdmin) baseCol else baseCol.whereArrayContains("allowedEmails", userEmailLower)
            val stats = try {
                query.get().await()
                    .toObjects(ListeningStat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            result[month.monthId] = stats
        }
        return result
    }

    suspend fun fetchAllMonthsMembers(
        months: List<PaymentMonth>,
        userEmailLower: String,
        isAdmin: Boolean,
    ): Map<String, List<MemberPaymentStatus>> {
        if (auth.currentUser == null || months.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, List<MemberPaymentStatus>>()
        for (month in months.take(12)) {
            val baseCol = db.collection("payments").document(month.monthId).collection("members")
            val query = if (isAdmin) baseCol else baseCol.whereArrayContains("allowedEmails", userEmailLower)
            val members = try {
                query.get().await()
                    .toObjects(MemberPaymentStatus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            result[month.monthId] = members
        }
        return result
    }

    suspend fun upsertWhitelistMember(
        email: String,
        displayName: String,
        phone: String,
        active: Boolean,
        allCurrentWhitelist: List<WhitelistMember>,
        activeMonth: PaymentMonth?,
        activeMonthMembers: List<MemberPaymentStatus>,
        spotifyEmail: String? = null,
        familyGroup: String? = null,
        groupAmountLkr: Long? = null,
    ): Result<Unit> {
        requireUserId()
        val emailLower = AtaPotifyConstants.normalizeEmail(email)
        require(emailLower.contains("@") && emailLower.length in 3..160) { "Enter a valid Google email" }
        val cleanName = displayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Name cannot be empty" }
        val cleanPhone = AtaPotifyConstants.sanitizeWhatsAppDigits(phone).take(24)
        val cleanSpotifyEmail = spotifyEmail?.let { AtaPotifyConstants.normalizeEmail(it).take(160) }
        val memberKey = AtaPotifyConstants.memberKeyForEmail(emailLower)
        val existingWlModel = allCurrentWhitelist.firstOrNull { it.memberKey == memberKey }
        val normalizedGroup = AtaPotifyConstants.normalizeFamilyGroup(
            familyGroup ?: existingWlModel?.familyGroup ?: AtaPotifyConstants.GROUP_1
        )

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
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "phone" to cleanPhone,
                "active" to active,
                "familyGroup" to normalizedGroup,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (cleanSpotifyEmail != null) {
                updates["spotifyEmail"] = cleanSpotifyEmail
            }
            batch.update(whitelistRef, updates)
        } else {
            batch.set(
                whitelistRef,
                mapOf(
                    "memberKey" to memberKey,
                    "email" to emailLower,
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "spotifyEmail" to (cleanSpotifyEmail ?: ""),
                    "role" to AtaPotifyConstants.ROLE_MEMBER,
                    "active" to active,
                    "familyGroup" to normalizedGroup,
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
                        "familyGroup" to normalizedGroup,
                        "allowedEmails" to mergedMonthEmails,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else if (active) {
                val effectiveAmount = groupAmountLkr?.takeIf { it in 1..100_000 } ?: activeMonth.amountLkr
                batch.set(
                    monthRef.collection("members").document(memberKey),
                    mapOf(
                        "memberKey" to memberKey,
                        "memberEmail" to emailLower,
                        "displayName" to cleanName,
                        "status" to AtaPotifyConstants.STATUS_PENDING,
                        "amountLkr" to effectiveAmount,
                        "hasReceipt" to false,
                        "allowedEmails" to mergedMonthEmails,
                        "reminderCount" to 0L,
                        "familyGroup" to normalizedGroup,
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

    suspend fun approveAccessRequest(
        request: AccessRequest,
        allCurrentWhitelist: List<WhitelistMember>,
        activeMonth: PaymentMonth?,
        activeMonthMembers: List<MemberPaymentStatus>,
        familyGroup: String = AtaPotifyConstants.GROUP_1,
        groupAmountLkr: Long? = null,
    ): Result<Unit> {
        requireUserId()
        val emailLower = AtaPotifyConstants.normalizeEmail(request.email)
        val memberKey = request.memberKey.ifBlank { AtaPotifyConstants.memberKeyForEmail(emailLower) }
        val cleanName = request.displayName.trim().ifBlank { emailLower.substringBefore("@") }.take(60)
        val cleanPhone = AtaPotifyConstants.sanitizeWhatsAppDigits(request.phone).take(24)
        val cleanSpotifyEmail = AtaPotifyConstants.normalizeEmail(request.spotifyEmail).take(160)
        val normalizedGroup = AtaPotifyConstants.normalizeFamilyGroup(familyGroup)

        val whitelistRef = db.collection("whitelist").document(memberKey)
        val existingWhitelistDoc = try {
            whitelistRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, whitelistRef.path, auth)
            null
        }

        val updatedActiveEmails = (allCurrentWhitelist
            .filter { it.active && it.memberKey != memberKey }
            .map { AtaPotifyConstants.normalizeEmail(it.email) } + emailLower)
            .distinct()
            .take(25)

        val batch = db.batch()
        if (existingWhitelistDoc != null && existingWhitelistDoc.exists()) {
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "phone" to cleanPhone,
                "active" to true,
                "familyGroup" to normalizedGroup,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (cleanSpotifyEmail.isNotBlank()) {
                updates["spotifyEmail"] = cleanSpotifyEmail
            }
            batch.update(whitelistRef, updates)
        } else {
            batch.set(
                whitelistRef,
                mapOf(
                    "memberKey" to memberKey,
                    "email" to emailLower,
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "spotifyEmail" to cleanSpotifyEmail,
                    "role" to AtaPotifyConstants.ROLE_MEMBER,
                    "active" to true,
                    "familyGroup" to normalizedGroup,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        }

        val reqUid = request.uid.ifBlank { request.id }
        if (reqUid.isNotBlank()) {
            batch.update(
                db.collection("access_requests").document(reqUid),
                mapOf(
                    "status" to "approved",
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
                        "familyGroup" to normalizedGroup,
                        "allowedEmails" to mergedMonthEmails,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                val effectiveAmount = groupAmountLkr?.takeIf { it in 1..100_000 } ?: activeMonth.amountLkr
                batch.set(
                    monthRef.collection("members").document(memberKey),
                    mapOf(
                        "memberKey" to memberKey,
                        "memberEmail" to emailLower,
                        "displayName" to cleanName,
                        "status" to AtaPotifyConstants.STATUS_PENDING,
                        "amountLkr" to effectiveAmount,
                        "hasReceipt" to false,
                        "allowedEmails" to mergedMonthEmails,
                        "reminderCount" to 0L,
                        "familyGroup" to normalizedGroup,
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

    suspend fun declineAccessRequest(request: AccessRequest): Result<Unit> {
        requireUserId()
        val reqUid = request.uid.ifBlank { request.id }
        val reqRef = db.collection("access_requests").document(reqUid)
        return try {
            reqRef.update(
                mapOf(
                    "status" to "rejected",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path, auth)
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
