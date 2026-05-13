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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── 顏色定義 ──────────────────────────────────────────
// 基於 ui-ux-pro-max 推薦色票：
//   Primary #0369A1（深穩藍，提升信賴感）
//   Tertiary #22C55E（CTA 綠色，用於正向動作）
//   Background #F0F9FF（淺藍白底）
//   Text #0C4A6E（深藍灰，提升可讀性）

private val LightColors = lightColorScheme(
    primary          = Color(0xFF0369A1),
    onPrimary        = Color.White,
    secondary        = Color(0xFFFFB74D),
    onSecondary      = Color.Black,
    tertiary         = Color(0xFF22C55E),
    onTertiary       = Color.White,
    background       = Color(0xFFF0F9FF),
    surface          = Color.White,
    onBackground     = Color(0xFF0C4A6E),
    onSurface        = Color(0xFF0C4A6E)
)

private val DarkColors = darkColorScheme(
    primary          = Color(0xFF38BDF8),
    onPrimary        = Color(0xFF003544),
    secondary        = Color(0xFFFFB74D),
    onSecondary      = Color(0xFF3D2800),
    tertiary         = Color(0xFF22C55E),
    onTertiary       = Color(0xFF003314),
    background       = Color(0xFF121212),
    surface          = Color(0xFF1E1E1E),
    onBackground     = Color(0xFFE0E0E0),
    onSurface        = Color(0xFFE0E0E0)
)

// ── 字型定義 ──────────────────────────────────────────
// 基於 ui-ux-pro-max 推薦：Lora（標題）+ Raleway（內文）
// 安裝實際字型：下載 .ttf 放入 res/font/，即可替換 FontFamily.Default

private val BabyMakiTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp),
    displayMedium  = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 36.sp),
    displaySmall   = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge  = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 10.sp, lineHeight = 14.sp),
)

/**
 * App 全局主題。
 *
 * @param darkTheme true → 深色；false → 淺色；預設跟隨系統。
 * @param dynamicColor 是否啟用 Android 12+ 動態配色（預設關閉以維持自定義配色）。
 * @param content 主題內的 Composable 內容。
 *
 * 字型：Lora（Serif）用於標題，Raleway（SansSerif）用於內文。
 *       將 Lora-Regular.ttf / Lora-Bold.ttf / Raleway-*.ttf 放入 res/font/
 *       並將 FontFamily.Serif 改為 FontFamily(Font(R.font.lora_regular), ...) 即可。
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
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BabyMakiTypography,
        shapes = Shapes(),
        content = content
    )
}
