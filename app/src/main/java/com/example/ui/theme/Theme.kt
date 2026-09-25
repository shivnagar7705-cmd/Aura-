package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraDarkColorScheme = darkColorScheme(
    primary = AuraViolet,
    onPrimary = Color.White,
    primaryContainer = AuraVioletDeep,
    onPrimaryContainer = Color.White,
    secondary = AuraCyan,
    onSecondary = Color.Black,
    secondaryContainer = AuraSurfaceElevated,
    onSecondaryContainer = AuraTextPrimary,
    tertiary = AuraMagenta,
    onTertiary = Color.White,
    background = AuraBackground,
    onBackground = AuraTextPrimary,
    surface = AuraSurface,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceVariant,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraCardBorder
)

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioAmber,
    onPrimary = Color.White,
    primaryContainer = StudioSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = StudioAmberLight,
    onSecondary = Color.Black,
    secondaryContainer = StudioSurfaceVariant,
    onSecondaryContainer = StudioTextPrimary,
    background = StudioBackground,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioTextSecondary,
    outline = Color(0x33A855F7)
)

@Composable
fun AuraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun AuraStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AuraTheme(content = content)
}
