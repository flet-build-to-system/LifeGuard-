package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = EmeraldHealth,
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = EmeraldLight,
    secondary = GoldAccent,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = VioletAccent,
    onTertiary = Color(0xFF2E1065),
    background = DarkNavyBackground,
    onBackground = TextPrimary,
    surface = DarkNavySurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkNavyCard,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = Color.White
)

val LightColorScheme = lightColorScheme(
    primary = EmeraldHealth,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = EmeraldDark,
    secondary = GoldAccent,
    onSecondary = Color.White,
    tertiary = VioletAccent,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightCard,
    onSurfaceVariant = Color(0xFF475569),
    error = DangerRed,
    onError = Color.White
)

@Composable
fun LifeGuardTheme(
    darkTheme: Boolean = true, // Default to elegant dark theme per spec
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
