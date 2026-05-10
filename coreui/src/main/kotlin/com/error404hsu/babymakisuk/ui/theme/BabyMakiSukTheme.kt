package com.error404hsu.babymakisuk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color.White,
    secondary = Color(0xFFFFB74D),
    onSecondary = Color.Black,
    background = Color(0xFFFAFAFA),
    surface = Color.White
)

@Composable
fun BabyMakiSukTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
