package com.babymakisuk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── 淺色主題 ──────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary          = Color(0xFF4FC3F7),
    onPrimary        = Color.White,
    secondary        = Color(0xFFFFB74D),
    onSecondary      = Color.Black,
    background       = Color(0xFFFAFAFA),
    surface          = Color.White,
    onBackground     = Color(0xFF1A1A1A),
    onSurface        = Color(0xFF1A1A1A)
)

// ── 深色主題 ──────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary          = Color(0xFF4FC3F7),
    onPrimary        = Color(0xFF003544),
    secondary        = Color(0xFFFFB74D),
    onSecondary      = Color(0xFF3D2800),
    background       = Color(0xFF121212),
    surface          = Color(0xFF1E1E1E),
    onBackground     = Color(0xFFE0E0E0),
    onSurface        = Color(0xFFE0E0E0)
)

/**
 * App 全局主題。
 *
 * @param darkTheme true → 深色；false → 淺色；預設跟隨系統。
 */
@Composable
fun BabyMakiSukTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
