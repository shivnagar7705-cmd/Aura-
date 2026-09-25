package com.example.aura.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.aura.model.Song
import kotlin.math.abs

/**
 * Vibrant dynamic color palette generated deterministically per song.
 */
data class SongDynamicPalette(
    val topColor: Color,
    val midColor: Color,
    val bottomColor: Color,
    val accentColor: Color,
    val iconType: Int // 0: MusicNote, 1: Headphones, 2: GraphicEq, 3: Radio
)

val DEFAULT_SONG_PALETTE = SongDynamicPalette(
    topColor = Color(0xFF6B21A8),
    midColor = Color(0xFF2E1065),
    bottomColor = Color(0xFF030712),
    accentColor = Color(0xFFA855F7),
    iconType = 0
)

private val CURATED_PALETTES = listOf(
    // 0: Ultra Electric Violet & Indigo Pulse
    SongDynamicPalette(
        topColor = Color(0xFF7E22CE),
        midColor = Color(0xFF3B0764),
        bottomColor = Color(0xFF090414),
        accentColor = Color(0xFFC084FC),
        iconType = 0
    ),
    // 1: Fiery Sunset Crimson & Ruby Glow
    SongDynamicPalette(
        topColor = Color(0xFFBE123C),
        midColor = Color(0xFF4C0519),
        bottomColor = Color(0xFF0D0205),
        accentColor = Color(0xFFFB7185),
        iconType = 2
    ),
    // 2: Cyber Cyan & Deep Oceanic Aqua
    SongDynamicPalette(
        topColor = Color(0xFF0369A1),
        midColor = Color(0xFF082F49),
        bottomColor = Color(0xFF020E17),
        accentColor = Color(0xFF38BDF8),
        iconType = 1
    ),
    // 3: Emerald Cyber Matrix & Jade Flare
    SongDynamicPalette(
        topColor = Color(0xFF047857),
        midColor = Color(0xFF064E3B),
        bottomColor = Color(0xFF011A12),
        accentColor = Color(0xFF34D399),
        iconType = 0
    ),
    // 4: Radiant Neon Magenta & Hot Fuchsia
    SongDynamicPalette(
        topColor = Color(0xFFA21CAF),
        midColor = Color(0xFF581C87),
        bottomColor = Color(0xFF14021C),
        accentColor = Color(0xFFF472B6),
        iconType = 2
    ),
    // 5: Golden Solar Flare & Warm Amber
    SongDynamicPalette(
        topColor = Color(0xFFB45309),
        midColor = Color(0xFF451A03),
        bottomColor = Color(0xFF120700),
        accentColor = Color(0xFFFBBF24),
        iconType = 1
    ),
    // 6: Deep Royal Sapphire & Electric Cobalt
    SongDynamicPalette(
        topColor = Color(0xFF1D4ED8),
        midColor = Color(0xFF172554),
        bottomColor = Color(0xFF050A1A),
        accentColor = Color(0xFF60A5FA),
        iconType = 3
    ),
    // 7: Electric Turquoise & Neo-Mint
    SongDynamicPalette(
        topColor = Color(0xFF0F766E),
        midColor = Color(0xFF134E4A),
        bottomColor = Color(0xFF021715),
        accentColor = Color(0xFF2DD4BF),
        iconType = 0
    ),
    // 8: Vivid Blood Orange & Tangerine Flame
    SongDynamicPalette(
        topColor = Color(0xFFC2410C),
        midColor = Color(0xFF431407),
        bottomColor = Color(0xFF120401),
        accentColor = Color(0xFFFB923C),
        iconType = 2
    ),
    // 9: Cosmic Purple & Stellar Amethyst
    SongDynamicPalette(
        topColor = Color(0xFF6D28D9),
        midColor = Color(0xFF2E1065),
        bottomColor = Color(0xFF0C0219),
        accentColor = Color(0xFFA78BFA),
        iconType = 1
    )
)

fun getSongDynamicPalette(song: Song?): SongDynamicPalette {
    if (song == null) return DEFAULT_SONG_PALETTE
    val key = "${song.id}_${song.title}_${song.artist}"
    val hash = abs(key.hashCode())
    return CURATED_PALETTES[hash % CURATED_PALETTES.size]
}

/**
 * Animated sound equalizer beats (3-4 jumping vertical bars).
 * Plays energetically when isPlaying is true.
 */
@Composable
fun AudioEqualizerBeats(
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    barColor: Color = Color.White,
    isPlaying: Boolean = true,
    barWidth: Dp = 3.dp,
    barMaxHeight: Dp = 16.dp,
    barSpacing: Dp = 2.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_beats")

    val h0 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 1.0f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar0"
    )
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isPlaying) 0.85f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 310, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isPlaying) 0.95f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 510, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = if (isPlaying) 0.80f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 370, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val heights = listOf(h0, h1, h2, h3)

    Row(
        modifier = modifier.height(barMaxHeight),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom
    ) {
        val count = barCount.coerceIn(2, 4)
        for (i in 0 until count) {
            val scale = heights[i % heights.size]
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(fraction = scale)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(barColor)
            )
        }
    }
}

/**
 * Dynamic Song Artwork Thumbnail:
 * - If coverUrl exists: displays Coil AsyncImage.
 * - If coverUrl is missing/empty: displays a UNIQUE colorful gradient + musical icon per song.
 * - If isCurrentlyPlaying == true: displays animated audio equalizer beats pulsing over the thumbnail!
 */
@Composable
fun SongArtworkThumbnail(
    song: Song,
    isCurrentlyPlaying: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    beatsSize: Dp = 14.dp
) {
    val palette = remember(song.id, song.title) { getSongDynamicPalette(song) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.topColor, palette.midColor, palette.bottomColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!song.coverUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = song.coverUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    DynamicPlaceholder(palette = palette)
                },
                loading = {
                    DynamicPlaceholder(palette = palette)
                }
            )
        } else {
            DynamicPlaceholder(palette = palette)
        }

        // Animated sound equalizer beats indicator overlay when song is actively playing!
        if (isCurrentlyPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), shape),
                contentAlignment = Alignment.Center
            ) {
                AudioEqualizerBeats(
                    barCount = 4,
                    barColor = Color.White,
                    isPlaying = true,
                    barWidth = 2.5.dp,
                    barMaxHeight = beatsSize,
                    barSpacing = 2.dp
                )
            }
        }
    }
}

@Composable
private fun DynamicPlaceholder(
    palette: SongDynamicPalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.topColor, palette.accentColor.copy(alpha = 0.8f), palette.midColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val icon = when (palette.iconType) {
            1 -> Icons.Default.Headphones
            2 -> Icons.Default.GraphicEq
            3 -> Icons.Default.Radio
            else -> Icons.Default.MusicNote
        }
        Icon(
            imageVector = icon,
            contentDescription = "Music",
            tint = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.size(24.dp)
        )
    }
}
