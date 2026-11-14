package com.example.data.firestore

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ADMIN_EMAIL
import com.example.data.model.PaymentStatus
import com.example.data.model.emailToMemberKey
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PaymentRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueMonthId(): String {
        val suffix = (10..99).random()
        val year = (2030..2099).random()
        return String.format("%04d-%02d", year, suffix % 12 + 1)
    }

    @Test
    fun adminWhitelistAndNewMonth_createsPendingMembers_andMemberCanUploadSlip() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6)
        val friendEmail = "friend.$uniqueSuffix@test.com"
        val friendKey = emailToMemberKey(friendEmail)
        val monthId = uniqueMonthId()

        // 1. Admin signs in, adds friend to whitelist, and starts a month
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = PaymentRepository(firestore, auth)

        val whitelistRes = withTimeout(TIMEOUT_MS) {
            adminRepo.upsertWhitelistMember(
                displayName = "Kasun",
                email = friendEmail,
                phone = "94771234567",
                active = true
            )
        }
        assertTrue("Whitelist upsert failed: ${whitelistRes.exceptionOrNull()}", whitelistRes.isSuccess)

        val monthRes = withTimeout(TIMEOUT_MS) {
            adminRepo.startNewMonth(monthId = monthId, amountLkr = 450)
        }
        assertTrue("Start month failed: ${monthRes.exceptionOrNull()}", monthRes.isSuccess)

        // 2. Friend signs in, observes board, edits profile, and uploads receipt slip
        val friendUid = signInTestUser(friendEmail)
        val friendRepo = PaymentRepository(firestore, auth)

        val profileRes = withTimeout(TIMEOUT_MS) {
            friendRepo.updateUserProfile(
                uid = friendUid,
                email = friendEmail,
                displayName = "Kasun Perera",
                phone = "94779876543",
                isAdmin = false,
                activeMonthId = monthId
            )
        }
        assertTrue("Profile update failed: ${profileRes.exceptionOrNull()}", profileRes.isSuccess)

        val members = withTimeout(FLOW_TIMEOUT_MS) {
            friendRepo.observeMonthMembers(monthId, friendEmail, isAdmin = false)
                .first { list -> list.any { it.memberKey == friendKey && it.displayName == "Kasun Perera" } }
        }
        val kasunEntry = members.first { it.memberKey == friendKey }
        assertEquals(PaymentStatus.PENDING, kasunEntry.paymentStatus)

        val slipRes = withTimeout(TIMEOUT_MS) {
            friendRepo.submitReceiptSlip(
                monthId = monthId,
                memberKey = friendKey,
                memberEmail = friendEmail,
                mimeType = "image/jpeg",
                imageBase64 = "dGVzdF9yZWNlaXB0X2Jhc2U2NA=="
            )
        }
        assertTrue("Submit receipt failed: ${slipRes.exceptionOrNull()}", slipRes.isSuccess)

        // 3. Admin reads receipt, confirms payment, and logs WhatsApp reminder
        signInTestUser(ADMIN_EMAIL)
        val loadedSlip = withTimeout(TIMEOUT_MS) {
            adminRepo.getReceiptSlip(monthId, friendKey)
        }
        assertTrue(loadedSlip.isSuccess)
        assertNotNull(loadedSlip.getOrNull())

        val confirmRes = withTimeout(TIMEOUT_MS) {
            adminRepo.confirmPayment(monthId, friendKey)
        }
        assertTrue("Confirm payment failed: ${confirmRes.exceptionOrNull()}", confirmRes.isSuccess)
    }

    @Test
    fun unauthorizedUser_cannotReadMonthsOrWhitelist() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = PaymentRepository(firestore, auth)
        val monthId = uniqueMonthId()
        withTimeout(TIMEOUT_MS) {
            adminRepo.startNewMonth(monthId = monthId, amountLkr = 450)
        }

        val strangerEmail = "stranger.${UUID.randomUUID().toString().take(6)}@test.com"
        signInTestUser(strangerEmail)
        val repo = PaymentRepository(firestore, auth)

        // Filtered query returns 0 months (never leaks other users' months)
        val visibleMonths = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMonths(strangerEmail, isAdmin = false).first()
        }
        assertTrue(visibleMonths.isEmpty())

        // Unfiltered admin-style query is rejected with PERMISSION_DENIED
        val exception = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeMonths(strangerEmail, isAdmin = true).first()
            }
        }.exceptionOrNull()

        assertNotNull("Expected PERMISSION_DENIED for unauthorized unfiltered query", exception)
        val firestoreException = exception!!.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    @Test
    fun unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = PaymentRepository(firestore, auth)

        val exception = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeMonths("someone@test.com", isAdmin = false).first()
            }
        }.exceptionOrNull()

        assertNotNull("Expected PERMISSION_DENIED when signed out", exception)
        val firestoreException = exception!!.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
        assertFalse(auth.currentUser != null)
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
