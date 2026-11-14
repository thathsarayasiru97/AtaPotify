package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AtaPotifyDarkColorScheme = darkColorScheme(
    primary = SpotifyEmerald,
    onPrimary = OnEmerald,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldSoftText,
    secondary = SpotifyEmeraldDeep,
    onSecondary = OnEmerald,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarning,
    onTertiary = OnEmerald,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberWarning,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceHighest,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle,
    error = CoralPending,
    onError = OnEmerald,
    errorContainer = CoralContainer,
    onErrorContainer = CoralPending,
)

private val AtaPotifyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun AtaPotifyTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AtaPotifyDarkColorScheme,
        typography = AtaPotifyTypography,
        shapes = AtaPotifyShapes,
        content = content,
    )
}
