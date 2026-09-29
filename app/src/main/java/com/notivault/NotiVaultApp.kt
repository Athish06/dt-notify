package com.notivault

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.notivault.worker.CleanupWorker
import com.notivault.worker.HealthCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NotiVaultApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Cleanup worker — runs every 24 hours
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )

        // Health check worker — runs every 6 hours
        val healthRequest = PeriodicWorkRequestBuilder<HealthCheckWorker>(
            6, TimeUnit.HOURS
        )
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            HealthCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            healthRequest
        )
    }
}
