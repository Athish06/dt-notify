package com.notivault.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.data.entity.NotificationEntity
import com.notivault.data.repository.NotificationRepository
import com.notivault.ui.component.PrimaryButton
import com.notivault.ui.component.SecondaryButton
import com.notivault.ui.theme.*
import com.notivault.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationDetailViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val notificationId: Long = savedStateHandle["notificationId"] ?: 0L

    private val _notification = MutableStateFlow<NotificationEntity?>(null)
    val notification: StateFlow<NotificationEntity?> = _notification.asStateFlow()

    init {
        viewModelScope.launch {
            _notification.value = notificationRepository.getNotificationById(notificationId)
        }
    }

    fun deleteNotification(onDeleted: () -> Unit) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
            onDeleted()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    onBack: () -> Unit,
    viewModel: NotificationDetailViewModel = hiltViewModel()
) {
    val notification by viewModel.notification.collectAsState()
    val n = notification
    val context = LocalContext.current

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (n != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(PrimarySoft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(n.appLabel.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = Primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(n.appLabel, style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("Notification")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor
                )
            )
        }
    ) { padding ->
        if (n == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Sender identity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(PrimarySoft, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = n.senderName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = n.senderName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink950
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${TimeUtils.formatDateHeader(n.postTime)} - ${TimeUtils.formatTime(n.postTime)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Ink500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Message content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    if (n.title.isNotBlank() && n.title != n.senderName) {
                        Text(
                            text = n.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink950
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = n.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink700
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Metadata
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    MetadataRow("Priority", if (n.tier == "HIGH") "High" else "Normal")
                    Spacer(modifier = Modifier.height(12.dp))
                    MetadataRow("Type", if (n.isConversation) "Conversation" else "Message")
                    Spacer(modifier = Modifier.height(12.dp))
                    MetadataRow("Sender ID", n.senderKey.substringAfter(":"))
                    Spacer(modifier = Modifier.height(12.dp))
                    MetadataRow("Captured", TimeUtils.formatTime(n.capturedAt))
                    if (n.conversationName != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MetadataRow("Conversation", n.conversationName)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(
                        text = "Delete",
                        onClick = { viewModel.deleteNotification(onDeleted = onBack) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    PrimaryButton(
                        text = "Open App",
                        onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage(n.packageName)
                            if (intent != null) {
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Opens the parent app",
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink500
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink500)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink950
        )
    }
}
