package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlyBillingSummaryTest {

    @Test
    fun calculatesCollectedPendingAndNetBalanceAcrossMixedPaymentLogs() {
        val members = listOf(
            MemberPaymentStatus(
                memberKey = "m1",
                memberEmail = "alice@gmail.com",
                displayName = "Alice",
                status = AtaPotifyConstants.STATUS_PAID,
                amountLkr = 450L,
            ),
            MemberPaymentStatus(
                memberKey = "m2",
                memberEmail = "bob@gmail.com",
                displayName = "Bob",
                status = AtaPotifyConstants.STATUS_PAID,
                amountLkr = 500L,
            ),
            MemberPaymentStatus(
                memberKey = "m3",
                memberEmail = "charlie@gmail.com",
                displayName = "Charlie",
                status = AtaPotifyConstants.STATUS_VERIFYING,
                amountLkr = 450L,
            ),
            MemberPaymentStatus(
                memberKey = "m4",
                memberEmail = "dave@gmail.com",
                displayName = "Dave",
                status = AtaPotifyConstants.STATUS_PENDING,
                amountLkr = 450L,
            ),
        )

        val summary = calculateMonthlyBillingSummary(members)

        assertEquals(1850L, summary.expectedTotalLkr)
        assertEquals(950L, summary.collectedLkr)
        assertEquals(450L, summary.verifyingLkr)
        assertEquals(450L, summary.pendingOnlyLkr)
        assertEquals(900L, summary.totalPendingLkr)
        assertEquals(-900L, summary.netBalanceLkr)
        assertEquals(2, summary.paidCount)
        assertEquals(1, summary.verifyingCount)
        assertEquals(1, summary.pendingCount)
        assertEquals(4, summary.totalCount)
        assertEquals(950f / 1850f, summary.collectionFraction, 0.0001f)
    }

    @Test
    fun calculatesSettledNetBalanceOfZeroWhenAllMembersPaid() {
        val members = listOf(
            MemberPaymentStatus(
                memberKey = "m1",
                status = AtaPotifyConstants.STATUS_PAID,
                amountLkr = 450L,
            ),
            MemberPaymentStatus(
                memberKey = "m2",
                status = AtaPotifyConstants.STATUS_PAID,
                amountLkr = 450L,
            ),
        )

        val summary = calculateMonthlyBillingSummary(members)

        assertEquals(900L, summary.expectedTotalLkr)
        assertEquals(900L, summary.collectedLkr)
        assertEquals(0L, summary.totalPendingLkr)
        assertEquals(0L, summary.netBalanceLkr)
        assertEquals(1.0f, summary.collectionFraction, 0.0001f)
    }

    @Test
    fun handlesEmptyMemberListGracefully() {
        val summary = calculateMonthlyBillingSummary(emptyList())

        assertEquals(0L, summary.expectedTotalLkr)
        assertEquals(0L, summary.collectedLkr)
        assertEquals(0L, summary.totalPendingLkr)
        assertEquals(0L, summary.netBalanceLkr)
        assertEquals(0, summary.totalCount)
        assertEquals(0f, summary.collectionFraction, 0.0001f)
    }
}
