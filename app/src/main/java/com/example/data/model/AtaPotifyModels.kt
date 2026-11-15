package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AtaPotifyConstants {
    const val ADMIN_EMAIL = "thathsarayasiru97@gmail.com"
    const val DEFAULT_PLAN_NAME = "Spotify Family"
    const val DEFAULT_MONTHLY_AMOUNT_LKR = 450L
    const val CURRENCY_LKR = "LKR"
    const val OVERDUE_DAYS_THRESHOLD = 7L
    const val OVERDUE_MILLIS_THRESHOLD = OVERDUE_DAYS_THRESHOLD * 24L * 60L * 60L * 1000L

    const val ROLE_ADMIN = "admin"
    const val ROLE_MEMBER = "member"

    const val STATUS_PENDING = "pending"
    const val STATUS_VERIFYING = "verifying"
    const val STATUS_PAID = "paid"

    const val PAYMENT_METHOD_BANK_SLIP = "bank_slip"
    const val PAYMENT_METHOD_CASH = "cash"
    const val PAYMENT_METHOD_DIRECT = "direct"

    const val PLAN_CONFIG_DOC_ID = "settings"
    const val SLIP_DOC_ID = "slip"

    const val GROUP_ALL = "all"
    const val GROUP_1 = "group_1"
    const val GROUP_2 = "group_2"
    const val DEFAULT_GROUP_1_COLOR_HEX = "#1ED760"
    const val DEFAULT_GROUP_2_COLOR_HEX = "#38BDF8"
    const val MAX_SPOTIFY_FAMILY_SLOTS = 6
    const val MAX_PINNED_ANNOUNCEMENTS = 8

    private val VALID_GROUP_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

    fun normalizeFamilyGroup(raw: String?): String {
        val clean = raw?.trim().orEmpty()
        return if (clean.isNotBlank() && clean != GROUP_ALL && VALID_GROUP_ID_REGEX.matches(clean)) {
            clean
        } else {
            GROUP_1
        }
    }

    fun isValidHexColor(raw: String): Boolean =
        raw.trim().matches(Regex("^#[0-9A-Fa-f]{6}$"))

    fun normalizeHexColor(raw: String?, fallback: String = DEFAULT_GROUP_1_COLOR_HEX): String {
        val clean = raw?.trim().orEmpty()
        return if (isValidHexColor(clean)) clean.uppercase(Locale.US) else fallback
    }

    fun generateGroupId(name: String, existingIds: Set<String>): String {
        val slug = name.trim().lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(24)
            .ifBlank { "group" }
        val base = "grp_$slug"
        if (base !in existingIds) return base
        var idx = 2
        while ("${base}_$idx" in existingIds) idx++
        return "${base}_$idx"
    }

    fun familyGroupLabel(group: String, groups: List<FamilyGroup> = emptyList()): String {
        if (group == GROUP_ALL) return "All Groups"
        groups.firstOrNull { it.effectiveId == group }?.name?.takeIf { it.isNotBlank() }?.let { return it }
        return when (group) {
            GROUP_2 -> "Family 2"
            GROUP_1 -> "Family 1"
            else -> group.removePrefix("grp_")
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "Family Group" }
        }
    }

    fun familyGroupShortBadge(group: String, groups: List<FamilyGroup> = emptyList()): String {
        val full = familyGroupLabel(group, groups)
        return when {
            group == GROUP_1 && full == "Family 1" -> "Fam 1"
            group == GROUP_2 && full == "Family 2" -> "Fam 2"
            full.startsWith("Family ", ignoreCase = true) && full.length <= 10 ->
                "Fam " + full.substringAfter("Family ").trim()
            full.length <= 11 -> full
            else -> full.take(10).trimEnd() + "…"
        }
    }

    fun familyGroupColorHex(groupId: String, groups: List<FamilyGroup> = emptyList()): String {
        groups.firstOrNull { it.effectiveId == groupId }?.colorHex?.takeIf { isValidHexColor(it) }?.let { return it }
        return if (groupId == GROUP_2) DEFAULT_GROUP_2_COLOR_HEX else DEFAULT_GROUP_1_COLOR_HEX
    }

    fun formatStartDateDisplay(startDateIso: String): String {
        val clean = startDateIso.trim()
        if (clean.isBlank()) return "Not set"
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = parser.parse(clean) ?: return clean
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        }.getOrDefault(clean)
    }

    fun billingDaySummary(startDateIso: String): String {
        val clean = startDateIso.trim()
        if (!clean.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) return ""
        val day = clean.substring(8, 10).toIntOrNull() ?: return ""
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "Renews ${day}$suffix monthly"
    }

    fun todayIsoDate(date: Date = Date()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.US)

    fun memberKeyForEmail(email: String): String {
        val normalized = normalizeEmail(email)
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    fun currentMonthId(date: Date = Date()): String {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        return fmt.format(date)
    }

    fun monthLabelFromId(monthId: String): String {
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
            val parsed = parser.parse(monthId)
            if (parsed != null) formatter.format(parsed) else monthId
        }.getOrDefault(monthId)
    }

    fun nextSuggestedMonthId(existingMonthIds: List<String>): String {
        val current = currentMonthId()
        if (current !in existingMonthIds) return current
        val latest = existingMonthIds.maxOrNull() ?: return current
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val cal = Calendar.getInstance()
            cal.time = parser.parse(latest) ?: Date()
            cal.add(Calendar.MONTH, 1)
            parser.format(cal.time)
        }.getOrDefault(current)
    }

    fun consecutiveMonthIds(startMonthId: String, count: Int): List<String> {
        val safeCount = count.coerceIn(1, 12)
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val startDate = parser.parse(startMonthId) ?: return listOf(startMonthId)
            val cal = Calendar.getInstance()
            buildList {
                for (offset in 0 until safeCount) {
                    cal.time = startDate
                    cal.add(Calendar.MONTH, offset)
                    add(parser.format(cal.time))
                }
            }
        }.getOrDefault(listOf(startMonthId))
    }

    fun sanitizeWhatsAppDigits(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("0") && digits.length == 10 -> "94" + digits.drop(1)
            else -> digits
        }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AtaPotifyConstants.ROLE_MEMBER,
    val phone: String = "",
    val spotifyEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PlanConfig(
    @DocumentId val id: String = "",
    val planName: String = AtaPotifyConstants.DEFAULT_PLAN_NAME,
    val monthlyAmountLkr: Long = AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
    val currency: String = AtaPotifyConstants.CURRENCY_LKR,
    val allowedEmails: List<String> = emptyList(),
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankAccountHolder: String = "",
    val bankBranch: String = "",
    val pinnedAnnouncement: String = "",
    val pinnedAnnouncements: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val hasBankDetails: Boolean
        get() = bankAccountNumber.isNotBlank() || bankName.isNotBlank()

    val effectiveAnnouncements: List<String>
        get() = pinnedAnnouncements
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty {
                listOfNotNull(pinnedAnnouncement.trim().takeIf { it.isNotEmpty() })
            }

    val hasPinnedAnnouncement: Boolean
        get() = effectiveAnnouncements.isNotEmpty()

    fun formattedBankCopyText(): String = buildString {
        if (bankName.isNotBlank()) appendLine("Bank: $bankName")
        if (bankBranch.isNotBlank()) appendLine("Branch: $bankBranch")
        if (bankAccountNumber.isNotBlank()) appendLine("Account No: $bankAccountNumber")
        if (bankAccountHolder.isNotBlank()) appendLine("Account Name: $bankAccountHolder")
    }.trim()
}

