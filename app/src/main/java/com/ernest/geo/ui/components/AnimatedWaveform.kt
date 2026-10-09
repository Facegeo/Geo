package com.ernest.geo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.ernest.geo.ui.model.GeoState
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun GeoWaveform(
    geoState: GeoState
) {
    val transition = rememberInfiniteTransition(label = "wave")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing)
        ),
        label = "phase"
    )

    // Using a Box with a very dark background to help the neon glow pop, matching the image
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color(0xFF050816)) // Dark space-like background
    ) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(120.dp)
        ) {
            val barCount = 75 // More bars for a denser look like the image
            val centerY = size.height / 2f
            val barWidth = 3f // Thinner lines for a more technical/AI feel
            val totalWidth = size.width * 0.9f
            val startX = (size.width - totalWidth) / 2
            val spacing = totalWidth / barCount

            // Colors extracted from your image
            val magenta = Color(0xFFFF00FF)
            val purple = Color(0xFF9D00FF)
            val deepBlue = Color(0xFF0044FF)
            val electricBlue = Color(0xFF00D5FF)
            val cyan = Color(0xFF00FFFF)

            for (i in 0 until barCount) {
                // 1. Precise Horizontal Color Gradient
                val fraction = i.toFloat() / barCount
                val barColor = when {
                    fraction < 0.25f -> lerp(magenta, purple, fraction * 4f)
                    fraction < 0.50f -> lerp(purple, deepBlue, (fraction - 0.25f) * 4f)
                    fraction < 0.75f -> lerp(deepBlue, electricBlue, (fraction - 0.50f) * 4f)
                    else -> lerp(electricBlue, cyan, (fraction - 0.75f) * 4f)
                }

                // 2. Wave Physics - Complex interference for the "moving signal" look
                val distance = abs(i - barCount / 2f) / (barCount / 2f)
                val envelope = 1f - (distance * distance) // Bell curve shape

                val animationScale = when (geoState) {
                    GeoState.IDLE -> 10f
                    GeoState.LISTENING -> 45f
                    GeoState.THINKING -> 25f
                    GeoState.SPEAKING -> 40f
                    GeoState.SETTINGS -> 10f
                    GeoState.VISION -> 15f
                    GeoState.ANALYZING -> 50f
                    GeoState.DASHBOARD -> 10f
                    GeoState.COMMAND_CENTER -> 10f
                }

                // Mixing three sine waves for that "bumpy" irregular look in the image
                val wave = sin(phase + i * 0.3f) * 0.6f + 
                           sin(phase * 0.8f + i * 0.15f) * 0.3f +
                           sin(phase * 1.5f + i * 0.5f) * 0.1f
                
                val height = 10f + (abs(wave) * animationScale * envelope)
                val x = startX + (i * spacing)

                // 3. Triple Layer Drawing for the Neon "Bloom" effect in the image
                
                // Outer Blur/Bloom (Widest)
                drawLine(
                    color = barColor.copy(alpha = 0.12f),
                    start = Offset(x, centerY - height - 6f),
                    end = Offset(x, centerY + height + 6f),
                    strokeWidth = barWidth * 5f,
                    cap = StrokeCap.Round
                )

                // Primary Glow (Medium)
                drawLine(
                    color = barColor.copy(alpha = 0.45f),
                    start = Offset(x, centerY - height - 2f),
                    end = Offset(x, centerY + height + 2f),
                    strokeWidth = barWidth * 2.5f,
                    cap = StrokeCap.Round
                )

                // White-ish Hot Core (Sharpest)
                drawLine(
                    color = lerp(Color.White, barColor, 0.25f),
                    start = Offset(x, centerY - height),
                    end = Offset(x, centerY + height),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
