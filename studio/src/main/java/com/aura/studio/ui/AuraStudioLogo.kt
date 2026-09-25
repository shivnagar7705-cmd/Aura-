package com.aura.studio.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.studio.R

/**
 * EXACT AURA STUDIO LOGO WAVE 'A' STYLE
 * Recreates the exact SVG Wave 'A' design in Aura Studio
 */
@Composable
fun AuraStudioLogoEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "studio_wave_a_transition")

    val glowAlpha by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.70f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "studio_glow_pulse"
        )
    } else {
        remember { mutableStateOf(0.9f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            val scaleFactor = (w / 500f).coerceAtMost(h / 350f)
            val offsetX = (w - 500f * scaleFactor) / 2f
            val offsetY = (h - 350f * scaleFactor) / 2f

            // Ambient Lens Flare
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA020F0).copy(alpha = 0.50f * glowAlpha),
                        Color(0xFF00D4FF).copy(alpha = 0.30f * glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.50f, h * 0.48f),
                    radius = w * 0.55f
                )
            )

            // Scaled exact SVG paths
            val outerWavePath = Path().apply {
                moveTo(50f * scaleFactor + offsetX, 180f * scaleFactor + offsetY)
                quadraticBezierTo(
                    90f * scaleFactor + offsetX, 220f * scaleFactor + offsetY,
                    130f * scaleFactor + offsetX, 180f * scaleFactor + offsetY
                )
                cubicTo(
                    150f * scaleFactor + offsetX, 160f * scaleFactor + offsetY,
                    170f * scaleFactor + offsetX, 150f * scaleFactor + offsetY,
                    190f * scaleFactor + offsetX, 140f * scaleFactor + offsetY
                )
                cubicTo(
                    210f * scaleFactor + offsetX, 100f * scaleFactor + offsetY,
                    230f * scaleFactor + offsetX, 40f * scaleFactor + offsetY,
                    250f * scaleFactor + offsetX, 40f * scaleFactor + offsetY
                )
                cubicTo(
                    270f * scaleFactor + offsetX, 40f * scaleFactor + offsetY,
                    290f * scaleFactor + offsetX, 100f * scaleFactor + offsetY,
                    310f * scaleFactor + offsetX, 140f * scaleFactor + offsetY
                )
                cubicTo(
                    330f * scaleFactor + offsetX, 150f * scaleFactor + offsetY,
                    350f * scaleFactor + offsetX, 160f * scaleFactor + offsetY,
                    370f * scaleFactor + offsetX, 180f * scaleFactor + offsetY
                )
                quadraticBezierTo(
                    410f * scaleFactor + offsetX, 220f * scaleFactor + offsetY,
                    450f * scaleFactor + offsetX, 180f * scaleFactor + offsetY
                )
                cubicTo(
                    410f * scaleFactor + offsetX, 130f * scaleFactor + offsetY,
                    370f * scaleFactor + offsetX, 130f * scaleFactor + offsetY,
                    340f * scaleFactor + offsetX, 170f * scaleFactor + offsetY
                )
                cubicTo(
                    310f * scaleFactor + offsetX, 210f * scaleFactor + offsetY,
                    280f * scaleFactor + offsetX, 210f * scaleFactor + offsetY,
                    260f * scaleFactor + offsetX, 170f * scaleFactor + offsetY
                )
                lineTo(240f * scaleFactor + offsetX, 170f * scaleFactor + offsetY)
                cubicTo(
                    220f * scaleFactor + offsetX, 210f * scaleFactor + offsetY,
                    190f * scaleFactor + offsetX, 210f * scaleFactor + offsetY,
                    160f * scaleFactor + offsetX, 170f * scaleFactor + offsetY
                )
                cubicTo(
                    130f * scaleFactor + offsetX, 130f * scaleFactor + offsetY,
                    90f * scaleFactor + offsetX, 130f * scaleFactor + offsetY,
                    50f * scaleFactor + offsetX, 180f * scaleFactor + offsetY
                )
                close()
            }

            val waveGradBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF9D4EDD),
                    Color(0xFF00D4FF),
                    Color(0xFFBF55FF),
                    Color(0xFF0055FF)
                ),
                start = Offset(50f * scaleFactor + offsetX, 0f),
                end = Offset(450f * scaleFactor + offsetX, 0f)
            )

            val brightLineBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFF00FFFF),
                    Color(0xFFFF70A6)
                ),
                start = Offset(70f * scaleFactor + offsetX, 55f * scaleFactor + offsetY),
                end = Offset(430f * scaleFactor + offsetX, 180f * scaleFactor + offsetY)
            )

            // Outer Bloom Glow
            drawPath(
                path = outerWavePath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFA020F0).copy(alpha = 0.55f * glowAlpha),
                        Color(0xFF00D4FF).copy(alpha = 0.65f * glowAlpha),
                        Color(0xFFBF55FF).copy(alpha = 0.55f * glowAlpha),
                        Color(0xFF0055FF).copy(alpha = 0.45f * glowAlpha)
                    ),
                    start = Offset(50f * scaleFactor + offsetX, 0f),
                    end = Offset(450f * scaleFactor + offsetX, 0f)
                ),
                style = Stroke(
                    width = 24f * scaleFactor,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Main Wave Path (stroke-width: 12)
            drawPath(
                path = outerWavePath,
                brush = waveGradBrush,
                style = Stroke(
                    width = 12f * scaleFactor,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Inner Sound Ribbon Lines (stroke-width: 3)
            val innerRibbonPath = Path().apply {
                moveTo(70f * scaleFactor + offsetX, 180f * scaleFactor + offsetY)
                quadraticBezierTo(
                    110f * scaleFactor + offsetX, 200f * scaleFactor + offsetY,
                    150f * scaleFactor + offsetX, 175f * scaleFactor + offsetY
                )
                cubicTo(
                    200f * scaleFactor + offsetX, 80f * scaleFactor + offsetY,
                    230f * scaleFactor + offsetX, 55f * scaleFactor + offsetY,
                    250f * scaleFactor + offsetX, 55f * scaleFactor + offsetY
                )
                cubicTo(
                    270f * scaleFactor + offsetX, 55f * scaleFactor + offsetY,
                    300f * scaleFactor + offsetX, 80f * scaleFactor + offsetY,
                    350f * scaleFactor + offsetX, 175f * scaleFactor + offsetY
                )
                quadraticBezierTo(
                    390f * scaleFactor + offsetX, 200f * scaleFactor + offsetY,
                    430f * scaleFactor + offsetX, 180f * scaleFactor + offsetY
                )
            }

            drawPath(
                path = innerRibbonPath,
                brush = brightLineBrush,
                style = Stroke(
                    width = 3.2f * scaleFactor,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                alpha = 0.95f
            )

            // Center Horizontal Cross-bar Wave (stroke-width: 4)
            val crossBarPath = Path().apply {
                moveTo(180f * scaleFactor + offsetX, 170f * scaleFactor + offsetY)
                cubicTo(
                    210f * scaleFactor + offsetX, 160f * scaleFactor + offsetY,
                    290f * scaleFactor + offsetX, 160f * scaleFactor + offsetY,
                    320f * scaleFactor + offsetX, 170f * scaleFactor + offsetY
                )
            }

            drawPath(
                path = crossBarPath,
                color = Color(0xFF00FFFF).copy(alpha = 0.5f * glowAlpha),
                style = Stroke(width = 8f * scaleFactor, cap = StrokeCap.Round)
            )

            drawPath(
                path = crossBarPath,
                brush = brightLineBrush,
                style = Stroke(
                    width = 4.5f * scaleFactor,
                    cap = StrokeCap.Round
                )
            )

            // Center Glow Flare Node (cx: 250, cy: 165, r: 5)
            val flareCenterX = 250f * scaleFactor + offsetX
            val flareCenterY = 165f * scaleFactor + offsetY

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFF00FFFF).copy(alpha = 0.8f * glowAlpha),
                        Color(0xFFFF70A6).copy(alpha = 0.3f * glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(flareCenterX, flareCenterY),
                    radius = 16f * scaleFactor
                ),
                radius = 16f * scaleFactor,
                center = Offset(flareCenterX, flareCenterY)
            )

            drawCircle(
                color = Color.White,
                radius = 5.2f * scaleFactor,
                center = Offset(flareCenterX, flareCenterY)
            )
        }
    }
}

/**
 * Real Master Graphic Image Logo for Aura Studio
 */
@Composable
fun AuraStudioImageLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "studio_img_pulse")
    val pulseScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val glowAlpha by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else {
        remember { mutableStateOf(0.7f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA020F0).copy(alpha = 0.55f * glowAlpha),
                        Color(0xFF00D4FF).copy(alpha = 0.35f * glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                    radius = this.size.width * 0.55f
                )
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_aura_logo),
            contentDescription = "Aura Studio Logo",
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale),
            contentScale = ContentScale.Fit
        )
    }
}

