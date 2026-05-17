package com.babymakisuk.featuremedical.fever

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coremodel.FeverRecord
import com.babymakisuk.coremodel.FeverSymptom
import com.babymakisuk.coremodel.displayName
import java.text.SimpleDateFormat
import java.time.*
import java.util.*

private val FeverRed = Color(0xFFE53935)
private val FeverOrange = Color(0xFFFF6D00)
private val FeverGreen = Color(0xFF43A047)

private fun tempColor(t: Float) = when {
    t >= 39f -> FeverRed
    t >= 38f -> FeverOrange
    else     -> FeverGreen
}

@Composable
fun FeverScreen(
    childId: Long,
    showAddDialog: Boolean = false,
    onDialogDismiss: () -> Unit = {},
    viewModel: FeverViewModel = hiltViewModel()
) {
    LaunchedEffect(childId) { viewModel.init(childId) }

    val uiState by viewModel.uiState.collectAsState()
    var editingRecord by remember { mutableStateOf<FeverRecord?>(null) }

    if (showAddDialog || editingRecord != null) {
        FeverLogDialog(
            initialRecord = editingRecord,
            onDismiss = {
                editingRecord = null
                onDialogDismiss()
            },
            onConfirm = { record ->
                viewModel.addRecord(record.copy(childId = childId))
                editingRecord = null
                onDialogDismiss()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is FeverUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is FeverUiState.Error -> Text("錯誤：${state.message}", Modifier.align(Alignment.Center))
            is FeverUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (state.episodes.isNotEmpty()) {
                        FeverSummaryHero(state)
                    }

                    Surface(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        if (state.episodes.isEmpty()) {
                            EmptyFeverState()
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.episodes, key = { it.id }) { episode ->
                                    FeverEpisodeCard(
                                        episode = episode,
                                        onEditRecord = { editingRecord = it },
                                        onDeleteRecord = { viewModel.deleteRecord(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeverState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌡️", fontSize = 48.sp, modifier = Modifier.alpha(0.3f))
            Spacer(Modifier.height(16.dp))
            Text("尚無發燒紀錄", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text("點擊右上角 + 記錄體溫", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeverSummaryHero(state: FeverUiState.Success) {
    val peak = state.peakTemperature ?: return
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("當前病程最高體溫", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(4.dp))
        Text(text = "${peak}°C", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black), color = tempColor(peak))
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroStatItem(label = "總紀錄", value = "${state.totalRecordsCount} 筆")
            HeroStatItem(label = "病程數", value = "${state.episodes.size} 次")
        }
    }
}

@Composable
private fun HeroStatItem(label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Text(text = value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun FeverEpisodeCard(
    episode: FeverEpisode,
    onEditRecord: (FeverRecord) -> Unit,
    onDeleteRecord: (FeverRecord) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${dateFormat.format(Date(episode.startTime))} 開始的病程",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "共 ${episode.records.size} 筆紀錄 · 最高 ${episode.records.maxOf { it.temperatureCelsius }}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                }
            }

            Spacer(Modifier.height(12.dp))
            // 折線圖區域
            FeverTrendChart(records = episode.records)

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                episode.records.reversed().forEach { record ->
                    FeverMiniRow(record, onEditRecord, onDeleteRecord)
                }
            }
        }
    }
}

@Composable
private fun FeverTrendChart(records: List<FeverRecord>) {
    val chartColor = MaterialTheme.colorScheme.primary
    val medicineColor = Color(0xFFFFA000)
    
    Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 8.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (records.size < 1) return@Canvas
            
            val minTemp = 36f
            val maxTemp = 41f
            val tempRange = maxTemp - minTemp
            
            val startTime = records.first().measuredAt
            val endTime = if (records.size == 1) startTime + 3600000 else records.last().measuredAt
            val timeRange = (endTime - startTime).coerceAtLeast(1)

            val points = records.map {
                val x = if (timeRange == 0L) 0f else (it.measuredAt - startTime).toFloat() / timeRange * size.width
                val y = size.height - ((it.temperatureCelsius - minTemp) / tempRange * size.height)
                Offset(x, y)
            }

            // 畫發燒基準線 (38度)
            val line38Y = size.height - ((38f - minTemp) / tempRange * size.height)
            drawLine(Color.Gray.copy(alpha = 0.2f), Offset(0f, line38Y), Offset(size.width, line38Y), strokeWidth = 1.dp.toPx())

            // 畫折線
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(path, chartColor, style = Stroke(width = 2.dp.toPx()))
            }

            // 畫點與標註
            records.forEachIndexed { index, record ->
                val p = points[index]
                drawCircle(tempColor(record.temperatureCelsius), radius = 4.dp.toPx(), center = p)
                
                if (record.isMedicineTaken) {
                    // 標註用藥 💊 符號位置 (簡化版用黃色圓圈代表)
                    drawCircle(medicineColor, radius = 6.dp.toPx(), center = Offset(p.x, p.y - 15.dp.toPx()))
                }
            }
        }
        // 用藥圖標提示
        records.forEachIndexed { index, record ->
            if (record.isMedicineTaken) {
                val startTime = records.first().measuredAt
                val endTime = if (records.size == 1) startTime + 3600000 else records.last().measuredAt
                val timeRange = (endTime - startTime).coerceAtLeast(1)
                val xPercent = (record.measuredAt - startTime).toFloat() / timeRange
                
                Text(
                    "💊", 
                    fontSize = 10.sp, 
                    modifier = Modifier.align(Alignment.TopStart).offset(x = (xPercent * 300).dp) // 概算位移
                )
            }
        }
    }
}

@Composable
private fun FeverMiniRow(
    record: FeverRecord,
    onEdit: (FeverRecord) -> Unit,
    onDelete: (FeverRecord) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(timeFormat.format(Date(record.measuredAt)), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
        Text("${record.temperatureCelsius}°C", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = tempColor(record.temperatureCelsius), modifier = Modifier.width(60.dp))
        
        if (record.isMedicineTaken) {
            Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(4.dp)) {
                Text("💊 已用藥", modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFF57C00))
            }
        }
        
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { onEdit(record) }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
        }
        IconButton(onClick = { onDelete(record) }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeverLogDialog(
    initialRecord: FeverRecord? = null,
    onDismiss: () -> Unit,
    onConfirm: (FeverRecord) -> Unit
) {
    var temperature by remember { mutableStateOf(initialRecord?.temperatureCelsius?.toString() ?: "") }
    var selectedSymptoms by remember { mutableStateOf(initialRecord?.symptoms?.toSet() ?: emptySet()) }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }
    var isMedicineTaken by remember { mutableStateOf(initialRecord?.isMedicineTaken ?: false) }
    var tempError by remember { mutableStateOf(false) }

    // 語音輸入 Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = data?.firstOrNull() ?: ""
            // 簡單解析數字：尋找字串中的數字部分
            val pattern = "(\\d+\\.\\d+|\\d+)".toRegex()
            val match = pattern.find(spokenText)
            match?.value?.let { temperature = it }
        }
    }

    val initialDateTime = initialRecord?.measuredAt?.let { 
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) 
    } ?: LocalDateTime.now()
    
    var selectedDate by remember { mutableStateOf(initialDateTime.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(initialDateTime.toLocalTime()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRecord == null) "🌡️ 新增發燒紀錄" else "🌡️ 編輯發燒紀錄") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it; tempError = false },
                        label = { Text("體溫 (°C) *") },
                        isError = tempError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出體溫，例如：三十八度五")
                            }
                            speechLauncher.launch(intent)
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Mic, "語音輸入")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedDate.toString(),
                        onValueChange = {},
                        label = { Text("日期") },
                        readOnly = true,
                        modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
                    )
                    OutlinedTextField(
                        value = selectedTime.toString().substring(0, 5),
                        onValueChange = {},
                        label = { Text("時間") },
                        readOnly = true,
                        modifier = Modifier.weight(1f).clickable { showTimePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.AccessTime, null) }
                    )
                }

                // 用藥切換
                Surface(
                    onClick = { isMedicineTaken = !isMedicineTaken },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMedicineTaken) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isMedicineTaken) Color(0xFFFFA000) else Color.Transparent)
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Medication, null, tint = if (isMedicineTaken) Color(0xFFF57C00) else MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(12.dp))
                        Text("本次紀錄已服用退燒藥", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Checkbox(checked = isMedicineTaken, onCheckedChange = { isMedicineTaken = it })
                    }
                }

                Text("症狀（可多選）", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeverSymptom.entries.forEach { symptom ->
                        FilterChip(
                            selected = symptom in selectedSymptoms,
                            onClick = { selectedSymptoms = if (symptom in selectedSymptoms) selectedSymptoms - symptom else selectedSymptoms + symptom },
                            label = { Text(symptom.displayName(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val temp = temperature.replace("度", ".").toFloatOrNull()
                    if (temp == null || temp < 35f || temp > 43f) { tempError = true; return@Button }
                    val timestamp = LocalDateTime.of(selectedDate, selectedTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onConfirm(FeverRecord(id = initialRecord?.id ?: 0L, childId = initialRecord?.childId ?: 0L, temperatureCelsius = temp, measuredAt = timestamp, symptoms = selectedSymptoms.toList(), note = note.trim(), isMedicineTaken = isMedicineTaken))
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() }; showDatePicker = false }) { Text("確定") } }) { DatePicker(state = datePickerState) }
    }
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedTime.hour, initialMinute = selectedTime.minute)
        AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = { selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute); showTimePicker = false }) { Text("確定") } }, text = { TimePicker(state = timePickerState) })
    }
}
