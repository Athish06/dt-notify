package com.notivault.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.notivault.data.model.AppSummary
import com.notivault.data.repository.NotificationRepository
import com.notivault.ui.component.EmptyState
import com.notivault.ui.component.SectionHeader
import com.notivault.ui.theme.*
import com.notivault.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    val highPriorityApps: Flow<List<AppSummary>> = notificationRepository.getHighPriorityApps()
}

@Composable
fun HomeScreen(
    onAppClick: (String, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val apps by viewModel.highPriorityApps.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = SurfaceColor
    ) { padding ->
        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(PrimarySoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("!", style = MaterialTheme.typography.headlineMedium, color = Primary)
                        }
                    },
                    title = "Nothing important yet.",
                    subtitle = "When Notify finds a high-priority\nnotification, it will appear here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Good evening", style = MaterialTheme.typography.bodyLarge, color = Ink700)
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Ink950, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your important\nnotifications",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Ink950
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Vault Ring
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { 0.7f },
                                modifier = Modifier.fillMaxSize(),
                                color = Warning,
                                trackColor = PrimarySoft,
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = apps.sumOf { it.count }.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Ink950
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = apps.sumOf { it.count }.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Ink950
                            )
                            Text(
                                text = "HIGH PRIORITY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Ink500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${apps.size} apps · ${apps.sumOf { it.count }} new today",
                                style = MaterialTheme.typography.labelMedium,
                                color = Ink950
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Segmented toggle
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Primary,
                                modifier = Modifier.weight(1f).clickable { }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text("High Priority", style = MaterialTheme.typography.labelLarge, color = Color.White)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent,
                                modifier = Modifier.weight(1f).clickable { }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text("All", style = MaterialTheme.typography.labelLarge, color = Ink700)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    SectionHeader("IMPORTANT APPS")
                }

                items(apps) { app ->
                    AppSummaryCard(
                        app = app,
                        onClick = { onAppClick(app.packageName, app.appLabel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSummaryCard(
    app: AppSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(PrimarySoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appLabel.take(2).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink950,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.count.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Ink700
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            val preview = if (app.lastSender != null && app.lastSender != app.appLabel) {
                "${app.lastSender} · ${app.lastText ?: ""}"
            } else {
                app.lastText ?: ""
            }
            
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = TimeUtils.formatRelativeTime(app.lastPostTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink500
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Ink500, modifier = Modifier.size(16.dp))
            }
        }
    }
}
