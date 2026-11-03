package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FbBlue,
    onPrimary = Color.White,
    primaryContainer = FbBlueHover,
    onPrimaryContainer = Color.White,
    secondary = FbBlueLight,
    onSecondary = FbTextPrimary,
    background = Color(0xFF18191A),
    onBackground = Color(0xFFE4E6EB),
    surface = Color(0xFF242526),
    onSurface = Color(0xFFE4E6EB),
    surfaceVariant = Color(0xFF3A3B3C),
    onSurfaceVariant = Color(0xFFB0B3B8),
    outline = Color(0xFF3E4042),
    outlineVariant = Color(0xFF3E4042)
)

private val LightColorScheme = lightColorScheme(
    primary = FbBlue,                  // #1877F2 primary blue
    onPrimary = Color.White,
    primaryContainer = FbBlueLight,
    onPrimaryContainer = FbBlue,
    secondary = FbBlue,
    onSecondary = Color.White,
    background = FbBackground,          // #F0F2F5 main app background
    onBackground = FbTextPrimary,       // #1C1E21 primary text
    surface = FbSurface,                // #FFFFFF pure white content & cards
    onSurface = FbTextPrimary,          // #1C1E21
    surfaceVariant = FbInputBackground, // #F0F2F5 input fields
    onSurfaceVariant = FbTextSecondary, // #65676B secondary text
    outline = FbBorder,                 // #DADDE1 dividers & borders
    outlineVariant = FbDivider
)

@Composable
fun YarkhoonTheme(
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
