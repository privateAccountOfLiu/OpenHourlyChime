package com.privacyaccountofliu.openhourlychime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEF8),
    secondary = Teal200,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFCCF5F0),
    tertiary = BlueMiku,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F5),
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFF666680),
    outline = Color(0xFFC0C0D0),
    outlineVariant = Color(0xFFE0E0EE),
)

private val DarkColors = darkColorScheme(
    primary = Purple200,
    onPrimary = Color.Black,
    primaryContainer = Purple500,
    secondary = Teal200,
    onSecondary = Color.Black,
    secondaryContainer = Teal700,
    tertiary = Teal200,
    background = Color(0xFF121218),
    surface = Color(0xFF1E1E28),
    surfaceVariant = Color(0xFF2A2A38),
    onBackground = Color(0xFFE8E8EE),
    onSurface = Color(0xFFE8E8EE),
    onSurfaceVariant = Color(0xFFB0B0C0),
    outline = Color(0xFF555570),
    outlineVariant = Color(0xFF3A3A4A),
)

@Composable
fun HourlyChimeTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
