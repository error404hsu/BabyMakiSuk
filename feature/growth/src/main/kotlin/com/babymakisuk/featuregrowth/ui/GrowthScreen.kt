package com.babymakisuk.featuregrowth.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.GrowthRecord
import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile
import com.babymakisuk.ui.components.BabyTopBar
import com.babymakisuk.ui.components.LocalDrawerState
import com.babymakisuk.ui.theme.BabyMakiSukTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@Composable
fun ChildSelectorRow(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreen(
    viewModel: GrowthViewModel? = if (LocalInspectionMode.current) null else hiltViewModel(),
    navController: NavController
) {
    if (viewModel == null) {
        Box(Modifier.fillMaxSize()) {
            Text("Growth Screen (No ViewModel)", modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val canEditData by viewModel.canEditData.collectAsState()

    GrowthScreenContent(
        uiState = uiState,
        canEditData = canEditData,
        navController = navController,
        onSelectChild = viewModel::selectChild,
        onDeleteRecord = { viewModel.deleteRecord(it.record) },
        onRefreshAiAnalysis = viewModel::refreshAiAnalysis
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreenContent(
    uiState: GrowthUiState,
    canEditData: Boolean,
    navController: NavController,
    onSelectChild: (Long) -> Unit,
    onDeleteRecord: (GrowthRecordWithPercentile) -> Unit,
    onRefreshAiAnalysis: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val aiScrollState = rememberScrollState()
    val tabTitles = listOf("卡片", "圖表", "AI 建議")

    val drawerState = LocalDrawerState.current
    val drawerScope = rememberCoroutineScope()

    val selectedChildColor = (uiState as? GrowthUiState.Success)?.let { state ->
        val gender = state.children.find { it.id == state.selectedChildId }?.gender
        if (gender == Gender.MALE) BoyBlue else GirlPink
    } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = selectedChildColor.copy(alpha = 0.05f),
        topBar = {
            BabyTopBar(
                title = {
                    if (uiState is GrowthUiState.Success) {
                        val state = uiState
                        ChildSelectorRow(
                            children = state.children,
                            selectedChildId = state.selectedChildId,
                            onSelectChild = onSelectChild
                        )
                    }
                },
                showSearch = true,
                showAi = false,
                showAdd = canEditData,
                onMenuClick = { drawerScope.launch { drawerState.open() } },
                onAddClick = {
                    val childId = (uiState as? GrowthUiState.Success)?.selectedChildId ?: 1L
                    navController.navigate("growth/edit?recordId=-1&childId=$childId")
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            when (val state = uiState) {
                is GrowthUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is GrowthUiState.Error -> Text("錯誤：${state.message}", Modifier.align(Alignment.Center))
                is GrowthUiState.Success -> {
                    if (state.records.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(40.dp))
                                Icon(
                                    Icons.Default.ShowChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "尚無成長紀錄",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "點擊上方 + 按鈕新增第一筆身高體重測量",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            state.records.maxByOrNull { it.record.date }?.let { latest ->
                                Spacer(Modifier.height(12.dp))
                                LatestGrowthHero(latest, selectedChildColor)
                            } ?: Box(Modifier.height(16.dp))

                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = selectedChildColor,
                                indicator = { tabPositions ->
                                    if (selectedTab < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                            color = selectedChildColor
                                        )
                                    }
                                },
                                divider = {}
                            ) {
                                tabTitles.forEachIndexed { index, title ->
                                    val icon = when (index) {
                                        0 -> Icons.Default.ViewAgenda
                                        1 -> Icons.AutoMirrored.Filled.ShowChart
                                        else -> Icons.Default.AutoAwesome
                                    }
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        icon = { Icon(icon, contentDescription = null) },
                                        selectedContentColor = selectedChildColor,
                                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                            scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith
                                            fadeOut(animationSpec = tween(90))
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                label = "TabContent"
                            ) { targetTab ->
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    when (targetTab) {
                                        0 -> GrowthListScreen(
                                            records = state.records,
                                            onEdit = { item ->
                                                navController.navigate("growth/edit?recordId=${item.record.id}&childId=${item.record.childId}")
                                            },
                                            onDelete = onDeleteRecord
                                        )
                                        1 -> GrowthChartScreen(records = state.records)
                                        2 -> GrowthAiTab(
                                            aiAnalysisText = state.aiAnalysisText,
                                            isAnalyzing = state.isAnalyzing,
                                            onRefresh = onRefreshAiAnalysis,
                                            scrollState = aiScrollState
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
}

@Composable
private fun GrowthAiTab(
    aiAnalysisText: String,
    isAnalyzing: Boolean,
    onRefresh: () -> Unit,
    scrollState: ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (aiAnalysisText.isBlank() && !isAnalyzing) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "尚無 AI 分析",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "點擊下方按鈕讓 AI 分析所有生長紀錄趨勢",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI 生長分析",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (isAnalyzing) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                aiAnalysisText,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "⚠️ AI 整理僅供參考，如有疑慮請諮詢兒科醫師",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnalyzing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isAnalyzing) "分析中..." else "開始分析")
        }
    }
}

@Composable
private fun LatestGrowthHero(item: GrowthRecordWithPercentile, accentColor: Color) {
    val r = item.record
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            HeroStat(label = "身高", value = r.heightCm.toString(), unit = "cm", pct = item.heightPercentile, color = accentColor)
            HeroStat(label = "體重", value = r.weightKg.toString(), unit = "kg", pct = item.weightPercentile, color = accentColor)
            r.headCircumferenceCm?.let {
                HeroStat(label = "頭圍", value = it.toString(), unit = "cm", pct = item.headCircPercentile ?: -1, color = accentColor)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            color = accentColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "最後更新於 ${r.date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"))}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, unit: String, pct: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = color
            )
            Text(
                unit,
                modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
                style = MaterialTheme.typography.labelLarge,
                color = color.copy(alpha = 0.7f)
            )
        }
        if (pct >= 0) {
            val (pctText, pctColor) = when {
                pct < 3 || pct > 97 -> "異常 P$pct" to MaterialTheme.colorScheme.error
                pct < 15 || pct > 85 -> "注意 P$pct" to MaterialTheme.colorScheme.tertiary
                else -> "標準 P$pct" to color
            }
            Surface(
                color = pctColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    pctText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = pctColor
                )
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
