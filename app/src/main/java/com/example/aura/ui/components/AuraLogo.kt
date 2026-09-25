package com.example.aura.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * EXACT AURA LOGO WAVE 'A' STYLE
 * Implements the user's exact SVG geometry and aesthetic:
 * viewBox: 0 0 500 350
 *
 * Gradients:
 * - waveGrad: #9D4EDD (0%) -> #00D4FF (35%) -> #BF55FF (65%) -> #0055FF (100%)
 * - brightLine: #FFFFFF (0%) -> #00FFFF (50%) -> #FF70A6 (100%)
 *
 * Paths:
 * 1. Main Outer Sound Wave Path loop forming 'A' (stroke-width: 12)
 * 2. Inner Sound Ribbon Lines for 3D Mesh Wave Effect (stroke-width: 3, opacity: 0.9)
 * 3. Center Horizontal Cross-bar Wave (stroke-width: 4)
 * 4. Center Glow Flare Node (cx: 250, cy: 165, r: 5)
 */
@Composable
fun AuraLogoEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_wave_a_transition")

    val glowAlpha by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.70f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aura_glow_pulse"
        )
    } else {
        remember { mutableStateOf(0.9f) }
    }

    val wavePhase by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(3500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "aura_wave_phase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Maintain exact aspect ratio: 500 x 350
            val scaleFactor = (w / 500f).coerceAtMost(h / 350f)
            val offsetX = (w - 500f * scaleFactor) / 2f
            val offsetY = (h - 350f * scaleFactor) / 2f

            // 1. Multi-layered Ambient Back Glow (Violet & Cyan Lens Flare as defined in CSS filter)
            // filter: drop-shadow(0px 0px 20px rgba(160, 32, 240, 0.6)) drop-shadow(0px 0px 40px rgba(0, 212, 255, 0.4));
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
                // Smooth continuation to 190, 140
                cubicTo(
                    150f * scaleFactor + offsetX, 160f * scaleFactor + offsetY,
                    170f * scaleFactor + offsetX, 150f * scaleFactor + offsetY,
                    190f * scaleFactor + offsetX, 140f * scaleFactor + offsetY
                )
                // Apex climb left
                cubicTo(
                    210f * scaleFactor + offsetX, 100f * scaleFactor + offsetY,
                    230f * scaleFactor + offsetX, 40f * scaleFactor + offsetY,
                    250f * scaleFactor + offsetX, 40f * scaleFactor + offsetY
                )
                // Apex descent right
                cubicTo(
                    270f * scaleFactor + offsetX, 40f * scaleFactor + offsetY,
                    290f * scaleFactor + offsetX, 100f * scaleFactor + offsetY,
                    310f * scaleFactor + offsetX, 140f * scaleFactor + offsetY
                )
                // Smooth continuation to 370, 180
                cubicTo(
                    330f * scaleFactor + offsetX, 150f * scaleFactor + offsetY,
                    350f * scaleFactor + offsetX, 160f * scaleFactor + offsetY,
                    370f * scaleFactor + offsetX, 180f * scaleFactor + offsetY
                )
                // Right wave loop
                quadraticBezierTo(
                    410f * scaleFactor + offsetX, 220f * scaleFactor + offsetY,
                    450f * scaleFactor + offsetX, 180f * scaleFactor + offsetY
                )
                // Inner underbelly curve right
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
                // Center bridge link
                lineTo(240f * scaleFactor + offsetX, 170f * scaleFactor + offsetY)
                cubicTo(
                    220f * scaleFactor + offsetX, 210f * scaleFactor + offsetY,
                    190f * scaleFactor + offsetX, 210f * scaleFactor + offsetY,
                    160f * scaleFactor + offsetX, 170f * scaleFactor + offsetY
                )
                // Inner underbelly curve left back to start
                cubicTo(
                    130f * scaleFactor + offsetX, 130f * scaleFactor + offsetY,
                    90f * scaleFactor + offsetX, 130f * scaleFactor + offsetY,
                    50f * scaleFactor + offsetX, 180f * scaleFactor + offsetY
                )
                close()
            }

            // Gradient: waveGrad (#9D4EDD -> #00D4FF -> #BF55FF -> #0055FF)
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

            // Gradient: brightLine (#FFFFFF -> #00FFFF -> #FF70A6)
            val brightLineBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFF00FFFF),
                    Color(0xFFFF70A6)
                ),
                start = Offset(70f * scaleFactor + offsetX, 55f * scaleFactor + offsetY),
                end = Offset(430f * scaleFactor + offsetX, 180f * scaleFactor + offsetY)
            )

            // A. Outer Bloom Glow for the main wave path
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

            // B. Exact Main Outer Sound Wave Path loop forming 'A' (stroke-width: 12)
            drawPath(
                path = outerWavePath,
                brush = waveGradBrush,
                style = Stroke(
                    width = 12f * scaleFactor,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Exact Inner Sound Ribbon Lines for 3D Mesh Wave Effect (stroke-width: 3)
            // d="M 70,180 Q 110,200 150,175 C 200,80 230,55 250,55 C 270,55 300,80 350,175 Q 390,200 430,180"
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

            // 3. Exact Center Horizontal Cross-bar Wave (A letter ki beech wali line, stroke-width: 4)
            // d="M 180,170 C 210,160 290,160 320,170"
            val crossBarPath = Path().apply {
                moveTo(180f * scaleFactor + offsetX, 170f * scaleFactor + offsetY)
                cubicTo(
                    210f * scaleFactor + offsetX, 160f * scaleFactor + offsetY,
                    290f * scaleFactor + offsetX, 160f * scaleFactor + offsetY,
                    320f * scaleFactor + offsetX, 170f * scaleFactor + offsetY
                )
            }

            // Cross-bar subtle bloom
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

            // 4. Exact Glow Center Flare Node (cx: 250, cy: 165, r: 5)
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

            // Subtle starlight micro-sparkles matching the luxury future-sound aesthetic
            drawSparkle(390f * scaleFactor + offsetX, 95f * scaleFactor + offsetY, 6f * scaleFactor * glowAlpha, Color(0xFFE0F2FE))
            drawSparkle(110f * scaleFactor + offsetX, 125f * scaleFactor + offsetY, 5f * scaleFactor * glowAlpha, Color(0xFFFDF4FF))
        }
    }
}

