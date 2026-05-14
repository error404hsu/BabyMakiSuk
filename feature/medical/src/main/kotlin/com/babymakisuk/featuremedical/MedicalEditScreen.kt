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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import java.io.File
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalEditScreen(
    navController: NavController,
    visitId: Long = -1L,
    childId: Long = -1L,
    viewModel: MedicalEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiAnalysisState by viewModel.aiAnalysisState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(visitId, childId) {
        viewModel.initialize(visitId, childId)
    }

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            navController.popBackStack()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(uiState.date) {
            uiState.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("確認") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    var prescriptionImageUri by remember { mutableStateOf<Uri?>(null) }

    // 當從資料庫讀取到現有圖片路徑時，更新預覽 Uri
    LaunchedEffect(uiState.imageStoragePath) {
        val path = uiState.imageStoragePath
        if (path != null && prescriptionImageUri == null) {
            prescriptionImageUri = Uri.fromFile(File(path))
        }
    }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (visitId > 0L) "編輯就診紀錄" else "新增就診紀錄") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }) {
                        Text("儲存")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.dateStr,
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

            OutlinedTextField(
                value = uiState.hospital,
                onValueChange = viewModel::updateHospital,
                label = { Text("醫院名稱 *") },
                isError = uiState.hospitalError,
                supportingText = if (uiState.hospitalError) {{ Text("必填") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.department,
                onValueChange = viewModel::updateDepartment,
                label = { Text("科別") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.diagnosis,
                onValueChange = viewModel::updateDiagnosis,
                label = { Text("主訴 / 症狀") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("備註") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                            viewModel.resetAiState()
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

            Button(
                onClick = { viewModel.analyzeImageWithAi(prescriptionImageUri, uiState.diagnosis) },
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

            AnimatedVisibility(visible = aiAnalysisState is AiAnalysisState.Reviewing) {
                (aiAnalysisState as? AiAnalysisState.Reviewing)?.let { reviewing ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔍", fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "AI 辨識完成，可信度 ${reviewing.confidence}%。請確認以下內容後選擇填入。",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "📋 診斷摘要",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    reviewing.diagnosisSummary.ifBlank { "（無）" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "💊 處方內容",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    reviewing.prescriptions.ifBlank { "（無）" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "🏠 照護建議",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    reviewing.careInstructions.ifBlank { "（無）" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.resetAiState() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("捨棄")
                            }
                            Button(
                                onClick = { viewModel.confirmAnalysis() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("確認填入")
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = aiAnalysisState is AiAnalysisState.Success) {
                val confidence = (aiAnalysisState as? AiAnalysisState.Success)?.confidence ?: 0
                val (bannerColor, bannerIcon) = when {
                    confidence >= 80 -> MaterialTheme.colorScheme.tertiaryContainer to "✅"
                    confidence >= 60 -> MaterialTheme.colorScheme.secondaryContainer to "⚠️"
                    else -> MaterialTheme.colorScheme.errorContainer to "❌"
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
                            "AI 分析完成，可信度 $confidence%。內容已自動填入，請確認後儲存。\n\u2695\uFE0F 僅供參考，請以醫師診斷為準。",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            AnimatedVisibility(visible = aiAnalysisState is AiAnalysisState.Error) {
                val errMsg = (aiAnalysisState as? AiAnalysisState.Error)?.message ?: ""
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "\u274C $errMsg",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "AI 診斷",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = uiState.diagnosisSummary,
                onValueChange = viewModel::updateDiagnosisSummary,
                label = { Text("診斷摘要") },
                placeholder = { Text("AI 分析後自動填入，亦可手動輸入") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            Text(
                "處方",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = uiState.prescriptions,
                onValueChange = viewModel::updatePrescriptions,
                label = { Text("藥物與用法") },
                placeholder = { Text("AI 分析後自動填入，亦可手動輸入") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            Text(
                "居家照護",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = uiState.careInstructions,
                onValueChange = viewModel::updateCareInstructions,
                label = { Text("照護建議") },
                placeholder = { Text("AI 分析後自動填入，亦可手動輸入") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("儲存")
            }
        }
    }
}
