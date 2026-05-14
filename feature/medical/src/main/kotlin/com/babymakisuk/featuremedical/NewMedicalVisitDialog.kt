package com.babymakisuk.featuremedical

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.babymakisuk.coremodel.MedicalVisit
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMedicalVisitDialog(
    childId: Long,
    initialVisit: MedicalVisit? = null,
    aiAnalysisState: AiAnalysisState = AiAnalysisState.Idle,
    onDismiss: () -> Unit,
    onConfirm: (MedicalVisit) -> Unit,
    onAnalyzeImage: (Uri?, String) -> Unit = { _, _ -> },
    onResetAiState: () -> Unit = {}
) {
    val context = LocalContext.current

    // ── 基本欄位 ─────────────────────────────────────────────────────────────
    var hospital      by remember { mutableStateOf(initialVisit?.hospital ?: "") }
    var department    by remember { mutableStateOf(initialVisit?.department ?: "") }
    var diagnosis     by remember { mutableStateOf(initialVisit?.diagnosis ?: "") }
    var notes         by remember { mutableStateOf(initialVisit?.notes ?: "") }
    var hospitalError by remember { mutableStateOf(false) }

    // ── 就診日期 ──────────────────────────────────────────────────────────────
    var selectedDate   by remember { mutableStateOf(initialVisit?.date ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    // ── AI 三欄位（AI 填入 or 手動編輯）────────────────────────────────────────
    var aiDiagnosis  by remember { mutableStateOf(initialVisit?.diagnosisSummary ?: "") }
    var prescription by remember { mutableStateOf(initialVisit?.prescriptions ?: "") }
    var homeCare     by remember { mutableStateOf(initialVisit?.careInstructions ?: "") }

    // ── 藥單圖片 ──────────────────────────────────────────────────────────────
    var prescriptionImageUri by remember { mutableStateOf<Uri?>(null) }
    val imageFile = remember {
        File(context.cacheDir, "prescription_${System.currentTimeMillis()}.jpg")
    }
    val imageUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) prescriptionImageUri = imageUri }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { prescriptionImageUri = it } }

    // ── AI 分析結果自動填入三欄位 ─────────────────────────────────────────────
    LaunchedEffect(aiAnalysisState) {
        if (aiAnalysisState is AiAnalysisState.Success) {
            aiDiagnosis  = aiAnalysisState.diagnosisSummary
            prescription = aiAnalysisState.prescriptions
            homeCare     = aiAnalysisState.careInstructions
        }
    }

    // ── DatePicker Dialog ─────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("確認") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // ── 主畫面：ModalBottomSheet ─────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 標題
            Text(
                text = if (initialVisit == null) "新增就診紀錄" else "編輯就診紀錄",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            // ① 就診日期
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDate.format(DateTimeFormatter.ofPattern("yyyy / MM / dd")),
                    onValueChange = {},
                    label = { Text("就診日期") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "選擇日期")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            // ② 醫院（必填）
            OutlinedTextField(
                value = hospital,
                onValueChange = { hospital = it; hospitalError = false },
                label = { Text("醫院名稱 *") },
                isError = hospitalError,
                supportingText = { if (hospitalError) Text("必填") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ③ 科別
            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                label = { Text("科別") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ④ 主訴 / 症狀
            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("主訴 / 症狀") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ⑤ 備註
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("備註") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ⑥ 藥單拍照區塊
            Text(
                "藥單照片",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch(imageUri) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("拍照")
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("相簿")
                }
            }

            // 圖片預覽
            AnimatedVisibility(visible = prescriptionImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = prescriptionImageUri,
                        contentDescription = "藥單預覽",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = {
                            prescriptionImageUri = null
                            onResetAiState()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "移除圖片",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ⑦ AI 分析按鈕
            Button(
                onClick = { onAnalyzeImage(prescriptionImageUri, diagnosis) },
                enabled = prescriptionImageUri != null && aiAnalysisState !is AiAnalysisState.Analyzing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                when (aiAnalysisState) {
                    is AiAnalysisState.Analyzing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI 分析中…")
                    }
                    else -> {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI 分析藥單")
                    }
                }
            }

            // AI 信心分數 Banner
            AnimatedVisibility(visible = aiAnalysisState is AiAnalysisState.Success) {
                val confidence = (aiAnalysisState as? AiAnalysisState.Success)?.confidence ?: 0
                val (bannerColor, bannerIcon) = when {
                    confidence >= 80 -> MaterialTheme.colorScheme.tertiaryContainer to "✅"
                    confidence >= 60 -> MaterialTheme.colorScheme.secondaryContainer to "⚠️"
                    else             -> MaterialTheme.colorScheme.errorContainer to "❌"
                }
                Surface(
                    color = bannerColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(bannerIcon, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI 分析完成，可信度 $confidence%。內容已自動填入，請確認後儲存。\n⚕️ 僅供參考，請以醫師診斷為準。",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // AI 錯誤提示
            AnimatedVisibility(visible = aiAnalysisState is AiAnalysisState.Error) {
                val errMsg = (aiAnalysisState as? AiAnalysisState.Error)?.message ?: ""
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "❌ $errMsg",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ⑧ AI 診斷
            Text(
                "AI 診斷",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = aiDiagnosis,
                onValueChange = { aiDiagnosis = it },
                label = { Text("診斷摘要") },
                placeholder = { Text("AI 分析後自動填入，亦可手動輸入") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            // ⑨ 處方
            Text(
                "處方",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = prescription,
                onValueChange = { prescription = it },
                label = { Text("藥物與用法") },
                placeholder = { Text("AI 分析後自動填入，亦可手動輸入") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            // ⑩ 居家照護
            Text(
                "居家照護",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = homeCare,
                onValueChange = { homeCare = it },
                label = { Text("照護建議") },
                placeholder = { Text("AI 分析後自動填入，亦可手動輸入") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            Spacer(Modifier.height(8.dp))

            // 儲存 / 取消
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        if (hospital.isBlank()) { hospitalError = true; return@Button }
                        onConfirm(
                            MedicalVisit(
                                id               = initialVisit?.id ?: 0L,
                                childId          = childId,
                                date             = selectedDate,
                                hospital         = hospital.trim(),
                                department       = department.trim(),
                                diagnosis        = diagnosis.trim(),
                                notes            = notes.trim(),
                                diagnosisSummary = aiDiagnosis.trim(),
                                prescriptions    = prescription.trim(),
                                careInstructions = homeCare.trim()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("儲存")
                }
            }
        }
    }
}
