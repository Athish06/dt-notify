package com.notivault.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["package_name", "post_time"]),
        Index(value = ["sender_key", "post_time"]),
        Index(value = ["tier", "post_time"]),
        Index(value = ["notification_key"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "notification_key")
    val notificationKey: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_label")
    val appLabel: String,

    @ColumnInfo(name = "sender_key")
    val senderKey: String,

    @ColumnInfo(name = "sender_name")
    val senderName: String,

    @ColumnInfo(name = "is_group")
    val isGroup: Boolean = false,

    @ColumnInfo(name = "conversation_name")
    val conversationName: String? = null,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "sub_text")
    val subText: String? = null,

    @ColumnInfo(name = "channel_id")
    val channelId: String? = null,

    @ColumnInfo(name = "channel_importance")
    val channelImportance: Int = 3,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "is_conversation")
    val isConversation: Boolean = false,

    @ColumnInfo(name = "tier")
    val tier: String,

    @ColumnInfo(name = "matched_rule_id")
    val matchedRuleId: Long? = null,

    @ColumnInfo(name = "post_time")
    val postTime: Long,

    @ColumnInfo(name = "post_time_iso")
    val postTimeIso: String,

    @ColumnInfo(name = "captured_at")
    val capturedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "removal_reason")
    val removalReason: Int? = null,

    @ColumnInfo(name = "was_clicked")
    val wasClicked: Boolean = false
)

@Entity(
    tableName = "rules",
    indices = [
        Index(value = ["entity_type", "package_name", "sender_key"], unique = true)
    ]
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "sender_key")
    val senderKey: String? = null,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "priority")
    val priority: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_counts",
    indices = [
        Index(value = ["entity_key", "date"], unique = true)
    ]
)
data class DailyCountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "entity_key")
    val entityKey: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "notification_count")
    val notificationCount: Int = 0,

    @ColumnInfo(name = "click_count")
    val clickCount: Int = 0,

    @ColumnInfo(name = "dismiss_count")
    val dismissCount: Int = 0
)

@Entity(
    tableName = "suggestions",
    indices = [
        Index(value = ["entity_key"], unique = true)
    ]
)
data class SuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "entity_key")
    val entityKey: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "suggested_priority")
    val suggestedPriority: String = "HIGH",

    @ColumnInfo(name = "score")
    val score: Float,

    @ColumnInfo(name = "status")
    val status: String = "PENDING",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "responded_at")
    val respondedAt: Long? = null,

    @ColumnInfo(name = "cooldown_until")
    val cooldownUntil: Long? = null
)

@Entity(tableName = "service_health")
data class ServiceHealthEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "event")
    val event: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "note")
    val note: String? = null
)
