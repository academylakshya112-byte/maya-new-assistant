package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.live.ZoyaState
import kotlin.math.cos
import kotlin.math.sin

/**
 * 100% Procedural Animated 3D Cosmic Planet Centerpiece matching the reference screenshot.
 * Features:
 * - 3D shaded glowing celestial sphere with internal atmospheric swirl
 * - Radiant magenta/pink sunrise flare arc on bottom-left rim with soft light beams
 * - 3D dual-layer planetary Saturn-like rings with front/back depth occlusion
 * - Multiple mini moons & satellites orbiting on 3D elliptical wire tracks
 * - Responsive energy pulse for Listening, Thinking, and Speaking states
 * - NO static image; completely rendered natively in Compose Canvas!
 */
@Composable
fun MayaCosmicOrbView(
    state: ZoyaState,
    modifier: Modifier = Modifier,
    orbSize: Dp = 175.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_orb_canvas")

    // 1. Soft Vertical Hover Floating Motion
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hover_offset"
    )

    // 2. Smooth Breathing Scale
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // 3. Orbit Rotation for Moons & Satellites (16 seconds per revolution)
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    // 4. Planet Internal Atmosphere Rotation (24 seconds per spin)
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    // 5. Core Luminescence & Audio Pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (state == ZoyaState.SPEAKING) 600
                else if (state == ZoyaState.LISTENING) 900
                else 2200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // 6. Ring Shimmer Wave
    val ringShimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_shimmer"
    )

    Box(
        modifier = modifier
            .size(orbSize * 1.35f)
            .offset(y = hoverOffset.dp)
            .scale(breathScale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val planetRadius = size.minDimension * 0.28f
            val ringTilt = -26f // Planetary ring tilt angle in degrees

            // -------------------------------------------------------------
            // STEP 1: Ambient Background Atmosphere & Radiant Cosmic Aura
            // -------------------------------------------------------------
            val ambientGlowColor = when (state) {
                ZoyaState.LISTENING -> Color(0xFF00E5FF)
                ZoyaState.THINKING -> Color(0xFF818CF8)
                ZoyaState.SPEAKING -> Color(0xFF38BDF8)
                else -> Color(0xFF60A5FA)
            }

            // Outer soft cosmic nebula cloud
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambientGlowColor.copy(alpha = 0.30f * glowPulse),
                        Color(0xFF3B82F6).copy(alpha = 0.16f * glowPulse),
                        Color(0xFFA855F7).copy(alpha = 0.08f * glowPulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.58f
                ),
                radius = size.minDimension * 0.58f,
                center = center
            )

            // Bottom-Left Ambient Radiant Sunrise Burst (Matching image)
            val flareCenter = Offset(center.x - planetRadius * 0.65f, center.y + planetRadius * 0.65f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF2A85).copy(alpha = 0.45f * glowPulse),
                        Color(0xFFFDA4AF).copy(alpha = 0.28f * glowPulse),
                        Color(0xFF60A5FA).copy(alpha = 0.15f * glowPulse),
                        Color.Transparent
                    ),
                    center = flareCenter,
                    radius = planetRadius * 1.5f
                ),
                radius = planetRadius * 1.5f,
                center = flareCenter
            )

            // Soft diagonal light rays from bottom left
            val rayPath = Path().apply {
                moveTo(flareCenter.x, flareCenter.y)
                lineTo(flareCenter.x - planetRadius * 1.4f, flareCenter.y + planetRadius * 1.4f)
                lineTo(flareCenter.x + planetRadius * 1.4f, flareCenter.y - planetRadius * 1.4f)
                close()
            }
            drawPath(
                path = rayPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF80BF).copy(alpha = 0.22f * glowPulse),
                        Color.Transparent
                    ),
                    start = flareCenter,
                    end = Offset(flareCenter.x - planetRadius * 1.2f, flareCenter.y + planetRadius * 1.2f)
                )
            )

            // -------------------------------------------------------------
            // STEP 2: Back Orbit Tracks & Moons with z < 0 (Behind Planet)
            // -------------------------------------------------------------
            drawOrbitTracksAndMoons(
                center = center,
                planetRadius = planetRadius,
                orbitAngle = orbitAngle,
                glowPulse = glowPulse,
                drawFront = false
            )

            // -------------------------------------------------------------
            // STEP 3: Back Half of Planetary Saturn Rings (Behind Planet)
            // -------------------------------------------------------------
            drawPlanetaryRings(
                center = center,
                planetRadius = planetRadius,
                tiltDegrees = ringTilt,
                shimmer = ringShimmer,
                drawFront = false
            )

            // -------------------------------------------------------------
            // STEP 4: Central Procedural 3D Cosmic Planet Sphere
            // -------------------------------------------------------------
            val planetPath = Path().apply {
                addOval(Rect(center, planetRadius))
            }

            clipPath(planetPath) {
                // A. Base 3D Shaded Spherical Gradient (Deep Indigo -> Electric Blue -> Cyan)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF67E8F9), // Specular Cyan Light
                            Color(0xFF38BDF8), // Electric Light Blue
                            Color(0xFF2563EB), // Royal Blue Midtones
                            Color(0xFF1D4ED8), // Deep Blue Body
                            Color(0xFF4338CA), // Deep Indigo / Violet
                            Color(0xFF1E1B4B)  // Dark Shadow Rim
                        ),
                        center = Offset(center.x - planetRadius * 0.22f, center.y - planetRadius * 0.18f),
                        radius = planetRadius * 1.25f
                    ),
                    radius = planetRadius,
                    center = center
                )

                // B. Internal Fluid Planetary Auroral Swirls (Rotating dynamically)
                rotate(degrees = spinAngle, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF818CF8).copy(alpha = 0.25f),
                                Color(0xFF06B6D4).copy(alpha = 0.35f),
                                Color(0xFFC084FC).copy(alpha = 0.25f),
                                Color(0xFF1E1B4B).copy(alpha = 0.40f),
                                Color(0xFF818CF8).copy(alpha = 0.25f)
                            ),
                            center = center
                        ),
                        radius = planetRadius,
                        center = center
                    )

                    // Planetary cloud bands
                    val bandY1 = center.y - planetRadius * 0.35f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color(0xFF38BDF8).copy(alpha = 0.22f), Color.Transparent)
                        ),
                        topLeft = Offset(center.x - planetRadius, bandY1),
                        size = Size(planetRadius * 2, planetRadius * 0.32f)
                    )

                    val bandY2 = center.y + planetRadius * 0.15f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color(0xFFA855F7).copy(alpha = 0.20f), Color.Transparent)
                        ),
                        topLeft = Offset(center.x - planetRadius, bandY2),
                        size = Size(planetRadius * 2, planetRadius * 0.28f)
                    )
                }

                // C. Bottom-Left Intense Radiant Flare Crescent (Sunrise on the planet rim)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,                        // Pure incandescent core
                            Color(0xFFFF85A1),                  // Radiant coral pink
                            Color(0xFFFF2A85),                  // Electric magenta
                            Color(0xFF9333EA).copy(alpha = 0.7f), // Purple twilight
                            Color.Transparent
                        ),
                        center = Offset(center.x - planetRadius * 0.55f, center.y + planetRadius * 0.55f),
                        radius = planetRadius * 0.95f
                    ),
                    radius = planetRadius,
                    center = center
                )

                // D. Top-Right Specular Atmosphere Rim Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color(0xFFBAE6FD).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = Offset(center.x + planetRadius * 0.45f, center.y - planetRadius * 0.45f),
                        radius = planetRadius * 0.65f
                    ),
                    radius = planetRadius,
                    center = center
                )

                // E. Inner Sharp Equatorial Neon Line (Cutting across the front of the globe)
                rotate(degrees = ringTilt, pivot = center) {
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF38BDF8).copy(alpha = 0.6f),
                                Color.White,
                                Color(0xFF67E8F9).copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(center.x - planetRadius * 0.96f, center.y),
                        end = Offset(center.x + planetRadius * 0.96f, center.y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Outer atmospheric rim border glow
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF67E8F9).copy(alpha = 0.7f),
                        Color(0xFF38BDF8).copy(alpha = 0.4f),
                        Color(0xFFFF2A85).copy(alpha = 0.9f), // Bright pink at bottom-left
                        Color(0xFFC084FC).copy(alpha = 0.6f),
                        Color(0xFF67E8F9).copy(alpha = 0.7f)
                    ),
                    center = center
                ),
                radius = planetRadius,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )

            // -------------------------------------------------------------
            // STEP 5: Front Half of Planetary Saturn Rings (In Front of Planet)
            // -------------------------------------------------------------
            drawPlanetaryRings(
                center = center,
                planetRadius = planetRadius,
                tiltDegrees = ringTilt,
                shimmer = ringShimmer,
                drawFront = true
            )

            // -------------------------------------------------------------
            // STEP 6: Front Orbit Tracks & Moons with z >= 0 (In Front of Planet)
            // -------------------------------------------------------------
            drawOrbitTracksAndMoons(
                center = center,
                planetRadius = planetRadius,
                orbitAngle = orbitAngle,
                glowPulse = glowPulse,
                drawFront = true
            )

            // -------------------------------------------------------------
            // STEP 7: Ambient Cosmic Dust Stars & Sparkles
            // -------------------------------------------------------------
            val sparkles = listOf(
                Offset(center.x - planetRadius * 1.55f, center.y - planetRadius * 0.65f),
                Offset(center.x + planetRadius * 1.62f, center.y - planetRadius * 0.82f),
                Offset(center.x + planetRadius * 1.48f, center.y + planetRadius * 0.95f),
                Offset(center.x - planetRadius * 1.25f, center.y + planetRadius * 1.35f),
                Offset(center.x - planetRadius * 0.85f, center.y - planetRadius * 1.45f),
                Offset(center.x + planetRadius * 0.75f, center.y - planetRadius * 1.55f)
            )

            sparkles.forEachIndexed { idx, pt ->
                val starPulse = 0.5f + 0.5f * sin((orbitAngle * 1.8f + idx * 60f) * Math.PI.toFloat() / 180f)
                val starRadius = (1.5.dp.toPx() + 1.2.dp.toPx() * starPulse)
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * starPulse),
                    radius = starRadius,
                    center = pt
                )
                drawCircle(
                    color = Color(0xFF67E8F9).copy(alpha = 0.45f * starPulse),
                    radius = starRadius * 2.5f,
                    center = pt
                )
            }
        }
    }
}

