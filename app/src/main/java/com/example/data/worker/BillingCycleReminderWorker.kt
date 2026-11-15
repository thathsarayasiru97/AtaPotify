package com.example.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.MemberPaymentStatus
import com.example.data.model.PaymentMonth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object BillingReminderNotifier {
    private const val CHANNEL_ID = "atapotify_billing_reminders"
    private const val CHANNEL_NAME = "Billing Cycle & Payment Reminders"
    private const val PREFS_NAME = "atapotify_billing_reminder_prefs"
    private const val NOTIF_ID_NEW_CYCLE = 4101
    private const val NOTIF_ID_ADMIN_REQUESTS = 4102
    private const val REMINDER_REPEAT_INTERVAL_MS = 3L * 24L * 60L * 60L * 1000L

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alerts when a new AtaPotify billing cycle begins or your monthly payment is pending."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun markCycleSettledLocally(context: Context, uid: String, monthId: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_cycle_$uid", monthId)
            .putBoolean("settled_${uid}_$monthId", true)
            .apply()
    }

    fun notifyMemberForCycleIfNeeded(
        context: Context,
        uid: String,
        month: PaymentMonth,
        memberStatus: MemberPaymentStatus?,
    ) {
        if (memberStatus == null) return
        if (memberStatus.status != AtaPotifyConstants.STATUS_PENDING) {
            markCycleSettledLocally(context, uid, month.monthId)
            return
        }
        if (!hasNotificationPermission(context)) return

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean("settled_${uid}_${month.monthId}", false)) {
            prefs.edit().remove("settled_${uid}_${month.monthId}").apply()
        }

        val lastNotifiedMonth = prefs.getString("last_cycle_$uid", null)
        val lastReminderTime = prefs.getLong("last_remind_ts_${uid}_${month.monthId}", 0L)
        val now = System.currentTimeMillis()

        val isBrandNewCycle = lastNotifiedMonth != month.monthId
        val isDueForFollowUp = !isBrandNewCycle &&
            lastReminderTime > 0L &&
            (now - lastReminderTime) >= REMINDER_REPEAT_INTERVAL_MS

        if (!isBrandNewCycle && !isDueForFollowUp) return

        val title = if (isBrandNewCycle) {
            "New Billing Cycle: ${month.displayLabel}"
        } else {
            "Payment Reminder: ${month.displayLabel}"
        }
        val body = if (isBrandNewCycle) {
            "Your AtaPotify share (LKR ${memberStatus.amountLkr}) is ready. Tap to view bank details and upload your slip."
        } else {
            "Your LKR ${memberStatus.amountLkr} Spotify Family share is still pending. Upload your transfer slip in AtaPotify."
        }

        postNotification(
            context = context,
            notificationId = NOTIF_ID_NEW_CYCLE,
            title = title,
            body = body,
        )

        prefs.edit()
            .putString("last_cycle_$uid", month.monthId)
            .putLong("last_remind_ts_${uid}_${month.monthId}", now)
            .apply()
    }

    fun notifyAdminPendingRequestsIfNeeded(
        context: Context,
        uid: String,
        pendingCount: Int,
    ) {
        if (pendingCount <= 0 || !hasNotificationPermission(context)) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCount = prefs.getInt("last_admin_req_count_$uid", 0)
        if (pendingCount <= lastCount) {
            if (pendingCount == 0 && lastCount != 0) {
                prefs.edit().putInt("last_admin_req_count_$uid", 0).apply()
            }
            return
        }

        postNotification(
            context = context,
            notificationId = NOTIF_ID_ADMIN_REQUESTS,
            title = "New AtaPotify Join Request",
            body = "$pendingCount friend(s) are awaiting your approval in the Admin tab.",
        )
        prefs.edit().putInt("last_admin_req_count_$uid", pendingCount).apply()
    }

    fun clearAdminRequestCountCache(context: Context, uid: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("last_admin_req_count_$uid", 0).apply()
    }

    private fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
    ) {
        ensureChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_atapotify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }.onFailure { err ->
            Log.w("BillingReminder", "Failed to post notification: ${err.message}")
        }
    }
}

class BillingCycleReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val firebaseUser = Firebase.auth.currentUser ?: return Result.success()
        val emailLower = AtaPotifyConstants.normalizeEmail(firebaseUser.email.orEmpty())
        if (emailLower.isBlank() || !firebaseUser.isEmailVerified) {
            return Result.success()
        }

        val databaseId = applicationContext.getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)
        val isAdmin = emailLower == AtaPotifyConstants.ADMIN_EMAIL
        val memberKey = AtaPotifyConstants.memberKeyForEmail(emailLower)

        return try {
            if (isAdmin) {
                val pendingReqCount = firestore.collection("access_requests")
                    .whereEqualTo("status", AtaPotifyConstants.STATUS_PENDING)
                    .get()
                    .await()
                    .size()
                if (pendingReqCount > 0) {
                    BillingReminderNotifier.notifyAdminPendingRequestsIfNeeded(
                        context = applicationContext,
                        uid = firebaseUser.uid,
                        pendingCount = pendingReqCount,
                    )
                } else {
                    BillingReminderNotifier.clearAdminRequestCountCache(applicationContext, firebaseUser.uid)
                }
            }

            val monthsQuery = if (isAdmin) {
                firestore.collection("payments")
            } else {
                firestore.collection("payments").whereArrayContains("allowedEmails", emailLower)
            }
            val latestMonth = monthsQuery.get().await()
                .toObjects(PaymentMonth::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .maxByOrNull { it.monthId }
                ?: return Result.success()

            if (!isAdmin) {
                val memberDoc = firestore.collection("payments")
                    .document(latestMonth.monthId)
                    .collection("members")
                    .document(memberKey)
                    .get()
                    .await()
                    .toObject(MemberPaymentStatus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

                BillingReminderNotifier.notifyMemberForCycleIfNeeded(
                    context = applicationContext,
                    uid = firebaseUser.uid,
                    month = latestMonth,
                    memberStatus = memberDoc,
                )
            }

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Billing cycle reminder check failed", e)
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    private companion object {
        const val TAG = "BillingReminderWorker"
    }
}

object BillingReminderScheduler {
    private const val UNIQUE_WORK_NAME = "atapotify_periodic_billing_reminders"

    fun schedulePeriodicChecks(context: Context) {
        BillingReminderNotifier.ensureChannel(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<BillingCycleReminderWorker>(
            6L,
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )
    }

    fun cancelPeriodicChecks(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
