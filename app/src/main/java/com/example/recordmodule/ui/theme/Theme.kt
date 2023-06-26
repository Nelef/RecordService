package com.example.recordmodule.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val LightColors = lightColors(
    primary = Purple40,
    secondary = PurpleGrey40
)

@Composable
fun RecordModuleTheme(content: @Composable () -> Unit) {
    val colors = LightColors

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}