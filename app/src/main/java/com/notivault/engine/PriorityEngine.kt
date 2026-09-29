package com.notivault.engine

import android.app.NotificationManager
import android.util.Log
import com.notivault.data.entity.RuleEntity
import com.notivault.data.model.*
import com.notivault.data.repository.RuleRepository
import com.notivault.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PriorityEngine — resolves a tier for each notification record.
 *
 * Resolution order (first match wins):
 * 1. User-defined SENDER rule
 * 2. User-defined APP rule
 * 3. Learned SENDER rule
 * 4. Learned APP rule
 * 5. Default ignore list (system apps)
 * 6. Channel importance >= HIGH → HIGH
 * 7. Conversation flag → HIGH
 * 8. Fallback → NORMAL
 *
 * Rules are cached in memory and refreshed on each evaluation cycle
 * to avoid per-notification DB queries.
 */
@Singleton
class PriorityEngine @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    companion object {
        private const val TAG = "PriorityEngine"
    }

    // In-memory cache: keyed by "APP:packageName" or "SENDER:senderKey"
    private var ruleCache: Map<String, RuleEntity> = emptyMap()
    private var lastRefresh: Long = 0
    private val refreshIntervalMs = 30_000L // Refresh cache every 30 seconds

    suspend fun resolve(record: NormalizedRecord): PriorityResult {
        refreshCacheIfNeeded()
        val userSenderKey = "${EntityType.SENDER}:${record.senderKey}"
        val userAppKey = "${EntityType.APP}:${record.packageName}"

        // 1. User SENDER rule
        ruleCache[userSenderKey]?.takeIf { it.source == RuleSource.USER }?.let {
            return PriorityResult(it.priority, it.id)
        }

        // 2. User APP rule
        ruleCache[userAppKey]?.takeIf { it.source == RuleSource.USER }?.let {
            return PriorityResult(it.priority, it.id)
        }

        // 3. Learned SENDER rule
        ruleCache[userSenderKey]?.takeIf { it.source == RuleSource.LEARNED }?.let {
            return PriorityResult(it.priority, it.id)
        }

        // 4. Learned APP rule
        ruleCache[userAppKey]?.takeIf { it.source == RuleSource.LEARNED }?.let {
            return PriorityResult(it.priority, it.id)
        }

        // 5. Default ignore list
        if (record.packageName in Constants.DEFAULT_IGNORE_PACKAGES) {
            return PriorityResult(Tier.IGNORE, null)
        }

        // 6. Channel importance >= HIGH
        if (record.channelImportance >= NotificationManager.IMPORTANCE_HIGH) {
            return PriorityResult(Tier.HIGH, null)
        }

        // 7. Conversation
        if (record.isConversation) {
            return PriorityResult(Tier.HIGH, null)
        }

        // 8. Fallback
        return PriorityResult(Tier.NORMAL, null)
    }

    /** Force refresh the rule cache (called after rule changes) */
    suspend fun invalidateCache() {
        loadCache()
    }

    private suspend fun refreshCacheIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastRefresh > refreshIntervalMs) {
            loadCache()
        }
    }

    private suspend fun loadCache() {
        try {
            val rules = ruleRepository.getAllRulesSnapshot()
            val map = mutableMapOf<String, RuleEntity>()

            for (rule in rules) {
                val key = when (rule.entityType) {
                    EntityType.APP -> "${EntityType.APP}:${rule.packageName}"
                    EntityType.SENDER -> "${EntityType.SENDER}:${rule.senderKey}"
                    else -> continue
                }
                // User rules take precedence — only store if no user rule already exists
                val existing = map[key]
                if (existing == null || (rule.source == RuleSource.USER && existing.source == RuleSource.LEARNED)) {
                    map[key] = rule
                }
            }

            ruleCache = map
            lastRefresh = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading rule cache", e)
        }
    }
}
