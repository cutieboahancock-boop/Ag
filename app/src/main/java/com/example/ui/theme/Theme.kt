package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioCoral,
    onPrimary = Color.White,
    primaryContainer = StudioCoralDark,
    onPrimaryContainer = StudioCoralSoft,
    secondary = StudioMint,
    onSecondary = StudioObsidian,
    secondaryContainer = StudioMintContainer,
    onSecondaryContainer = StudioMint,
    tertiary = StudioAmber,
    onTertiary = StudioObsidian,
    tertiaryContainer = StudioAmberContainer,
    onTertiaryContainer = StudioAmber,
    background = StudioObsidian,
    onBackground = StudioInk,
    surface = StudioGraphite,
    onSurface = StudioInk,
    surfaceVariant = StudioSlate,
    onSurfaceVariant = StudioInkMuted,
    surfaceContainerLowest = StudioObsidian,
    surfaceContainerLow = StudioGraphite,
    surfaceContainer = StudioSlate,
    surfaceContainerHigh = StudioSlateElevated,
    outline = StudioOutline,
    outlineVariant = StudioSlateElevated,
    error = Color(0xFFFF6B6B),
    onError = StudioObsidian
)

private val StudioLightColorScheme = lightColorScheme(
    primary = LightCoral,
    onPrimary = Color.White,
    primaryContainer = LightCoralContainer,
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF007A60),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5F7E8),
    onSecondaryContainer = Color(0xFF002118),
    tertiary = Color(0xFFA86B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE5B8),
    onTertiaryContainer = Color(0xFF331E00),
    background = LightCanvas,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightInkMuted,
    outline = LightOutline
)

val ClipForgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClipForgeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ClipForgeShapes,
        content = content
    )
}
