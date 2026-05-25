package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = YarkhwoonBlue,
    secondary = YarkhwoonSecondary,
    tertiary = YarkhwoonGreen,
    background = DarkGrayBackground,
    surface = DarkGraySurface,
    onPrimary = LightGraySurface,
    onSecondary = LightGraySurface,
    onBackground = LightGraySurface,
    onSurface = LightGraySurface
)

private val LightColorScheme = lightColorScheme(
    primary = YarkhwoonBlue,
    secondary = YarkhwoonSecondary,
    tertiary = YarkhwoonGreen,
    background = LightGrayBackground,
    surface = LightGraySurface,
    onPrimary = LightGraySurface,
    onSecondary = LightGraySurface,
    onBackground = DarkGrayBackground,
    onSurface = DarkGrayBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to enforce our branded Yarkhwoon palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
