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

    const val PLAN_CONFIG_DOC_ID = "settings"
    const val SLIP_DOC_ID = "slip"

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
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WhitelistMember(
    @DocumentId val id: String = "",
    val memberKey: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String = "",
    val role: String = AtaPotifyConstants.ROLE_MEMBER,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

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
    val hasReceipt: Boolean = false,
    val allowedEmails: List<String> = emptyList(),
    val reminderCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val submittedAt: Timestamp? = null,
    val confirmedAt: Timestamp? = null,
    val lastRemindedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
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
        expectedTotal += amount
        when (member.status) {
            AtaPotifyConstants.STATUS_PAID -> {
                collected += amount
                paidCount++
            }
            AtaPotifyConstants.STATUS_VERIFYING -> {
                verifying += amount
                verifyingCount++
            }
            else -> {
                pendingOnly += amount
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

