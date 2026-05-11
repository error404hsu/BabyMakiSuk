package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    viewModel: GrowthViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
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
    var showChart by remember { mutableStateOf(false) }

    // 取得當前選中的寶寶顏色
    val selectedChildColor = (uiState as? GrowthUiState.Success)?.let { state ->
        val gender = state.children.find { it.id == state.selectedChildId }?.gender
        if (gender == Gender.MALE) BoyBlue else GirlPink
    } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = selectedChildColor.copy(alpha = 0.05f), // 輕微的底色延伸
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("成長紀錄", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { showChart = !showChart }) {
                        Icon(
                            imageVector = if (showChart) Icons.AutoMirrored.Filled.List else Icons.Filled.ShowChart,
                            contentDescription = if (showChart) "列表" else "圖表"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
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
                        // 1. 寶寶選擇器
                        ChildAvatarSelector(
                            children = state.children,
                            selectedId = state.selectedChildId,
                            onSelect = viewModel::selectChild,
                            accentColor = selectedChildColor
                        )

                        // 2. 最新數據 Hero Section
                        state.records.maxByOrNull { it.record.date }?.let { latest ->
                            LatestGrowthHero(latest, selectedChildColor)
                        } ?: Box(Modifier.height(100.dp))

                        // 3. 滿版內容容器
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

    if (showForm) {
        NewGrowthRecordDialog(
            initialRecord = editingRecord,
            onDismiss = viewModel::closeForm,
            onConfirm = { h, w, hc, date, note -> viewModel.saveRecord(h, w, hc, date, note) }
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
