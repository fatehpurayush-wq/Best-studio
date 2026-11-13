package com.beatstudio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val BeatStudioColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = StudioObsidian,
    primaryContainer = NeonCyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = PhonkCoral,
    onSecondary = TextPrimary,
    secondaryContainer = PhonkCoralDim,
    onSecondaryContainer = TextPrimary,
    tertiary = StudioAmber,
    onTertiary = StudioObsidian,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = StudioOutline,
    error = PhonkCoral,
    onError = TextPrimary
)

val BeatStudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BeatStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BeatStudioColorScheme,
        typography = BeatStudioTypography,
        shapes = BeatStudioShapes,
        content = content
    )
}
