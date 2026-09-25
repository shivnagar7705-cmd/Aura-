package com.example.aura.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.ui.components.AuraImageLogo
import com.example.aura.ui.components.AuraLogoEmblem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Breathtaking, cinematic Intro & Splash Animation for AURA Music.
 * Features:
 * - Expanding multi-ring acoustic sonic ripples (Shockwave ripples)
 * - Luminous pulsing emblem with cosmic glow
 * - Dynamic live equalizer frequency audio spectrum bars
 * - Shimmering luxury brand typography: "A U R A • FEEL EVERY BEAT"
 */
@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    // 1. Entrance and scale animations
    val entranceAnim = remember { Animatable(0f) }
    val emblemScale = remember { Animatable(0.7f) }
    val exitAlpha = remember { Animatable(1f) }

    // 2. Continuous cosmic & audio ripple animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash_cinematic_pulse")

    // Expanding sonic soundwave ripples
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleProgress"
    )

    // Breathing glow
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowIntensity"
    )

    // Equalizer rhythm frequency phase
    val eqPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eqPhase"
    )

    // Shimmer highlight for title
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    LaunchedEffect(Unit) {
        // Smooth entrance
        entranceAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
        emblemScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Hold cinematic presentation for 2.2 seconds
        delay(2200)
        // Smooth exit fade
        exitAlpha.animateTo(0f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
            .alpha(exitAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        // Canvas: Sonic Soundwave Rings & Cosmic Starlight Ambient
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h * 0.44f

            // Atmospheric Ambient Cosmic Glow (Behind Emblem)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AuraViolet.copy(alpha = 0.35f * glowIntensity),
                        AuraCyan.copy(alpha = 0.20f * glowIntensity),
                        AuraMagenta.copy(alpha = 0.12f * glowIntensity),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = w * 0.70f
                )
            )

            // Dynamic Futuristic Horizontal Frequency Light Beams across the background
            val beamCount = 4
            for (i in 0 until beamCount) {
                val yOffset = centerY - (w * 0.20f) + (i * (w * 0.13f))
                val waveShift = sin(eqPhase + i.toFloat()).toFloat() * (w * 0.08f)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (i % 2 == 0) AuraCyan.copy(alpha = 0.25f * glowIntensity) else AuraViolet.copy(alpha = 0.30f * glowIntensity),
                            if (i % 2 == 0) AuraViolet.copy(alpha = 0.35f * glowIntensity) else AuraPink.copy(alpha = 0.25f * glowIntensity),
                            Color.Transparent
                        ),
                        startX = centerX - (w * 0.45f) + waveShift,
                        endX = centerX + (w * 0.45f) + waveShift
                    ),
                    start = Offset(centerX - (w * 0.45f) + waveShift, yOffset),
                    end = Offset(centerX + (w * 0.45f) + waveShift, yOffset),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Twinkling Starlight Sparkles
            val stars = listOf(
                Pair(0.18f, 0.20f), Pair(0.82f, 0.18f), Pair(0.12f, 0.72f),
                Pair(0.88f, 0.75f), Pair(0.75f, 0.35f), Pair(0.25f, 0.45f)
            )
            for (star in stars) {
                val starX = w * star.first
                val starY = h * star.second
                val starPulse = (sin(eqPhase + star.first * 10).toFloat() + 1f) / 2f
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f + starPulse * 0.55f),
                    radius = (1.5f + starPulse * 2f).dp.toPx(),
                    center = Offset(starX, starY)
                )
            }
        }

        // Center Hero Presentation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(emblemScale.value)
                .alpha(entranceAnim.value)
                .padding(horizontal = 24.dp)
        ) {
            // Official AURA Live Glowing Neon Emblem (Matches Reference Image exactly: Letter 'A' + Cosmic Soundwave Ribbon)
            AuraImageLogo(
                size = 140.dp,
                animated = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Luxury Brand Typography "A U R A"
            Text(
                text = "A U R A",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            AuraCyanBright,
                            Color(0xFFFDF4FF),
                            AuraViolet,
                            AuraPink
                        ),
                        startX = shimmerOffset,
                        endX = shimmerOffset + 300f
                    )
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline matching exact design spec
            Text(
                text = "FUTURE SOUNDS & AI MUSIC",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = Color(0xFF8A99AD)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "LOSSLESS AUDIO STREAMING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = AuraCyan.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Dynamic Equalizer Frequency Spectrum Bars (Live Sound Wave Visualizer)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(32.dp)
            ) {
                val barCount = 7
                for (i in 0 until barCount) {
                    val normalizedIndex = i.toFloat() / barCount
                    val barHeightFraction = (sin(eqPhase + normalizedIndex * Math.PI * 2.5).toFloat() + 1f) / 2f
                    val heightDp = (8 + (barHeightFraction * 20)).dp

                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(heightDp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        AuraCyan,
                                        if (i % 2 == 0) AuraViolet else AuraPink
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
