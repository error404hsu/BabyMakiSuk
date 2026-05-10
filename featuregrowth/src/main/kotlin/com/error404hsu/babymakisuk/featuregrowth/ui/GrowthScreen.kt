package com.error404hsu.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.error404hsu.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile

@Composable
fun GrowthScreen(viewModel: GrowthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val showForm by viewModel.showForm.collectAsState()
    var showChart by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成長紀錄") },
                actions = {
                    IconButton(onClick = { showChart = !showChart }) {
                        Icon(
                            if (showChart) Icons.Filled.List else Icons.Filled.BarChart,
                            contentDescription = if (showChart) "列表" else "圖表"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openForm) {
                Icon(Icons.Filled.Add, contentDescription = "新增紀錄")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (val state = uiState) {
                is GrowthUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is GrowthUiState.Error   -> Text("錯誤：${state.message}", Modifier.align(Alignment.Center))
                is GrowthUiState.Success ->
                    if (showChart) {
                        GrowthChartScreen(records = state.records)
                    } else {
                        GrowthListScreen(
                            records = state.records,
                            onDelete = { viewModel.deleteRecord(it.record) }
                        )
                    }
            }
        }
    }

    if (showForm) {
        NewGrowthRecordDialog(
            onDismiss = viewModel::closeForm,
            onConfirm = { h, w, hc, date, note -> viewModel.saveRecord(h, w, hc, date, note) }
        )
    }
}