/**
 * Draws the 3D Saturn-like Planetary Rings with realistic depth occlusion
 */
private fun DrawScope.drawPlanetaryRings(
    center: Offset,
    planetRadius: Float,
    tiltDegrees: Float,
    shimmer: Float,
    drawFront: Boolean
) {
    val ringMajorA = planetRadius * 1.72f // Outer major radius
    val ringMinorB = planetRadius * 0.58f // Outer minor radius

    val innerMajorA = planetRadius * 1.22f // Inner major radius
    val innerMinorB = planetRadius * 0.40f // Inner minor radius

    rotate(degrees = tiltDegrees, pivot = center) {
        val sweepStartAngle = if (drawFront) 0f else 180f
        val sweepAngle = 180f

        // Outer Translucent Luminous Ring Band
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (drawFront) 0.85f else 0.45f),
                    Color(0xFF67E8F9).copy(alpha = if (drawFront) 0.65f else 0.30f),
                    Color(0xFF93C5FD).copy(alpha = if (drawFront) 0.40f else 0.20f),
                    Color.White.copy(alpha = if (drawFront) 0.80f else 0.40f)
                )
            ),
            startAngle = sweepStartAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - ringMajorA, center.y - ringMinorB),
            size = Size(ringMajorA * 2, ringMinorB * 2),
            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
        )

        // Outer Sharp Neon Edge
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFE0F2FE),
                    Color(0xFF67E8F9),
                    Color.White
                )
            ),
            startAngle = sweepStartAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - ringMajorA, center.y - ringMinorB),
            size = Size(ringMajorA * 2, ringMinorB * 2),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner Sharp Glowing Neon Ring Edge
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (drawFront) 0.95f else 0.50f),
                    Color(0xFF00E5FF).copy(alpha = if (drawFront) 0.80f else 0.40f),
                    Color.White.copy(alpha = if (drawFront) 0.95f else 0.50f)
                )
            ),
            startAngle = sweepStartAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - innerMajorA, center.y - innerMinorB),
            size = Size(innerMajorA * 2, innerMinorB * 2),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Shimmering reflection spot sweeping through ring
        if (drawFront) {
            val shimmerAngle = (shimmer * 180f)
            val rad = Math.toRadians(shimmerAngle.toDouble())
            val sx = center.x + ringMajorA * cos(rad).toFloat()
            val sy = center.y + ringMinorB * sin(rad).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 4.dp.toPx(),
                center = Offset(sx, sy)
            )
            drawCircle(
                color = Color(0xFF67E8F9).copy(alpha = 0.45f),
                radius = 9.dp.toPx(),
                center = Offset(sx, sy)
            )
        }
    }
}

