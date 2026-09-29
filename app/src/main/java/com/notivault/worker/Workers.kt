package com.notivault.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notivault.data.repository.DailyCountRepository
import com.notivault.data.repository.HealthRepository
import com.notivault.data.repository.NotificationRepository
import com.notivault.engine.LearningEngine
import com.notivault.util.Constants
import com.notivault.util.TimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration

/**
 * CleanupWorker — purges expired data daily.
 *
 * Retention policy:
 * - HIGH tier: 14 days
 * - NORMAL tier: 24 hours
 * - Daily counts: 7 days
 * - Health logs: 7 days
 *
 * After cleanup, triggers the learning engine evaluation.
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val dailyCountRepository: DailyCountRepository,
    private val healthRepository: HealthRepository,
    private val learningEngine: LearningEngine
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "CleanupWorker"
        const val WORK_NAME = "notivault_cleanup"
    }

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()

            // Purge Normal-tier notifications older than 24 hours
            val normalCutoff = now - Duration.ofHours(Constants.NORMAL_RETENTION_HOURS).toMillis()
            val normalPurged = notificationRepository.purgeNormal(normalCutoff)

            // Purge High-tier notifications older than 14 days
            val highCutoff = now - Duration.ofDays(Constants.HIGH_RETENTION_DAYS).toMillis()
            val highPurged = notificationRepository.purgeHigh(highCutoff)

            // Purge daily_counts older than 7 days
            val countCutoff = TimeUtils.daysAgoDateString(Constants.DAILY_COUNTS_RETENTION_DAYS)
            val countsPurged = dailyCountRepository.purgeOld(countCutoff)

            // Purge health logs older than 7 days
            val healthCutoff = now - Duration.ofDays(Constants.HEALTH_LOG_RETENTION_DAYS).toMillis()
            val healthPurged = healthRepository.purgeOld(healthCutoff)

            Log.i(TAG, "Cleanup done: normal=$normalPurged, high=$highPurged, counts=$countsPurged, health=$healthPurged")

            // Run learning engine after cleanup
            learningEngine.evaluate()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            Result.retry()
        }
    }
}

/**
 * HealthCheckWorker — periodic check to verify the listener service is alive.
 * Runs every 6 hours. If the listener appears dead, logs a warning event.
 */
@HiltWorker
class HealthCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthRepository: HealthRepository,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "HealthCheckWorker"
        const val WORK_NAME = "notivault_health_check"
        const val STALE_THRESHOLD_MS = 6 * 60 * 60 * 1000L // 6 hours
    }

    override suspend fun doWork(): Result {
        return try {
            val lastNotification = notificationRepository.getLastNotificationTime()
            val lastEvent = healthRepository.getLatestEvent()

            if (lastNotification != null) {
                val timeSinceLast = System.currentTimeMillis() - lastNotification
                if (timeSinceLast > STALE_THRESHOLD_MS) {
                    healthRepository.logEvent(
                        "STALE_WARNING",
                        "No notifications received in ${timeSinceLast / 3_600_000}h"
                    )
                    Log.w(TAG, "Listener may be dead — no notifications in ${timeSinceLast / 3_600_000}h")
                }
            }

            // Check if last event was a disconnect without a subsequent connect
            if (lastEvent?.event == "DISCONNECTED" || lastEvent?.event == "REBIND_FAILED") {
                healthRepository.logEvent(
                    "HEALTH_WARNING",
                    "Last known state: ${lastEvent.event}"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            Result.success() // Don't retry health checks
        }
    }
}
