package com.babymakisuk.featuremedical

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun MedicalScreen(viewModel: MedicalViewModel = hiltViewModel()) {
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
        containerColor = selectedChildColor.copy(alpha = 0.05f),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("就醫紀錄", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            // 僅 canEditData 角色顯示 FAB
            if (canEditData) {
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
                        ChildAvatarSelector(
                            children = state.children,
                            selectedId = state.selectedChildId,
                            onSelect = viewModel::selectChild,
                            accentColor = selectedChildColor
                        )
                        state.visits.firstOrNull()?.let { latest ->
                            LatestMedicalHero(latest, selectedChildColor)
                        } ?: Box(Modifier.height(100.dp))

                        Surface(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            if (state.visits.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("尚無就診紀錄", color = Color.Gray)
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
                                            onDelete = { viewModel.deleteVisit(visit) }
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
private fun ChildAvatarSelector(
    children: List<ChildProfile>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        children.forEach { child ->
            val isSelected = child.id == selectedId
            val childColor = if (child.gender == Gender.MALE) BoyBlue else GirlPink
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(child.id) }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) childColor else childColor.copy(alpha = 0.1f))
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = childColor.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (child.gender == Gender.MALE) "👦" else "👧",
                        fontSize = 24.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = child.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accentColor else Color.Gray
                )
            }
        }
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
        Text("最近一次就診", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(
            text = visit.hospital,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = accentColor
        )
        if (visit.diagnosis.isNotBlank()) {
            Surface(
                color = accentColor.copy(alpha = 0.1f),
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
            color = Color.Gray,
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
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
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
                        color = Color.Gray
                    )
                }
                // 編輯 / 刪除僅 canEditData 顯示
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "編輯", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "刪除", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (visit.diagnosis.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "診斷：${visit.diagnosis}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242)
                )
            }

            // AI 提問按鈕：本機 AI 限 canUseLocalAi；Cloud API 所有角色可用
            val aiButtonLabel = when {
                canUseLocalAi -> "查看 AI 建議與備註"
                else -> "雲端 AI 提問"
            }
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    if (expanded) "收合詳情" else aiButtonLabel,
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

            if (expanded) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = accentColor.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                if (!canUseLocalAi) {
                    // DATA_MANAGER 提示訊息
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

                if (visit.diagnosisSummary.isNotBlank()) {
                    AiInfoCard(icon = "📋", title = "AI 診斷摘要", content = visit.diagnosisSummary, color = accentColor)
                    Spacer(Modifier.height(8.dp))
                }
                if (visit.prescriptions.isNotBlank()) {
                    AiInfoCard(icon = "💊", title = "處方內容", content = visit.prescriptions, color = accentColor)
                    Spacer(Modifier.height(8.dp))
                }
                if (visit.careInstructions.isNotBlank()) {
                    AiInfoCard(icon = "🏠", title = "居家照護建議", content = visit.careInstructions, color = accentColor)
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
private fun AiInfoCard(icon: String, title: String, content: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.05f),
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
                color = Color(0xFF424242),
                lineHeight = 18.sp
            )
        }
    }
}