/**
 * Draws the 3D Elliptical Orbit Wire Tracks and Orbiting Moons/Satellites
 */
private fun DrawScope.drawOrbitTracksAndMoons(
    center: Offset,
    planetRadius: Float,
    orbitAngle: Float,
    glowPulse: Float,
    drawFront: Boolean
) {
    // ---------------------------------------------------------
    // ORBIT 1: Tilted +24 degrees (Primary Satellite Moons Track)
    // ---------------------------------------------------------
    val orbit1MajorA = planetRadius * 1.95f
    val orbit1MinorB = planetRadius * 0.90f
    val tilt1 = 24f

    rotate(degrees = tilt1, pivot = center) {
        val sweepStart = if (drawFront) 0f else 180f
        // Thin glowing wire track
        drawArc(
            color = Color(0xFF93C5FD).copy(alpha = if (drawFront) 0.45f else 0.22f),
            startAngle = sweepStart,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - orbit1MajorA, center.y - orbit1MinorB),
            size = Size(orbit1MajorA * 2, orbit1MinorB * 2),
            style = Stroke(width = 1.3.dp.toPx())
        )

        // Moon 1 (Top-Right Deep Purple Planet from screenshot)
        val angle1 = (orbitAngle % 360f)
        val isFront1 = (angle1 in 0f..180f)
        if (isFront1 == drawFront) {
            drawSatelliteSphere(
                center = center,
                majorA = orbit1MajorA,
                minorB = orbit1MinorB,
                angleDeg = angle1,
                radiusDp = 10.dp,
                coreColor = Color(0xFF7C3AED),
                rimColor = Color(0xFFC084FC),
                highlightColor = Color(0xFFEDE9FE),
                glowPulse = glowPulse
            )
        }

        // Moon 2 (Bottom-Left Violet/Pink Satellite Moon)
        val angle2 = ((orbitAngle + 155f) % 360f)
        val isFront2 = (angle2 in 0f..180f)
        if (isFront2 == drawFront) {
            drawSatelliteSphere(
                center = center,
                majorA = orbit1MajorA,
                minorB = orbit1MinorB,
                angleDeg = angle2,
                radiusDp = 8.5.dp,
                coreColor = Color(0xFF6D28D9),
                rimColor = Color(0xFFF43F5E),
                highlightColor = Color.White,
                glowPulse = glowPulse
            )
        }

        // Moon 3 (Tiny Coral Node Moon near bottom-left flare)
        val angle3 = ((orbitAngle + 215f) % 360f)
        val isFront3 = (angle3 in 0f..180f)
        if (isFront3 == drawFront) {
            drawSatelliteSphere(
                center = center,
                majorA = orbit1MajorA,
                minorB = orbit1MinorB,
                angleDeg = angle3,
                radiusDp = 5.dp,
                coreColor = Color(0xFFF43F5E),
                rimColor = Color(0xFFFDA4AF),
                highlightColor = Color.White,
                glowPulse = glowPulse
            )
        }
    }

    // ---------------------------------------------------------
    // ORBIT 2: Tilted -38 degrees (Secondary Satellite Moons Track)
    // ---------------------------------------------------------
    val orbit2MajorA = planetRadius * 1.82f
    val orbit2MinorB = planetRadius * 0.78f
    val tilt2 = -38f

    rotate(degrees = tilt2, pivot = center) {
        val sweepStart = if (drawFront) 0f else 180f
        drawArc(
            color = Color(0xFFC4B5FD).copy(alpha = if (drawFront) 0.38f else 0.18f),
            startAngle = sweepStart,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - orbit2MajorA, center.y - orbit2MinorB),
            size = Size(orbit2MajorA * 2, orbit2MinorB * 2),
            style = Stroke(width = 1.1.dp.toPx())
        )

        // Moon 4 (Top-Left Electric Cyan / Sapphire Moon)
        val angle4 = ((orbitAngle + 85f) % 360f)
        val isFront4 = (angle4 in 0f..180f)
        if (isFront4 == drawFront) {
            drawSatelliteSphere(
                center = center,
                majorA = orbit2MajorA,
                minorB = orbit2MinorB,
                angleDeg = angle4,
                radiusDp = 9.dp,
                coreColor = Color(0xFF0284C7),
                rimColor = Color(0xFF38BDF8),
                highlightColor = Color.White,
                glowPulse = glowPulse
            )
        }

        // Moon 5 (Right side Dark Indigo Moon)
        val angle5 = ((orbitAngle + 270f) % 360f)
        val isFront5 = (angle5 in 0f..180f)
        if (isFront5 == drawFront) {
            drawSatelliteSphere(
                center = center,
                majorA = orbit2MajorA,
                minorB = orbit2MinorB,
                angleDeg = angle5,
                radiusDp = 6.5.dp,
                coreColor = Color(0xFF1E3A8A),
                rimColor = Color(0xFF60A5FA),
                highlightColor = Color.White,
                glowPulse = glowPulse
            )
        }
    }
}

