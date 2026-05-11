package com.babymakisuk.featurelog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("成長紀錄") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // TODO: Phase D - DailyLog + AI weekly summary
            Text("日誌功能開發中...", modifier = Modifier.padding(16.dp))
        }
    }
}
