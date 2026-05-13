package com.babymakisuk.featuremedical

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.MedicalVisit

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalScreen(
    viewModel: MedicalViewModel = hiltViewModel(),
    onNavigateToAi: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val showForm by viewModel.showForm.collectAsState()
    val editingVisit by viewModel.editingVisit.collectAsState()
    val canEditData by viewModel.canEditData.collectAsState()
    val canUseLocalAi by viewModel.canUseLocalAi.collectAsState()

    val selectedChildColor = (uiState as? MedicalUiState.Success)?.let { state ->
        val gender = state.children.find { it.id == state.selectedChildId }?.gender
        if (gender == Gender.MALE) BoyBlue else GirlPink
    } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                "就醫紀錄",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        },
                        actions = {
                            IconButton(onClick = { /* TODO: Search */ }) {
                                Icon(Icons.Default.Search, contentDescription = "搜尋")
                            }
                            IconButton(onClick = { onNavigateToAi("PEDIATRIC_DOCTOR") }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "問問AI",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    // 精緻版寶寶篩選列
                    if (uiState is MedicalUiState.Success) {
                        val state = uiState as MedicalUiState.Success
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.children) { child ->
                                val isSelected = child.id == state.selectedChildId
                                val childColor = if (child.gender == Gender.MALE) BoyBlue else GirlPink
                                
                                Surface(
                                    onClick = { viewModel.selectChild(child.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) childColor else childColor.copy(alpha = 0.1f),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) childColor else childColor.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color.White.copy(alpha = 0.2f) else childColor.copy(alpha = 0.1f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(if (child.gender == Gender.MALE) "👦" else "👧", fontSize = 16.sp)
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = child.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else childColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { onNavigateToAi("PEDIATRIC_DOCTOR") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI 兒科醫師")
                }

                if (canEditData) {
                    Spacer(Modifier.height(16.dp))
                    ExtendedFloatingActionButton(
                        onClick = viewModel::openForm,
                        containerColor = selectedChildColor,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.Add, "新增")
                        Spacer(Modifier.width(8.dp))
                        Text("新增就診")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is MedicalUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is MedicalUiState.Error -> Text("錯誤：${state.message}", Modifier.align(Alignment.Center))
                is MedicalUiState.Success -> {
                    Column(Modifier.fillMaxSize()) {
                        state.visits.firstOrNull()?.let { latest ->
                            Spacer(Modifier.height(12.dp))
                            LatestMedicalHero(latest, selectedChildColor)
                        } ?: Box(Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            if (state.visits.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.MedicalServices,
                                            contentDescription = null,
                                            modifier = Modifier.size(72.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            "尚無就診紀錄",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "記錄寶寶的每一次就醫，方便追蹤健康狀況",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (canEditData) {
                                            Spacer(Modifier.height(24.dp))
                                            Button(
                                                onClick = viewModel::openForm,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("新增就診")
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.visits, key = { it.id }) { visit ->
                                        MedicalVisitCard(
                                            visit = visit,
                                            accentColor = selectedChildColor,
                                            canEdit = canEditData,
                                            canUseLocalAi = canUseLocalAi,
                                            onEdit = { viewModel.editVisit(visit) },
                                            onDelete = { viewModel.deleteVisit(visit) },
                                            onTriggerAi = { viewModel.triggerAiSummary(visit) },
                                            onUpdateAiFields = { id, diagnosisSummary, prescriptions, careInstructions, isUrgent ->
                                                viewModel.updateAiFields(id, diagnosisSummary, prescriptions, careInstructions, isUrgent)
                                            }
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

    if (showForm && canEditData) {
        val selectedChildId = (uiState as? MedicalUiState.Success)?.selectedChildId ?: 1L
        NewMedicalVisitDialog(
            childId = selectedChildId,
            initialVisit = editingVisit,
            onDismiss = viewModel::closeForm,
            onConfirm = viewModel::saveVisit
        )
    }
}

@Composable
private fun LatestMedicalHero(visit: MedicalVisit, accentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "最近一次就診",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = visit.hospital,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = accentColor
        )
        if (visit.diagnosis.isNotBlank()) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = visit.diagnosis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
        }
        Text(
            text = visit.date.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun MedicalVisitCard(
    visit: MedicalVisit,
    accentColor: Color,
    canEdit: Boolean,
    canUseLocalAi: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTriggerAi: () -> Unit,
    onUpdateAiFields: (Long, String, String, String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var editingDiagnosis by remember(visit.id, visit.diagnosisSummary) { mutableStateOf(false) }
    var editingPrescriptions by remember(visit.id, visit.prescriptions) { mutableStateOf(false) }
    var editingCare by remember(visit.id, visit.careInstructions) { mutableStateOf(false) }

    var editDiagnosisText by remember(visit.id, visit.diagnosisSummary) { mutableStateOf(visit.diagnosisSummary) }
    var editPrescriptionsText by remember(visit.id, visit.prescriptions) { mutableStateOf(visit.prescriptions) }
    var editCareText by remember(visit.id, visit.careInstructions) { mutableStateOf(visit.careInstructions) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        visit.hospital,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = buildString {
                            append(visit.date.toString())
                            if (visit.department.isNotBlank()) append("  ·  ${visit.department}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "編輯",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "刪除",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (visit.diagnosis.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "診斷：${visit.diagnosis}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 安全提示 Banner：AI 已整理時顯示
            if (visit.diagnosisSummary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                val (bannerColor, bannerText) = if (visit.isUrgent) {
                    MaterialTheme.colorScheme.errorContainer to
                        "🚨 AI 偵測到緊急提示，請立即就醫或聯絡醫師"
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer to
                        "⚕️ AI 整理僅供參考，請以醫師診斷為準"
                }
                Surface(
                    color = bannerColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bannerText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // AI 提問按鈕 + AI 整理觸發按鈕
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (canUseLocalAi && canEdit) {
                    TextButton(
                        onClick = onTriggerAi,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "AI 整理",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor
                        )
                    }
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (expanded) "收合詳情" else "展開",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accentColor
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = accentColor.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                if (!canUseLocalAi) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "此角色僅支援雲端 API 提問，本機 LLM 功能需切換至 AI 操作員",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // AI 診斷摘要（可編輯）
                if (visit.diagnosisSummary.isNotBlank() || canEdit) {
                    EditableAiField(
                        icon = "📋",
                        title = "AI 診斷摘要",
                        isEditing = editingDiagnosis,
                        editText = editDiagnosisText,
                        displayText = visit.diagnosisSummary.ifBlank { "尚無紀錄" },
                        color = accentColor,
                        canEdit = canEdit,
                        onStartEdit = {
                            editDiagnosisText = visit.diagnosisSummary
                            editingDiagnosis = true
                        },
                        onConfirmEdit = {
                            onUpdateAiFields(visit.id, editDiagnosisText, visit.prescriptions, visit.careInstructions, visit.isUrgent)
                            editingDiagnosis = false
                        },
                        onCancelEdit = { editingDiagnosis = false },
                        onTextChange = { editDiagnosisText = it },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // 處方內容（可編輯）
                if (visit.prescriptions.isNotBlank() || canEdit) {
                    EditableAiField(
                        icon = "💊",
                        title = "處方內容",
                        isEditing = editingPrescriptions,
                        editText = editPrescriptionsText,
                        displayText = visit.prescriptions.ifBlank { "尚無紀錄" },
                        color = accentColor,
                        canEdit = canEdit,
                        onStartEdit = {
                            editPrescriptionsText = visit.prescriptions
                            editingPrescriptions = true
                        },
                        onConfirmEdit = {
                            onUpdateAiFields(visit.id, visit.diagnosisSummary, editPrescriptionsText, visit.careInstructions, visit.isUrgent)
                            editingPrescriptions = false
                        },
                        onCancelEdit = { editingPrescriptions = false },
                        onTextChange = { editPrescriptionsText = it },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // 居家照護建議（可編輯）
                if (visit.careInstructions.isNotBlank() || canEdit) {
                    EditableAiField(
                        icon = "🏠",
                        title = "居家照護建議",
                        isEditing = editingCare,
                        editText = editCareText,
                        displayText = visit.careInstructions.ifBlank { "尚無紀錄" },
                        color = accentColor,
                        canEdit = canEdit,
                        onStartEdit = {
                            editCareText = visit.careInstructions
                            editingCare = true
                        },
                        onConfirmEdit = {
                            onUpdateAiFields(visit.id, visit.diagnosisSummary, visit.prescriptions, editCareText, visit.isUrgent)
                            editingCare = false
                        },
                        onCancelEdit = { editingCare = false },
                        onTextChange = { editCareText = it },
                        singleLine = false
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (visit.notes.isNotBlank()) {
                    AiInfoCard(icon = "📝", title = "備註", content = visit.notes, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun EditableAiField(
    icon: String,
    title: String,
    isEditing: Boolean,
    editText: String,
    displayText: String,
    color: Color,
    canEdit: Boolean,
    onStartEdit: () -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onTextChange: (String) -> Unit,
    singleLine: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                if (canEdit) {
                    if (isEditing) {
                        IconButton(onClick = onConfirmEdit, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Check, contentDescription = "確認", tint = color, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onCancelEdit, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "取消", tint = color, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        IconButton(onClick = onStartEdit, modifier = Modifier.size(20.dp)) {
                            Text("✏️", fontSize = 14.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun AiInfoCard(icon: String, title: String, content: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
