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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.MedicalVisit
import com.babymakisuk.featuremedical.fever.FeverScreen
import com.babymakisuk.ui.components.BabyTopBar
import com.babymakisuk.ui.components.LocalDrawerState
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalScreen(
    viewModel: MedicalViewModel = hiltViewModel(),
    navController: NavController,
    onNavigateToAi: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val canEditData by viewModel.canEditData.collectAsState()
    val canUseLocalAi by viewModel.canUseLocalAi.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showFeverDialog by remember { mutableStateOf(false) }
    val tabs = listOf("就診紀錄", "發燒日誌")

    val drawerState = LocalDrawerState.current
    val drawerScope = rememberCoroutineScope()

    val selectedChildColor = (uiState as? MedicalUiState.Success)?.let { state ->
        val gender = state.children.find { it.id == state.selectedChildId }?.gender
        if (gender == Gender.MALE) BoyBlue else GirlPink
    } ?: MaterialTheme.colorScheme.primary

    val selectedChildId = (uiState as? MedicalUiState.Success)?.selectedChildId
    val children = (uiState as? MedicalUiState.Success)?.children ?: emptyList()

    Scaffold(
        containerColor = selectedChildColor.copy(alpha = 0.05f),
        topBar = {
            BabyTopBar(
                title = {
                    if (children.isNotEmpty()) {
                        ChildSelectorRow(
                            children = children,
                            selectedChildId = selectedChildId ?: -1L,
                            onSelectChild = { viewModel.selectChild(it) }
                        )
                    }
                },
                showSearch = true,
                showAi = false,
                showAdd = true,
                onMenuClick = { drawerScope.launch { drawerState.open() } },
                onAddClick = {
                    val childId = (uiState as? MedicalUiState.Success)?.selectedChildId ?: 1L
                    if (selectedTab == 0) {
                        navController.navigate("medical/edit?visitId=-1&childId=$childId")
                    } else {
                        showFeverDialog = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> MedicalVisitList(
                        uiState = uiState,
                        selectedChildColor = selectedChildColor,
                        canEditData = canEditData,
                        canUseLocalAi = canUseLocalAi,
                        navController = navController,
                        viewModel = viewModel
                    )
                    1 -> FeverScreen(
                        childId = selectedChildId ?: -1L,
                        showAddDialog = showFeverDialog,
                        onDialogDismiss = { showFeverDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicalVisitList(
    uiState: MedicalUiState,
    selectedChildColor: Color,
    canEditData: Boolean,
    canUseLocalAi: Boolean,
    navController: NavController,
    viewModel: MedicalViewModel
) {
    Column(Modifier.fillMaxSize()) {
        when (uiState) {
            is MedicalUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is MedicalUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("錯誤：${uiState.message}")
            }
            is MedicalUiState.Success -> {
                uiState.visits.firstOrNull()?.let { latest ->
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
                    if (uiState.visits.isEmpty()) {
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
                                        onClick = {
                                            navController.navigate("medical/edit?visitId=-1&childId=${uiState.selectedChildId}")
                                        },
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
                            items(uiState.visits, key = { it.id }) { visit ->
                                MedicalVisitCard(
                                    visit = visit,
                                    accentColor = selectedChildColor,
                                    canEdit = canEditData,
                                    canUseLocalAi = canUseLocalAi,
                                    onEdit = {
                                        navController.navigate("medical/edit?visitId=${visit.id}&childId=${visit.childId}")
                                    },
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

@Composable
private fun ChildSelectorRow(
    children: List<ChildProfile>,
    selectedChildId: Long,
    onSelectChild: (Long) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(children) { child ->
            val isSelected = child.id == selectedChildId
            val childColor = if (child.gender == Gender.MALE) BoyBlue else GirlPink

            Surface(
                onClick = { onSelectChild(child.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) childColor else childColor.copy(alpha = 0.1f),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) childColor else childColor.copy(alpha = 0.3f)
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color.White.copy(alpha = 0.2f) else childColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (child.gender == Gender.MALE) "👦" else "👧", fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
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
            text = visit.date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd")),
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

    // 整張卡片可點擊切換展開
    Card(
        onClick = { expanded = !expanded },
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
                            append(visit.date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd")))
                            if (visit.department.isNotBlank()) append("  ·  ${visit.department}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 展開指示箭頭（替代原展開按鈕）
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收合" else "展開",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
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

                if (visit.diagnosisSummary.isNotBlank()) {
                    AiInfoCard(
                        icon = "📋",
                        title = "AI 診斷摘要",
                        content = visit.diagnosisSummary,
                        color = accentColor
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (visit.prescriptions.isNotBlank()) {
                    AiInfoCard(
                        icon = "💊",
                        title = "處方內容",
                        content = visit.prescriptions,
                        color = accentColor
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (visit.careInstructions.isNotBlank()) {
                    AiInfoCard(
                        icon = "🏠",
                        title = "居家照護建議",
                        content = visit.careInstructions,
                        color = accentColor
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
