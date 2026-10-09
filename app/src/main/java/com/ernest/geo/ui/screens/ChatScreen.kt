package com.ernest.geo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ernest.geo.R
import com.ernest.geo.ui.model.GeoState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.blur
import com.ernest.geo.ui.components.GeoWaveform
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Brush
import androidx.activity.compose.BackHandler

@Composable
fun CameraPreview(
    onPreviewViewCreated: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    AndroidView(
        factory = { 
            PreviewView(context).also {
                onPreviewViewCreated(it)
            }
        },
        modifier = modifier.clip(CircleShape).border(2.dp, Color(0xFF00D9FF), CircleShape)
    )
}

@Composable
fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp) // Align with your header
            .height(2.dp) // Slightly thicker to let the blur show up
            .blur(radius = 4.dp) // The magic: creates the glow effect
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00D9FF).copy(alpha = 0.8f), // Vibrant blue center
                        Color.Transparent
                    )
                )
            )
    )
}
@Composable
fun GeoGlowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(75.dp)
    ) {

        // Glow
        Box(
            modifier = Modifier
                .size(70.dp)
                .blur(20.dp)
                .background(
                    Color(0xFFC44DFF).copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )

        // Ring
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    Color.Transparent,
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = Color(0xFF7B4DFF),
                    shape = CircleShape
                )
        )

        IconButton(
            onClick = onClick
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
@Composable
fun OrbExpandingWaves(geoState: GeoState) {
    // Only show waves when active
    if (geoState == GeoState.IDLE || geoState == GeoState.SETTINGS) return

    val transition = rememberInfiniteTransition(label = "orb_waves")
    val waveCount = 8 // Increased count for "flooding" effect

    Box(modifier = Modifier.size(360.dp), contentAlignment = Alignment.Center) {
        for (i in 0 until waveCount) {
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2500, // Slightly slower for more "fluid" flood
                        delayMillis = i * 300, // Tight interval for many waves
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "wave_$i"
            )

            // Waves start at center (0) and grow past the orb
            val alpha = if (progress < 0.1f) progress * 5f else (1f - progress)
            val scale = progress * 1.8f 

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha * 0.7f
                    }
                    .border(
                        width = (2.dp + (4.dp * progress)), // Waves get thicker as they expand
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00FFFF), // Cyan
                                Color(0xFF9D00FF), // Purple
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun ChatScreen(

    geoState: GeoState,
    isVisionFlow: Boolean = false,
    capturedBitmap: Bitmap? = null,

    onTalkClick: () -> Unit,
    onVisionClick: () -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPreviewViewCreated: (PreviewView) -> Unit = {}
) {
    BackHandler(enabled = geoState != GeoState.IDLE, onBack = onStopClick)

    // Orb Pulse Animation
    val scale = rememberInfiniteTransition()

    val pulse by scale.animateFloat(

        initialValue = 1f,

        targetValue =

            if (
                geoState ==
                GeoState.SPEAKING
            ) 1.08f
            else 1f,

        animationSpec = infiniteRepeatable(

            animation =
                tween(800),

            repeatMode =
                RepeatMode.Reverse
        )
    )

    // Orb Glow While Speaking
    val glow by rememberInfiniteTransition(
        label = "glow"
    ).animateFloat(

        initialValue = 10f,

        targetValue =
            if (
                geoState ==
                GeoState.SPEAKING
            ) 30f
            else 10f,

        animationSpec =
            infiniteRepeatable(

                animation =
                    tween(700),

                repeatMode =
                    RepeatMode.Reverse
            ),

        label = ""
    )

    // Waveform Animation

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050816))
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally, // Add this
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 40.dp
                    ),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Geo",
                    color = Color.White,
                    fontSize = 34.sp
                )

                Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                    GeoGlowButton(
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            // ORB
            Box(
                contentAlignment =
                    Alignment.Center
            ) {
                if (isVisionFlow) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = "Captured Image",
                            modifier = Modifier
                                .size(360.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF00D9FF), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CameraPreview(
                            onPreviewViewCreated = onPreviewViewCreated,
                            modifier = Modifier.size(360.dp)
                        )
                    }
                } else {
                    // Background Glow Layer (Under Orb)
                    Image(
                        painter =
                            painterResource(
                                R.drawable.geo_orb
                            ),

                        contentDescription = null,

                        modifier = Modifier
                            .size(360.dp)
                            .clip(CircleShape) // Hides square corners
                            .blur(glow.dp)
                            .graphicsLayer {
                                // Blends the image's background into the UI
                                // Use BlendMode.Screen if the image has a black background
                                blendMode = BlendMode.Screen
                            }
                    )

                    // Main Orb Layer
                    Image(
                        painter =
                            painterResource(
                                R.drawable.geo_orb
                            ),

                        contentDescription = null,

                        modifier = Modifier
                            .size(360.dp)
                            .clip(CircleShape) // Hides square corners
                            .graphicsLayer {
                                scaleX = pulse
                                scaleY = pulse
                                blendMode = BlendMode.Screen
                            }
                    )

                    // Expanding animated waves (NOW ON TOP)
                    OrbExpandingWaves(geoState)
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // WAVEFORM
            GeoWaveform(geoState)

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            val statusText =
                when (geoState) {

                    GeoState.IDLE ->
                        "Ready"

                    GeoState.LISTENING ->
                        "Listening..."

                    GeoState.THINKING ->
                        "Thinking..."

                    GeoState.SPEAKING ->
                        "Speaking..."

                    GeoState.SETTINGS ->
                        "Settings"

                    GeoState.VISION ->
                        "Vision Mode"

                    GeoState.ANALYZING ->
                        "Analyzing..."

                    GeoState.DASHBOARD ->
                        "Dashboard"

                    GeoState.COMMAND_CENTER ->
                        "Command Center"
                }

            Text(
                text = statusText,
                color = Color(0xFFC44DFF),
                fontSize = 20.sp
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            // FOOTER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement =
                    Arrangement.SpaceEvenly,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                // Vision Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GeoGlowButton(icon = Icons.Default.CameraAlt, onClick = onVisionClick)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Vision", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }

                // MAIN MIC BUTTON
                IconButton(
                    onClick = onTalkClick,
                    modifier =
                        Modifier.size(140.dp)
                ) {

                    Image(
                        painter =
                            painterResource(
                                R.drawable.geo_mic_ring
                            ),

                        contentDescription = null,

                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .graphicsLayer {
                                    blendMode = BlendMode.Screen
                                }
                    )
                }

                // Close Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GeoGlowButton(icon = Icons.Default.Close, onClick = onStopClick)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Cancel", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
    }
}