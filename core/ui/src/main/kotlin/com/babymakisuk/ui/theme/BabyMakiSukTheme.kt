package com.babymakisuk.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── 顏色定義 ──────────────────────────────────────────

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
 * @param dynamicColor 是否啟用 Android 12+ 動態配色（預設關閉以維持自定義配色）。
 * @param content 主題內的 Composable 內容。
 */
@Composable
fun BabyMakiSukTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // 如果 MainActivity 已經呼叫 enableEdgeToEdge()，這裡主要處理 Icon 顏色
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
