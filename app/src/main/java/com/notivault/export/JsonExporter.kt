package com.notivault.export

import android.content.Context
import com.notivault.data.entity.NotificationEntity
import com.notivault.data.model.ExportData
import com.notivault.data.model.ExportMetadata
import com.notivault.data.model.ExportNotification
import com.notivault.data.repository.NotificationRepository
import com.notivault.util.TimeUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON Exporter — produces structured JSON files with date/time properly formatted.
 *
 * Export format includes:
 * - Metadata: app name, export date, date range, filters, total count
 * - Notifications: full record with ISO timestamps
 */
@Singleton
class JsonExporter @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Export notifications to a JSON string.
     *
     * @param from Start of date range (epoch millis)
     * @param to End of date range (epoch millis)
     * @param tier Optional tier filter ("HIGH", "NORMAL", or null for all)
     * @return Pair of JSON string and notification count
     */
    suspend fun export(from: Long, to: Long, tier: String? = null): Pair<String, Int> {
        val notifications = if (tier != null) {
            notificationRepository.getForExportFiltered(from, to, tier)
        } else {
            notificationRepository.getForExport(from, to)
        }

        val exportData = ExportData(
            exportMetadata = ExportMetadata(
                exportDate = TimeUtils.epochToIso(System.currentTimeMillis()),
                dateRangeFrom = TimeUtils.epochToIso(from),
                dateRangeTo = TimeUtils.epochToIso(to),
                filterTier = tier,
                filterApps = null,
                totalNotifications = notifications.size
            ),
            notifications = notifications.map { it.toExportNotification() }
        )

        val jsonString = json.encodeToString(exportData)
        return Pair(jsonString, notifications.size)
    }

    /**
     * Export and write to a file in app-specific storage.
     *
     * @return The file path of the exported file
     */
    suspend fun exportToFile(
        context: Context,
        from: Long,
        to: Long,
        tier: String? = null
    ): Result<File> {
        return try {
            val (jsonString, count) = export(from, to, tier)

            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val fileName = "notivault_export_$timestamp.json"
            val file = File(exportDir, fileName)
            file.writeText(jsonString)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun NotificationEntity.toExportNotification() = ExportNotification(
        id = id,
        appPackage = packageName,
        appLabel = appLabel,
        senderName = senderName,
        isGroup = isGroup,
        conversationName = conversationName,
        title = title,
        text = text,
        channelImportance = channelImportance,
        isConversation = isConversation,
        tier = tier,
        postedAt = postTimeIso,
        postedAtEpoch = postTime,
        wasClicked = wasClicked
    )
}
