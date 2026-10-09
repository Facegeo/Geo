package com.ernest.geo.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val GeoColors = darkColorScheme(

    primary = NeonPurple,

    secondary = NeonBlue,

    tertiary = NeonCyan,

    background = Background,

    surface = SurfaceDark,

    onBackground = WhiteText,

    onSurface = WhiteText
)

@Composable
fun GeoTheme(
    content: @Composable () -> Unit
) {

    MaterialTheme(

        colorScheme = GeoColors,

        typography = Typography,

        content = content
    )
}