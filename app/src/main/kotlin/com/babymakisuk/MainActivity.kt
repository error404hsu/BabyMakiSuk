package com.babymakisuk

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

    private val settingsViewModel: SettingsViewModel by viewModels()

    // Android 13 (API 33)+ 通知 Runtime 權限申請 Launcher
    // 結果不強制阻擋 UX；拒絕時 SettingsScreen 通知開關會引導用戶至系統設定
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // 用戶拒絕通知權限，靜默處理
                // SettingsViewModel.notificationsEnabled 仍可讓用戶手動開啟提醒入口
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ 才需要 Runtime 申請 POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val darkModeOption by settingsViewModel.darkMode.collectAsState()

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
