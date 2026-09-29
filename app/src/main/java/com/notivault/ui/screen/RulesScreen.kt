package com.notivault.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.notivault.data.entity.RuleEntity
import com.notivault.data.model.*
import com.notivault.data.repository.NotificationRepository
import com.notivault.data.repository.RuleRepository
import com.notivault.engine.PriorityEngine
import com.notivault.ui.component.*
import com.notivault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.map

data class AppRuleState(
    val packageName: String,
    val appLabel: String,
    val notificationCount: Int,
    val currentPriority: String?, // null = no rule set (uses engine default)
    val ruleId: Long? = null
)

data class SenderRuleState(
    val senderKey: String,
    val senderName: String,
    val packageName: String,
    val notificationCount: Int,
    val currentPriority: String?,
    val ruleId: Long? = null
)

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val notificationRepository: NotificationRepository,
    private val priorityEngine: PriorityEngine
) : ViewModel() {

    val rules: Flow<List<RuleEntity>> = ruleRepository.getAllRules().map { list ->
        list.filter { it.entityType == EntityType.SENDER }
    }



    private val _senderRules = MutableStateFlow<List<SenderRuleState>>(emptyList())
    val senderRules: StateFlow<List<SenderRuleState>> = _senderRules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _availableApps = MutableStateFlow<List<SimpleAppInfo>>(emptyList())
    val availableApps: StateFlow<List<SimpleAppInfo>> = _availableApps.asStateFlow()

    init {
        loadAvailableApps()
    }

    fun loadSendersForApp(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val senders = notificationRepository.getDistinctSendersForApp(packageName)
                val allRules = ruleRepository.getAllRulesSnapshot()
                val senderRuleMap = allRules.filter { it.entityType == EntityType.SENDER }
                    .associateBy { it.senderKey }

                _senderRules.value = senders.map { sender ->
                    val rule = senderRuleMap[sender.senderKey]
                    SenderRuleState(
                        senderKey = sender.senderKey,
                        senderName = sender.senderName,
                        packageName = packageName,
                        notificationCount = sender.count,
                        currentPriority = rule?.priority,
                        ruleId = rule?.id
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
            _isLoading.value = false
        }
    }

    private fun loadAvailableApps() {
        viewModelScope.launch {
            try {
                _availableApps.value = notificationRepository.getAllDistinctApps()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun setAppPriority(packageName: String, appLabel: String, priority: String) {
        viewModelScope.launch {
            if (priority == Tier.NORMAL) {
                removeAppRule(packageName)
            } else {
                val existing = ruleRepository.getAppRule(packageName)
                if (existing != null) {
                    ruleRepository.update(existing.copy(priority = priority, updatedAt = System.currentTimeMillis()))
                } else {
                    ruleRepository.insert(
                        RuleEntity(
                            entityType = EntityType.APP,
                            packageName = packageName,
                            senderKey = null,
                            displayName = appLabel,
                            priority = priority,
                            source = RuleSource.USER
                        )
                    )
                }
                notificationRepository.updateTierForApp(packageName, priority)
                priorityEngine.invalidateCache()
            }
        }
    }

    fun removeAppRule(packageName: String) {
        viewModelScope.launch {
            val rule = ruleRepository.getAppRule(packageName)
            if (rule != null) {
                ruleRepository.delete(rule)
                notificationRepository.updateTierForApp(packageName, Tier.NORMAL)
            }
            priorityEngine.invalidateCache()
        }
    }

    fun setSenderPriority(senderKey: String, packageName: String, senderName: String, priority: String) {
        viewModelScope.launch {
            val existing = ruleRepository.getSenderRule(senderKey)
            if (existing != null) {
                ruleRepository.update(existing.copy(priority = priority, updatedAt = System.currentTimeMillis()))
            } else {
                ruleRepository.insert(
                    RuleEntity(
                        entityType = EntityType.SENDER,
                        packageName = packageName,
                        senderKey = senderKey,
                        displayName = senderName,
                        priority = priority,
                        source = RuleSource.USER
                    )
                )
            }
            notificationRepository.updateTierForSender(senderKey, priority)
            priorityEngine.invalidateCache()
            loadSendersForApp(packageName)
        }
    }

    fun removeSenderRule(senderKey: String, packageName: String) {
        viewModelScope.launch {
            val rule = ruleRepository.getSenderRule(senderKey)
            if (rule != null) {
                ruleRepository.delete(rule)
                notificationRepository.updateTierForSender(senderKey, Tier.NORMAL)
            }
            priorityEngine.invalidateCache()
            loadSendersForApp(packageName)
        }
    }

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch {
            ruleRepository.delete(rule)
            if (rule.entityType == EntityType.SENDER && rule.senderKey != null) {
                notificationRepository.updateTierForSender(rule.senderKey, Tier.NORMAL)
            }
            priorityEngine.invalidateCache()
        }
    }

    fun addRule(
        entityType: String,
        packageName: String,
        senderKey: String?,
        displayName: String,
        priority: String
    ) {
        viewModelScope.launch {
            ruleRepository.insert(
                RuleEntity(
                    entityType = entityType,
                    packageName = packageName,
                    senderKey = senderKey,
                    displayName = displayName,
                    priority = priority,
                    source = RuleSource.USER
                )
            )
            if (entityType == EntityType.SENDER && senderKey != null) {
                notificationRepository.updateTierForSender(senderKey, priority)
            }
            priorityEngine.invalidateCache()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onBack: () -> Unit,
    onAppConfig: ((String, String) -> Unit)? = null,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle(initialValue = emptyList())
    val availableApps by viewModel.availableApps.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rules", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Control what deserves your attention.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink500
                        )
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
        },
        floatingActionButton = {
            if (rules.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add rule", style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (rules.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(PrimarySoft, RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Rule, contentDescription = null, modifier = Modifier.size(32.dp), tint = Primary)
                            }
                        },
                        title = "No custom rules yet.",
                        subtitle = "Create a rule to decide\nwhat deserves your attention.",
                        action = {
                            PrimaryButton(
                                text = "Add rule",
                                icon = Icons.Default.Add,
                                onClick = { showAddSheet = true }
                            )
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        SectionHeader("YOUR RULES")
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(rules, key = { it.id }) { rule ->
                        RuleRow(
                            rule = rule,
                            onClick = {
                                if (rule.entityType == EntityType.APP && onAppConfig != null) {
                                    onAppConfig(rule.packageName, rule.displayName)
                                }
                            },
                            onUpdatePriority = { newPriority ->
                                if (rule.entityType == EntityType.APP) {
                                    viewModel.setAppPriority(rule.packageName, rule.displayName, newPriority)
                                } else if (rule.senderKey != null) {
                                    viewModel.setSenderPriority(rule.senderKey, rule.packageName, rule.displayName, newPriority)
                                }
                            },
                            onDelete = { viewModel.deleteRule(rule) }
                        )
                    }
                }

            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AddRuleSheet(
                availableApps = availableApps,
                onDismiss = { showAddSheet = false },
                onAdd = { entityType, packageName, senderKey, displayName, priority ->
                    viewModel.addRule(entityType, packageName, senderKey, displayName, priority)
                    showAddSheet = false
                }
            )
        }
    }
}

@Composable
private fun RuleRow(
    rule: RuleEntity,
    onClick: () -> Unit,
    onUpdatePriority: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val priorityColor = when (rule.priority) {
        Tier.HIGH -> High
        Tier.IGNORE -> Ink500
        else -> Primary
    }
    val priorityLabel = when (rule.priority) {
        Tier.HIGH -> "HIGH"
        Tier.IGNORE -> "IGNORE"
        else -> "NORMAL"
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimarySoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    rule.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink950
                )
                Text(
                    text = if (rule.entityType == EntityType.APP) "App" else "Sender",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink500
                )
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (rule.priority == Tier.HIGH) HighSoft else if (rule.priority == Tier.IGNORE) SurfaceColor else PrimarySoft,
                    modifier = Modifier.clickable { showMenu = true }
                ) {
                    Text(
                        priorityLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Set High Priority") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = High) },
                        onClick = { onUpdatePriority(Tier.HIGH); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Set Normal") },
                        leadingIcon = { Icon(Icons.Default.Remove, contentDescription = null, tint = Primary) },
                        onClick = { onUpdatePriority(Tier.NORMAL); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Set Ignore") },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Ink500) },
                        onClick = { onUpdatePriority(Tier.IGNORE); showMenu = false }
                    )
                    HorizontalDivider(color = BorderColor)
                    DropdownMenuItem(
                        text = { Text("Delete Rule", color = Danger) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Danger) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Ink500, modifier = Modifier.size(20.dp))
        }

        HorizontalDivider(color = BorderColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleSheet(
    availableApps: List<SimpleAppInfo>,
    onDismiss: () -> Unit,
    onAdd: (entityType: String, packageName: String, senderKey: String?, displayName: String, priority: String) -> Unit
) {
    var selectedApp by remember { mutableStateOf<SimpleAppInfo?>(null) }
    var senderName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(Tier.HIGH) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("New sender rule", style = MaterialTheme.typography.headlineMedium)

        // App selection dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedApp?.appLabel ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select App") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                if (availableApps.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No apps with notifications yet", color = Ink500) },
                        onClick = {}
                    )
                } else {
                    availableApps.forEach { appInfo ->
                        DropdownMenuItem(
                            text = { Text(appInfo.appLabel) },
                            onClick = {
                                selectedApp = appInfo
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Sender name
        OutlinedTextField(
            value = senderName,
            onValueChange = { senderName = it },
            label = { Text("Sender name (e.g. John Doe)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        // Priority
        Column {
            Text("Priority", style = MaterialTheme.typography.labelLarge, color = Ink500)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriorityOption("High", Tier.HIGH, priority == Tier.HIGH, HighSoft, High) { priority = Tier.HIGH }
                PriorityOption("Normal", Tier.NORMAL, priority == Tier.NORMAL, PrimarySoft, Primary) { priority = Tier.NORMAL }
                PriorityOption("Ignore", Tier.IGNORE, priority == Tier.IGNORE, SurfaceColor, Ink500) { priority = Tier.IGNORE }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Save rule",
                onClick = {
                    val app = selectedApp
                    if (app != null && senderName.isNotBlank()) {
                        val pkg = app.packageName
                        val sKey = "$pkg:${senderName.lowercase().trim()}"
                        val name = senderName.trim()
                        onAdd(EntityType.SENDER, pkg, sKey, name, priority)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = selectedApp != null && senderName.isNotBlank()
            )
        }
    }
}

@Composable
private fun PriorityOption(
    label: String,
    tier: String,
    selected: Boolean,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) bgColor else Color.White,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) textColor else Ink700,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App Config Screen — per-app sender management
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    packageName: String,
    appLabel: String,
    onBack: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val senders by viewModel.senderRules.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(packageName) {
        viewModel.loadSendersForApp(packageName)
    }

    // Get the current app rule to show toggle state
    val rules by viewModel.rules.collectAsStateWithLifecycle(initialValue = emptyList())
    val appRule = rules.find { it.entityType == EntityType.APP && it.packageName == packageName }

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimarySoft, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(appLabel.take(2).uppercase(), style = MaterialTheme.typography.labelLarge, color = Primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(appLabel, style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // App-level priority control
            item {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        "APP PRIORITY",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = Ink500
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Set this app's overall priority. You can also control individual senders below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink700
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PriorityOption(
                            "High", Tier.HIGH, appRule?.priority == Tier.HIGH, HighSoft, High
                        ) { viewModel.setAppPriority(packageName, appLabel, Tier.HIGH) }
                        PriorityOption(
                            "Normal", Tier.NORMAL, appRule?.priority == Tier.NORMAL, PrimarySoft, Primary
                        ) { viewModel.setAppPriority(packageName, appLabel, Tier.NORMAL) }
                        PriorityOption(
                            "Ignore", Tier.IGNORE, appRule?.priority == Tier.IGNORE, SurfaceColor, Ink500
                        ) { viewModel.setAppPriority(packageName, appLabel, Tier.IGNORE) }
                    }

                    if (appRule != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.removeAppRule(packageName) }) {
                            Text("Reset to default", color = Ink500, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)
            }

            // Senders section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "SENDERS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Ink500
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Configure individual senders within this app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink700
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                    }
                }
            } else if (senders.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.PersonOff, contentDescription = null, tint = Ink500, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No senders found", style = MaterialTheme.typography.titleLarge, color = Ink700)
                        Text(
                            "Senders will appear here once\n$appLabel sends notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(senders, key = { it.senderKey }) { sender ->
                    SenderRow(
                        sender = sender,
                        onSetPriority = { priority ->
                            viewModel.setSenderPriority(sender.senderKey, sender.packageName, sender.senderName, priority)
                        },
                        onRemoveRule = {
                            viewModel.removeSenderRule(sender.senderKey, sender.packageName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SenderRow(
    sender: SenderRuleState,
    onSetPriority: (String) -> Unit,
    onRemoveRule: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMenu = true }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PrimarySoft, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(sender.senderName.take(1).uppercase(), style = MaterialTheme.typography.labelLarge, color = Primary)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(sender.senderName, style = MaterialTheme.typography.titleLarge, color = Ink950)
                Text("${sender.notificationCount} notifications", style = MaterialTheme.typography.bodyMedium, color = Ink500)
            }

            if (sender.currentPriority != null) {
                val (label, bgColor, textColor) = when (sender.currentPriority) {
                    Tier.HIGH -> Triple("HIGH", HighSoft, High)
                    Tier.IGNORE -> Triple("IGNORE", SurfaceColor, Ink500)
                    else -> Triple("NORMAL", PrimarySoft, Primary)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Text("Default", style = MaterialTheme.typography.labelMedium, color = Ink500)
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(
                text = { Text("Set High Priority") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = High) },
                onClick = { onSetPriority(Tier.HIGH); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Set Normal") },
                leadingIcon = { Icon(Icons.Default.Remove, contentDescription = null, tint = Primary) },
                onClick = { onSetPriority(Tier.NORMAL); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Set Ignore") },
                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Ink500) },
                onClick = { onSetPriority(Tier.IGNORE); showMenu = false }
            )
            if (sender.currentPriority != null) {
                HorizontalDivider(color = BorderColor)
                DropdownMenuItem(
                    text = { Text("Reset to default") },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Ink700) },
                    onClick = { onRemoveRule(); showMenu = false }
                )
            }
        }

        HorizontalDivider(color = BorderColor)
    }
}