private fun DrawScope.drawSparkle(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - radius)
        quadraticBezierTo(cx, cy, cx + radius, cy)
        quadraticBezierTo(cx, cy, cx, cy + radius)
        quadraticBezierTo(cx, cy, cx - radius, cy)
        quadraticBezierTo(cx, cy, cx, cy - radius)
        close()
    }
    drawPath(path = path, color = color)
}

/**
 * Full AURA Brand Presentation with typography matching the exact HTML/CSS spec:
 * - AURA (Font weight 800, letter-spacing 12px, #FFFFFF)
 * - FUTURE SOUNDS & AI MUSIC (Letter-spacing 4px, #8A99AD)
 */
@Composable
fun AuraBrandHeader(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 150.dp,
    titleSize: Dp = 34.dp,
    showSubtitle: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuraLogoEmblem(
            size = emblemSize,
            animated = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // "AURA" with crisp white bold lettering & wide letter spacing
        Text(
            text = "AURA",
            fontSize = titleSize.value.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 8.sp,
            color = Color(0xFFFFFFFF)
        )

        if (showSubtitle) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "FUTURE SOUNDS & AI MUSIC",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = Color(0xFF8A99AD)
            )
        }
    }
}

/**
 * High-definition Raster Logo directly rendered from the official asset,
 * with breathing cosmic neon glow aura and smooth scaling animation.
 */
@Composable
fun AuraImageLogo(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_img_pulse")
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
        // Multi-layer Ambient Backglow
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
            contentDescription = "AURA Music Logo",
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Full AURA Image Banner with title and subtitle
 */
@Composable
fun AuraImageBanner(
    modifier: Modifier = Modifier,
    width: Dp = 260.dp
) {
    Image(
        painter = painterResource(id = R.drawable.aura_banner_logo),
        contentDescription = "AURA - Future Sounds & AI Music",
        modifier = modifier.width(width),
        contentScale = ContentScale.Fit
    )
}