/**
 * Draws a 3D Shaded Orbiting Satellite Moon with specular reflection and aura
 */
private fun DrawScope.drawSatelliteSphere(
    center: Offset,
    majorA: Float,
    minorB: Float,
    angleDeg: Float,
    radiusDp: Dp,
    coreColor: Color,
    rimColor: Color,
    highlightColor: Color,
    glowPulse: Float
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val posX = center.x + majorA * cos(rad).toFloat()
    val posY = center.y + minorB * sin(rad).toFloat()
    val pos = Offset(posX, posY)
    val r = radiusDp.toPx()

    // Outer Aura Glow
    drawCircle(
        color = rimColor.copy(alpha = 0.35f * glowPulse),
        radius = r * 1.7f,
        center = pos
    )

    // 3D Spherical Body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                highlightColor,
                rimColor,
                coreColor,
                Color(0xFF0F172A)
            ),
            center = Offset(posX - r * 0.28f, posY - r * 0.28f),
            radius = r * 1.2f
        ),
        radius = r,
        center = pos
    )

    // Orbit Wire Connection Node Dot
    drawCircle(
        color = Color.White,
        radius = 1.4.dp.toPx(),
        center = pos
    )
}

/**
 * Clean Cyan/Blue Sound Waveform visualizer matching reference image:
 * Starts with subtle dots, transitions into dancing vertical bars, and closes with subtle dots.
 */
@Composable
fun MayaWaveformView(
    state: ZoyaState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_clean")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (state == ZoyaState.SPEAKING) 450
                else if (state == ZoyaState.LISTENING) 700
                else 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = modifier.height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val totalBars = 20

        for (i in 0 until totalBars) {
            val normalizedIdx = i.toFloat() / (totalBars - 1)
            
            // Outer bars are tiny dots, inner bars are taller equalizer waves
            val isDot = (i < 4 || i >= totalBars - 4)
            
            if (isDot) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 1.2.dp)
                        .size(2.2.dp)
                        .background(Color(0xFF60A5FA).copy(alpha = 0.8f), CircleShape)
                )
            } else {
                val centerWeight = 1f - kotlin.math.abs((normalizedIdx - 0.5f) * 2.2f)
                val envelope = centerWeight.coerceIn(0.2f, 1f)
                val sineVal = kotlin.math.abs(sin((wavePhase + normalizedIdx * 2f) * 2 * Math.PI.toFloat()))

                val maxBarHeight = when (state) {
                    ZoyaState.SPEAKING -> 22f
                    ZoyaState.LISTENING -> 18f
                    ZoyaState.THINKING -> 14f
                    else -> 8f
                }
                val minBarHeight = 3f
                val heightDp = (minBarHeight + (maxBarHeight - minBarHeight) * sineVal * envelope).dp

                Box(
                    modifier = Modifier
                        .padding(horizontal = 1.2.dp)
                        .width(2.2.dp)
                        .height(heightDp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8),
                                    Color(0xFF0284C7),
                                    Color(0xFF2563EB)
                                )
                            ),
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }
        }
    }
}

/**
 * Centered Glowing Blue Microphone button matching reference image
 */
@Composable
fun PulsingMicButton(
    isActive: Boolean,
    onClick: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blue_mic_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val btnSize = if (isCompact) 48.dp else 56.dp
    val outerSize = if (isCompact) 62.dp else 72.dp
    val iconSize = if (isCompact) 24.dp else 28.dp

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing outer ripple ring
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(btnSize)
                    .scale(pulseScale)
                    .background(Color(0xFF0284C7).copy(alpha = pulseAlpha * 0.35f), CircleShape)
                    .border(2.dp, Color(0xFF00B4D8).copy(alpha = pulseAlpha), CircleShape)
            )
        }

        // Soft ambient blue shadow glow
        Box(
            modifier = Modifier
                .size(btnSize)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFF0284C7),
                    ambientColor = Color(0xFF38BDF8)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00B4D8),
                            Color(0xFF0077F6)
                        )
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Listening Button",
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Animated Neon Edge Glow frame perfectly fitted to the device's 4 edges and corners.
 * Adapts to ANY screen size/aspect ratio without rotating across the screen.
 * Supports multiple premium animation styles:
 * - "Cyber Comet": High-velocity neon laser comet with fading tail running along the 4 borders
 * - "Aurora Flow": Prismatic chromatic sweep gradient flowing seamlessly along the border
 * - "Dual Orbit": Two opposing energy sparks racing around the perimeter and crossing at corners
 * - "Neon Pulse": Synchronized rhythmic ambient neon breathing along all 4 edges and corners
 */
