package com.notivault.data.model

import kotlinx.serialization.Serializable

/** Summary of notifications per app for the home screen */
data class AppSummary(
    val packageName: String,
    val appLabel: String,
    val count: Int,
    val lastText: String?,
    val lastSender: String?,
    val lastPostTime: Long
)

/** Minimal app information for dropdowns */
data class SimpleAppInfo(
    val packageName: String,
    val appLabel: String
)

/** Summary of notifications per sender within an app */
data class SenderSummary(
    val senderKey: String,
    val senderName: String,
    val count: Int,
    val lastPostTime: Long
)

/** Frequent entity detected by the learning engine */
data class FrequentEntity(
    val entityKey: String,
    val entityType: String,
    val packageName: String,
    val totalNotifications: Int,
    val totalClicks: Int,
    val totalDismisses: Int
)

/** Result from the priority engine */
data class PriorityResult(
    val tier: String,
    val matchedRuleId: Long?
)

/** Normalized notification record — common format from all adapters */
data class NormalizedRecord(
    val notificationKey: String,
    val packageName: String,
    val appLabel: String,
    val senderName: String,
    val senderKey: String,
    val isGroup: Boolean,
    val conversationName: String?,
    val title: String,
    val text: String,
    val subText: String?,
    val channelId: String?,
    val channelImportance: Int,
    val category: String?,
    val isConversation: Boolean,
    val postTime: Long,
    val postTimeIso: String,
    val messageTimestamp: Long?
)

/** Tiers for notification classification */
object Tier {
    const val HIGH = "HIGH"
    const val NORMAL = "NORMAL"
    const val IGNORE = "IGNORE"
}

/** Entity types for rules and suggestions */
object EntityType {
    const val APP = "APP"
    const val SENDER = "SENDER"
}

/** Rule sources */
object RuleSource {
    const val USER = "USER"
    const val LEARNED = "LEARNED"
}

/** Suggestion statuses */
object SuggestionStatus {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val REJECTED = "REJECTED"
    const val NEVER_ASK = "NEVER_ASK"
}

/** JSON export data classes */
@Serializable
data class ExportData(
    val exportMetadata: ExportMetadata,
    val notifications: List<ExportNotification>
)

@Serializable
data class ExportMetadata(
    val appName: String = "NotiVault",
    val exportDate: String,
    val dateRangeFrom: String,
    val dateRangeTo: String,
    val filterTier: String?,
    val filterApps: List<String>?,
    val totalNotifications: Int
)

@Serializable
data class ExportNotification(
    val id: Long,
    val appPackage: String,
    val appLabel: String,
    val senderName: String,
    val isGroup: Boolean,
    val conversationName: String?,
    val title: String,
    val text: String,
    val channelImportance: Int,
    val isConversation: Boolean,
    val tier: String,
    val postedAt: String,
    val postedAtEpoch: Long,
    val wasClicked: Boolean
)

/** Date group header for UI display */
data class DateGroup(
    val dateLabel: String,
    val dateKey: String
)

/** Health status enum */
enum class HealthStatus {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN
}

/** Stats for settings screen */
data class StorageStats(
    val totalNotifications: Long,
    val highPriorityCount: Long,
    val normalCount: Long,
    val rulesCount: Long,
    val dbSizeBytes: Long
)
