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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * Animated Neon Edge Glow frame around the screen while Maya is live.
 * Matches the Behaviour setting "Edge glow: Animated neon frame around the screen while Maya is live"
 */
@Composable
fun MayaEdgeGlowOverlay(
    state: ZoyaState,
    modifier: Modifier = Modifier
) {
    if (state == ZoyaState.IDLE) return

    val infiniteTransition = rememberInfiniteTransition(label = "edge_glow_transition")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "edge_glow_phase"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edge_glow_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidthPx = 4.dp.toPx()
        val glowColorList = when (state) {
            ZoyaState.SPEAKING -> listOf(
                Color(0xFFFF2A85),
                Color(0xFF9333EA),
                Color(0xFF38BDF8),
                Color(0xFFFF2A85)
            )
            ZoyaState.THINKING -> listOf(
                Color(0xFFA855F7),
                Color(0xFF6366F1),
                Color(0xFF38BDF8),
                Color(0xFFA855F7)
            )
            else -> listOf(
                Color(0xFF00E5FF),
                Color(0xFF2563EB),
                Color(0xFF7C3AED),
                Color(0xFF00E5FF)
            )
        }

        rotate(degrees = glowPhase) {
            // Soft outer neon glow
            drawRect(
                brush = Brush.sweepGradient(
                    colors = glowColorList.map { it.copy(alpha = 0.35f * glowAlpha) }
                ),
                style = Stroke(width = 10.dp.toPx())
            )
        }

        rotate(degrees = glowPhase) {
            // Inner crisp glowing border
            drawRect(
                brush = Brush.sweepGradient(
                    colors = glowColorList.map { it.copy(alpha = 0.85f * glowAlpha) }
                ),
                style = Stroke(width = strokeWidthPx)
            )
        }
    }
}

