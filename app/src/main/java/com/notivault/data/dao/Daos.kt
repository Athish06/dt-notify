package com.notivault.data.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.notivault.data.entity.*
import com.notivault.data.model.AppSummary
import com.notivault.data.model.FrequentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("""
        SELECT package_name AS packageName, app_label AS appLabel, 
               COUNT(*) AS count,
               (SELECT text FROM notifications n2 WHERE n2.package_name = n.package_name AND n2.tier = 'HIGH' ORDER BY n2.post_time DESC LIMIT 1) AS lastText,
               (SELECT sender_name FROM notifications n3 WHERE n3.package_name = n.package_name AND n3.tier = 'HIGH' ORDER BY n3.post_time DESC LIMIT 1) AS lastSender,
               MAX(post_time) AS lastPostTime
        FROM notifications n
        WHERE tier = 'HIGH'
        GROUP BY package_name
        ORDER BY MAX(post_time) DESC
    """)
    fun getHighPriorityApps(): Flow<List<AppSummary>>

    @Query("""
        SELECT * FROM notifications
        WHERE tier = 'HIGH' AND package_name = :packageName
        ORDER BY post_time DESC
    """)
    fun getHighPriorityByApp(packageName: String): PagingSource<Int, NotificationEntity>

    @Query("""
        SELECT * FROM notifications
        WHERE tier = 'HIGH'
          AND post_time >= :startOfDay
          AND post_time < :endOfDay
        ORDER BY post_time DESC
    """)
    fun getHighPriorityByDate(startOfDay: Long, endOfDay: Long): PagingSource<Int, NotificationEntity>

    @Query("""
        SELECT * FROM notifications
        WHERE post_time >= :since
        ORDER BY post_time DESC
    """)
    fun getAllRecent(since: Long): PagingSource<Int, NotificationEntity>

    @Query("""
        SELECT * FROM notifications
        WHERE notification_key = :key
        ORDER BY captured_at DESC
        LIMIT 1
    """)
    suspend fun getLatestByKey(key: String): NotificationEntity?

    @Query("""
        SELECT * FROM notifications
        WHERE (title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%' OR sender_name LIKE '%' || :query || '%')
          AND tier = 'HIGH'
        ORDER BY post_time DESC
    """)
    fun searchHighPriority(query: String): PagingSource<Int, NotificationEntity>

    @Query("UPDATE notifications SET removal_reason = :reason, was_clicked = :clicked WHERE notification_key = :key AND removal_reason IS NULL")
    suspend fun updateRemoval(key: String, reason: Int, clicked: Boolean)

    @Query("DELETE FROM notifications WHERE tier = 'NORMAL' AND post_time < :cutoff")
    suspend fun purgeNormal(cutoff: Long): Int

    @Query("DELETE FROM notifications WHERE tier = 'HIGH' AND post_time < :cutoff")
    suspend fun purgeHigh(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getTotalCount(): Long

    @Query("SELECT COUNT(*) FROM notifications WHERE tier = 'HIGH'")
    suspend fun getHighCount(): Long

    @Query("SELECT COUNT(*) FROM notifications WHERE tier = 'NORMAL'")
    suspend fun getNormalCount(): Long

    @Query("""
        SELECT * FROM notifications
        WHERE post_time >= :from AND post_time <= :to
        ORDER BY post_time DESC
    """)
    suspend fun getForExport(from: Long, to: Long): List<NotificationEntity>

    @Query("""
        SELECT * FROM notifications
        WHERE post_time >= :from AND post_time <= :to AND tier = :tier
        ORDER BY post_time DESC
    """)
    suspend fun getForExportFiltered(from: Long, to: Long, tier: String): List<NotificationEntity>

    @Query("SELECT MAX(post_time) FROM notifications")
    suspend fun getLastNotificationTime(): Long?

    @Query("""
        SELECT * FROM notifications 
        WHERE (:appFilter IS NULL OR package_name = :appFilter)
          AND (:priorityFilter IS NULL OR tier = :priorityFilter)
          AND post_time >= :timeFilter
        ORDER BY post_time DESC
    """)
    fun getFilteredActivity(appFilter: String?, priorityFilter: String?, timeFilter: Long): PagingSource<Int, NotificationEntity>

    @Query("""
        SELECT sender_key AS senderKey, sender_name AS senderName, 
               COUNT(*) AS count, MAX(post_time) AS lastPostTime
        FROM notifications
        WHERE package_name = :packageName
        GROUP BY sender_key
        ORDER BY MAX(post_time) DESC
    """)
    suspend fun getDistinctSendersForApp(packageName: String): List<com.notivault.data.model.SenderSummary>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): NotificationEntity?

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE notifications SET tier = :tier WHERE package_name = :packageName")
    suspend fun updateTierForApp(packageName: String, tier: String)

    @Query("UPDATE notifications SET tier = :tier WHERE sender_key = :senderKey")
    suspend fun updateTierForSender(senderKey: String, tier: String)

    @Query("SELECT DISTINCT package_name AS packageName, app_label AS appLabel FROM notifications ORDER BY app_label ASC")
    suspend fun getAllDistinctApps(): List<com.notivault.data.model.SimpleAppInfo>

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}

