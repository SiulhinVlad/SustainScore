package com.example.sustainscore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenDark,
    secondary = SageGreenDark,
    tertiary = SunlitClayDark,
    background = NightCanopy,
    surface = NightSurface,
    surfaceVariant = Color(0xFF243126),
    onPrimary = NightCanopy,
    onSecondary = NightCanopy,
    onTertiary = NightCanopy,
    onBackground = Color(0xFFEAF1E7),
    onSurface = Color(0xFFEAF1E7),
    onSurfaceVariant = Color(0xFFB9C8BB),
    outline = Color(0xFF405344)
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    secondary = SageGreen,
    tertiary = SunlitClay,
    background = MistBackground,
    surface = CardSurface,
    surfaceVariant = Color(0xFFE7EEE1),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A221B),
    onSurface = Color(0xFF1A221B),
    onSurfaceVariant = Color(0xFF5F6E61),
    outline = SoftOutline
)

@Composable
fun SustainScoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
