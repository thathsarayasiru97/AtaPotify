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

    @Test
    fun consecutiveMonthIdsRollsAcrossYearBoundary() {
        val ids = AtaPotifyConstants.consecutiveMonthIds("2026-11", 4)
        assertEquals(listOf("2026-11", "2026-12", "2027-01", "2027-02"), ids)
    }

    @Test
    fun calculatesAllTimeWrappedSummaryAndStandings() {
        val monthRates = mapOf("2026-10" to 450L, "2026-11" to 450L)
        val statsByMonth = mapOf(
            "2026-10" to listOf(
                ListeningStat(
                    memberKey = "alice",
                    displayName = "Alice",
                    minutesListened = 600L,
                    tracksCounted = 150L,
                    topArtist = "Daft Punk",
                    topTrack = "Get Lucky",
                ),
                ListeningStat(
                    memberKey = "bob",
                    displayName = "Bob",
                    minutesListened = 300L,
                    tracksCounted = 80L,
                    topArtist = "Coldplay",
                    topTrack = "Yellow",
                ),
            ),
            "2026-11" to listOf(
                ListeningStat(
                    memberKey = "alice",
                    displayName = "Alice",
                    minutesListened = 600L,
                    tracksCounted = 140L,
                    topArtist = "Daft Punk",
                    topTrack = "Instant Crush",
                ),
            ),
        )

        val wrapped = checkNotNull(calculateAllTimeWrappedSummary(statsByMonth, monthRates))
        assertEquals(2, wrapped.monthsTracked)
        assertEquals(1500L, wrapped.totalGroupMinutes)
        assertEquals(370L, wrapped.totalGroupTracks)
        assertEquals("Alice", wrapped.topListener?.displayName)
        assertEquals(1200L, wrapped.topListener?.totalMinutes)
        assertEquals("Daft Punk", wrapped.topListener?.favoriteArtist)
    }

    @Test
    fun calculatesConsecutivePaymentStreaksAndLongestStreakStanding() {
        val months = listOf(
            PaymentMonth(monthId = "2026-11", displayLabel = "November 2026"),
            PaymentMonth(monthId = "2026-10", displayLabel = "October 2026"),
            PaymentMonth(monthId = "2026-09", displayLabel = "September 2026"),
        )
        val membersByMonth = mapOf(
            "2026-09" to listOf(
                MemberPaymentStatus(memberKey = "alice", displayName = "Alice", status = AtaPotifyConstants.STATUS_PAID),
                MemberPaymentStatus(memberKey = "bob", displayName = "Bob", status = AtaPotifyConstants.STATUS_PAID),
            ),
            "2026-10" to listOf(
                MemberPaymentStatus(memberKey = "alice", displayName = "Alice", status = AtaPotifyConstants.STATUS_PAID),
                MemberPaymentStatus(memberKey = "bob", displayName = "Bob", status = AtaPotifyConstants.STATUS_PENDING),
            ),
            "2026-11" to listOf(
                MemberPaymentStatus(memberKey = "alice", displayName = "Alice", status = AtaPotifyConstants.STATUS_PENDING),
                MemberPaymentStatus(memberKey = "bob", displayName = "Bob", status = AtaPotifyConstants.STATUS_PAID),
            ),
        )

        val streaks = calculatePaymentStreaks(months, membersByMonth)
        // Alice paid 2026-10 and 2026-09, and 2026-11 is still pending (doesn't break her prior streak)
        assertEquals(2, streaks["alice"])
        // Bob paid 2026-11, but missed 2026-10, so streak stops at 1
        assertEquals(1, streaks["bob"])

        val longest = checkNotNull(calculateLongestStreakStanding(months, membersByMonth))
        assertEquals("Alice", longest.displayName)
        assertEquals(2, longest.streakMonths)
    }

    @Test
    fun buildsDigitalVerifiedPaymentReceiptTextWithRefAndStreak() {
        val month = PaymentMonth(monthId = "2026-11", displayLabel = "November 2026", amountLkr = 450L)
        val member = MemberPaymentStatus(
            memberKey = "alice_key",
            memberEmail = "alice@gmail.com",
            displayName = "Alice",
            status = AtaPotifyConstants.STATUS_PAID,
            amountLkr = 450L,
            hasReceipt = true,
        )

        val receiptText = buildVerifiedPaymentReceiptText(month = month, member = member, streakCount = 3)
        org.junit.Assert.assertTrue(receiptText.contains("ATAPOTIFY VERIFIED RECEIPT"))
        org.junit.Assert.assertTrue(receiptText.contains("#ATP-202611-ALIC"))
        org.junit.Assert.assertTrue(receiptText.contains("LKR 450"))
        org.junit.Assert.assertTrue(receiptText.contains("PAID & SETTLED"))
        org.junit.Assert.assertTrue(receiptText.contains("3 consecutive months"))
    }
}

