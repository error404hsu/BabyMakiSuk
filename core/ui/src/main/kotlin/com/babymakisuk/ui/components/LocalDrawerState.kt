package com.babymakisuk.ui.components

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDrawerState = staticCompositionLocalOf<DrawerState> {
    error("No DrawerState provided")
}
