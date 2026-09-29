package com.notivault.engine

import android.util.Log
import com.notivault.data.entity.RuleEntity
import com.notivault.data.entity.SuggestionEntity
import com.notivault.data.model.*
import com.notivault.data.repository.DailyCountRepository
import com.notivault.data.repository.RuleRepository
import com.notivault.data.repository.SuggestionRepository
import com.notivault.util.Constants
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * LearningEngine — detects frequently-used apps/senders and suggests
 * promoting them to HIGH priority.
 *
 * Scoring formula (0.0 to ~10.0):
 *   score = min(freq/5, 3.0) + engagement*3.0 + conversation*2.0 + importance/2.0
 *
 * Why frequency alone isn't enough:
 * - A food delivery app sends 10 promos/day → high freq, zero engagement → score ~4.0
 * - A colleague's WhatsApp: 12 msgs/3d, 60% clicked → score ~7.7 → suggestion shown
 *
 * Rate limiting:
 * - Max 2 suggestions per day
 * - 30-day cooldown after rejection
 * - 7-day cooldown after dismiss
 * - NEVER_ASK is permanent
 */
@Singleton
class LearningEngine @Inject constructor(
    private val dailyCountRepository: DailyCountRepository,
    private val suggestionRepository: SuggestionRepository,
    private val ruleRepository: RuleRepository
) {
    companion object {
        private const val TAG = "LearningEngine"
    }

    /**
     * Evaluate all unruled entities and generate suggestions where appropriate.
     * Called after the daily cleanup worker and when the app opens.
     */
    suspend fun evaluate() {
        try {
            val threshold = Constants.DEFAULT_FREQUENCY_THRESHOLD
            val frequent = dailyCountRepository.getFrequentEntities(threshold)

            if (frequent.isEmpty()) return

            val suggestionsToday = suggestionRepository.getSuggestionsCreatedToday()
            if (suggestionsToday >= Constants.MAX_DAILY_SUGGESTIONS) {
                Log.d(TAG, "Max daily suggestions reached ($suggestionsToday)")
                return
            }

            var created = 0
            val now = System.currentTimeMillis()

            for (entity in frequent) {
                if (created + suggestionsToday >= Constants.MAX_DAILY_SUGGESTIONS) break

                // Skip if there's already a rule for this entity
                val hasRule = when (entity.entityType) {
                    EntityType.APP -> ruleRepository.getAppRule(entity.packageName) != null
                    EntityType.SENDER -> ruleRepository.getSenderRule(entity.entityKey) != null
                    else -> false
                }
                if (hasRule) continue

                // Check existing suggestion status
                val existing = suggestionRepository.getByEntityKey(entity.entityKey)
                if (existing != null) {
                    when (existing.status) {
                        SuggestionStatus.NEVER_ASK -> continue
                        SuggestionStatus.ACCEPTED -> continue
                        SuggestionStatus.PENDING -> continue
                        SuggestionStatus.REJECTED -> {
                            if (existing.cooldownUntil != null && existing.cooldownUntil > now) continue
                        }
                    }
                }

                // Compute score
                val score = computeScore(entity)
                if (score < Constants.DEFAULT_SCORE_THRESHOLD) continue

                // Create suggestion
                val displayName = entity.entityKey
                    .substringAfter(':')
                    .replaceFirstChar { it.uppercase() }

                suggestionRepository.insert(
                    SuggestionEntity(
                        entityKey = entity.entityKey,
                        entityType = entity.entityType,
                        packageName = entity.packageName,
                        displayName = displayName,
                        suggestedPriority = Tier.HIGH,
                        score = score
                    )
                )

                created++
                Log.d(TAG, "Created suggestion for ${entity.entityKey} with score $score")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during evaluation", e)
        }
    }

    /**
     * Handle user accepting a suggestion — create a LEARNED rule.
     */
    suspend fun acceptSuggestion(suggestion: SuggestionEntity) {
        suggestionRepository.accept(suggestion.id)

        val rule = RuleEntity(
            entityType = suggestion.entityType,
            packageName = suggestion.packageName,
            senderKey = if (suggestion.entityType == EntityType.SENDER) suggestion.entityKey else null,
            displayName = suggestion.displayName,
            priority = suggestion.suggestedPriority,
            source = RuleSource.LEARNED
        )

        ruleRepository.insert(rule)
        Log.d(TAG, "Accepted suggestion for ${suggestion.entityKey}, created LEARNED rule")
    }

    /**
     * Compute a score for an entity based on frequency, engagement, and metadata.
     *
     * Score breakdown:
     * - Frequency: min(total/5, 3.0) — caps at 3 points
     * - Engagement: (clicks / total) * 3.0 — caps at 3 points
     * - Conversation bonus: 2.0 if it's a messaging-style notification
     * - Channel importance: importance / 2.0 — max ~2.0 points
     *
     * Maximum theoretical score: ~10.0
     */
    private fun computeScore(entity: FrequentEntity): Float {
        val total = entity.totalNotifications.toFloat()
        if (total <= 0) return 0f

        // Frequency component (max 3.0)
        val freqScore = min(total / Constants.FREQ_DIVISOR, Constants.FREQ_MAX_SCORE)

        // Engagement component (max 3.0)
        val engagementRatio = entity.totalClicks.toFloat() / total
        val engagementScore = engagementRatio * Constants.ENGAGEMENT_WEIGHT

        // Conversation bonus (2.0 if entity type suggests messaging)
        // We can infer from entity key: WhatsApp/Telegram senders are conversations
        val conversationBonus = if (isLikelyConversation(entity)) Constants.CONVERSATION_BONUS else 0f

        // We don't have channel importance in daily_counts, so skip that component
        // and compensate by slightly lowering the threshold

        return freqScore + engagementScore + conversationBonus
    }

    private fun isLikelyConversation(entity: FrequentEntity): Boolean {
        val messagingPackages = setOf(
            "com.whatsapp", "com.whatsapp.w4b",
            "org.telegram.messenger", "org.telegram.messenger.web",
            "com.google.android.apps.messaging", "com.samsung.android.messaging",
            "com.discord", "com.slack",
            "com.facebook.orca", "com.instagram.android"
        )
        return entity.packageName in messagingPackages
    }
}
