package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.WhitelistMember
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtaPotifyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueMonthId(): String {
        val year = (2100..9999).random()
        val num = (10..99).random()
        return "$year-$num"
    }

    private suspend fun <T> step(name: String, block: suspend () -> T): T {
        return try {
            withTimeout(TIMEOUT_MS) { block() }
        } catch (e: Throwable) {
            throw AssertionError("RuleTest failed at step [$name]: ${e.message}", e)
        }
    }

    @Test
    fun adminCanWhitelistMemberStartMonthAndConfirmReceipt() = runBlocking {
        signInTestUser(AtaPotifyConstants.ADMIN_EMAIL)
        val authRepo = AuthRepository(firestore, auth)
        val paymentRepo = PaymentRepository(firestore, auth)

        val adminAccess = step("admin resolveAccessAndSyncProfile") {
            authRepo.resolveAccessAndSyncProfile(auth.currentUser!!).getOrThrow()
        }
        assertTrue(adminAccess is AccessResolution.Authorized && adminAccess.isAdmin)

        val friendEmail = "friend_${UUID.randomUUID().toString().take(6)}@gmail.com"
        val friendKey = AtaPotifyConstants.memberKeyForEmail(friendEmail)
        val monthId = uniqueMonthId()

        step("admin upsertWhitelistMember") {
            paymentRepo.upsertWhitelistMember(
                email = friendEmail,
                displayName = "Kasun",
                phone = "94771234567",
                active = true,
                allCurrentWhitelist = emptyList(),
                activeMonth = null,
                activeMonthMembers = emptyList(),
            ).getOrThrow()
        }

        val whitelist = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeWhitelist().first { list -> list.any { it.memberKey == friendKey } }
        }
        val member = whitelist.first { it.memberKey == friendKey }

        step("admin updatePinnedAnnouncements") {
            paymentRepo.updatePinnedAnnouncements(
                announcements = listOf(
                    "New billing cycle is open! Please transfer before the 5th.",
                    "Welcome to Family Group 2 members!",
                ),
                existingAllowedEmails = listOf(friendEmail, AtaPotifyConstants.ADMIN_EMAIL),
            ).getOrThrow()
        }

        step("admin startNewMonth") {
            paymentRepo.startNewMonth(
                monthId = monthId,
                displayLabel = "Test Month",
                amountLkr = 450L,
                activeMembers = listOf(member),
            ).getOrThrow()
        }

        // Friend signs in, verifies access, edits profile, uploads slip, syncs Spotify stats
        signInTestUser(friendEmail)
        val friendAccess = step("friend resolveAccessAndSyncProfile") {
            authRepo.resolveAccessAndSyncProfile(auth.currentUser!!).getOrThrow()
        }
        assertTrue(friendAccess is AccessResolution.Authorized && !friendAccess.isAdmin)

        step("friend saveOwnSpotifyEmail") {
            authRepo.saveOwnSpotifyEmail(
                emailLower = friendEmail,
                isAdmin = false,
                spotifyEmail = "kasun.music@gmail.com",
            ).getOrThrow()
        }

        step("friend updateOwnProfile") {
            authRepo.updateOwnProfile(
                emailLower = friendEmail,
                isAdmin = false,
                newDisplayName = "Kasun Perera",
                newPhone = "0779876543",
                newSpotifyEmail = "kasun.spotify@gmail.com",
                monthIdsToSync = listOf(monthId),
            ).getOrThrow()
        }

        step("friend uploadReceiptSlip") {
            paymentRepo.uploadReceiptSlip(
                monthId = monthId,
                memberKey = friendKey,
                memberEmail = friendEmail,
                mimeType = "image/jpeg",
                imageBase64 = "dGVzdF9yZWNlaXB0X2Jhc2U2NA==",
            ).getOrThrow()
        }

        val monthData = withTimeout(TIMEOUT_MS) {
            firestore.collection("payments").document(monthId).get().await().data
        }
        try {
            withTimeout(TIMEOUT_MS) {
                paymentRepo.syncListeningStats(
                    monthId = monthId,
                    memberKey = friendKey,
                    memberEmail = friendEmail,
                    displayName = "Kasun Perera",
                    spotifyDisplayName = "Kasun Spotify",
                    minutesListened = 240L,
                    tracksCounted = 60L,
                    topArtist = "Coldplay",
                    topTrack = "Yellow",
                    allowedEmails = listOf(friendEmail, AtaPotifyConstants.ADMIN_EMAIL),
                ).getOrThrow()
            }
        } catch (e: Throwable) {
            throw AssertionError(
                "RuleTest failed at step [friend syncListeningStats]: monthId=$monthId friendEmail=$friendEmail monthData=$monthData err=${e.message}",
                e
            )
        }

        // Admin confirms payment
        signInTestUser(AtaPotifyConstants.ADMIN_EMAIL)
        step("admin confirmPayment") {
            paymentRepo.confirmPayment(monthId, friendKey).getOrThrow()
        }

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.status == AtaPotifyConstants.STATUS_PAID } }
        }
        assertEquals("Kasun Perera", entries.first { it.memberKey == friendKey }.displayName)

        // Admin can also sync own Spotify listening stats
        step("admin syncListeningStats") {
            paymentRepo.syncListeningStats(
                monthId = monthId,
                memberKey = AtaPotifyConstants.memberKeyForEmail(AtaPotifyConstants.ADMIN_EMAIL),
                memberEmail = AtaPotifyConstants.ADMIN_EMAIL,
                displayName = "Thathsara",
                spotifyDisplayName = "Thathsara Spotify",
                minutesListened = 310L,
                tracksCounted = 72L,
                topArtist = "Daft Punk",
                topTrack = "Get Lucky",
                allowedEmails = listOf(friendEmail, AtaPotifyConstants.ADMIN_EMAIL),
            ).getOrThrow()
        }

        // Admin rewinds Paid back to Verifying (clears confirmedAt)
        step("admin rewindPayment") {
            paymentRepo.rewindPayment(
                monthId = monthId,
                memberKey = friendKey,
                targetStatus = AtaPotifyConstants.STATUS_VERIFYING,
            ).getOrThrow()
        }

        val rewoundEntry = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.status == AtaPotifyConstants.STATUS_VERIFYING } }
                .first { it.memberKey == friendKey }
        }
        assertEquals(null, rewoundEntry.confirmedAt)

        // Admin edits payment log (custom amount, name, clear receipt, back to pending)
        step("admin editMemberPayment") {
            paymentRepo.editMemberPayment(
                monthId = monthId,
                member = rewoundEntry,
                newDisplayName = "Kasun P.",
                newStatus = AtaPotifyConstants.STATUS_PENDING,
                newAmountLkr = 500L,
                clearReceipt = true,
                resetReminders = false,
            ).getOrThrow()
        }

        val editedEntry = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.amountLkr == 500L } }
                .first { it.memberKey == friendKey }
        }
        assertEquals("Kasun P.", editedEntry.displayName)
        assertEquals(false, editedEntry.hasReceipt)
        assertEquals(AtaPotifyConstants.STATUS_PENDING, editedEntry.status)

        // Admin removes pending entry from month, then adds a member, then deletes the test month
        step("admin deleteMemberPayment") {
            paymentRepo.deleteMemberPayment(
                monthId = monthId,
                member = editedEntry,
                allMonthMembers = listOf(editedEntry),
            ).getOrThrow()
        }

        val monthSnap = withTimeout(TIMEOUT_MS) {
            paymentRepo.observePaymentMonths(AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.monthId == monthId } }
                .first { it.monthId == monthId }
        }

        step("admin addMemberToMonth") {
            paymentRepo.addMemberToMonth(
                month = monthSnap,
                existingMembers = emptyList(),
                email = friendEmail,
                displayName = "Kasun Readded",
                amountLkr = 480L,
            ).getOrThrow()
        }

        val monthMembers = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey } }
        }
        val monthStats = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthStats(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true).first()
        }

        step("admin deleteBillingMonth") {
            paymentRepo.deleteBillingMonth(
                monthId = monthId,
                memberKeys = monthMembers.map { it.memberKey },
                statKeys = monthStats.map { it.memberKey },
            ).getOrThrow()
        }
    }

    @Test
    fun newUserEntersAwaitingApprovalModeAndAdminCanApprove() = runBlocking {
        val strangerEmail = "stranger_${UUID.randomUUID().toString().take(6)}@gmail.com"
        signInTestUser(strangerEmail)
        val authRepo = AuthRepository(firestore, auth)

        val resolution = step("new user resolveAccessAndSyncProfile") {
            authRepo.resolveAccessAndSyncProfile(auth.currentUser!!).getOrThrow()
        }
        assertTrue(resolution is AccessResolution.AwaitingApproval)

        // Waiting user can attach WhatsApp & Spotify email for the Admin
        step("new user updatePendingAccessRequestDetails") {
            authRepo.updatePendingAccessRequestDetails(
                displayName = "New Friend",
                phone = "0775551234",
                spotifyEmail = "newfriend.spotify@gmail.com",
            ).getOrThrow()
        }

        // Unapproved user still cannot read payment months
        val paymentRepo = PaymentRepository(firestore, auth)
        val error = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                paymentRepo.observePaymentMonths(strangerEmail, isAdmin = true).first()
            }
        }.exceptionOrNull()

        val firestoreException = checkNotNull(error?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $error"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)

        // Admin observes the pending request and approves it
        signInTestUser(AtaPotifyConstants.ADMIN_EMAIL)
        val pendingReq = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observePendingAccessRequests()
                .first { list -> list.any { it.email == strangerEmail } }
                .first { it.email == strangerEmail }
        }
        assertEquals("New Friend", pendingReq.displayName)
        assertEquals("newfriend.spotify@gmail.com", pendingReq.spotifyEmail)

        step("admin approveAccessRequest") {
            paymentRepo.approveAccessRequest(
                request = pendingReq,
                allCurrentWhitelist = emptyList(),
                activeMonth = null,
                activeMonthMembers = emptyList(),
                familyGroup = AtaPotifyConstants.GROUP_2,
            ).getOrThrow()
        }

        // Stranger now unlocks automatically via whitelist approval observer & resolveAccess
        signInTestUser(strangerEmail)
        val approvedSignal = withTimeout(FLOW_TIMEOUT_MS) {
            authRepo.observeOwnWhitelistApproval(strangerEmail).first { it }
        }
        assertTrue(approvedSignal)

        val unlockedResolution = step("approved user resolveAccessAndSyncProfile") {
            authRepo.resolveAccessAndSyncProfile(auth.currentUser!!).getOrThrow()
        }
        assertTrue(unlockedResolution is AccessResolution.Authorized && !unlockedResolution.isAdmin)
        assertEquals(
            AtaPotifyConstants.GROUP_2,
            (unlockedResolution as AccessResolution.Authorized).familyGroup,
        )
    }

    @Test
    fun adminCanCreateCustomizeTransferAndDeleteFamilyGroups() = runBlocking {
        signInTestUser(AtaPotifyConstants.ADMIN_EMAIL)
        val authRepo = AuthRepository(firestore, auth)
        val paymentRepo = PaymentRepository(firestore, auth)

        step("admin resolveAccess") {
            authRepo.resolveAccessAndSyncProfile(auth.currentUser!!).getOrThrow()
        }
        step("admin seed default family groups") {
            paymentRepo.ensureDefaultFamilyGroupsIfAdmin(450L).getOrThrow()
        }

        val initialGroups = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeFamilyGroups().first { it.size >= 2 }
        }
        assertTrue(initialGroups.any { it.effectiveId == AtaPotifyConstants.GROUP_1 })
        assertTrue(initialGroups.any { it.effectiveId == AtaPotifyConstants.GROUP_2 })

        val customGroupId = step("admin create custom family group") {
            paymentRepo.saveFamilyGroup(
                groupId = null,
                name = "Uni Squad",
                startDate = "2026-11-15",
                amountLkr = 520L,
                colorHex = "#A855F7",
                maxSlots = 6L,
                existingGroups = initialGroups,
                alsoUpdateActiveMonthMembers = false,
                activeMonth = null,
                activeMonthMembers = emptyList(),
            ).getOrThrow()
        }

        val groupsWithCustom = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeFamilyGroups().first { list -> list.any { it.effectiveId == customGroupId } }
        }
        val customGroup = groupsWithCustom.first { it.effectiveId == customGroupId }
        assertEquals("Uni Squad", customGroup.name)
        assertEquals("2026-11-15", customGroup.startDate)
        assertEquals(520L, customGroup.amountLkr)
        assertEquals("#A855F7", customGroup.colorHex)

        val friendEmail = "squad_${UUID.randomUUID().toString().take(6)}@gmail.com"
        val friendKey = AtaPotifyConstants.memberKeyForEmail(friendEmail)
        val monthId = uniqueMonthId()

        step("admin add member into custom group") {
            paymentRepo.upsertWhitelistMember(
                email = friendEmail,
                displayName = "Kavindu",
                phone = "94771112233",
                active = true,
                allCurrentWhitelist = emptyList(),
                activeMonth = null,
                activeMonthMembers = emptyList(),
                familyGroup = customGroupId,
                groupAmountLkr = 520L,
            ).getOrThrow()
        }

        val whitelist = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeWhitelist().first { list -> list.any { it.memberKey == friendKey } }
        }
        val wlMember = whitelist.first { it.memberKey == friendKey }
        assertEquals(customGroupId, wlMember.normalizedGroup)

        step("admin startNewMonth with per-group pricing") {
            paymentRepo.startNewMonth(
                monthId = monthId,
                displayLabel = "Custom Group Month",
                amountLkr = 450L,
                activeMembers = listOf(wlMember),
                familyGroups = groupsWithCustom,
            ).getOrThrow()
        }

        val activeMonth = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observePaymentMonths(AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.monthId == monthId } }
                .first { it.monthId == monthId }
        }
        val initialMonthMembers = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey } }
        }
        assertEquals(520L, initialMonthMembers.first { it.memberKey == friendKey }.amountLkr)

        step("admin update group price split & color") {
            paymentRepo.saveFamilyGroup(
                groupId = customGroupId,
                name = "Uni Squad",
                startDate = "2026-11-18",
                amountLkr = 550L,
                colorHex = "#FB7185",
                maxSlots = 6L,
                existingGroups = groupsWithCustom,
                alsoUpdateActiveMonthMembers = true,
                activeMonth = activeMonth,
                activeMonthMembers = initialMonthMembers,
            ).getOrThrow()
        }

        val updatedMonthMembers = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.amountLkr == 550L } }
        }
        assertEquals(550L, updatedMonthMembers.first { it.memberKey == friendKey }.amountLkr)

        step("admin transfer member to group_1") {
            paymentRepo.transferMemberToGroup(
                memberKey = friendKey,
                targetGroupId = AtaPotifyConstants.GROUP_1,
                targetGroupAmountLkr = 450L,
                updateActiveMonthAmount = true,
                activeMonth = activeMonth,
                activeMonthMembers = updatedMonthMembers,
            ).getOrThrow()
        }

        val afterTransferWl = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeWhitelist().first { list ->
                list.any { it.memberKey == friendKey && it.normalizedGroup == AtaPotifyConstants.GROUP_1 }
            }
        }
        assertEquals(AtaPotifyConstants.GROUP_1, afterTransferWl.first { it.memberKey == friendKey }.normalizedGroup)

        val monthMembersAfterTransfer = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.normalizedGroup == AtaPotifyConstants.GROUP_1 } }
        }
        step("admin move member back before deletion test") {
            paymentRepo.transferMemberToGroup(
                memberKey = friendKey,
                targetGroupId = customGroupId,
                targetGroupAmountLkr = 550L,
                updateActiveMonthAmount = false,
                activeMonth = activeMonth,
                activeMonthMembers = monthMembersAfterTransfer,
            ).getOrThrow()
        }
        val wlBeforeDelete = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeWhitelist().first { list ->
                list.any { it.memberKey == friendKey && it.normalizedGroup == customGroupId }
            }
        }
        val membersBeforeDelete = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.normalizedGroup == customGroupId } }
        }

        step("admin delete custom group with reassignment to group_2") {
            paymentRepo.deleteFamilyGroup(
                groupId = customGroupId,
                reassignToGroupId = AtaPotifyConstants.GROUP_2,
                whitelist = wlBeforeDelete,
                activeMonth = activeMonth,
                activeMonthMembers = membersBeforeDelete,
            ).getOrThrow()
        }

        val wlAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeWhitelist().first { list ->
                list.any { it.memberKey == friendKey && it.normalizedGroup == AtaPotifyConstants.GROUP_2 }
            }
        }
        assertEquals(AtaPotifyConstants.GROUP_2, wlAfterDelete.first { it.memberKey == friendKey }.normalizedGroup)

        val groupsAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeFamilyGroups().first { list -> list.none { it.effectiveId == customGroupId } }
        }
        assertTrue(groupsAfterDelete.none { it.effectiveId == customGroupId })

        step("cleanup test month") {
            paymentRepo.deleteBillingMonth(
                monthId = monthId,
                memberKeys = listOf(friendKey),
                statKeys = emptyList(),
            ).getOrThrow()
        }
    }

    @Test
    fun unauthenticatedCallerIsRejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val paymentRepo = PaymentRepository(firestore, auth)
        val error = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                paymentRepo.observeWhitelist().first()
            }
        }.exceptionOrNull()

        val firestoreException = checkNotNull(error?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $error"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun adminCanRecordPartialPaymentsMarkCashPaidAndRewind() = runBlocking {
        signInTestUser(AtaPotifyConstants.ADMIN_EMAIL)
        val authRepo = AuthRepository(firestore, auth)
        val paymentRepo = PaymentRepository(firestore, auth)

        step("admin resolveAccess") {
            authRepo.resolveAccessAndSyncProfile(auth.currentUser!!).getOrThrow()
        }

        val friendEmail = "cash_partial_${UUID.randomUUID().toString().take(6)}@gmail.com"
        val friendKey = AtaPotifyConstants.memberKeyForEmail(friendEmail)
        val monthId = uniqueMonthId()

        step("whitelist friend") {
            paymentRepo.upsertWhitelistMember(
                email = friendEmail,
                displayName = "Nimal",
                phone = "94772223344",
                active = true,
                allCurrentWhitelist = emptyList(),
                activeMonth = null,
                activeMonthMembers = emptyList(),
            ).getOrThrow()
        }
        val wlMember = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeWhitelist().first { list -> list.any { it.memberKey == friendKey } }
                .first { it.memberKey == friendKey }
        }

        step("start month") {
            paymentRepo.startNewMonth(
                monthId = monthId,
                displayLabel = "Partial & Cash Test Month",
                amountLkr = 450L,
                activeMembers = listOf(wlMember),
            ).getOrThrow()
        }

        val initialMember = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey } }
                .first { it.memberKey == friendKey }
        }

        // First partial payment: 200 LKR in cash -> still pending with paidAmountLkr = 200
        val settledFirst = step("record first partial payment (200 cash)") {
            paymentRepo.recordPartialPayment(
                monthId = monthId,
                member = initialMember,
                additionalAmountLkr = 200L,
                isCash = true,
            ).getOrThrow()
        }
        assertEquals(false, settledFirst)

        val afterFirstPartial = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.paidAmountLkr == 200L } }
                .first { it.memberKey == friendKey }
        }
        assertEquals(AtaPotifyConstants.STATUS_PENDING, afterFirstPartial.status)
        assertEquals(200L, afterFirstPartial.effectivePaidLkr)
        assertEquals(250L, afterFirstPartial.remainingLkr)
        assertTrue(afterFirstPartial.hasPartialPayment)
        assertTrue(afterFirstPartial.isCashPayment)

        // Second partial payment: remaining 250 LKR -> settles to paid
        val settledSecond = step("record second partial payment (250 cash)") {
            paymentRepo.recordPartialPayment(
                monthId = monthId,
                member = afterFirstPartial,
                additionalAmountLkr = 250L,
                isCash = true,
            ).getOrThrow()
        }
        assertEquals(true, settledSecond)

        val afterSettled = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.status == AtaPotifyConstants.STATUS_PAID } }
                .first { it.memberKey == friendKey }
        }
        assertEquals(450L, afterSettled.effectivePaidLkr)
        assertEquals(0L, afterSettled.remainingLkr)
        assertTrue(afterSettled.isCashPayment)

        // Rewind clears paidAmountLkr and paymentMethod back to 0 / empty
        step("rewind paid back to pending") {
            paymentRepo.rewindPayment(monthId, friendKey, AtaPotifyConstants.STATUS_PENDING).getOrThrow()
        }
        val afterRewind = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.status == AtaPotifyConstants.STATUS_PENDING } }
                .first { it.memberKey == friendKey }
        }
        assertEquals(0L, afterRewind.paidAmountLkr)
        assertEquals("", afterRewind.paymentMethod)

        // Direct Cash in Hand (Mark Paid) sets status = paid, paidAmountLkr = 450, paymentMethod = cash
        step("markPaidInCash") {
            paymentRepo.markPaidInCash(monthId, afterRewind).getOrThrow()
        }
        val afterCashPaid = withTimeout(FLOW_TIMEOUT_MS) {
            paymentRepo.observeMonthMembers(monthId, AtaPotifyConstants.ADMIN_EMAIL, isAdmin = true)
                .first { list -> list.any { it.memberKey == friendKey && it.status == AtaPotifyConstants.STATUS_PAID } }
                .first { it.memberKey == friendKey }
        }
        assertEquals(450L, afterCashPaid.paidAmountLkr)
        assertTrue(afterCashPaid.isCashPayment)

        step("cleanup partial/cash test month") {
            paymentRepo.deleteBillingMonth(
                monthId = monthId,
                memberKeys = listOf(friendKey),
                statKeys = emptyList(),
            ).getOrThrow()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
