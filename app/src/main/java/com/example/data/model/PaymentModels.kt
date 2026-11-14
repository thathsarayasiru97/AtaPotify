package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Default fixed rounded monthly payment per member in LKR.
 * Can be changed here or edited in-app by the Admin.
 */
const val DEFAULT_MONTHLY_LKR = 450

/**
 * Hardcoded Admin Google email.
 */
const val ADMIN_EMAIL = "thathsarayasiru97@gmail.com"

/**
 * Days a Pending payment can remain unpaid before triggering the automated WhatsApp reminder queue.
 */
const val OVERDUE_DAYS_THRESHOLD = 7L

enum class UserRole(val value: String) {
    ADMIN("admin"),
    MEMBER("member");

    companion object {
        fun fromValue(raw: String?): UserRole =
            if (raw.equals("admin", ignoreCase = true)) ADMIN else MEMBER
    }
}

enum class PaymentStatus(val value: String, val label: String) {
    PENDING("pending", "Pending"),
    VERIFYING("verifying", "Verifying"),
    PAID("paid", "Paid");

    companion object {
        fun fromValue(raw: String?): PaymentStatus = when (raw?.lowercase(Locale.ROOT)) {
            "paid" -> PAID
            "verifying" -> VERIFYING
            else -> PENDING
        }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = UserRole.MEMBER.value,
    val phone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlanConfig(
    @DocumentId val id: String = "settings",
    val planName: String = "Spotify Family",
    val monthlyAmountLkr: Long = DEFAULT_MONTHLY_LKR.toLong(),
    val currency: String = "LKR",
    val allowedEmails: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WhitelistMember(
    @DocumentId val id: String = "",
    val memberKey: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String = "",
    val role: String = UserRole.MEMBER.value,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentMonth(
    @DocumentId val id: String = "",
    val monthId: String = "",
    val displayLabel: String = "",
    val amountLkr: Long = DEFAULT_MONTHLY_LKR.toLong(),
    val allowedEmails: List<String> = emptyList(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MemberPaymentStatus(
    @DocumentId val id: String = "",
    val memberKey: String = "",
    val memberEmail: String = "",
    val displayName: String = "",
    val status: String = PaymentStatus.PENDING.value,
    val amountLkr: Long = DEFAULT_MONTHLY_LKR.toLong(),
    val hasReceipt: Boolean = false,
    val allowedEmails: List<String> = emptyList(),
    val reminderCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val submittedAt: Timestamp? = null,
    val confirmedAt: Timestamp? = null,
    val lastRemindedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val paymentStatus: PaymentStatus
        get() = PaymentStatus.fromValue(status)
}

@IgnoreExtraProperties
data class ReceiptSlip(
    @DocumentId val id: String = "slip",
    val memberEmail: String = "",
    val uploadedByUid: String = "",
    val mimeType: String = "image/jpeg",
    val imageBase64: String = "",
    val uploadedAt: Timestamp? = null
)

fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

fun emailToMemberKey(email: String): String {
    val normalized = normalizeEmail(email)
    val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }.take(32)
}

fun currentYearMonthId(): String {
    val cal = Calendar.getInstance()
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

fun formatMonthLabel(monthId: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
        val date = parser.parse(monthId)
        if (date != null) formatter.format(date) else monthId
    } catch (_: Exception) {
        monthId
    }
}

fun sanitizeWhatsAppPhone(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    return when {
        digits.startsWith("0") && digits.length == 10 -> "94" + digits.drop(1)
        else -> digits
    }
}
