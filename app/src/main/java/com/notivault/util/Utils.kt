package com.notivault.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object TimeUtils {

    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun epochToIso(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(isoFormatter)
    }

    fun todayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }

    fun daysAgoDateString(days: Long): String {
        return LocalDate.now().minusDays(days).format(dateFormatter)
    }

    fun startOfDayEpoch(date: LocalDate = LocalDate.now()): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun endOfDayEpoch(date: LocalDate = LocalDate.now()): Long {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
    }

    fun formatRelativeTime(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMillis

        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 172_800_000 -> "Yesterday"
            else -> {
                val date = Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                date.format(displayDateFormatter)
            }
        }
    }

    fun formatTime(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(displayTimeFormatter)
    }

    fun formatDateHeader(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()

        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(displayDateFormatter)
        }
    }

    fun formatDateKey(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1048576 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / 1048576.0)
        }
    }

    fun formatCount(count: Long): String {
        return when {
            count < 1000 -> count.toString()
            count < 1_000_000 -> String.format("%.1fK", count / 1000.0)
            else -> String.format("%.1fM", count / 1_000_000.0)
        }
    }
}

object Constants {
    // Retention periods
    const val HIGH_RETENTION_DAYS = 14L
    const val NORMAL_RETENTION_HOURS = 24L
    const val DAILY_COUNTS_RETENTION_DAYS = 7L
    const val HEALTH_LOG_RETENTION_DAYS = 7L

    // Learning thresholds
    const val DEFAULT_FREQUENCY_THRESHOLD = 8
    const val DEFAULT_SCORE_THRESHOLD = 5.0f
    const val MAX_DAILY_SUGGESTIONS = 2
    const val REJECTION_COOLDOWN_DAYS = 30L
    const val DISMISS_COOLDOWN_DAYS = 7L

    // Scoring weights
    const val FREQ_WEIGHT = 3.0f
    const val FREQ_MAX_SCORE = 3.0f
    const val FREQ_DIVISOR = 5.0f
    const val ENGAGEMENT_WEIGHT = 3.0f
    const val CONVERSATION_BONUS = 2.0f
    const val IMPORTANCE_DIVISOR = 2.0f

    // Default ignore list (system apps that spam notifications)
    val DEFAULT_IGNORE_PACKAGES = setOf(
        "com.google.android.gms",
        "com.android.systemui",
        "com.android.vending",
        "com.google.android.deskclock",
        "com.google.android.apps.wellbeing",
        "com.miui.securitycenter",
        "com.coloros.safecenter",
        "com.samsung.android.forest",
        "android",
        "com.android.providers.downloads"
    )

    // DataStore keys
    const val PREFS_NAME = "notivault_prefs"
    const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    const val KEY_FREQ_THRESHOLD = "freq_threshold"
    const val KEY_SCORE_THRESHOLD = "score_threshold"
    const val KEY_MAX_DAILY_SUGGESTIONS = "max_daily_suggestions"
    const val KEY_TOTAL_CAPTURED = "total_captured"
    const val KEY_LAST_NOTIFICATION_TIME = "last_notification_time"

    // Database
    const val DB_NAME = "notivault.db"
}
