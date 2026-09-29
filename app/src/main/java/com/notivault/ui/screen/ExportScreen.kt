package com.notivault.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.data.model.Tier
import com.notivault.export.JsonExporter
import com.notivault.ui.component.PrimaryButton
import com.notivault.ui.component.SectionHeader
import com.notivault.ui.theme.*
import com.notivault.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val jsonExporter: JsonExporter
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting = _exporting.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result = _result.asStateFlow()

    fun export(
        context: android.content.Context,
        daysBack: Int,
        tierFilter: String?
    ) {
        viewModelScope.launch {
            _exporting.value = true
            _result.value = null

            try {
                val to = System.currentTimeMillis()
                val from = to - (daysBack.toLong() * 24 * 60 * 60 * 1000)

                val exportResult = jsonExporter.exportToFile(context, from, to, tierFilter)

                exportResult.fold(
                    onSuccess = { file ->
                        _result.value = "Exported to: ${file.absolutePath}"
                    },
                    onFailure = { e ->
                        _result.value = "Export failed: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _result.value = "Export failed: ${e.message}"
            } finally {
                _exporting.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val exporting by viewModel.exporting.collectAsState()
    val result by viewModel.result.collectAsState()

    var daysBack by remember { mutableIntStateOf(14) }
    var tierFilter by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = { Text("Export", style = MaterialTheme.typography.headlineLarge) },
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
                .padding(24.dp)
        ) {
            Text(
                "Create a secure JSON export\nof your notification history.",
                style = MaterialTheme.typography.bodyLarge,
                color = Ink500
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            // Date Range
            Column {
                SectionHeader("DATE RANGE")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${TimeUtils.formatDateHeader(System.currentTimeMillis() - (daysBack.toLong() * 24 * 60 * 60 * 1000))}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink950
                    )
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Ink500, modifier = Modifier.size(16.dp))
                    Text(
                        text = TimeUtils.formatDateHeader(System.currentTimeMillis()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink950
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 7, 14).forEach { days ->
                        FilterChip(
                            selected = daysBack == days,
                            onClick = { daysBack = days },
                            label = { Text("${days}d") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimarySoft,
                                selectedLabelColor = Primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = daysBack == days)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(32.dp))

            // Priority
            Column {
                SectionHeader("PRIORITY")
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        listOf(Tier.HIGH, Tier.NORMAL, null).forEach { filter ->
                            val label = when(filter) { Tier.HIGH -> "High"; Tier.NORMAL -> "Normal"; else -> "All" }
                            val isSelected = tierFilter == filter
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Primary else Color.Transparent,
                                modifier = Modifier.weight(1f).clickable { tierFilter = filter }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (isSelected) Color.White else Ink700)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(32.dp))
            
            // App selection mock
            Column {
                SectionHeader("APPS")
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("All apps", style = MaterialTheme.typography.bodyLarge, color = Ink950)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Ink500)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (result != null) {
                Surface(
                    color = if (result!!.startsWith("Exported")) SuccessSoft else Danger.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (result!!.startsWith("Exported")) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (result!!.startsWith("Exported")) Primary else Danger
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (result!!.startsWith("Exported")) "Export ready" else "Export failed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink950
                        )
                    }
                }
            }

            PrimaryButton(
                text = if (exporting) "Exporting..." else "Export JSON",
                onClick = { viewModel.export(context, daysBack, tierFilter) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !exporting
            )
        }
    }
}
