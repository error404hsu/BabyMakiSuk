package com.babymakisuk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.featuresettings.SettingsViewModel
import com.babymakisuk.navigation.BabyMakiSukNavHost
import com.babymakisuk.ui.theme.BabyMakiSukTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 由 Hilt 注入 SettingsViewModel，它的生命週期跟隨 Activity
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkModeOption by settingsViewModel.darkMode.collectAsState()

            // 將 DarkModeOption 映射為 Boolean
            val isDarkTheme = when (darkModeOption) {
                DarkModeOption.DARK   -> true
                DarkModeOption.LIGHT  -> false
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
            }

            BabyMakiSukTheme(darkTheme = isDarkTheme) {
                BabyMakiSukNavHost()
            }
        }
    }
}