@IgnoreExtraProperties
data class WhitelistMember(
    @DocumentId val id: String = "",
    val memberKey: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String = "",
    val spotifyEmail: String = "",
    val role: String = AtaPotifyConstants.ROLE_MEMBER,
    val active: Boolean = true,
    val familyGroup: String = AtaPotifyConstants.GROUP_1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val normalizedGroup: String
        get() = AtaPotifyConstants.normalizeFamilyGroup(familyGroup)
}

@IgnoreExtraProperties
data class PaymentMonth(
    @DocumentId val id: String = "",
    val monthId: String = "",
    val displayLabel: String = "",
    val amountLkr: Long = AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
    val allowedEmails: List<String> = emptyList(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MemberPaymentStatus(
    @DocumentId val id: String = "",
    val memberKey: String = "",
    val memberEmail: String = "",
    val displayName: String = "",
    val status: String = AtaPotifyConstants.STATUS_PENDING,
    val amountLkr: Long = AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
    val paidAmountLkr: Long = 0L,
    val paymentMethod: String = "",
    val hasReceipt: Boolean = false,
    val allowedEmails: List<String> = emptyList(),
    val reminderCount: Long = 0L,
    val familyGroup: String = AtaPotifyConstants.GROUP_1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val submittedAt: Timestamp? = null,
    val confirmedAt: Timestamp? = null,
    val lastRemindedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val normalizedGroup: String
        get() = AtaPotifyConstants.normalizeFamilyGroup(familyGroup)

    val effectivePaidLkr: Long
        get() = when {
            status == AtaPotifyConstants.STATUS_PAID -> amountLkr.coerceAtLeast(0L)
            else -> paidAmountLkr.coerceIn(0L, amountLkr.coerceAtLeast(0L))
        }

    val remainingLkr: Long
        get() = (amountLkr.coerceAtLeast(0L) - effectivePaidLkr).coerceAtLeast(0L)

    val hasPartialPayment: Boolean
        get() = status != AtaPotifyConstants.STATUS_PAID && effectivePaidLkr > 0L

    val isCashPayment: Boolean
        get() = paymentMethod == AtaPotifyConstants.PAYMENT_METHOD_CASH
    fun pendingAgeDays(nowMillis: Long = System.currentTimeMillis()): Long {
        val startMillis = createdAt?.toDate()?.time ?: return 0L
        return ((nowMillis - startMillis).coerceAtLeast(0L)) / (24L * 60L * 60L * 1000L)
    }

    fun isOverdueForAutoReminder(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (status != AtaPotifyConstants.STATUS_PENDING) return false
        val startMillis = createdAt?.toDate()?.time ?: return false
        if (nowMillis - startMillis < AtaPotifyConstants.OVERDUE_MILLIS_THRESHOLD) return false
        val lastReminded = lastRemindedAt?.toDate()?.time ?: return true
        return nowMillis - lastReminded >= AtaPotifyConstants.OVERDUE_MILLIS_THRESHOLD
    }
}

@IgnoreExtraProperties
data class ReceiptSlip(
    val memberEmail: String = "",
    val uploadedByUid: String = "",
    val mimeType: String = "image/jpeg",
    val imageBase64: String = "",
    @ServerTimestamp val uploadedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AccessRequest(
    @DocumentId val id: String = "",
    val uid: String = "",
    val memberKey: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String = "",
    val spotifyEmail: String = "",
    val status: String = AtaPotifyConstants.STATUS_PENDING,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ListeningStat(
    @DocumentId val id: String = "",
    val memberKey: String = "",
    val memberEmail: String = "",
    val displayName: String = "",
    val spotifyDisplayName: String = "",
    val minutesListened: Long = 0L,
    val tracksCounted: Long = 0L,
    val topArtist: String = "",
    val topTrack: String = "",
    val allowedEmails: List<String> = emptyList(),
    @ServerTimestamp val syncedAt: Timestamp? = null,
)

data class GroupColorPreset(
    val name: String,
    val hex: String,
)

val FAMILY_GROUP_COLOR_PRESETS = listOf(
    GroupColorPreset("Emerald", "#1ED760"),
    GroupColorPreset("Sky Cyan", "#38BDF8"),
    GroupColorPreset("Violet", "#A78BFA"),
    GroupColorPreset("Rose Pink", "#F472B6"),
    GroupColorPreset("Amber Gold", "#FBBF24"),
    GroupColorPreset("Coral Red", "#FB7185"),
    GroupColorPreset("Teal", "#2DD4BF"),
    GroupColorPreset("Indigo", "#818CF8"),
    GroupColorPreset("Sunset", "#FB923C"),
    GroupColorPreset("Mint", "#34D399"),
)

@IgnoreExtraProperties
data class FamilyGroup(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val name: String = "",
    val startDate: String = "",
    val amountLkr: Long = AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
    val colorHex: String = AtaPotifyConstants.DEFAULT_GROUP_1_COLOR_HEX,
    val maxSlots: Long = AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS.toLong(),
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val effectiveId: String
        get() = AtaPotifyConstants.normalizeFamilyGroup(groupId.ifBlank { id })
}

fun resolveEffectiveFamilyGroups(
    rawGroups: List<FamilyGroup>,
    defaultAmountLkr: Long = AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR,
    whitelist: List<WhitelistMember> = emptyList(),
    members: List<MemberPaymentStatus> = emptyList(),
): List<FamilyGroup> {
    val paletteCycle = listOf("#A78BFA", "#F472B6", "#FBBF24", "#2DD4BF", "#FB7185", "#818CF8")
    if (rawGroups.isEmpty()) {
        val referenced = (whitelist.map { it.normalizedGroup } + members.map { it.normalizedGroup }).toSet()
        val list = mutableListOf(
            FamilyGroup(
                id = AtaPotifyConstants.GROUP_1,
                groupId = AtaPotifyConstants.GROUP_1,
                name = "Family 1",
                startDate = "${AtaPotifyConstants.currentMonthId()}-01",
                amountLkr = defaultAmountLkr,
                colorHex = AtaPotifyConstants.DEFAULT_GROUP_1_COLOR_HEX,
                maxSlots = AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS.toLong(),
                sortOrder = 0L,
            ),
            FamilyGroup(
                id = AtaPotifyConstants.GROUP_2,
                groupId = AtaPotifyConstants.GROUP_2,
                name = "Family 2",
                startDate = "${AtaPotifyConstants.currentMonthId()}-01",
                amountLkr = defaultAmountLkr,
                colorHex = AtaPotifyConstants.DEFAULT_GROUP_2_COLOR_HEX,
                maxSlots = AtaPotifyConstants.MAX_SPOTIFY_FAMILY_SLOTS.toLong(),
                sortOrder = 1L,
            ),
        )
        referenced.filter { it != AtaPotifyConstants.GROUP_1 && it != AtaPotifyConstants.GROUP_2 }
            .forEachIndexed { idx, refId ->
                list += FamilyGroup(
                    id = refId,
                    groupId = refId,
                    name = AtaPotifyConstants.familyGroupLabel(refId),
                    amountLkr = defaultAmountLkr,
                    colorHex = paletteCycle[idx % paletteCycle.size],
                    sortOrder = (idx + 2).toLong(),
                )
            }
        return list
    }

    val sorted = rawGroups.sortedWith(
        compareBy<FamilyGroup> { it.sortOrder }.thenBy { it.name.lowercase(Locale.US) }
    ).toMutableList()
    val existingIds = sorted.map { it.effectiveId }.toSet()
    val orphanedIds = (whitelist.map { it.normalizedGroup } + members.map { it.normalizedGroup })
        .distinct()
        .filter { it !in existingIds }
    orphanedIds.forEachIndexed { idx, orphanId ->
        sorted += FamilyGroup(
            id = orphanId,
            groupId = orphanId,
            name = AtaPotifyConstants.familyGroupLabel(orphanId),
            amountLkr = defaultAmountLkr,
            colorHex = if (orphanId == AtaPotifyConstants.GROUP_2) {
                AtaPotifyConstants.DEFAULT_GROUP_2_COLOR_HEX
            } else {
                paletteCycle[idx % paletteCycle.size]
            },
            sortOrder = (sorted.size + idx).toLong(),
        )
    }
    return sorted
}

data class MonthlyBillingSummary(
    val expectedTotalLkr: Long,
    val collectedLkr: Long,
    val pendingOnlyLkr: Long,
    val verifyingLkr: Long,
    val totalPendingLkr: Long,
    val netBalanceLkr: Long,
    val paidCount: Int,
    val verifyingCount: Int,
    val pendingCount: Int,
    val totalCount: Int,
    val collectionFraction: Float,
)

fun calculateMonthlyBillingSummary(members: List<MemberPaymentStatus>): MonthlyBillingSummary {
    var expectedTotal = 0L
    var collected = 0L
    var pendingOnly = 0L
    var verifying = 0L
    var paidCount = 0
    var verifyingCount = 0
    var pendingCount = 0

    for (member in members) {
        val amount = member.amountLkr.coerceAtLeast(0L)
        val paidPart = member.effectivePaidLkr
        val remaining = (amount - paidPart).coerceAtLeast(0L)
        expectedTotal += amount
        collected += paidPart
        when (member.status) {
            AtaPotifyConstants.STATUS_PAID -> {
                paidCount++
            }
            AtaPotifyConstants.STATUS_VERIFYING -> {
                verifying += remaining
                verifyingCount++
            }
            else -> {
                pendingOnly += remaining
                pendingCount++
            }
        }
    }

    val totalPending = pendingOnly + verifying
    val netBalance = collected - expectedTotal
    val fraction = if (expectedTotal > 0L) {
        (collected.toFloat() / expectedTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    return MonthlyBillingSummary(
        expectedTotalLkr = expectedTotal,
        collectedLkr = collected,
        pendingOnlyLkr = pendingOnly,
        verifyingLkr = verifying,
        totalPendingLkr = totalPending,
        netBalanceLkr = netBalance,
        paidCount = paidCount,
        verifyingCount = verifyingCount,
        pendingCount = pendingCount,
        totalCount = members.size,
        collectionFraction = fraction,
    )
}

data class AllTimeMemberStanding(
    val memberKey: String,
    val displayName: String,
    val totalMinutes: Long,
    val totalTracks: Long,
    val totalLkrAllocated: Long,
    val favoriteArtist: String,
)

data class LongestStreakStanding(
    val memberKey: String,
    val displayName: String,
    val streakMonths: Int,
)

data class AllTimeWrappedSummary(
    val monthsTracked: Int,
    val totalGroupMinutes: Long,
    val totalGroupTracks: Long,
    val topListener: AllTimeMemberStanding?,
    val topArtists: List<String>,
    val standings: List<AllTimeMemberStanding>,
    val longestStreak: LongestStreakStanding? = null,
)

fun calculatePaymentStreaks(
    monthsSortedDesc: List<PaymentMonth>,
    membersByMonth: Map<String, List<MemberPaymentStatus>>,
): Map<String, Int> {
    if (monthsSortedDesc.isEmpty() || membersByMonth.isEmpty()) return emptyMap()
    val orderedMonths = monthsSortedDesc.sortedByDescending { it.monthId }
    val allMemberKeys = membersByMonth.values.flatten().map { it.memberKey }.toSet()
    val streaks = mutableMapOf<String, Int>()

    for (memberKey in allMemberKeys) {
        var count = 0
        var started = false
        for ((index, month) in orderedMonths.withIndex()) {
            val entry = membersByMonth[month.monthId]?.firstOrNull { it.memberKey == memberKey }
            if (entry == null) {
                if (started) break else continue
            }
            if (entry.status == AtaPotifyConstants.STATUS_PAID) {
                count++
                started = true
            } else {
                // If the newest cycle is still in progress (pending/verifying), don't reset prior streak yet
                if (index == 0 && !started) {
                    continue
                }
                break
            }
        }
        if (count > 0) {
            streaks[memberKey] = count
        }
    }
    return streaks
}

fun calculateLongestStreakStanding(
    monthsSortedDesc: List<PaymentMonth>,
    membersByMonth: Map<String, List<MemberPaymentStatus>>,
): LongestStreakStanding? {
    val streaks = calculatePaymentStreaks(monthsSortedDesc, membersByMonth)
    if (streaks.isEmpty()) return null
    val nameByKey = mutableMapOf<String, String>()
    for (month in monthsSortedDesc.sortedByDescending { it.monthId }) {
        for (m in membersByMonth[month.monthId].orEmpty()) {
            if (m.displayName.isNotBlank() && m.memberKey !in nameByKey) {
                nameByKey[m.memberKey] = m.displayName
            }
        }
    }
    val best = streaks.entries.maxWithOrNull(
        compareBy<Map.Entry<String, Int>> { it.value }
            .thenBy { nameByKey[it.key]?.lowercase().orEmpty() }
    ) ?: return null
    if (best.value <= 0) return null
    return LongestStreakStanding(
        memberKey = best.key,
        displayName = nameByKey[best.key].orEmpty().ifBlank { "Member" },
        streakMonths = best.value,
    )
}

fun calculateAllTimeWrappedSummary(
    allStatsByMonth: Map<String, List<ListeningStat>>,
    monthAmountByMonth: Map<String, Long>,
    longestStreak: LongestStreakStanding? = null,
): AllTimeWrappedSummary? {
    val nonEmptyMonths = allStatsByMonth.filterValues { stats ->
        stats.any { it.minutesListened > 0L || it.tracksCounted > 0L }
    }
    if (nonEmptyMonths.isEmpty() && longestStreak == null) return null

    data class Accumulator(
        var displayName: String = "",
        var minutes: Long = 0L,
        var tracks: Long = 0L,
        var totalLkr: Long = 0L,
        val artistCounts: MutableMap<String, Int> = mutableMapOf(),
    )

    val byMember = mutableMapOf<String, Accumulator>()
    val globalArtists = mutableMapOf<String, Int>()

    for ((monthId, stats) in nonEmptyMonths) {
        val monthRate = monthAmountByMonth[monthId] ?: AtaPotifyConstants.DEFAULT_MONTHLY_AMOUNT_LKR
        for (stat in stats) {
            if (stat.minutesListened <= 0L && stat.tracksCounted <= 0L) continue
            val acc = byMember.getOrPut(stat.memberKey) { Accumulator(displayName = stat.displayName) }
            if (stat.displayName.isNotBlank()) acc.displayName = stat.displayName
            acc.minutes += stat.minutesListened
            acc.tracks += stat.tracksCounted
            acc.totalLkr += monthRate
            if (stat.topArtist.isNotBlank()) {
                acc.artistCounts[stat.topArtist] = (acc.artistCounts[stat.topArtist] ?: 0) + 1
                globalArtists[stat.topArtist] = (globalArtists[stat.topArtist] ?: 0) + 1
            }
        }
    }

    val standings = byMember.map { (key, acc) ->
        AllTimeMemberStanding(
            memberKey = key,
            displayName = acc.displayName.ifBlank { "Member" },
            totalMinutes = acc.minutes,
            totalTracks = acc.tracks,
            totalLkrAllocated = acc.totalLkr,
            favoriteArtist = acc.artistCounts.maxByOrNull { it.value }?.key.orEmpty(),
        )
    }.sortedByDescending { it.totalMinutes }

    if (standings.isEmpty() && longestStreak == null) return null

    val topListener = standings.firstOrNull { it.totalMinutes > 0L }
    val topArtists = globalArtists.entries
        .sortedByDescending { it.value }
        .map { it.key }
        .take(4)

    return AllTimeWrappedSummary(
        monthsTracked = maxOf(nonEmptyMonths.size, if (longestStreak != null) 1 else 0),
        totalGroupMinutes = standings.sumOf { it.totalMinutes },
        totalGroupTracks = standings.sumOf { it.totalTracks },
        topListener = topListener,
        topArtists = topArtists,
        standings = standings,
        longestStreak = longestStreak,
    )
}

fun buildReceiptReferenceCode(monthId: String, memberKey: String): String {
    val cleanMonth = monthId.replace("-", "").take(6)
    val shortMember = memberKey.take(4).uppercase(Locale.US)
    return "ATP-$cleanMonth-$shortMember"
}

fun buildVerifiedPaymentReceiptText(
    month: PaymentMonth,
    member: MemberPaymentStatus,
    streakCount: Int = 0,
    planName: String = AtaPotifyConstants.DEFAULT_PLAN_NAME,
): String {
    val refCode = buildReceiptReferenceCode(month.monthId, member.memberKey)
    val confirmedDateStr = member.confirmedAt?.toDate()?.let {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(it)
    } ?: "Verified by Admin"
    val methodStr = when {
        member.isCashPayment -> "Cash in Hand (Verified by Admin)"
        member.hasReceipt -> "Bank Transfer Slip (Verified)"
        else -> "Direct Admin Confirmation"
    }

    return buildString {
        appendLine("✅ *ATAPOTIFY VERIFIED RECEIPT*")
        appendLine("Ref: `#$refCode`")
        appendLine("────────────────────")
        appendLine("• *Member:* ${member.displayName}")
        appendLine("• *Plan:* $planName")
        appendLine("• *Billing Cycle:* ${month.displayLabel} (${month.monthId})")
        appendLine("• *Amount Paid:* LKR ${member.amountLkr}")
        appendLine("• *Status:* PAID & SETTLED")
        appendLine("• *Method:* $methodStr")
        appendLine("• *Verified On:* $confirmedDateStr")
        if (streakCount >= 2) {
            appendLine("• *Payment Streak:* 🔥 $streakCount consecutive months")
        }
    }.trim()
}

fun buildWhatsAppMonthlySummaryText(
    month: PaymentMonth,
    members: List<MemberPaymentStatus>,
    stats: List<ListeningStat>,
    planConfig: PlanConfig,
    groupFilter: String = AtaPotifyConstants.GROUP_ALL,
    familyGroups: List<FamilyGroup> = emptyList(),
): String {
    val summary = calculateMonthlyBillingSummary(members)
    val hasMultipleGroups = members.map { it.normalizedGroup }.distinct().size > 1
    val groupSuffix = if (groupFilter != AtaPotifyConstants.GROUP_ALL) {
        " (${AtaPotifyConstants.familyGroupLabel(groupFilter, familyGroups)})"
    } else {
        ""
    }
    return buildString {
        appendLine("*AtaPotify — ${month.displayLabel}$groupSuffix*")
        appendLine("Share: LKR ${month.amountLkr} • Collected: LKR ${summary.collectedLkr}/${summary.expectedTotalLkr} (${summary.paidCount}/${summary.totalCount} Paid)")
        appendLine()
        if (members.isNotEmpty()) {
            appendLine("*Payment Status:*")
            members.forEach { m ->
                val badge = when (m.status) {
                    AtaPotifyConstants.STATUS_PAID -> if (m.isCashPayment) "[PAID • Cash]" else "[PAID]"
                    AtaPotifyConstants.STATUS_VERIFYING -> "[VERIFYING]"
                    else -> if (m.hasPartialPayment) "[PARTIAL]" else "[PENDING]"
                }
                val groupTag = if (groupFilter == AtaPotifyConstants.GROUP_ALL && hasMultipleGroups) {
                    " [${AtaPotifyConstants.familyGroupShortBadge(m.normalizedGroup, familyGroups)}]"
                } else {
                    ""
                }
                val amountDetail = if (m.hasPartialPayment) {
                    "LKR ${m.effectivePaidLkr}/${m.amountLkr} paid • LKR ${m.remainingLkr} left"
                } else {
                    "LKR ${m.amountLkr}"
                }
                appendLine("• ${m.displayName}$groupTag: $badge ($amountDetail)")
            }
        }
        val activeStats = stats.filter { it.minutesListened > 0L }.sortedByDescending { it.minutesListened }
        if (activeStats.isNotEmpty()) {
            appendLine()
            appendLine("*Spotify Listening Leaderboard:*")
            activeStats.take(5).forEachIndexed { idx, s ->
                val artist = if (s.topArtist.isNotBlank()) " • ${s.topArtist}" else ""
                val tracks = if (s.tracksCounted > 0L) " (${s.tracksCounted} tracks)" else ""
                appendLine("#${idx + 1} ${s.displayName}: ${s.minutesListened} min$tracks$artist")
            }
        }
        if (planConfig.hasBankDetails && summary.pendingCount > 0) {
            appendLine()
            appendLine("*Bank Transfer Details:*")
            appendLine(planConfig.formattedBankCopyText())
        }
    }.trim()
}