@Composable
fun MayaEdgeGlowOverlay(
    state: ZoyaState,
    modifier: Modifier = Modifier,
    forcedStyle: String? = null
) {
    if (state == ZoyaState.IDLE) return

    val context = LocalContext.current
    val prefs = androidx.compose.runtime.remember {
        context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
    }
    val currentStyle = forcedStyle ?: prefs.getString("edge_glow_style", "Cyber Comet") ?: "Cyber Comet"

    val infiniteTransition = rememberInfiniteTransition(label = "edge_glow_transition")

    // Comet 1 forward progress (0..1)
    val cometProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == ZoyaState.SPEAKING) 2400 else if (state == ZoyaState.THINKING) 3000 else 3600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "comet_progress"
    )

    // Comet 2 reverse progress (1..0) for Dual Orbit
    val cometReverseProgress by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == ZoyaState.SPEAKING) 2700 else if (state == ZoyaState.THINKING) 3300 else 4000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "comet_reverse_progress"
    )

    // Aurora sweep angle phase (0..360)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    // Breathing glow alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == ZoyaState.SPEAKING) 700 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edge_glow_alpha"
    )

    val pathMeasure = androidx.compose.runtime.remember { PathMeasure() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val cornerPx = 28.dp.toPx()
        val marginPx = 2.dp.toPx()
        val baseStrokeWidth = 3.dp.toPx()
        val bloomStrokeWidth = 7.dp.toPx()

        // Palette matching Maya's active state
        val primaryColor: Color
        val secondaryColor: Color
        val accentColor: Color

        when (state) {
            ZoyaState.SPEAKING -> {
                primaryColor = Color(0xFFFF2A85)   // Hot Electric Magenta
                secondaryColor = Color(0xFF9333EA) // Cyber Purple
                accentColor = Color(0xFF38BDF8)    // Neon Sky Cyan
            }
            ZoyaState.THINKING -> {
                primaryColor = Color(0xFFA855F7)   // Quantum Violet
                secondaryColor = Color(0xFF6366F1) // Indigo
                accentColor = Color(0xFF00E5FF)    // Electric Cyan
            }
            else -> {
                primaryColor = Color(0xFF00E5FF)   // Neon Cyan
                secondaryColor = Color(0xFF2563EB) // Royal Blue
                accentColor = Color(0xFF10B981)    // Emerald Glow
            }
        }

        // Exact screen perimeter round-rect path locked to device edges
        val rect = Rect(marginPx, marginPx, width - marginPx, height - marginPx)
        val roundRect = RoundRect(rect, CornerRadius(cornerPx, cornerPx))
        val perimeterPath = Path().apply {
            addRoundRect(roundRect)
        }

        pathMeasure.setPath(perimeterPath, forceClosed = true)
        val totalLength = pathMeasure.length
        if (totalLength <= 0f) return@Canvas

        // Helper function to draw an arc segment along the perimeter with wraparound support
        fun drawPerimeterSegment(startDist: Float, endDist: Float, brush: Brush, strokeW: Float) {
            val segStart = ((startDist % totalLength) + totalLength) % totalLength
            val segEnd = ((endDist % totalLength) + totalLength) % totalLength

            if (segStart < segEnd) {
                val segPath = Path()
                pathMeasure.getSegment(segStart, segEnd, segPath, startWithMoveTo = true)
                drawPath(segPath, brush = brush, style = Stroke(width = strokeW, cap = StrokeCap.Round))
            } else if (segStart > segEnd) {
                val p1 = Path()
                val p2 = Path()
                pathMeasure.getSegment(segStart, totalLength, p1, startWithMoveTo = true)
                pathMeasure.getSegment(0f, segEnd, p2, startWithMoveTo = true)
                drawPath(p1, brush = brush, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                drawPath(p2, brush = brush, style = Stroke(width = strokeW, cap = StrokeCap.Round))
            }
        }

        when (currentStyle) {
            "Aurora Flow" -> {
                // Style 2: Smooth chromatic sweep gradient moving along the fixed rounded border
                val centerOffset = Offset(width / 2f, height / 2f)
                val auroraColors = listOf(
                    primaryColor.copy(alpha = 0.9f * glowAlpha),
                    secondaryColor.copy(alpha = 0.75f * glowAlpha),
                    accentColor.copy(alpha = 0.9f * glowAlpha),
                    Color(0xFFFBBF24).copy(alpha = 0.7f * glowAlpha),
                    primaryColor.copy(alpha = 0.9f * glowAlpha)
                )

                // Soft outer ambient neon glow
                drawRoundRect(
                    brush = Brush.sweepGradient(auroraColors, center = centerOffset),
                    topLeft = Offset(marginPx, marginPx),
                    size = Size(width - 2 * marginPx, height - 2 * marginPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = bloomStrokeWidth)
                )

                // Crisp inner border
                drawRoundRect(
                    brush = Brush.sweepGradient(auroraColors, center = centerOffset),
                    topLeft = Offset(marginPx, marginPx),
                    size = Size(width - 2 * marginPx, height - 2 * marginPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = baseStrokeWidth)
                )
            }

            "Dual Orbit" -> {
                // Style 3: Two opposing energy comets racing around the screen perimeter
                val cometLength = totalLength * 0.20f

                // Comet 1: Clockwise (primaryColor)
                val head1 = cometProgress * totalLength
                val tail1 = head1 - cometLength
                drawPerimeterSegment(
                    startDist = tail1,
                    endDist = head1,
                    brush = Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.1f), primaryColor.copy(alpha = 0.95f))),
                    strokeW = bloomStrokeWidth
                )
                drawPerimeterSegment(
                    startDist = tail1,
                    endDist = head1,
                    brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White)),
                    strokeW = baseStrokeWidth
                )
                val pos1 = pathMeasure.getPosition(head1 % totalLength)
                drawCircle(Color.White, radius = 3.5.dp.toPx(), center = pos1)
                drawCircle(primaryColor.copy(alpha = 0.6f * glowAlpha), radius = 7.dp.toPx(), center = pos1)

                // Comet 2: Counter-Clockwise (secondaryColor / accentColor)
                val head2 = cometReverseProgress * totalLength
                val tail2 = head2 - cometLength
                drawPerimeterSegment(
                    startDist = tail2,
                    endDist = head2,
                    brush = Brush.linearGradient(listOf(accentColor.copy(alpha = 0.1f), accentColor.copy(alpha = 0.95f))),
                    strokeW = bloomStrokeWidth
                )
                drawPerimeterSegment(
                    startDist = tail2,
                    endDist = head2,
                    brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White)),
                    strokeW = baseStrokeWidth
                )
                val pos2 = pathMeasure.getPosition(((head2 % totalLength) + totalLength) % totalLength)
                drawCircle(Color.White, radius = 3.5.dp.toPx(), center = pos2)
                drawCircle(accentColor.copy(alpha = 0.6f * glowAlpha), radius = 7.dp.toPx(), center = pos2)
            }

            "Neon Pulse" -> {
                // Style 4: Rhythmic breathing ambient neon glow hugging the 4 edges & corners
                // Subtle dark ambient backdrop stroke
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.22f * glowAlpha),
                    topLeft = Offset(marginPx, marginPx),
                    size = Size(width - 2 * marginPx, height - 2 * marginPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = bloomStrokeWidth * 1.5f)
                )

                // Primary glowing crisp stroke
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.85f * glowAlpha),
                    topLeft = Offset(marginPx, marginPx),
                    size = Size(width - 2 * marginPx, height - 2 * marginPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = baseStrokeWidth)
                )
            }

            else -> {
                // Style 1 (DEFAULT): "Cyber Comet"
                // A brilliant neon laser stream running continuously along the 4 borders
                val cometLength = totalLength * 0.24f
                val headDist = cometProgress * totalLength
                val tailDist = headDist - cometLength

                // 1. Subtle ambient base border around all 4 edges so screen is subtly framed
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.12f * glowAlpha),
                    topLeft = Offset(marginPx, marginPx),
                    size = Size(width - 2 * marginPx, height - 2 * marginPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 2. Soft glowing bloom tail
                drawPerimeterSegment(
                    startDist = tailDist,
                    endDist = headDist,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.05f),
                            secondaryColor.copy(alpha = 0.45f * glowAlpha),
                            primaryColor.copy(alpha = 0.85f * glowAlpha)
                        )
                    ),
                    strokeW = bloomStrokeWidth
                )

                // 3. Crisp bright laser core stream
                drawPerimeterSegment(
                    startDist = tailDist + (cometLength * 0.25f),
                    endDist = headDist,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.3f),
                            primaryColor,
                            Color.White
                        )
                    ),
                    strokeW = baseStrokeWidth
                )

                // 4. Brilliant Head Particle with radial halo
                val headPos = pathMeasure.getPosition(headDist % totalLength)
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = headPos
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.7f * glowAlpha),
                    radius = 8.dp.toPx(),
                    center = headPos
                )
            }
        }

        // Universal 4-Corner Futuristic Cyber Accents
        // Enhances all 4 device corners with subtle luminous corner ticks
        val cornerTickLen = 14.dp.toPx()
        val cornerColor = Color.White.copy(alpha = 0.55f * glowAlpha)
        val tickStroke = 2.dp.toPx()

        // Top-Left Corner Bracket
        drawLine(cornerColor, Offset(marginPx + cornerPx, marginPx), Offset(marginPx + cornerPx + cornerTickLen, marginPx), tickStroke)
        drawLine(cornerColor, Offset(marginPx, marginPx + cornerPx), Offset(marginPx, marginPx + cornerPx + cornerTickLen), tickStroke)

        // Top-Right Corner Bracket
        drawLine(cornerColor, Offset(width - marginPx - cornerPx, marginPx), Offset(width - marginPx - cornerPx - cornerTickLen, marginPx), tickStroke)
        drawLine(cornerColor, Offset(width - marginPx, marginPx + cornerPx), Offset(width - marginPx, marginPx + cornerPx + cornerTickLen), tickStroke)

        // Bottom-Left Corner Bracket
        drawLine(cornerColor, Offset(marginPx + cornerPx, height - marginPx), Offset(marginPx + cornerPx + cornerTickLen, height - marginPx), tickStroke)
        drawLine(cornerColor, Offset(marginPx, height - marginPx - cornerPx), Offset(marginPx, height - marginPx - cornerPx - cornerTickLen), tickStroke)

        // Bottom-Right Corner Bracket
        drawLine(cornerColor, Offset(width - marginPx - cornerPx, height - marginPx), Offset(width - marginPx - cornerPx - cornerTickLen, height - marginPx), tickStroke)
        drawLine(cornerColor, Offset(width - marginPx, height - marginPx - cornerPx), Offset(width - marginPx, height - marginPx - cornerPx - cornerTickLen), tickStroke)
    }
}

