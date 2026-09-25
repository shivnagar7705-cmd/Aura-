package com.aura.studio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// AURA Studio Theme: Cosmic Obsidian, Electric Violet & Cyber Cyan
val StudioAmber = Color(0xFFA855F7)        // Signature Electric Violet primary
val StudioAmberLight = Color(0xFF00F5D4)   // Radiant Cyber Cyan highlight
val StudioBackground = Color(0xFF080812)   // Deep Cosmic Obsidian Space Void
val StudioSurface = Color(0xFF101222)      // Sleek dark indigo-charcoal
val StudioSurfaceVariant = Color(0xFF181A32)
val StudioCardBorder = Color(0x33A855F7)   // Neon violet glow border
val StudioTextPrimary = Color(0xFFFFFFFF)  // Crisp white
val StudioTextSecondary = Color(0xFF94A3B8)
val StudioTextMuted = Color(0xFF64748B)
val StudioGreen = Color(0xFF10B981)
val StudioRed = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = StudioAmber,
    onPrimary = Color.White,
    primaryContainer = Color(0x33A855F7),
    onPrimaryContainer = StudioAmberLight,
    secondary = Color(0xFF00F5D4),
    onSecondary = Color.Black,
    background = StudioBackground,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioTextSecondary,
    error = StudioRed,
    onError = Color.White
)

@Composable
fun AuraStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
