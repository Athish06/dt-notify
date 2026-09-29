package com.notivault.service.adapter

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.StatusBarNotification
import com.notivault.data.model.NormalizedRecord
import com.notivault.util.TimeUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Strategy interface for app-specific notification parsing.
 * Each adapter knows how to extract structured data from a specific app's notifications.
 */
interface NotificationAdapter {
    fun canHandle(packageName: String): Boolean
    fun normalize(sbn: StatusBarNotification, ranking: Ranking): List<NormalizedRecord>
}

// ─────────────────────────────────────────────────────────────────────────────
// WhatsApp Adapter
// ─────────────────────────────────────────────────────────────────────────────

class WhatsAppAdapter : NotificationAdapter {

    companion object {
        private val PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
    }

    override fun canHandle(packageName: String) = packageName in PACKAGES

    override fun normalize(sbn: StatusBarNotification, ranking: Ranking): List<NormalizedRecord> {
        val extras = sbn.notification.extras
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        val appLabel = if (sbn.packageName == "com.whatsapp.w4b") "WhatsApp Business" else "WhatsApp"

        if (messages != null && messages.isNotEmpty()) {
            return messages.mapNotNull { msg ->
                val bundle = msg as? Bundle ?: return@mapNotNull null
                val sender = bundle.getCharSequence("sender")?.toString() ?: "Unknown"
                val text = bundle.getCharSequence("text")?.toString() ?: ""
                val time = bundle.getLong("time", sbn.postTime)
                val isGroup = conversationTitle != null

                NormalizedRecord(
                    notificationKey = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = appLabel,
                    senderName = sender,
                    senderKey = "${sbn.packageName}:${sender.lowercase().trim()}",
                    isGroup = isGroup,
                    conversationName = conversationTitle,
                    title = if (isGroup) "$conversationTitle - $sender" else sender,
                    text = text,
                    subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                    channelId = sbn.notification.channelId,
                    channelImportance = ranking.importance,
                    category = sbn.notification.category,
                    isConversation = true,
                    postTime = sbn.postTime,
                    postTimeIso = TimeUtils.epochToIso(sbn.postTime),
                    messageTimestamp = time
                )
            }
        }

        // Fallback for non-MessagingStyle
        return listOf(buildFallback(sbn, ranking, appLabel))
    }

