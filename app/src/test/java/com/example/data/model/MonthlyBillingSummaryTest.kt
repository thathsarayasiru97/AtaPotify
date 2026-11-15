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

    @Test
    fun planConfigResolvesMultiplePinnedAnnouncementsAndLegacyFallback() {
        val multiConfig = PlanConfig(
            pinnedAnnouncement = "Slide 1",
            pinnedAnnouncements = listOf("Slide 1", "  Slide 2  ", "", "Slide 3"),
        )
        assertEquals(listOf("Slide 1", "Slide 2", "Slide 3"), multiConfig.effectiveAnnouncements)
        org.junit.Assert.assertTrue(multiConfig.hasPinnedAnnouncement)

        val legacyConfig = PlanConfig(
            pinnedAnnouncement = "Legacy single message",
            pinnedAnnouncements = emptyList(),
        )
        assertEquals(listOf("Legacy single message"), legacyConfig.effectiveAnnouncements)
        org.junit.Assert.assertTrue(legacyConfig.hasPinnedAnnouncement)
    }

    @Test
    fun whatsappSummaryIncludesFamilyGroupHeaderAndBadges() {
        val month = PaymentMonth(monthId = "2026-11", displayLabel = "November 2026", amountLkr = 450L)
        val group2Members = listOf(
            MemberPaymentStatus(
                memberKey = "nadun",
                displayName = "Nadun",
                status = AtaPotifyConstants.STATUS_PAID,
                amountLkr = 450L,
                familyGroup = AtaPotifyConstants.GROUP_2,
            ),
            MemberPaymentStatus(
                memberKey = "sachin",
                displayName = "Sachin",
                status = AtaPotifyConstants.STATUS_PENDING,
                amountLkr = 450L,
                familyGroup = AtaPotifyConstants.GROUP_2,
            ),
        )

        val summaryText = buildWhatsAppMonthlySummaryText(
            month = month,
            members = group2Members,
            stats = emptyList(),
            planConfig = PlanConfig(),
            groupFilter = AtaPotifyConstants.GROUP_2,
        )
        org.junit.Assert.assertTrue(summaryText.contains("November 2026 (Family 2)"))
        org.junit.Assert.assertTrue(summaryText.contains("Nadun"))
        org.junit.Assert.assertTrue(summaryText.contains("Sachin"))
    }

    @Test
    fun familyGroupHelpersGenerateUniqueIdsAndFormatRenewalCadence() {
        val existing = setOf("group_1", "group_2", "grp_uni_squad")
        assertEquals("grp_office_crew", AtaPotifyConstants.generateGroupId("Office Crew!", existing))
        assertEquals("grp_uni_squad_2", AtaPotifyConstants.generateGroupId("Uni Squad", existing))

        assertEquals("Renews 1st monthly", AtaPotifyConstants.billingDaySummary("2026-11-01"))
        assertEquals("Renews 2nd monthly", AtaPotifyConstants.billingDaySummary("2026-11-02"))
        assertEquals("Renews 3rd monthly", AtaPotifyConstants.billingDaySummary("2026-11-03"))
        assertEquals("Renews 11th monthly", AtaPotifyConstants.billingDaySummary("2026-11-11"))
        assertEquals("Renews 22nd monthly", AtaPotifyConstants.billingDaySummary("2026-11-22"))
        assertEquals("#A855F7", AtaPotifyConstants.normalizeHexColor(" #a855f7 "))
    }

    @Test
    fun resolveEffectiveFamilyGroupsSupportsDefaultsAndCustomGroups() {
        val customGroups = listOf(
            FamilyGroup(
                groupId = "grp_uni_squad",
                name = "Uni Squad",
                startDate = "2026-11-15",
                amountLkr = 520L,
                colorHex = "#A855F7",
                maxSlots = 6L,
            ),
        )
        val resolvedDefaults = resolveEffectiveFamilyGroups(
            rawGroups = emptyList(),
            defaultAmountLkr = 450L,
            whitelist = listOf(WhitelistMember(memberKey = "u1", familyGroup = "grp_uni_squad")),
        )
        assertEquals(3, resolvedDefaults.size)
        assertEquals("Family 1", resolvedDefaults[0].name)
        assertEquals("Family 2", resolvedDefaults[1].name)

        val resolvedWithCustom = resolveEffectiveFamilyGroups(
            rawGroups = customGroups,
            defaultAmountLkr = 450L,
            whitelist = listOf(WhitelistMember(memberKey = "u1", familyGroup = "grp_uni_squad")),
        )
        assertEquals(1, resolvedWithCustom.size)
        assertEquals("Uni Squad", resolvedWithCustom.first().name)

        val month = PaymentMonth(monthId = "2026-11", displayLabel = "November 2026", amountLkr = 450L)
        val summaryText = buildWhatsAppMonthlySummaryText(
            month = month,
            members = listOf(
                MemberPaymentStatus(
                    memberKey = "u1",
                    displayName = "Kavindu",
                    status = AtaPotifyConstants.STATUS_PAID,
                    amountLkr = 520L,
                    familyGroup = "grp_uni_squad",
                ),
            ),
            stats = emptyList(),
            planConfig = PlanConfig(),
            groupFilter = "grp_uni_squad",
            familyGroups = resolvedWithCustom,
        )
        org.junit.Assert.assertTrue(summaryText.contains("November 2026 (Uni Squad)"))
        org.junit.Assert.assertTrue(summaryText.contains("Kavindu"))
    }

    @Test
    fun partialAndCashPaymentsUpdateBillingSummaryReceiptsAndWhatsAppText() {
        val month = PaymentMonth(monthId = "2026-11", displayLabel = "November 2026", amountLkr = 450L)
        val members = listOf(
            MemberPaymentStatus(
                memberKey = "alice",
                displayName = "Alice",
                status = AtaPotifyConstants.STATUS_PAID,
                amountLkr = 450L,
                paidAmountLkr = 450L,
                paymentMethod = AtaPotifyConstants.PAYMENT_METHOD_CASH,
            ),
            MemberPaymentStatus(
                memberKey = "bob",
                displayName = "Bob",
                status = AtaPotifyConstants.STATUS_PENDING,
                amountLkr = 450L,
                paidAmountLkr = 200L,
                paymentMethod = AtaPotifyConstants.PAYMENT_METHOD_CASH,
            ),
            MemberPaymentStatus(
                memberKey = "charlie",
                displayName = "Charlie",
                status = AtaPotifyConstants.STATUS_VERIFYING,
                amountLkr = 450L,
                paidAmountLkr = 150L,
                paymentMethod = AtaPotifyConstants.PAYMENT_METHOD_BANK_SLIP,
            ),
        )

        val bob = members[1]
        assertEquals(200L, bob.effectivePaidLkr)
        assertEquals(250L, bob.remainingLkr)
        org.junit.Assert.assertTrue(bob.hasPartialPayment)
        org.junit.Assert.assertTrue(bob.isCashPayment)

        val summary = calculateMonthlyBillingSummary(members)
        assertEquals(1350L, summary.expectedTotalLkr)
        assertEquals(800L, summary.collectedLkr) // 450 + 200 + 150
        assertEquals(250L, summary.pendingOnlyLkr)
        assertEquals(300L, summary.verifyingLkr)
        assertEquals(550L, summary.totalPendingLkr)
        assertEquals(-550L, summary.netBalanceLkr)
        assertEquals(1, summary.paidCount)
        assertEquals(1, summary.verifyingCount)
        assertEquals(1, summary.pendingCount)

        val cashReceipt = buildVerifiedPaymentReceiptText(month = month, member = members[0], streakCount = 1)
        org.junit.Assert.assertTrue(cashReceipt.contains("Cash in Hand (Verified by Admin)"))

        val whatsappSummary = buildWhatsAppMonthlySummaryText(
            month = month,
            members = members,
            stats = emptyList(),
            planConfig = PlanConfig(),
        )
        org.junit.Assert.assertTrue(whatsappSummary.contains("[PAID • Cash]"))
        org.junit.Assert.assertTrue(whatsappSummary.contains("[PARTIAL] (LKR 200/450 paid • LKR 250 left)"))
    }

    @Test
    fun familyGroupHasMemberVisibleInfoReflectsConfiguredDetails() {
        val emptyGroup = FamilyGroup(groupId = "group_1", name = "Family 1")
        org.junit.Assert.assertFalse(emptyGroup.hasMemberVisibleInfo)

        org.junit.Assert.assertTrue(emptyGroup.copy(address = "123 Galle Rd, Colombo").hasMemberVisibleInfo)
        org.junit.Assert.assertTrue(emptyGroup.copy(managerName = "Thathsara").hasMemberVisibleInfo)
        org.junit.Assert.assertTrue(emptyGroup.copy(managerEmail = "manager@gmail.com").hasMemberVisibleInfo)
        org.junit.Assert.assertTrue(emptyGroup.copy(inviteLink = "https://spotify.com/family/join/abc").hasMemberVisibleInfo)
        org.junit.Assert.assertTrue(emptyGroup.copy(notes = "Enter the address above when joining").hasMemberVisibleInfo)
        org.junit.Assert.assertFalse(emptyGroup.copy(address = "   ", notes = "\n ").hasMemberVisibleInfo)
    }
}


