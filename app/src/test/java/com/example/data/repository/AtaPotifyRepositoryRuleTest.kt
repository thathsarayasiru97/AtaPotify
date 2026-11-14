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
    fun unauthorizedUserIsDeniedAndCannotReadMonths() = runBlocking {
        val strangerEmail = "stranger_${UUID.randomUUID().toString().take(6)}@gmail.com"
        signInTestUser(strangerEmail)
        val authRepo = AuthRepository(firestore, auth)

        val resolution = withTimeout(TIMEOUT_MS) {
            authRepo.resolveAccessAndSyncProfile(auth.currentUser!!).getOrThrow()
        }
        assertTrue(resolution is AccessResolution.Denied)

        // Unfiltered query without Admin fails with PERMISSION_DENIED
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

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
