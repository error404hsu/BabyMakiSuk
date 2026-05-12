package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreen(
    viewModel: GrowthViewModel? = if (LocalInspectionMode.current) null else hiltViewModel(),
    onNavigateToAi: (String?) -> Unit = {}
) {
    if (viewModel == null) {
        Box(Modifier.fillMaxSize()) {
            Text("Growth Screen Preview", modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val showForm by viewModel.showForm.collectAsState()
    val editingRecord by viewModel.editingRecord.collectAsState()
    val canEditData by viewModel.canEditData.collectAsState()
    var showChart by remember { mutableStateOf(false) }

    val selectedChildColor = (uiState as? GrowthUiState.Success)?.let { state ->
        val gender = state.children.find { it.id == state.selectedChildId }?.gender
        if (gender == Gender.MALE) BoyBlue else GirlPink
    } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = selectedChildColor.copy(alpha = 0.05f),
        topBar = {
            Surface(shadowElevation = 3.dp) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    TopAppBar(
                        title = {
                            Text(
                                "成長紀錄",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        },
                        actions = {
                            IconButton(onClick = { showChart = !showChart }) {
                                Icon(
                                    imageVector = if (showChart) Icons.AutoMirrored.Filled.List else Icons.Default.ShowChart,
                                    contentDescription = if (showChart) "列表" else "圖表"
                                )
                            }
                            IconButton(onClick = { /* TODO: Search */ }) {
                                Icon(Icons.Default.Search, contentDescription = "搜尋")
                            }
                            IconButton(onClick = { onNavigateToAi("GROWTH_ANALYST") }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "問問AI",
                                    tint = Color(0xFF673AB7)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    // 精緻版寶寶篩選列
                    if (uiState is GrowthUiState.Success) {
                        val state = uiState as GrowthUiState.Success
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
                                            color = Color.White.copy(alpha = 0.8f),
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
                    onClick = { onNavigateToAi("GROWTH_ANALYST") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI 發育分析師")
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
                        Text("新增紀錄")
                    }
                }
            }
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
                    Column(Modifier.fillMaxSize()) {
                        state.records.maxByOrNull { it.record.date }?.let { latest ->
                            Spacer(Modifier.height(12.dp))
                            LatestGrowthHero(latest, selectedChildColor)
                        } ?: Box(Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            if (showChart) {
                                GrowthChartScreen(records = state.records)
                            } else {
                                GrowthListScreen(
                                    records = state.records,
                                    onEdit = { viewModel.editRecord(it) },
                                    onDelete = { viewModel.deleteRecord(it.record) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm && canEditData) {
        NewGrowthRecordDialog(
            initialRecord = editingRecord,
            onDismiss = viewModel::closeForm,
            onConfirm = { h, w, hc, date, note -> viewModel.saveRecord(h, w, hc, date, note) }
        )
    }
}

@Composable
private fun LatestGrowthHero(item: GrowthRecordWithPercentile, accentColor: Color) {
    val r = item.record
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
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
                HeroStat(label = "頭圍", value = it.toString(), unit = "cm", pct = -1, color = accentColor)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            color = accentColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "最後更新於 ${r.date}",
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
        }
    }
}
