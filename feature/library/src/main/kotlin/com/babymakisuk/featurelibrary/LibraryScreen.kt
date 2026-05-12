package com.babymakisuk.featurelibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    Scaffold(
        topBar = {
            Surface(shadowElevation = 3.dp) {
                TopAppBar(
                    title = { Text("成長日誌", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) },
                    actions = {
                        IconButton(onClick = { /* TODO */ }) { Icon(Icons.Default.Search, "搜尋") }
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(Icons.Default.AutoAwesome, "問問AI", tint = Color(0xFF673AB7))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // TODO: Phase D - DailyLog + AI weekly summary
            Text("日誌功能開發中...", modifier = Modifier.padding(16.dp))
        }
    }
}
