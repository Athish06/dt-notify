package com.notivault.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.notivault.data.dao.*
import com.notivault.data.entity.*

@Database(
    entities = [
        NotificationEntity::class,
        RuleEntity::class,
        DailyCountEntity::class,
        SuggestionEntity::class,
        ServiceHealthEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NotiVaultDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun ruleDao(): RuleDao
    abstract fun dailyCountDao(): DailyCountDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun serviceHealthDao(): ServiceHealthDao
}
