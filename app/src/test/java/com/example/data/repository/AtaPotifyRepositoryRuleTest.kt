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
        val num = (10..99).random()
        return "2099-$num"
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

        step("friend updateOwnProfile") {
            authRepo.updateOwnProfile(
                emailLower = friendEmail,
                isAdmin = false,
                newDisplayName = "Kasun Perera",
                newPhone = "0779876543",
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
                    allowedEmails = listOf(friendEmail),
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
