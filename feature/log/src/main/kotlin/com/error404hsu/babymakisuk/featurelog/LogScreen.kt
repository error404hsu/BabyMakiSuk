package com.error404hsu.babymakisuk.featurelog

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
        topBar = { TopAppBar(title = { Text("每日日誌") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // TODO: Phase D - DailyLog + AI weekly summary
            Text("日誌內容建設中...", modifier = Modifier.padding(16.dp))
        }
    }
}
