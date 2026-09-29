package com.notivault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notivault.data.entity.NotificationEntity
import com.notivault.data.model.HealthStatus
import com.notivault.data.model.Tier
import com.notivault.ui.theme.*
import com.notivault.util.TimeUtils

/**
 * Primary Button (Dark Green background, 14px radius, 48px height)
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White,
            disabledContainerColor = PrimarySoft,
            disabledContentColor = Ink500
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

/**
 * Secondary Button (White background + border, 14px radius, 48px height)
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Ink950,
            disabledContainerColor = Color.White,
            disabledContentColor = Ink500
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Ink950)
    }
}

/**
 * Tertiary Button (Text only)
 */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Ink700)
    }
}

/**
 * Destructive Button (Soft red background)
 */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HighSoft,
            contentColor = High,
            disabledContainerColor = HighSoft.copy(alpha = 0.5f),
            disabledContentColor = Ink500
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = High)
    }
}

/**
 * Clean subtle Priority text indicator, instead of heavy chips.
 */
@Composable
fun PriorityChip(tier: String) {
    val (text, color) = when (tier) {
        Tier.HIGH -> Pair("HIGH", High)
        Tier.NORMAL -> Pair("NORMAL", Primary)
        else -> Pair("IGNORE", Ink500)
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = color
    )
}

/**
 * NotificationCard — clean, subtle border, 18px radius, matching the precise spec.
 */
@Composable
fun NotificationCard(
    notification: NotificationEntity,
    modifier: Modifier = Modifier,
    showAppLabel: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small App Icon placeholder (12px radius, 40x40)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimarySoft, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notification.appLabel.take(2).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = notification.senderName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink950,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showAppLabel) {
                            Text(
                                text = notification.appLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Ink500
                            )
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = TimeUtils.formatTime(notification.postTime),
                        style = MaterialTheme.typography.labelMedium,
                        color = Ink500
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (notification.conversationName != null) {
                Text(
                    text = notification.conversationName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = notification.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink700,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * HealthIndicator (Vault Ring style)
 */
@Composable
fun HealthIndicator(
    status: HealthStatus,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status) {
        HealthStatus.CONNECTED -> Pair(Success, "Connected")
        HealthStatus.DISCONNECTED -> Pair(Danger, "Disconnected")
        HealthStatus.UNKNOWN -> Pair(Warning, "Unknown")
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/**
 * EmptyState
 */
@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Ink950
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink500,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(32.dp))
            action()
        }
    }
}

/**
 * DateHeader
 */
@Composable
fun DateHeader(
    label: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = Ink500,
        modifier = modifier.padding(horizontal = 0.dp, vertical = 12.dp)
    )
}

/**
 * SectionHeader
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        color = Ink500,
        modifier = modifier.padding(horizontal = 0.dp, vertical = 8.dp)
    )
}