@Dao
interface RuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("SELECT * FROM rules ORDER BY source ASC, updated_at DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE entity_type = 'APP' AND package_name = :packageName LIMIT 1")
    suspend fun getAppRule(packageName: String): RuleEntity?

    @Query("SELECT * FROM rules WHERE entity_type = 'SENDER' AND sender_key = :senderKey LIMIT 1")
    suspend fun getSenderRule(senderKey: String): RuleEntity?

    @Query("SELECT * FROM rules")
    suspend fun getAllRulesSnapshot(): List<RuleEntity>

    @Query("SELECT COUNT(*) FROM rules")
    suspend fun getRulesCount(): Long

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface DailyCountDao {

    @Query("""
        SELECT entity_key AS entityKey, entity_type AS entityType, package_name AS packageName,
               SUM(notification_count) AS totalNotifications,
               SUM(click_count) AS totalClicks,
               SUM(dismiss_count) AS totalDismisses
        FROM daily_counts
        WHERE date >= :threeDaysAgo
        GROUP BY entity_key
        HAVING totalNotifications >= :threshold
    """)
    suspend fun getFrequentEntities(threeDaysAgo: String, threshold: Int): List<FrequentEntity>

    @Query("""
        SELECT * FROM daily_counts 
        WHERE entity_key = :entityKey AND date = :date 
        LIMIT 1
    """)
    suspend fun getByKeyAndDate(entityKey: String, date: String): DailyCountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(count: DailyCountEntity)

    @Query("DELETE FROM daily_counts WHERE date < :cutoff")
    suspend fun purgeOld(cutoff: String): Int
}

@Dao
interface SuggestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suggestion: SuggestionEntity): Long

    @Update
    suspend fun update(suggestion: SuggestionEntity)

    @Query("SELECT * FROM suggestions WHERE status = 'PENDING' ORDER BY score DESC")
    fun getPendingSuggestions(): Flow<List<SuggestionEntity>>

    @Query("SELECT COUNT(*) FROM suggestions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM suggestions WHERE entity_key = :entityKey LIMIT 1")
    suspend fun getByEntityKey(entityKey: String): SuggestionEntity?

    @Query("""
        SELECT COUNT(*) FROM suggestions 
        WHERE status = 'PENDING' 
          AND created_at >= :todayStart
    """)
    suspend fun getSuggestionsCreatedToday(todayStart: Long): Int

    @Query("UPDATE suggestions SET status = :status, responded_at = :respondedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, respondedAt: Long)

    @Query("UPDATE suggestions SET status = :status, responded_at = :respondedAt, cooldown_until = :cooldownUntil WHERE id = :id")
    suspend fun updateStatusWithCooldown(id: Long, status: String, respondedAt: Long, cooldownUntil: Long)
}

@Dao
interface ServiceHealthDao {

    @Insert
    suspend fun insert(event: ServiceHealthEntity)

    @Query("SELECT * FROM service_health ORDER BY timestamp DESC LIMIT 20")
    fun getRecentEvents(): Flow<List<ServiceHealthEntity>>

    @Query("SELECT * FROM service_health ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEvent(): ServiceHealthEntity?

    @Query("DELETE FROM service_health WHERE timestamp < :cutoff")
    suspend fun purgeOld(cutoff: Long): Int
}
