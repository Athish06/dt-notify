package com.notivault.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notivault.data.repository.HealthRepository
import com.notivault.service.adapter.AdapterRegistry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * NotiVaultListenerService — the heart of the app.
 *
 * Android feeds every notification to this service via onNotificationPosted.
 * We keep processing minimal on the main thread: extract, enqueue to coroutine, return.
 *
 * Key behaviors:
 * - Skips own notifications, ongoing/foreground-service, group summaries
 * - Reconnects via requestRebind after OEM battery kills
 * - Backfills active notifications on reconnect via getActiveNotifications()
 */
@AndroidEntryPoint
class NotiVaultListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotiVaultListener"
    }

    @Inject lateinit var processor: NotificationProcessor
    @Inject lateinit var healthRepository: HealthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var adapterRegistry: AdapterRegistry? = null

    override fun onCreate() {
        super.onCreate()
        adapterRegistry = AdapterRegistry(packageManager)
        processor.setAdapterRegistry(adapterRegistry!!)
        Log.i(TAG, "Service created")
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Listener connected")
        serviceScope.launch {
            healthRepository.logEvent("CONNECTED")

            // Backfill any notifications that arrived while we were dead
            try {
                val active = getActiveNotifications() ?: return@launch
                val ranking = currentRanking ?: return@launch
                Log.i(TAG, "Backfilling ${active.size} active notifications")

                for (sbn in active) {
                    if (shouldSkip(sbn)) continue
                    val r = Ranking()
                    ranking.getRanking(sbn.key, r)
                    processor.process(sbn, r, isBackfill = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during backfill", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (shouldSkip(sbn)) return

        serviceScope.launch {
            try {
                val ranking = Ranking()
                currentRanking?.getRanking(sbn.key, ranking)
                processor.process(sbn, ranking)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from ${sbn.packageName}", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        if (shouldSkip(sbn)) return

        serviceScope.launch {
            try {
                val ranking = Ranking()
                rankingMap.getRanking(sbn.key, ranking)
                processor.process(sbn, ranking)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from ${sbn.packageName}", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        serviceScope.launch {
            processor.recordRemoval(sbn.key, reason)
        }
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Listener disconnected — OEM may have killed the service")
        serviceScope.launch {
            healthRepository.logEvent("DISCONNECTED", "Attempting rebind")
            delay(3000)
            try {
                requestRebind(ComponentName(this@NotiVaultListenerService, NotiVaultListenerService::class.java))
                healthRepository.logEvent("REBIND_ATTEMPT")
                Log.i(TAG, "Rebind requested")
            } catch (e: Exception) {
                Log.e(TAG, "Rebind failed", e)
                healthRepository.logEvent("REBIND_FAILED", e.message)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
    }

    private fun shouldSkip(sbn: StatusBarNotification): Boolean {
        // Skip own notifications
        if (sbn.packageName == packageName) return true

        // Skip ongoing/foreground-service notifications (media players, navigation, etc.)
        if (sbn.isOngoing) return true

        val extras = sbn.notification.extras

        // Skip group summaries (synthetic containers, not real messages)
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return true

        // Skip if both title and text are empty (icon-only notifications)
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            // Check if there are messages (MessagingStyle might have them without EXTRA_TITLE)
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages.isNullOrEmpty()) return true
        }

        return false
    }
}
