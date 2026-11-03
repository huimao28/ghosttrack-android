package com.huimao.ghosttrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF315DA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF0B2F63),
    secondary = Color(0xFF006B5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF9FF2DF),
    onSecondaryContainer = Color(0xFF00201B),
    tertiary = Color(0xFF735471),
    tertiaryContainer = Color(0xFFFFD7F8),
    background = Color(0xFFF8F9FD),
    surface = Color(0xFFF8F9FD),
    surfaceVariant = Color(0xFFE1E5EE),
    surfaceContainer = Color(0xFFF0F2F8),
    surfaceContainerLow = Color(0xFFF5F6FB),
    surfaceContainerHigh = Color(0xFFE9EBF2),
    outline = Color(0xFF747781),
    outlineVariant = Color(0xFFC4C7D0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF17477F),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFF83D5C3),
    onSecondary = Color(0xFF00372F),
    secondaryContainer = Color(0xFF005046),
    onSecondaryContainer = Color(0xFF9FF2DF),
    tertiary = Color(0xFFE2BBDD),
    tertiaryContainer = Color(0xFF5A3D58),
    background = Color(0xFF101318),
    surface = Color(0xFF101318),
    surfaceVariant = Color(0xFF43474F),
    surfaceContainer = Color(0xFF1C2026),
    surfaceContainerLow = Color(0xFF181C22),
    surfaceContainerHigh = Color(0xFF272A31),
    outline = Color(0xFF8E919A),
    outlineVariant = Color(0xFF44474F),
)

private val GhostTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun GhostTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = GhostTrackShapes,
        content = content,
    )
}
