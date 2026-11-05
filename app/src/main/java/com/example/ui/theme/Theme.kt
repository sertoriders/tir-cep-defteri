package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = NavyDarkest,
    primaryContainer = BluePrimary,
    onPrimaryContainer = PureWhite,
    secondary = BlueAccent,
    onSecondary = PureWhite,
    secondaryContainer = NavySurfaceVariant,
    onSecondaryContainer = PureWhite,
    tertiary = StatusSuccess,
    onTertiary = PureWhite,
    background = NavyBackground,
    onBackground = OffWhite,
    surface = NavySurface,
    onSurface = OffWhite,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = LightGray,
    error = StatusDanger,
    onError = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = BluePrimary,
    secondary = BlueAccent,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = BluePrimary,
    tertiary = StatusSuccess,
    onTertiary = PureWhite,
    background = Color(0xFFF1F5F9),
    onBackground = NavyDarkest,
    surface = PureWhite,
    onSurface = NavyDarkest,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = DarkGray,
    error = StatusDanger,
    onError = PureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to modern dark navy as requested
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
