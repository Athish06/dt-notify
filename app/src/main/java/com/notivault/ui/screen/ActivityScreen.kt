package com.notivault.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import com.notivault.data.entity.NotificationEntity
import com.notivault.data.model.Tier
import com.notivault.data.repository.NotificationRepository
import com.notivault.ui.component.DateHeader
import com.notivault.ui.component.EmptyState
import com.notivault.ui.theme.*
import com.notivault.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

enum class TimeFilter(val label: String, val msOffset: Long) {
    TODAY("Today", 24 * 60 * 60 * 1000L),
    LAST_7_DAYS("Last 7 days", 7 * 24 * 60 * 60 * 1000L),
    ALL_TIME("All time", 365L * 24 * 60 * 60 * 1000L)
}

enum class PriorityFilter(val label: String, val tierValue: String?) {
    ALL("All Priority", null),
    HIGH("High", Tier.HIGH),
    NORMAL("Normal", Tier.NORMAL)
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val selectedApp = MutableStateFlow<String?>("All Apps")
    val selectedPriority = MutableStateFlow(PriorityFilter.ALL)
    val selectedTime = MutableStateFlow(TimeFilter.TODAY)

    val availableApps: Flow<List<String>> = notificationRepository.getHighPriorityApps().map { list ->
        listOf("All Apps") + list.map { it.appLabel }
    }

    private val _availableAppPackages = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        viewModelScope.launch {
            notificationRepository.getHighPriorityApps().collect { list ->
                _availableAppPackages.value = list.associate { it.appLabel to it.packageName }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: Flow<PagingData<NotificationEntity>> = combine(
        selectedApp, selectedPriority, selectedTime
    ) { app, priority, time ->
        Triple(app, priority, time)
    }.flatMapLatest { (app, priority, time) ->
        val appFilter = if (app == "All Apps") null else _availableAppPackages.value[app] ?: app
        val timeFilter = System.currentTimeMillis() - time.msOffset
        notificationRepository.getFilteredActivity(appFilter, priority.tierValue, timeFilter)
    }.cachedIn(viewModelScope)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onNotificationClick: (Long) -> Unit = {},
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val items = viewModel.notifications.collectAsLazyPagingItems()
    
    val selectedApp by viewModel.selectedApp.collectAsState()
    val selectedPriority by viewModel.selectedPriority.collectAsState()
    val selectedTime by viewModel.selectedTime.collectAsState()
    val apps by viewModel.availableApps.collectAsState(initial = listOf("All Apps"))

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Activity", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Today, ${TimeUtils.formatDateHeader(System.currentTimeMillis())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink500
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = selectedApp ?: "All Apps",
                    options = apps,
                    onSelect = { viewModel.selectedApp.value = it }
                )
                FilterDropdown(
                    label = selectedPriority.label,
                    options = PriorityFilter.values().map { it.label },
                    onSelect = { label -> viewModel.selectedPriority.value = PriorityFilter.values().first { it.label == label } }
                )
                FilterDropdown(
                    label = selectedTime.label,
                    options = TimeFilter.values().map { it.label },
                    onSelect = { label -> viewModel.selectedTime.value = TimeFilter.values().first { it.label == label } }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (items.itemCount == 0) {
                EmptyState(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(PrimarySoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Primary
                            )
                        }
                    },
                    title = "No recent activity",
                    subtitle = "Adjust your filters or wait for notifications"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    var lastDateKey = ""
                    items(items.itemCount) { index ->
                        val notification = items[index] ?: return@items
                        val dateKey = TimeUtils.formatDateKey(notification.postTime)

                        if (dateKey != lastDateKey) {
                            lastDateKey = dateKey
                            DateHeader(label = TimeUtils.formatDateHeader(notification.postTime))
                        }

                        TimelineNotificationItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification.id) },
                            isLast = index == items.itemCount - 1 || 
                                     (index + 1 < items.itemCount && 
                                      TimeUtils.formatDateKey(items[index+1]?.postTime ?: 0) != dateKey)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(label: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Ink950)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Ink500)
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TimelineNotificationItem(
    notification: NotificationEntity,
    onClick: () -> Unit,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
    ) {
        // Time column (left of the line)
        Column(
            modifier = Modifier.width(60.dp).padding(top = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = TimeUtils.formatTime(notification.postTime),
                style = MaterialTheme.typography.labelSmall,
                color = Ink500
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Timeline column (the line and dot)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                if (!isLast) {
                    Canvas(modifier = Modifier.fillMaxHeight()) {
                        drawLine(
                            color = BorderColor,
                            start = Offset(size.width / 2, 8.dp.toPx()),
                            end = Offset(size.width / 2, size.height)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = PrimarySoft,
                    modifier = Modifier.size(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(shape = CircleShape, color = Primary, modifier = Modifier.size(6.dp)) {}
                    }
                }
            }
        }

        // Content column (right of the line)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp, top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = PrimarySoft,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(notification.appLabel.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = Primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notification.appLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink950,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val priorityColor = if (notification.tier == Tier.HIGH) High else Primary
                val priorityLabel = if (notification.tier == Tier.HIGH) "HIGH" else "NORMAL"
                Text(
                    priorityLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = priorityColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = notification.senderName,
                style = MaterialTheme.typography.titleLarge,
                color = Ink950,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = notification.text,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink700,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (notification.conversationName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.conversationName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
