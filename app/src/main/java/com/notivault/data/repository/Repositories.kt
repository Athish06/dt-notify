package com.notivault.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.notivault.data.dao.*
import com.notivault.data.entity.*
import com.notivault.data.model.*
import com.notivault.util.Constants
import com.notivault.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    private val pagingConfig = PagingConfig(
        pageSize = 30,
        prefetchDistance = 10,
        enablePlaceholders = false
    )

    fun getHighPriorityApps(): Flow<List<AppSummary>> =
        notificationDao.getHighPriorityApps()

    fun getHighPriorityByApp(packageName: String): Flow<PagingData<NotificationEntity>> =
        Pager(config = pagingConfig) { notificationDao.getHighPriorityByApp(packageName) }.flow

    fun getHighPriorityByDate(date: LocalDate): Flow<PagingData<NotificationEntity>> {
        val start = TimeUtils.startOfDayEpoch(date)
        val end = TimeUtils.endOfDayEpoch(date)
        return Pager(config = pagingConfig) { notificationDao.getHighPriorityByDate(start, end) }.flow
    }

    fun getAllRecent(): Flow<PagingData<NotificationEntity>> {
        val since = System.currentTimeMillis() - Duration.ofHours(Constants.NORMAL_RETENTION_HOURS).toMillis()
        return Pager(config = pagingConfig) { notificationDao.getAllRecent(since) }.flow
    }

    fun getFilteredActivity(appFilter: String?, priorityFilter: String?, timeFilter: Long): Flow<PagingData<NotificationEntity>> =
        Pager(config = pagingConfig) { notificationDao.getFilteredActivity(appFilter, priorityFilter, timeFilter) }.flow

    fun searchHighPriority(query: String): Flow<PagingData<NotificationEntity>> =
        Pager(config = pagingConfig) { notificationDao.searchHighPriority(query) }.flow

    suspend fun insert(notification: NotificationEntity): Long =
        notificationDao.insert(notification)

    suspend fun insertAll(notifications: List<NotificationEntity>) =
        notificationDao.insertAll(notifications)

    suspend fun getLatestByKey(key: String): NotificationEntity? =
        notificationDao.getLatestByKey(key)

    suspend fun updateRemoval(key: String, reason: Int, clicked: Boolean) =
        notificationDao.updateRemoval(key, reason, clicked)

    suspend fun purgeNormal(cutoff: Long): Int = notificationDao.purgeNormal(cutoff)
    suspend fun purgeHigh(cutoff: Long): Int = notificationDao.purgeHigh(cutoff)

    suspend fun getTotalCount(): Long = notificationDao.getTotalCount()
    suspend fun getHighCount(): Long = notificationDao.getHighCount()
    suspend fun getNormalCount(): Long = notificationDao.getNormalCount()
    suspend fun getLastNotificationTime(): Long? = notificationDao.getLastNotificationTime()

    suspend fun getForExport(from: Long, to: Long): List<NotificationEntity> =
        notificationDao.getForExport(from, to)

    suspend fun getForExportFiltered(from: Long, to: Long, tier: String): List<NotificationEntity> =
        notificationDao.getForExportFiltered(from, to, tier)

    suspend fun getDistinctSendersForApp(packageName: String): List<com.notivault.data.model.SenderSummary> =
        notificationDao.getDistinctSendersForApp(packageName)

    suspend fun getNotificationById(id: Long): NotificationEntity? =
        notificationDao.getNotificationById(id)

    suspend fun deleteAll() = notificationDao.deleteAll()
    
    suspend fun deleteNotification(id: Long) = notificationDao.deleteById(id)
    
    suspend fun updateTierForApp(packageName: String, tier: String) = notificationDao.updateTierForApp(packageName, tier)
    
    suspend fun updateTierForSender(senderKey: String, tier: String) = notificationDao.updateTierForSender(senderKey, tier)
    
    suspend fun getAllDistinctApps(): List<com.notivault.data.model.SimpleAppInfo> = notificationDao.getAllDistinctApps()
}

@Singleton
class RuleRepository @Inject constructor(
    private val ruleDao: RuleDao
) {
    fun getAllRules(): Flow<List<RuleEntity>> = ruleDao.getAllRules()

    suspend fun getAllRulesSnapshot(): List<RuleEntity> = ruleDao.getAllRulesSnapshot()

    suspend fun getAppRule(packageName: String): RuleEntity? = ruleDao.getAppRule(packageName)

    suspend fun getSenderRule(senderKey: String): RuleEntity? = ruleDao.getSenderRule(senderKey)

    suspend fun insert(rule: RuleEntity): Long = ruleDao.insert(rule)

    suspend fun update(rule: RuleEntity) = ruleDao.update(rule)

    suspend fun delete(rule: RuleEntity) = ruleDao.delete(rule)

    suspend fun deleteById(id: Long) = ruleDao.deleteById(id)

    suspend fun getRulesCount(): Long = ruleDao.getRulesCount()
}