    private fun buildFallback(sbn: StatusBarNotification, ranking: Ranking, appLabel: String): NormalizedRecord {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        return NormalizedRecord(
            notificationKey = sbn.key,
            packageName = sbn.packageName,
            appLabel = appLabel,
            senderName = title,
            senderKey = "${sbn.packageName}:${title.lowercase().trim()}",
            isGroup = false,
            conversationName = null,
            title = title,
            text = text,
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            channelId = sbn.notification.channelId,
            channelImportance = ranking.importance,
            category = sbn.notification.category,
            isConversation = false,
            postTime = sbn.postTime,
            postTimeIso = TimeUtils.epochToIso(sbn.postTime),
            messageTimestamp = null
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Telegram Adapter
// ─────────────────────────────────────────────────────────────────────────────

class TelegramAdapter : NotificationAdapter {

    companion object {
        private val PACKAGES = setOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")
    }

    override fun canHandle(packageName: String) = packageName in PACKAGES

    override fun normalize(sbn: StatusBarNotification, ranking: Ranking): List<NormalizedRecord> {
        val extras = sbn.notification.extras
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        if (messages != null && messages.isNotEmpty()) {
            return messages.mapNotNull { msg ->
                val bundle = msg as? Bundle ?: return@mapNotNull null
                val sender = bundle.getCharSequence("sender")?.toString() ?: "Unknown"
                val text = bundle.getCharSequence("text")?.toString() ?: ""
                val time = bundle.getLong("time", sbn.postTime)
                val isGroup = conversationTitle != null

                NormalizedRecord(
                    notificationKey = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = "Telegram",
                    senderName = sender,
                    senderKey = "${sbn.packageName}:${sender.lowercase().trim()}",
                    isGroup = isGroup,
                    conversationName = conversationTitle,
                    title = if (isGroup) "$conversationTitle - $sender" else sender,
                    text = text,
                    subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                    channelId = sbn.notification.channelId,
                    channelImportance = ranking.importance,
                    category = sbn.notification.category,
                    isConversation = true,
                    postTime = sbn.postTime,
                    postTimeIso = TimeUtils.epochToIso(sbn.postTime),
                    messageTimestamp = time
                )
            }
        }

        // Fallback
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        return listOf(
            NormalizedRecord(
                notificationKey = sbn.key,
                packageName = sbn.packageName,
                appLabel = "Telegram",
                senderName = title,
                senderKey = "${sbn.packageName}:${title.lowercase().trim()}",
                isGroup = false,
                conversationName = null,
                title = title,
                text = text,
                subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                channelId = sbn.notification.channelId,
                channelImportance = ranking.importance,
                category = sbn.notification.category,
                isConversation = extras.getParcelableArray(Notification.EXTRA_MESSAGES) != null,
                postTime = sbn.postTime,
                postTimeIso = TimeUtils.epochToIso(sbn.postTime),
                messageTimestamp = null
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gmail Adapter
// ─────────────────────────────────────────────────────────────────────────────

class GmailAdapter : NotificationAdapter {

    companion object {
        private val PACKAGES = setOf("com.google.android.gm", "com.microsoft.office.outlook")
    }

    override fun canHandle(packageName: String) = packageName in PACKAGES

    override fun normalize(sbn: StatusBarNotification, ranking: Ranking): List<NormalizedRecord> {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        val appLabel = when (sbn.packageName) {
            "com.microsoft.office.outlook" -> "Outlook"
            else -> "Gmail"
        }

        // Gmail: title = sender name, text = subject, bigText = subject + snippet
        // subText = account email
        return listOf(
            NormalizedRecord(
                notificationKey = sbn.key,
                packageName = sbn.packageName,
                appLabel = appLabel,
                senderName = title,
                senderKey = "${sbn.packageName}:${title.lowercase().trim()}",
                isGroup = false,
                conversationName = null,
                title = title,
                text = bigText ?: text,
                subText = subText,
                channelId = sbn.notification.channelId,
                channelImportance = ranking.importance,
                category = sbn.notification.category,
                isConversation = false,
                postTime = sbn.postTime,
                postTimeIso = TimeUtils.epochToIso(sbn.postTime),
                messageTimestamp = null
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SMS Adapter
// ─────────────────────────────────────────────────────────────────────────────

class SmsAdapter : NotificationAdapter {

    companion object {
        private val PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms"
        )
    }

    override fun canHandle(packageName: String) = packageName in PACKAGES

    override fun normalize(sbn: StatusBarNotification, ranking: Ranking): List<NormalizedRecord> {
        val extras = sbn.notification.extras
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)

        if (messages != null && messages.isNotEmpty()) {
            return messages.mapNotNull { msg ->
                val bundle = msg as? Bundle ?: return@mapNotNull null
                val sender = bundle.getCharSequence("sender")?.toString() ?: "Unknown"
                val text = bundle.getCharSequence("text")?.toString() ?: ""
                val time = bundle.getLong("time", sbn.postTime)

                NormalizedRecord(
                    notificationKey = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = "Messages",
                    senderName = sender,
                    senderKey = "sms:${sender.lowercase().trim()}",
                    isGroup = false,
                    conversationName = null,
                    title = sender,
                    text = text,
                    subText = null,
                    channelId = sbn.notification.channelId,
                    channelImportance = ranking.importance,
                    category = sbn.notification.category,
                    isConversation = true,
                    postTime = sbn.postTime,
                    postTimeIso = TimeUtils.epochToIso(sbn.postTime),
                    messageTimestamp = time
                )
            }
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        return listOf(
            NormalizedRecord(
                notificationKey = sbn.key,
                packageName = sbn.packageName,
                appLabel = "Messages",
                senderName = title,
                senderKey = "sms:${title.lowercase().trim()}",
                isGroup = false,
                conversationName = null,
                title = title,
                text = text,
                subText = null,
                channelId = sbn.notification.channelId,
                channelImportance = ranking.importance,
                category = sbn.notification.category,
                isConversation = true,
                postTime = sbn.postTime,
                postTimeIso = TimeUtils.epochToIso(sbn.postTime),
                messageTimestamp = null
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Generic Adapter (catch-all fallback)
// ─────────────────────────────────────────────────────────────────────────────

class GenericAdapter(private val pm: PackageManager) : NotificationAdapter {

    override fun canHandle(packageName: String) = true

    override fun normalize(sbn: StatusBarNotification, ranking: Ranking): List<NormalizedRecord> {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val appLabel = try {
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            sbn.packageName.substringAfterLast('.')
                .replaceFirstChar { it.uppercase() }
        }

        val hasMessages = extras.getParcelableArray(Notification.EXTRA_MESSAGES) != null

        return listOf(
            NormalizedRecord(
                notificationKey = sbn.key,
                packageName = sbn.packageName,
                appLabel = appLabel,
                senderName = title,
                senderKey = "${sbn.packageName}:${title.lowercase().trim()}",
                isGroup = false,
                conversationName = null,
                title = title,
                text = text,
                subText = subText,
                channelId = sbn.notification.channelId,
                channelImportance = ranking.importance,
                category = sbn.notification.category,
                isConversation = hasMessages,
                postTime = sbn.postTime,
                postTimeIso = TimeUtils.epochToIso(sbn.postTime),
                messageTimestamp = null
            )
        )
    }
}

/**
 * Registry that resolves the correct adapter for a given package name.
 * Order matters — specific adapters are tried before the generic fallback.
 */
class AdapterRegistry(pm: PackageManager) {
    private val adapters: List<NotificationAdapter> = listOf(
        WhatsAppAdapter(),
        TelegramAdapter(),
        GmailAdapter(),
        SmsAdapter(),
        GenericAdapter(pm)
    )

    fun resolve(packageName: String): NotificationAdapter {
        return adapters.first { it.canHandle(packageName) }
    }
}