/**
 * Maya Nova Orb: High-Tech Quantum Arc Reactor HUD Centerpiece
 * Features concentric rotating HUD gauge rings, scanning bright energy beam,
 * glowing crosshairs, center "M.A.Y.A" title, and pulsing audio equalizer.
 */
@Composable
fun MayaNovaOrbView(
    state: ZoyaState,
    modifier: Modifier = Modifier,
    orbSize: Dp = 175.dp,
    tintColor: Color = Color(0xFF00E5FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nova_orb_transition")

    val outerRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotate"
    )

    val innerRotate by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotate"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (state == ZoyaState.SPEAKING) 500 else if (state == ZoyaState.LISTENING) 800 else 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nova_hover"
    )

    Box(
        modifier = modifier
            .size(orbSize * 1.25f)
            .offset(y = hoverOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.38f

            // Outer Soft Ambient Radial Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tintColor.copy(alpha = 0.28f * corePulse),
                        Color(0xFF2563EB).copy(alpha = 0.15f * corePulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.45f
                ),
                radius = baseRadius * 1.45f,
                center = center
            )

            // Inner Dark Reactor Core Cavity
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF090D16),
                        Color(0xFF030712)
                    ),
                    center = center,
                    radius = baseRadius * 0.85f
                ),
                radius = baseRadius * 0.85f,
                center = center
            )

            // 1. Outer Segmented Arc Ring (Clockwise)
            rotate(degrees = outerRotate, pivot = center) {
                // Segment 1 (Long Arc with Bright Sweep)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            tintColor.copy(alpha = 0.2f),
                            tintColor,
                            Color.White,
                            tintColor.copy(alpha = 0.3f)
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                    size = Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Segment 2 (Shorter Opposing Arc)
                drawArc(
                    color = tintColor.copy(alpha = 0.85f),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                    size = Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Outer tick dots
                for (i in 0 until 12) {
                    val angle = i * 30.0
                    val rad = Math.toRadians(angle)
                    val dotDist = baseRadius * 1.14f
                    val dx = center.x + dotDist * cos(rad).toFloat()
                    val dy = center.y + dotDist * sin(rad).toFloat()
                    drawCircle(
                        color = if (i % 3 == 0) Color.White else tintColor.copy(alpha = 0.5f),
                        radius = if (i % 3 == 0) 2.dp.toPx() else 1.2.dp.toPx(),
                        center = Offset(dx, dy)
                    )
                }
            }

            // 2. Middle Counter-Rotating Holographic Ring
            rotate(degrees = innerRotate, pivot = center) {
                val midRadius = baseRadius * 0.72f
                drawArc(
                    color = Color(0xFF60A5FA).copy(alpha = 0.65f),
                    startAngle = 45f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - midRadius, center.y - midRadius),
                    size = Size(midRadius * 2, midRadius * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFFA855F7).copy(alpha = 0.55f),
                    startAngle = 210f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(center.x - midRadius, center.y - midRadius),
                    size = Size(midRadius * 2, midRadius * 2),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Crosshairs (+)
                val crossDist = midRadius * 0.95f
                val crossOffsets = listOf(
                    Offset(center.x - crossDist, center.y),
                    Offset(center.x + crossDist, center.y),
                    Offset(center.x, center.y - crossDist),
                    Offset(center.x, center.y + crossDist)
                )
                crossOffsets.forEach { pos ->
                    val len = 3.5.dp.toPx()
                    drawLine(Color.White.copy(alpha = 0.8f), Offset(pos.x - len, pos.y), Offset(pos.x + len, pos.y), 1.2.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.8f), Offset(pos.x, pos.y - len), Offset(pos.x, pos.y + len), 1.2.dp.toPx())
                }
            }

            // 3. Central Neon Ring Outline
            val coreRingRadius = baseRadius * 0.55f
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(tintColor, Color.White, tintColor),
                    center = center
                ),
                radius = coreRingRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Center M.A.Y.A Text & Equalizer Waveform
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "M.A.Y.A",
                color = Color.White,
                fontSize = (orbSize.value * 0.09f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Mini Equalizer Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barHeights = listOf(4.dp, 8.dp, 12.dp, 16.dp, 10.dp, 6.dp, 3.dp)
                barHeights.forEachIndexed { i, h ->
                    val dynamicH = h * corePulse
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(dynamicH)
                            .background(tintColor, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

/**
 * J.A.R.V.I.S. Orb: Iron Man Stark Industries 3D Holographic Amber Grid Sphere
 * Features 3D rotating latitude & longitude grid lines, glowing data nodes, and golden particle halo.
 */
@Composable
fun JarvisOrbView(
    state: ZoyaState,
    modifier: Modifier = Modifier,
    orbSize: Dp = 175.dp,
    tintColor: Color = Color(0xFFF59E0B)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb_transition")

    val rotateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "jarvis_rotate_y"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (state == ZoyaState.SPEAKING) 450 else if (state == ZoyaState.LISTENING) 750 else 2400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jarvis_pulse"
    )

    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jarvis_hover"
    )

    Box(
        modifier = modifier
            .size(orbSize * 1.25f)
            .offset(y = hoverOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val sphereRadius = size.minDimension * 0.32f

            // Golden Atmospheric Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tintColor.copy(alpha = 0.30f * corePulse),
                        Color(0xFFEA580C).copy(alpha = 0.15f * corePulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = sphereRadius * 1.6f
                ),
                radius = sphereRadius * 1.6f,
                center = center
            )

            // Inner Dark Core Cavity
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1C1405),
                        Color(0xFF0D0A03),
                        Color.Black
                    ),
                    center = center,
                    radius = sphereRadius
                ),
                radius = sphereRadius,
                center = center
            )

            // 1. 3D Longitude Ellipses (Spinning around Y axis)
            for (i in 0 until 6) {
                val angleDeg = (rotateY + i * 30f) % 180f
                val rad = Math.toRadians(angleDeg.toDouble())
                val scaleX = kotlin.math.abs(cos(rad)).toFloat().coerceAtLeast(0.08f)

                drawOval(
                    color = tintColor.copy(alpha = if (scaleX > 0.5f) 0.75f else 0.40f),
                    topLeft = Offset(center.x - sphereRadius * scaleX, center.y - sphereRadius),
                    size = Size(sphereRadius * 2 * scaleX, sphereRadius * 2),
                    style = Stroke(width = 1.3.dp.toPx())
                )
            }

            // 2. 3D Latitude Rings
            val latitudes = listOf(-0.65f, -0.35f, 0f, 0.35f, 0.65f)
            latitudes.forEach { lat ->
                val ringY = center.y + sphereRadius * lat
                val ringRadX = sphereRadius * kotlin.math.sqrt((1f - lat * lat).coerceAtLeast(0f))
                val ringRadY = ringRadX * 0.28f

                drawOval(
                    color = Color(0xFFFEF08A).copy(alpha = if (lat == 0f) 0.85f else 0.50f),
                    topLeft = Offset(center.x - ringRadX, ringY - ringRadY),
                    size = Size(ringRadX * 2, ringRadY * 2),
                    style = Stroke(width = if (lat == 0f) 1.8.dp.toPx() else 1.1.dp.toPx())
                )
            }

            // 3. Orbiting Data Halos & Particles
            rotate(degrees = rotateY * 0.6f, pivot = center) {
                drawOval(
                    color = tintColor.copy(alpha = 0.35f),
                    topLeft = Offset(center.x - sphereRadius * 1.35f, center.y - sphereRadius * 0.55f),
                    size = Size(sphereRadius * 2.7f, sphereRadius * 1.1f),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Golden data nodes on halo
                for (j in 0 until 4) {
                    val angle = (rotateY * 1.2f + j * 90f) % 360f
                    val rad = Math.toRadians(angle.toDouble())
                    val nx = center.x + sphereRadius * 1.35f * cos(rad).toFloat()
                    val ny = center.y + sphereRadius * 0.55f * sin(rad).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 2.2.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                    drawCircle(
                        color = tintColor.copy(alpha = 0.5f),
                        radius = 5.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }
            }
        }

        // Center J.A.R.V.I.S. Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "M.A.Y.A",
                color = Color(0xFFFEF08A),
                fontSize = (orbSize.value * 0.085f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }
    }
}

