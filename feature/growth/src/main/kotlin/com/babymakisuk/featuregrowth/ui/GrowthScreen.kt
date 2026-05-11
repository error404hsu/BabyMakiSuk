package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile

private val BoyBlue  = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreen(
    viewModel: GrowthViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel == null) {
        Box(Modifier.fillMaxSize()) {
            Text("Growth Screen Preview", modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val uiState  by viewModel.uiState.collectAsState()
    val showForm by viewModel.showForm.collectAsState()
    var showChart by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成長紀錄") },
                actions = {
                    IconButton(onClick = { showChart = !showChart }) {
                        Icon(
                            imageVector = if (showChart) Icons.AutoMirrored.Filled.List else Icons.Filled.ShowChart,
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
                is GrowthUiState.Success -> {
                    Column(Modifier.fillMaxSize()) {
                        state.records.maxByOrNull { it.record.date }?.let { latest ->
                            LatestGrowthBanner(latest)
                        }
                        Box(Modifier.weight(1f)) {
                            if (showChart) {
                                GrowthChartScreen(records = state.records)
                            } else {
                                GrowthListScreen(records = state.records) {
                                    viewModel.deleteRecord(it.record)
                                }
                            }
                        }
                    }
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

@Composable
private fun LatestGrowthBanner(item: GrowthRecordWithPercentile) {
    val accentColor = if (item.gender == Gender.MALE) BoyBlue else GirlPink
    val r = item.record
    Surface(
        color = accentColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BannerStat(label = "身高", value = java.lang.String.format("%.1f cm", r.heightCm), pct = item.heightPercentile, color = accentColor)
            BannerStat(label = "體重", value = java.lang.String.format("%.1f kg", r.weightKg), pct = item.weightPercentile, color = accentColor)
            r.headCircumferenceCm?.let {
                BannerStat(label = "頭圍", value = java.lang.String.format("%.1f cm", it), pct = -1, color = accentColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("最近更新", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    r.date.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun BannerStat(label: String, value: String, pct: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        if (pct >= 0) {
            val pctColor = when {
                pct < 3 || pct > 97  -> MaterialTheme.colorScheme.error
                pct < 15 || pct > 85 -> MaterialTheme.colorScheme.tertiary
                else                 -> color
            }
            Text("P$pct", style = MaterialTheme.typography.labelSmall, color = pctColor)
        }
    }
}
