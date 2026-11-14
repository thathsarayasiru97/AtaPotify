package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.sanitizeWhatsAppPhone

object WhatsAppHelper {

    fun buildReminderMessage(
        memberName: String,
        monthLabel: String,
        amountLkr: Long,
        daysPending: Long? = null
    ): String {
        val firstName = memberName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "there" }
        return if (daysPending != null && daysPending >= 7L) {
            "Hi $firstName! Quick reminder: your AtaPotify (Spotify Family) share of LKR $amountLkr for $monthLabel has been pending for $daysPending days. Please transfer and upload your receipt slip when you can!"
        } else {
            "Hi $firstName! Friendly reminder to send your AtaPotify (Spotify Family) share of LKR $amountLkr for $monthLabel and upload the payment slip in the app."
        }
    }

    fun openWhatsAppReminder(
        context: Context,
        rawPhone: String,
        message: String
    ): Result<Unit> {
        val digits = sanitizeWhatsAppPhone(rawPhone)
        if (digits.length < 7) {
            return Result.failure(IllegalArgumentException("Missing valid WhatsApp number for this member"))
        }
        val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Result.success(Unit)
        } catch (_: ActivityNotFoundException) {
            Result.failure(IllegalStateException("No browser or WhatsApp app found to open reminder link"))
        }
    }
}
