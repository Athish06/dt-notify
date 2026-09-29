package com.notivault.ui.screen

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.data.entity.RuleEntity
import com.notivault.data.model.EntityType
import com.notivault.data.model.RuleSource
import com.notivault.data.model.Tier
import com.notivault.data.repository.RuleRepository
import com.notivault.data.repository.NotificationRepository
import com.notivault.engine.PriorityEngine
import com.notivault.ui.theme.Ink500
import com.notivault.ui.theme.Ink950
import com.notivault.ui.theme.Primary
import com.notivault.ui.theme.PrimarySoft
import com.notivault.ui.theme.SurfaceColor
import com.notivault.ui.theme.BorderColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstalledApp(
    val packageName: String,
    val appLabel: String,
    val currentPriority: String
)

@HiltViewModel
class AppPriorityViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val notificationRepository: NotificationRepository,
    private val priorityEngine: PriorityEngine
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    fun loadApps(packageManager: PackageManager) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val rules = ruleRepository.getAllRulesSnapshot().filter { 
                    it.entityType == EntityType.APP 
                }.associateBy { it.packageName }
                
                val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val appList = installed.map { info ->
                    val label = packageManager.getApplicationLabel(info).toString()
                    val packageName = info.packageName
                    val rule = rules[packageName]
                    InstalledApp(
                        packageName = packageName,
                        appLabel = label,
                        currentPriority = rule?.priority ?: Tier.NORMAL
                    )
                }.sortedBy { it.appLabel }
                
                _apps.value = appList
            }
        }
    }

    fun setPriority(app: InstalledApp, priority: String) {
        viewModelScope.launch {
            if (priority == Tier.NORMAL) {
                // Remove the rule since NORMAL is default
                val rule = ruleRepository.getAppRule(app.packageName)
                if (rule != null) {
                    ruleRepository.delete(rule)
                }
            } else {
                val existing = ruleRepository.getAppRule(app.packageName)
                if (existing != null) {
                    ruleRepository.update(existing.copy(priority = priority, updatedAt = System.currentTimeMillis()))
                } else {
                    ruleRepository.insert(
                        RuleEntity(
                            entityType = EntityType.APP,
                            packageName = app.packageName,
                            senderKey = null,
                            displayName = app.appLabel,
                            priority = priority,
                            source = RuleSource.USER
                        )
                    )
                }
            }
            notificationRepository.updateTierForApp(app.packageName, priority)
            priorityEngine.invalidateCache()
            
            // Update local state
            _apps.value = _apps.value.map { 
                if (it.packageName == app.packageName) it.copy(currentPriority = priority) else it 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPriorityScreen(
    onBack: () -> Unit,
    viewModel: AppPriorityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForEdit by remember { mutableStateOf<InstalledApp?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadApps(context.packageManager)
    }

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = { Text("App Priorities", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = "Manage notification priority for every installed app.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink500
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            val filteredApps = apps.filter { it.appLabel.contains(searchQuery, ignoreCase = true) }

            if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    val customApps = filteredApps.filter { it.currentPriority != Tier.NORMAL }
                    val normalApps = filteredApps.filter { it.currentPriority == Tier.NORMAL }

                    if (customApps.isNotEmpty()) {
                        item {
                            com.notivault.ui.component.SectionHeader("CONFIGURED APPS")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(customApps, key = { it.packageName }) { app ->
                            AppPriorityRow(app, onClick = { selectedAppForEdit = app })
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (normalApps.isNotEmpty()) {
                        item {
                            com.notivault.ui.component.SectionHeader("OTHER APPS")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(normalApps, key = { it.packageName }) { app ->
                            AppPriorityRow(app, onClick = { selectedAppForEdit = app })
                        }
                    }
                }
            }
        }
    }

    if (selectedAppForEdit != null) {
        val app = selectedAppForEdit!!
        ModalBottomSheet(
            onDismissRequest = { selectedAppForEdit = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(app.appLabel, style = MaterialTheme.typography.headlineMedium)
                
                Column {
                    Text("Select Priority", style = MaterialTheme.typography.labelLarge, color = Ink500)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PriorityOptionRow(
                        title = "High",
                        description = "Always highlight these notifications.",
                        selected = app.currentPriority == Tier.HIGH,
                        onClick = { viewModel.setPriority(app, Tier.HIGH); selectedAppForEdit = null }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PriorityOptionRow(
                        title = "Normal",
                        description = "Default standard priority.",
                        selected = app.currentPriority == Tier.NORMAL,
                        onClick = { viewModel.setPriority(app, Tier.NORMAL); selectedAppForEdit = null }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PriorityOptionRow(
                        title = "Ignore",
                        description = "Do not store these notifications.",
                        selected = app.currentPriority == Tier.IGNORE,
                        onClick = { viewModel.setPriority(app, Tier.IGNORE); selectedAppForEdit = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PrimarySoft else Color.Transparent,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) Primary else Ink950
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) Primary else Ink500
            )
        }
    }
}

@Composable
fun AppPriorityRow(app: InstalledApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = app.appLabel,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            color = Ink950
        )
        
        val priorityLabel = when (app.currentPriority) {
            Tier.HIGH -> "High"
            Tier.IGNORE -> "Ignore"
            else -> "Normal"
        }
        val priorityColor = when (app.currentPriority) {
            Tier.HIGH -> com.notivault.ui.theme.High
            Tier.IGNORE -> Ink500
            else -> Primary
        }
        
        Text(
            text = priorityLabel,
            style = MaterialTheme.typography.labelMedium,
            color = priorityColor
        )
    }
    HorizontalDivider(color = BorderColor)
}
