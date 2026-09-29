package com.notivault.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.notivault.data.entity.NotificationEntity
import com.notivault.data.repository.NotificationRepository
import com.notivault.ui.component.*
import com.notivault.ui.theme.*
import com.notivault.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val packageName: String = savedStateHandle["packageName"] ?: ""

    val notifications: Flow<PagingData<NotificationEntity>> =
        notificationRepository.getHighPriorityByApp(packageName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    appLabel: String,
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val items = viewModel.notifications.collectAsLazyPagingItems()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(appLabel.take(1).uppercase(), style = MaterialTheme.typography.titleSmall, color = Primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(appLabel, style = MaterialTheme.typography.titleMedium)
                            if (items.itemCount > 0) {
                                Text("${items.itemCount} important notifications", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (items.itemCount == 0) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = OnSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    title = "No notifications",
                    subtitle = "Priority notifications from $appLabel will appear here"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var lastDateKey = ""
                items(items.itemCount) { index ->
                    val notification = items[index] ?: return@items
                    val dateKey = TimeUtils.formatDateKey(notification.postTime)

                    if (dateKey != lastDateKey) {
                        lastDateKey = dateKey
                        DateHeader(label = TimeUtils.formatDateHeader(notification.postTime))
                    }

                    NotificationCard(
                        notification = notification,
                        showAppLabel = false,
                        onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                            if (intent != null) {
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}