/**
 * Ultron Orb: Neural Cybernetic Polyhedron Matrix
 * Features 3D rotating interconnected geometric nodes and luminous faceted polygon mesh lines.
 */
@Composable
fun UltronOrbView(
    state: ZoyaState,
    modifier: Modifier = Modifier,
    orbSize: Dp = 175.dp,
    tintColor: Color = Color(0xFF00E5FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ultron_orb_transition")

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ultron_rotate"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (state == ZoyaState.SPEAKING) 480 else if (state == ZoyaState.LISTENING) 800 else 2200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ultron_pulse"
    )

    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ultron_hover"
    )

    Box(
        modifier = modifier
            .size(orbSize * 1.25f)
            .offset(y = hoverOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.32f

            // Cybernetic Ice-Blue Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tintColor.copy(alpha = 0.32f * corePulse),
                        Color(0xFF1D4ED8).copy(alpha = 0.18f * corePulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r * 1.55f
                ),
                radius = r * 1.55f,
                center = center
            )

            // Dark Matrix Cavity
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF04182B),
                        Color(0xFF020E1A),
                        Color.Black
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )

            // Polyhedron Vertices in 3D
            val rawVertices = listOf(
                Triple(0f, 0.9f, 0.4f),
                Triple(0.85f, 0.3f, 0.4f),
                Triple(0.52f, -0.75f, 0.4f),
                Triple(-0.52f, -0.75f, 0.4f),
                Triple(-0.85f, 0.3f, 0.4f),
                Triple(0.45f, 0.55f, -0.6f),
                Triple(0.72f, -0.3f, -0.6f),
                Triple(0f, -0.85f, -0.6f),
                Triple(-0.72f, -0.3f, -0.6f),
                Triple(-0.45f, 0.55f, -0.6f),
                Triple(0f, 0f, 0.95f),
                Triple(0f, 0f, -0.95f)
            )

            val radRot = Math.toRadians(rotateAngle.toDouble())
            val cosRot = cos(radRot).toFloat()
            val sinRot = sin(radRot).toFloat()

            // Rotate around Y and Z slightly
            val projected = rawVertices.map { (vx, vy, vz) ->
                val rx = vx * cosRot - vz * sinRot
                val rz = vx * sinRot + vz * cosRot
                val px = center.x + rx * r
                val py = center.y + vy * r * 0.95f
                Triple(px, py, rz)
            }

            // Draw Facet Connection Lines
            val edges = listOf(
                0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 0,
                5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 5,
                0 to 10, 1 to 10, 2 to 10, 3 to 10, 4 to 10,
                5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11,
                0 to 5, 1 to 6, 2 to 7, 3 to 8, 4 to 9
            )

            edges.forEach { (a, b) ->
                val p1 = projected[a]
                val p2 = projected[b]
                val avgZ = (p1.third + p2.third) / 2f
                val isFront = avgZ > 0f

                drawLine(
                    color = if (isFront) Color.White.copy(alpha = 0.85f) else tintColor.copy(alpha = 0.35f),
                    start = Offset(p1.first, p1.second),
                    end = Offset(p2.first, p2.second),
                    strokeWidth = if (isFront) 1.6.dp.toPx() else 1.0.dp.toPx()
                )
            }

            // Draw Glowing Nodes at Vertices
            projected.forEach { (px, py, pz) ->
                val isFront = pz > 0f
                val nodeRad = if (isFront) 2.5.dp.toPx() else 1.5.dp.toPx()
                drawCircle(
                    color = if (isFront) Color.White else tintColor.copy(alpha = 0.6f),
                    radius = nodeRad,
                    center = Offset(px, py)
                )
            }
        }

        // Center M.A.Y.A Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "M.A.Y.A",
                color = Color.White,
                fontSize = (orbSize.value * 0.085f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }
    }
}

