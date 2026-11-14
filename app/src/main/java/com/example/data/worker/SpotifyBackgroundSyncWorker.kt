package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.model.AtaPotifyConstants
import com.example.data.model.ListeningStat
import com.example.data.model.PaymentMonth
import com.example.data.model.UserProfile
import com.example.data.repository.PaymentRepository
import com.example.data.service.SpotifyStatsService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class SpotifyBackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val firebaseUser = Firebase.auth.currentUser ?: return Result.success()
        val emailLower = AtaPotifyConstants.normalizeEmail(firebaseUser.email.orEmpty())
        if (emailLower.isBlank() || !firebaseUser.isEmailVerified) {
            return Result.success()
        }

        val spotifyService = SpotifyStatsService(applicationContext)
        if (!spotifyService.isClientIdConfigured || !spotifyService.isConnected) {
            return Result.success()
        }

        val databaseId = applicationContext.getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)
        val isAdmin = emailLower == AtaPotifyConstants.ADMIN_EMAIL
        val memberKey = AtaPotifyConstants.memberKeyForEmail(emailLower)

        return try {
            val monthsQuery = if (isAdmin) {
                firestore.collection("payments")
            } else {
                firestore.collection("payments").whereArrayContains("allowedEmails", emailLower)
            }
            val latestMonth = monthsQuery.get().await()
                .toObjects(PaymentMonth::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .maxByOrNull { it.monthId }
                ?: return Result.success()

            // Seed baseline from Firestore if local prefs were cleared or reinstalled
            val remoteStat = runCatching {
                firestore.collection("payments")
                    .document(latestMonth.monthId)
                    .collection("stats")
                    .document(memberKey)
                    .get()
                    .await()
                    .toObject(ListeningStat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull()
            if (remoteStat != null) {
                spotifyService.ensureMonthBaselineFromRemote(
                    monthId = latestMonth.monthId,
                    remoteMinutes = remoteStat.minutesListened,
                    remoteTracks = remoteStat.tracksCounted,
                )
            }

            val snap = spotifyService.fetchAndAccumulateMonthlyStats(latestMonth.monthId).getOrElse { err ->
                Log.w(TAG, "Spotify background sync fetch failed", err)
                return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }

            val userProfile = runCatching {
                firestore.collection("users").document(firebaseUser.uid).get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull()

            val displayName = userProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: emailLower.substringBefore("@").replaceFirstChar { it.uppercase() }

            val paymentRepository = PaymentRepository(db = firestore, auth = Firebase.auth)
            val writeRes = paymentRepository.syncListeningStats(
                monthId = latestMonth.monthId,
                memberKey = memberKey,
                memberEmail = emailLower,
                displayName = displayName,
                spotifyDisplayName = snap.spotifyDisplayName,
                minutesListened = snap.minutesListened,
                tracksCounted = snap.tracksCounted,
                topArtist = snap.topArtist,
                topTrack = snap.topTrack,
                allowedEmails = latestMonth.allowedEmails,
            )

            if (writeRes.isSuccess) {
                Result.success()
            } else if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected background Spotify sync error", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val TAG = "SpotifyBgSyncWorker"
        const val MAX_RETRIES = 3
    }
}

object SpotifySyncScheduler {
    private const val UNIQUE_WORK_NAME = "atapotify_periodic_spotify_sync"

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SpotifyBackgroundSyncWorker>(
            1L,
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

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
