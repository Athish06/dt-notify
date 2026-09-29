package com.notivault.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.notivault.data.entity.ServiceHealthEntity
import com.notivault.data.model.HealthStatus
import com.notivault.data.model.StorageStats
import com.notivault.data.repository.HealthRepository
import com.notivault.data.repository.NotificationRepository
import com.notivault.data.repository.RuleRepository
import com.notivault.ui.component.*
import com.notivault.ui.theme.*
import com.notivault.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val ruleRepository: RuleRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _stats = MutableStateFlow<StorageStats?>(null)
    val stats: StateFlow<StorageStats?> = _stats.asStateFlow()

    val healthEvents: Flow<List<ServiceHealthEntity>> = healthRepository.getRecentEvents()

    val healthStatus: Flow<HealthStatus> = healthRepository.getRecentEvents().map { events ->
        val latest = events.firstOrNull()
        when (latest?.event) {
            "CONNECTED" -> HealthStatus.CONNECTED
            "DISCONNECTED", "REBIND_FAILED" -> HealthStatus.DISCONNECTED
            else -> HealthStatus.UNKNOWN
        }
    }

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            try {
                _stats.value = StorageStats(
                    totalNotifications = notificationRepository.getTotalCount(),
                    highPriorityCount = notificationRepository.getHighCount(),
                    normalCount = notificationRepository.getNormalCount(),
                    rulesCount = ruleRepository.getRulesCount(),
                    dbSizeBytes = 0
                )
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            notificationRepository.deleteAll()
            refreshStats()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onRulesClick: () -> Unit,
    onExportClick: () -> Unit,
    onServiceHealthClick: () -> Unit,
    onStorageClick: () -> Unit,
    onIgnoredAppsClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSection(
                    title = "NOTIFICATIONS",
                    items = listOf(
                        SettingsItemData(
                            icon = Icons.Outlined.Rule,
                            title = "Rules",
                            subtitle = "Manage notification priorities",
                            onClick = onRulesClick
                        ),
                        SettingsItemData(
                            icon = Icons.Outlined.AppRegistration,
                            title = "App Priorities",
                            subtitle = "Manage priority per application",
                            onClick = onIgnoredAppsClick
                        )
                    )
                )
            }

            item {
                SettingsSection(
                    title = "SYSTEM",
                    items = listOf(
                        SettingsItemData(
                            icon = Icons.Outlined.MonitorHeart,
                            title = "Service health",
                            subtitle = "Monitor background service",
                            onClick = onServiceHealthClick
                        ),
                        SettingsItemData(
                            icon = Icons.Outlined.Storage,
                            title = "Storage",
                            subtitle = "View database usage",
                            onClick = onStorageClick
                        ),
                        SettingsItemData(
                            icon = Icons.Outlined.FileDownload,
                            title = "Export",
                            subtitle = "Backup your notifications",
                            onClick = onExportClick
                        )
                    )
                )
            }

            item {
                SettingsSection(
                    title = "PRIVACY",
                    items = listOf(
                        SettingsItemData(
                            icon = Icons.Outlined.DeleteOutline,
                            title = "Delete stored data",
                            subtitle = "Remove all stored data",
                            onClick = { showDeleteDialog = true }
                        )
                    )
                )
            }

            item {
                SettingsSection(
                    title = "ABOUT",
                    items = listOf(
                        SettingsItemData(
                            icon = Icons.Outlined.Info,
                            title = "About Notify",
                            subtitle = "Version 1.0.0",
                            onClick = { showAboutDialog = true }
                        )
                    )
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete stored data?") },
            text = { Text("This will permanently remove all stored notifications from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllData()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.White,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Notify") },
            text = { Text("Version 1.0.0\nYour notifications, kept private.") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = Primary)
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.White,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }
}

data class SettingsItemData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsSection(title: String, items: List<SettingsItemData>) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = Ink500,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Ink700,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, style = MaterialTheme.typography.titleLarge, color = Ink950)
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink500
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Ink500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp, end = 20.dp),
                        color = BorderColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHealthScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val health by viewModel.healthStatus.collectAsStateWithLifecycle(initialValue = HealthStatus.UNKNOWN)
    
    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Service health", style = MaterialTheme.typography.headlineLarge)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Notify is running normally.",
                style = MaterialTheme.typography.bodyLarge,
                color = Ink500
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(SuccessSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = Success)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Connected", style = MaterialTheme.typography.headlineMedium)
            Text("Listening for notifications", style = MaterialTheme.typography.bodyMedium, color = Ink700)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("SERVICE")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = SuccessSoft, modifier = Modifier.size(24.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(shape = CircleShape, color = Success, modifier = Modifier.size(8.dp)) {}
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Listener", style = MaterialTheme.typography.bodyLarge, color = Ink950)
                    }
                    Text("Connected", style = MaterialTheme.typography.bodyLarge, color = Ink700)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = SuccessSoft, modifier = Modifier.size(24.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(shape = CircleShape, color = Success, modifier = Modifier.size(8.dp)) {}
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Last received", style = MaterialTheme.typography.bodyLarge, color = Ink950)
                    }
                    Text("Today, 4:12 PM", style = MaterialTheme.typography.bodyLarge, color = Ink700)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.BatteryStd, contentDescription = null, tint = Ink500)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Battery optimization", style = MaterialTheme.typography.bodyLarge, color = Ink950)
                        Text("Optimized for background use", style = MaterialTheme.typography.bodyMedium, color = Ink500)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Ink500)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    
    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = { Text("Storage", style = MaterialTheme.typography.headlineLarge) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Your local notification vault",
                style = MaterialTheme.typography.bodyLarge,
                color = Ink500
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 0.85f }, // Hardcoded storage usage visual for now
                    modifier = Modifier.fillMaxSize(),
                    color = Primary,
                    trackColor = PrimarySoft,
                    strokeWidth = 6.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("4.8 MB", style = MaterialTheme.typography.headlineSmall, color = Ink950)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("STATISTICS")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Captured today", style = MaterialTheme.typography.bodyLarge, color = Ink700)
                    Text(TimeUtils.formatCount(stats?.highPriorityCount ?: 0), style = MaterialTheme.typography.bodyLarge, color = Ink950)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Captured lifetime", style = MaterialTheme.typography.bodyLarge, color = Ink700)
                    Text(TimeUtils.formatCount(stats?.totalNotifications ?: 0), style = MaterialTheme.typography.bodyLarge, color = Ink950)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Database size", style = MaterialTheme.typography.bodyLarge, color = Ink700)
                    Text("4.8 MB", style = MaterialTheme.typography.bodyLarge, color = Ink950)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("RETENTION")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("High priority", style = MaterialTheme.typography.bodyLarge, color = Ink700)
                    Text("14 days", style = MaterialTheme.typography.bodyLarge, color = Ink950)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Normal priority", style = MaterialTheme.typography.bodyLarge, color = Ink700)
                    Text("24 hours", style = MaterialTheme.typography.bodyLarge, color = Ink950)
                }
            }
        }
    }
}
