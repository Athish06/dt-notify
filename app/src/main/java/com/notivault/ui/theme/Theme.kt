package com.notivault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Color Palette (from Design System)
// ─────────────────────────────────────────────────────────────────────────────

val Ink950 = Color(0xFF17201D)       // Main headings
val Ink700 = Color(0xFF47514D)       // Secondary text
val Ink500 = Color(0xFF7D8783)       // Supporting text
val SurfaceColor = Color(0xFFFAFBF9) // Main background
val White = Color(0xFFFFFFFF)        // Cards
val BorderColor = Color(0xFFE7EBE8)  // Dividers

val Primary = Color(0xFF315C52)       // Main actions
val PrimarySoft = Color(0xFFE8F0ED)   // Selected states

val High = Color(0xFFC75C4B)          // High priority
val HighSoft = Color(0xFFFBEDEA)      // High priority background

val Success = Color(0xFF3F8068)       // Connected
val SuccessSoft = Color(0xFFEAF4EF)   // Health status

val Warning = Color(0xFFB98535)       // Attention
val WarningSoft = Color(0xFFFAF2E3)   // Warning

val Danger = Color(0xFFB64B4B)        // Destructive

val OnPrimary = Color.White
val OnSurface = Ink950
val OnSurfaceVariant = Ink700

// Semantic colors
val HighPriorityColor = High
val NormalPriorityColor = Primary
val IgnorePriorityColor = Ink500
val HealthConnected = Success
val HealthDisconnected = Danger
val HealthUnknown = Warning

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = Primary,
    secondary = Primary,
    onSecondary = OnPrimary,
    secondaryContainer = PrimarySoft,
    onSecondaryContainer = Primary,
    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = SuccessSoft,
    onTertiaryContainer = Success,
    error = Danger,
    onError = Color.White,
    errorContainer = Danger.copy(alpha = 0.1f),
    onErrorContainer = Danger,
    background = SurfaceColor,
    onBackground = OnSurface,
    surface = SurfaceColor,
    onSurface = OnSurface,
    surfaceVariant = White,
    onSurfaceVariant = OnSurfaceVariant,
    outline = BorderColor,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = White
)

// The design uses a clean light theme.
private val DarkColorScheme = LightColorScheme 

// ─────────────────────────────────────────────────────────────────────────────
// Typography (Inter style exact mappings)
// ─────────────────────────────────────────────────────────────────────────────

val NotifyTypography = Typography(
    // Display: 32 / 38 / 700
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        color = Ink950
    ),
    // H1: 28 / 34 / 700
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
        color = Ink950
    ),
    // H2: 22 / 28 / 650 (SemiBold)
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        color = Ink950
    ),
    // H3: 17 / 23 / 600 (SemiBold)
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        color = Ink950
    ),
    // Body Large: 16 / 24 / 400
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = Ink950
    ),
    // Body: 14 / 21 / 400
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = Ink700
    ),
    // Label: 13 / 18 / 600
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = Ink950
    ),
    // Caption: 12 / 17 / 500
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = Ink500
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = Ink500
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// Theme
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NotiVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = NotifyTypography,
        content = content
    )
}
