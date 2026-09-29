package com.notivault.service

import android.app.Notification
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notivault.data.entity.NotificationEntity
import com.notivault.data.model.EntityType
import com.notivault.data.model.NormalizedRecord
import com.notivault.data.model.Tier
import com.notivault.data.repository.DailyCountRepository
import com.notivault.data.repository.NotificationRepository
import com.notivault.engine.PriorityEngine
import com.notivault.service.adapter.AdapterRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deduplicator — prevents storing duplicate notifications.
 *
 * WhatsApp and Telegram re-post cumulative messages. Each update replaces
 * the previous notification with all recent messages appended. Without dedup,
 * we'd store the same messages repeatedly.
 *
 * Strategy:
 * - If the notification key is new → keep all records
 * - If the key exists and we have a message timestamp → only keep newer messages
 * - If the key exists but no timestamp → only keep if text changed
 */
@Singleton
class Deduplicator @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend fun deduplicate(records: List<NormalizedRecord>): List<NormalizedRecord> {
        if (records.isEmpty()) return emptyList()

        val existing = notificationRepository.getLatestByKey(records.first().notificationKey)

        return records.filter { record ->
            when {
                // Brand new notification key
                existing == null -> true

                // Has message timestamp — only keep if strictly newer
                record.messageTimestamp != null -> {
                    record.messageTimestamp > existing.postTime
                }

                // No timestamp — only keep if text content changed
                else -> record.text != existing.text
            }
        }
    }
}

/**
 * NotificationProcessor — orchestrates the full pipeline:
 * normalize → deduplicate → classify → store → update counters
 *
 * Called from the ListenerService for every notification event.
 */
@Singleton
class NotificationProcessor @Inject constructor(
    private val deduplicator: Deduplicator,
    private val priorityEngine: PriorityEngine,
    private val notificationRepository: NotificationRepository,
    private val dailyCountRepository: DailyCountRepository
) {
    private var adapterRegistry: AdapterRegistry? = null

    fun setAdapterRegistry(registry: AdapterRegistry) {
        this.adapterRegistry = registry
    }

    suspend fun process(sbn: StatusBarNotification, ranking: Ranking, isBackfill: Boolean = false) {
        val registry = adapterRegistry ?: run {
            Log.w("NotiVaultProcessor", "AdapterRegistry not initialized, skipping")
            return
        }

        try {
            // 1. Normalize
            val adapter = registry.resolve(sbn.packageName)
            val records = adapter.normalize(sbn, ranking)

            if (records.isEmpty()) return

            // 2. Deduplicate
            val unique = deduplicator.deduplicate(records)
            if (unique.isEmpty()) return

            // 3. Classify and store each record
            for (record in unique) {
                val result = priorityEngine.resolve(record)

                // Don't store IGNORE tier content, only count it
                if (result.tier == Tier.IGNORE) {
                    bumpCounters(record, EntityType.APP)
                    continue
                }

                val entity = NotificationEntity(
                    notificationKey = record.notificationKey,
                    packageName = record.packageName,
                    appLabel = record.appLabel,
                    senderKey = record.senderKey,
                    senderName = record.senderName,
                    isGroup = record.isGroup,
                    conversationName = record.conversationName,
                    title = record.title,
                    text = record.text,
                    subText = record.subText,
                    channelId = record.channelId,
                    channelImportance = record.channelImportance,
                    category = record.category,
                    isConversation = record.isConversation,
                    tier = result.tier,
                    matchedRuleId = result.matchedRuleId,
                    postTime = record.messageTimestamp ?: record.postTime,
                    postTimeIso = record.postTimeIso,
                    capturedAt = System.currentTimeMillis()
                )

                notificationRepository.insert(entity)
                bumpCounters(record, EntityType.APP)
            }
        } catch (e: Exception) {
            Log.e("NotiVaultProcessor", "Error processing notification from ${sbn.packageName}", e)
        }
    }

    suspend fun recordRemoval(key: String, reason: Int) {
        try {
            // REASON_CLICK = 1 — user tapped the notification
            val clicked = reason == 1
            notificationRepository.updateRemoval(key, reason, clicked)

            if (clicked) {
                // Get the notification to find its sender_key for engagement tracking
                val notification = notificationRepository.getLatestByKey(key)
                if (notification != null) {
                    dailyCountRepository.incrementClickCount(notification.senderKey)
                    dailyCountRepository.incrementClickCount("app:${notification.packageName}")
                }
            } else if (reason == 2) { // REASON_CANCEL — user swiped away
                val notification = notificationRepository.getLatestByKey(key)
                if (notification != null) {
                    dailyCountRepository.incrementDismissCount(notification.senderKey)
                    dailyCountRepository.incrementDismissCount("app:${notification.packageName}")
                }
            }
        } catch (e: Exception) {
            Log.e("NotiVaultProcessor", "Error recording removal for $key", e)
        }
    }

    private suspend fun bumpCounters(record: NormalizedRecord, entityType: String) {
        // Bump app-level counter
        dailyCountRepository.incrementNotificationCount(
            entityKey = "app:${record.packageName}",
            entityType = EntityType.APP,
            packageName = record.packageName
        )
        // Bump sender-level counter
        dailyCountRepository.incrementNotificationCount(
            entityKey = record.senderKey,
            entityType = EntityType.SENDER,
            packageName = record.packageName
        )
    }
}
