package com.error404hsu.babymakisuk.featuremedical

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("醫療紀錄") }) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // TODO: Phase C - MedicalListScreen + AI summarizer
            Text("醫療紀錄建設中...", modifier = Modifier.padding(16.dp))
        }
    }
}
