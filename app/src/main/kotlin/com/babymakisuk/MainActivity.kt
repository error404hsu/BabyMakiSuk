package com.babymakisuk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.babymakisuk.navigation.BabyMakiSukNavHost
import com.babymakisuk.ui.theme.BabyMakiSukTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BabyMakiSukTheme {
                BabyMakiSukNavHost()
            }
        }
    }
}