/**
 * Universal Dynamic Orb View: Renders the active orb style chosen in Settings (Appearance)
 * Default is "MAYA 2047" (Our 3D Cosmic Celestial Planet with rings and moons).
 */
@Composable
fun MayaDynamicOrbView(
    state: ZoyaState,
    modifier: Modifier = Modifier,
    orbSize: Dp = 175.dp
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = androidx.compose.runtime.remember {
        context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
    }
    val currentStyle = prefs.getString("orb_style", "MAYA 2047") ?: "MAYA 2047"
    val currentColor = prefs.getString("orb_color", "Persona") ?: "Persona"

    val tintColor = when {
        currentColor.contains("Jarvis", ignoreCase = true) -> Color(0xFFF59E0B)
        currentColor.contains("Ultron", ignoreCase = true) -> Color(0xFF00E5FF)
        currentColor.contains("Neon", ignoreCase = true) -> Color(0xFFFF2A85)
        currentColor.contains("Emerald", ignoreCase = true) -> Color(0xFF10B981)
        else -> Color(0xFF38BDF8)
    }

    when (currentStyle) {
        "Maya Nova" -> {
            MayaNovaOrbView(state = state, modifier = modifier, orbSize = orbSize, tintColor = tintColor)
        }
        "J.A.R.V.I.S." -> {
            JarvisOrbView(state = state, modifier = modifier, orbSize = orbSize, tintColor = tintColor)
        }
        "Ultron" -> {
            UltronOrbView(state = state, modifier = modifier, orbSize = orbSize, tintColor = tintColor)
        }
        else -> {
            // Default: MAYA 2047 3D Cosmic Celestial Planet
            MayaCosmicOrbView(state = state, modifier = modifier, orbSize = orbSize)
        }
    }
}


