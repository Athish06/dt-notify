package com.notivault.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.notivault.data.entity.SuggestionEntity
import com.notivault.data.repository.SuggestionRepository
import com.notivault.engine.LearningEngine
import com.notivault.engine.PriorityEngine
import com.notivault.ui.component.*
import com.notivault.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val suggestionRepository: SuggestionRepository,
    private val learningEngine: LearningEngine,
    private val priorityEngine: PriorityEngine
) : ViewModel() {

    val suggestions: Flow<List<SuggestionEntity>> = suggestionRepository.getPendingSuggestions()

    fun accept(suggestion: SuggestionEntity) {
        viewModelScope.launch {
            learningEngine.acceptSuggestion(suggestion)
            priorityEngine.invalidateCache()
        }
    }

    fun reject(suggestion: SuggestionEntity) {
        viewModelScope.launch {
            suggestionRepository.reject(suggestion.id)
        }
    }

    fun neverAsk(suggestion: SuggestionEntity) {
        viewModelScope.launch {
            suggestionRepository.neverAsk(suggestion.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    viewModel: SuggestionsViewModel = hiltViewModel()
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle(initialValue = emptyList())
    var acceptedSuggestion by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Suggestions", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Notify is learning what matters to you.",
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
        if (acceptedSuggestion != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(Success.copy(alpha = 0.1f), CircleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(64.dp))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Priority updated", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "$acceptedSuggestion is now treated as High Priority.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink500,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                PrimaryButton(
                    text = "Done",
                    onClick = { acceptedSuggestion = null },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        } else if (suggestions.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Ink500.copy(alpha = 0.5f)
                    )
                },
                title = "All caught up",
                subtitle = "Notify suggests priority rules\nwhen it learns a useful pattern."
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(suggestions, key = { it.id }) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onAccept = {
                            viewModel.accept(suggestion)
                            acceptedSuggestion = suggestion.displayName
                        },
                        onReject = { viewModel.reject(suggestion) },
                        onNeverAsk = { viewModel.neverAsk(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: SuggestionEntity,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onNeverAsk: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(PrimarySoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(suggestion.displayName.take(1).uppercase(), style = MaterialTheme.typography.titleSmall, color = Primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = suggestion.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You've interacted with ${suggestion.displayName} notifications frequently recently.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink700
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = Ink500)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Notifications: ${String.format("%.1f", suggestion.score)} score", style = MaterialTheme.typography.labelSmall, color = Ink500)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Add as high priority?", style = MaterialTheme.typography.titleSmall)
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    text = "Add priority",
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                )

                SecondaryButton(
                    text = "Not now",
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TertiaryButton(
                    text = "Never ask again",
                    onClick = onNeverAsk
                )
            }
        }
    }
}
