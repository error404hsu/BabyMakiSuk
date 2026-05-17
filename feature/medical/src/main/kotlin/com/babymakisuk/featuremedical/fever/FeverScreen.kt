package com.babymakisuk.featuremedical.fever

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coremodel.FeverRecord
import com.babymakisuk.coremodel.FeverSymptom
import com.babymakisuk.coremodel.displayName
import java.text.SimpleDateFormat
import java.util.*

private val FeverRed = Color(0xFFE53935)
private val FeverOrange = Color(0xFFFF6D00)
private val FeverGreen = Color(0xFF43A047)

/** 體溫顏色 */
private fun tempColor(t: Float) = when {
    t >= 39f -> FeverRed
    t >= 38f -> FeverOrange
    else     -> FeverGreen
}

/**
 * 發燒日誌 Tab。放在 MemoScreen 的 Tab 2 。
 *
 * @param childId 當前選中孩子 ID
 */
@Composable
fun FeverScreen(
    childId: Long,
    viewModel: FeverViewModel = hiltViewModel()
) {
    LaunchedEffect(childId) { viewModel.init(childId) }

    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var prefillMinutes by remember { mutableIntStateOf(0) }

    if (showDialog) {
        FeverLogDialog(
            prefillDurationMinutes = prefillMinutes,
            onDismiss = { showDialog = false },
            onConfirm = { record ->
                viewModel.addRecord(record)
                showDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 發燒計時器 FAB
                if (uiState is FeverUiState.Success) {
                    val s = uiState as FeverUiState.Success
                    SmallFloatingActionButton(
                        onClick = {
                            if (s.timerRunning) {
                                prefillMinutes = viewModel.stopTimer()
                                showDialog = true
                            } else {
                                viewModel.startTimer()
                            }
                        },
                        containerColor = if (s.timerRunning) FeverRed else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            if (s.timerRunning) Icons.Default.Stop else Icons.Default.Timer,
                            contentDescription = if (s.timerRunning) "停止計時" else "發燒計時"
                        )
                    }
                }
                // 新增紀錄 FAB
                FloatingActionButton(
                    onClick = { prefillMinutes = 0; showDialog = true },
                    containerColor = FeverRed
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增發燒紀錄")
                }
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is FeverUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is FeverUiState.Error -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("錯誤：${state.message}") }

            is FeverUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // 計時器提示橫幅
                    AnimatedVisibility(visible = state.timerRunning) {
                        Surface(
                            color = FeverRed.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = FeverRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "發燒計時中…點擊橙色按鈕停止并記錄",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FeverRed
                                )
                            }
                        }
                    }

                    // 頂部摘要卡
                    if (state.records.isNotEmpty()) {
                        FeverSummaryCard(state)
                    }

                    if (state.records.isEmpty()) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌡️", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "尚無發燒紀錄",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "點擊右下角 + 新增第一筆",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.records, key = { it.id }) { record ->
                                FeverRecordCard(
                                    record = record,
                                    onDelete = { viewModel.deleteRecord(record) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeverSummaryCard(state: FeverUiState.Success) {
    val peak = state.peakTemperature ?: return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = tempColor(peak).copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SummaryItem(
                label = "最高熱",
                value = "${peak}°C",
                color = tempColor(peak)
            )
            SummaryItem(
                label = "紀錄筆數",
                value = "${state.records.size} 筆",
                color = MaterialTheme.colorScheme.primary
            )
            if (state.totalDurationMinutes > 0) {
                val h = state.totalDurationMinutes / 60
                val m = state.totalDurationMinutes % 60
                SummaryItem(
                    label = "累積時長",
                    value = if (h > 0) "${h}h ${m}m" else "${m}m",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeverRecordCard(
    record: FeverRecord,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 體溫 Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tempColor(record.temperatureCelsius).copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${record.temperatureCelsius}°",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = tempColor(record.temperatureCelsius)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        dateFormat.format(Date(record.measuredAt)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (record.durationMinutes != null) {
                        Text(
                            "持續 ${record.durationMinutes} 分鐘",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "刪除",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 症狀 chips
            if (record.symptoms.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    record.symptoms.forEach { symptom ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                symptom.displayName(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // 展開區
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                if (record.medicineGiven.isNotBlank()) {
                    Text(
                        "💊 用藥：${record.medicineGiven}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (record.note.isNotBlank()) {
                    Text(
                        "📝 備註：${record.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record.linkedVisitId != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "🏥 已關聯就醫紀錄 #${record.linkedVisitId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 新增 / 編輯發燒紀錄 Dialog。
 *
 * @param prefillDurationMinutes 由計時器帶入的持續分鐘數
 */
@Composable
fun FeverLogDialog(
    prefillDurationMinutes: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (FeverRecord) -> Unit
) {
    var temperature by remember { mutableStateOf("") }
    var durationStr by remember { mutableStateOf(if (prefillDurationMinutes > 0) prefillDurationMinutes.toString() else "") }
    var selectedSymptoms by remember { mutableStateOf(setOf<FeverSymptom>()) }
    var note by remember { mutableStateOf("") }
    var medicine by remember { mutableStateOf("") }
    var tempError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🌡️ 發燒紀錄") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it; tempError = false },
                    label = { Text("體溫 (°C) *") },
                    isError = tempError,
                    supportingText = if (tempError) {{ Text("請輸入有效體溫，例如 38.5") }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("🌡️", fontSize = 16.sp) }
                )
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it },
                    label = { Text("持續時長 (分鐘)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                Text("症狀（可多選）", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeverSymptom.entries.forEach { symptom ->
                        FilterChip(
                            selected = symptom in selectedSymptoms,
                            onClick = {
                                selectedSymptoms = if (symptom in selectedSymptoms)
                                    selectedSymptoms - symptom
                                else
                                    selectedSymptoms + symptom
                            },
                            label = { Text(symptom.displayName(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = medicine,
                    onValueChange = { medicine = it },
                    label = { Text("用藥記錄") },
                    placeholder = { Text("藥名、劑量…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val temp = temperature.toFloatOrNull()
                if (temp == null || temp < 35f || temp > 43f) {
                    tempError = true
                    return@Button
                }
                onConfirm(
                    FeverRecord(
                        childId = 0L,
                        temperatureCelsius = temp,
                        measuredAt = System.currentTimeMillis(),
                        durationMinutes = durationStr.toIntOrNull(),
                        symptoms = selectedSymptoms.toList(),
                        note = note.trim(),
                        medicineGiven = medicine.trim()
                    )
                )
            }) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
