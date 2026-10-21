package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = DeepViolet,
    onPrimaryContainer = Color.White,
    secondary = NeonCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5A1A2B),
    onSecondaryContainer = Color.White,
    tertiary = MintGreen,
    onTertiary = Color.Black,
    background = GameBackground,
    onBackground = Color.White,
    surface = GameSurface,
    onSurface = Color.White,
    surfaceVariant = GameCard,
    onSurfaceVariant = Color(0xFFD4D0E8),
    outline = GameCardBorder
)

@Composable
fun LudoLaddersTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
