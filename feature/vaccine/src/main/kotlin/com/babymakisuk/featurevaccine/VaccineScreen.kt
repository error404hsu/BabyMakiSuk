package com.babymakisuk.featurevaccine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("疫苗接種") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // TODO: Phase E - Vaccine reminders
            Text("疫苗接種功能開發中...", modifier = Modifier.padding(16.dp))
        }
    }
}
