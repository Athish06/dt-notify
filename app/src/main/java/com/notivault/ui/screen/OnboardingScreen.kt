package com.notivault.ui.screen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.service.NotiVaultListenerService
import com.notivault.ui.component.PrimaryButton
import com.notivault.ui.component.SecondaryButton
import com.notivault.ui.component.TertiaryButton
import com.notivault.ui.theme.*
import com.notivault.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(Constants.KEY_ONBOARDING_COMPLETE)] = true
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    var notificationAccessGranted by remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }

    LaunchedEffect(pagerState.currentPage) {
        notificationAccessGranted = isNotificationListenerEnabled(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // Page indicator at top left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = String.format("%02d", pagerState.currentPage + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                Text(
                    text = " . ",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                Text(
                    text = when(pagerState.currentPage) {
                        0 -> "Welcome"
                        1 -> "Notification Access"
                        else -> "Battery Optimization"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> OnboardingPage1()
                    1 -> OnboardingPage2(isGranted = notificationAccessGranted)
                    2 -> OnboardingPage3()
                }
            }

            // Bottom Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (pagerState.currentPage) {
                    0 -> {
                        PrimaryButton(
                            text = "Get started",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TertiaryButton(
                            text = "Privacy policy",
                            onClick = { /* open link */ }
                        )
                    }
                    1 -> {
                        if (notificationAccessGranted) {
                            PrimaryButton(
                                text = "Continue",
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(2) }
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                        } else {
                            PrimaryButton(
                                text = "Connect",
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                        }
                    }
                    2 -> {
                        PrimaryButton(
                            text = "Fix battery settings",
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(fallback)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TertiaryButton(
                            text = "Skip & Complete",
                            onClick = {
                                viewModel.completeOnboarding()
                                onComplete()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun OnboardingPage1() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // App Icon Mock
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Primary, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Notify",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Privacy-first notification storage",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        FeatureRow(
            icon = Icons.Outlined.SaveAlt,
            title = "Stored locally",
            desc = "Your notification data never leaves this device."
        )
        Spacer(modifier = Modifier.height(32.dp))
        FeatureRow(
            icon = Icons.Outlined.Lock,
            title = "Encrypted",
            desc = "Your local database is protected."
        )
        Spacer(modifier = Modifier.height(32.dp))
        FeatureRow(
            icon = Icons.Outlined.CloudOff,
            title = "No cloud sync",
            desc = "No analytics or remote processing."
        )
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(PrimarySoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun OnboardingPage2(isGranted: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "One permission\nto get started.",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Notify needs notification access to organize notifications you receive.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Mock Phone UI
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(240.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(8.dp, PrimarySoft, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isGranted) Success else Primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGranted) "Notification access connected" else "Notification access not connected",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingPage3() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Keep Notify\nrunning reliably.",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Some phones restrict apps running in the background. We'll help you check your battery settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Smartphone, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Device detected", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        Text(Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SettingsApplications, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Background activity", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        Text("Recommended unrestricted", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NotiVaultListenerService::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(cn.flattenToString()) == true
}
