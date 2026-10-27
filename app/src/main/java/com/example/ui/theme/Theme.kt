package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = Slate800,
    onSecondaryContainer = CyanAccent,
    tertiary = AmberLight,
    onTertiary = Color.Black,
    tertiaryContainer = Slate800,
    onTertiaryContainer = AmberLight,
    background = Slate950,
    onBackground = Slate200,
    surface = Slate900,
    onSurface = Slate200,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    error = CrimsonError,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme(
    // Retain dark aesthetics for server terminal console tool even in light mode
    primary = EmeraldGreen,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    background = Slate950,
    onBackground = Slate200,
    surface = Slate900,
    onSurface = Slate200,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