@Singleton
class DailyCountRepository @Inject constructor(
    private val dailyCountDao: DailyCountDao
) {
    suspend fun incrementNotificationCount(entityKey: String, entityType: String, packageName: String) {
        val date = TimeUtils.todayDateString()
        val existing = dailyCountDao.getByKeyAndDate(entityKey, date)
        if (existing != null) {
            dailyCountDao.upsert(existing.copy(notificationCount = existing.notificationCount + 1))
        } else {
            dailyCountDao.upsert(
                DailyCountEntity(
                    entityKey = entityKey,
                    entityType = entityType,
                    packageName = packageName,
                    date = date,
                    notificationCount = 1
                )
            )
        }
    }

    suspend fun incrementClickCount(entityKey: String) {
        val date = TimeUtils.todayDateString()
        val existing = dailyCountDao.getByKeyAndDate(entityKey, date)
        if (existing != null) {
            dailyCountDao.upsert(existing.copy(clickCount = existing.clickCount + 1))
        }
    }

    suspend fun incrementDismissCount(entityKey: String) {
        val date = TimeUtils.todayDateString()
        val existing = dailyCountDao.getByKeyAndDate(entityKey, date)
        if (existing != null) {
            dailyCountDao.upsert(existing.copy(dismissCount = existing.dismissCount + 1))
        }
    }

    suspend fun getFrequentEntities(threshold: Int): List<FrequentEntity> {
        val threeDaysAgo = TimeUtils.daysAgoDateString(3)
        return dailyCountDao.getFrequentEntities(threeDaysAgo, threshold)
    }

    suspend fun purgeOld(cutoff: String): Int = dailyCountDao.purgeOld(cutoff)
}

@Singleton
class SuggestionRepository @Inject constructor(
    private val suggestionDao: SuggestionDao
) {
    fun getPendingSuggestions(): Flow<List<SuggestionEntity>> =
        suggestionDao.getPendingSuggestions()

    fun getPendingCount(): Flow<Int> = suggestionDao.getPendingCount()

    suspend fun getByEntityKey(entityKey: String): SuggestionEntity? =
        suggestionDao.getByEntityKey(entityKey)

    suspend fun getSuggestionsCreatedToday(): Int {
        val todayStart = TimeUtils.startOfDayEpoch()
        return suggestionDao.getSuggestionsCreatedToday(todayStart)
    }

    suspend fun insert(suggestion: SuggestionEntity): Long =
        suggestionDao.insert(suggestion)

    suspend fun accept(id: Long) {
        suggestionDao.updateStatus(id, SuggestionStatus.ACCEPTED, System.currentTimeMillis())
    }

    suspend fun reject(id: Long) {
        val cooldownUntil = System.currentTimeMillis() +
            Duration.ofDays(Constants.REJECTION_COOLDOWN_DAYS).toMillis()
        suggestionDao.updateStatusWithCooldown(
            id, SuggestionStatus.REJECTED, System.currentTimeMillis(), cooldownUntil
        )
    }

    suspend fun neverAsk(id: Long) {
        suggestionDao.updateStatus(id, SuggestionStatus.NEVER_ASK, System.currentTimeMillis())
    }

    suspend fun dismiss(id: Long) {
        val cooldownUntil = System.currentTimeMillis() +
            Duration.ofDays(Constants.DISMISS_COOLDOWN_DAYS).toMillis()
        suggestionDao.updateStatusWithCooldown(
            id, SuggestionStatus.REJECTED, System.currentTimeMillis(), cooldownUntil
        )
    }
}

@Singleton
class HealthRepository @Inject constructor(
    private val serviceHealthDao: ServiceHealthDao
) {
    fun getRecentEvents(): Flow<List<ServiceHealthEntity>> =
        serviceHealthDao.getRecentEvents()

    suspend fun logEvent(event: String, note: String? = null) {
        serviceHealthDao.insert(ServiceHealthEntity(event = event, note = note))
    }

    suspend fun getLatestEvent(): ServiceHealthEntity? =
        serviceHealthDao.getLatestEvent()

    suspend fun purgeOld(cutoff: Long): Int =
        serviceHealthDao.purgeOld(cutoff)
}
