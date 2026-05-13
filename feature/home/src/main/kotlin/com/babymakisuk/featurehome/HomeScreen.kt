package com.babymakisuk.featurehome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.GrowthRecord
import com.babymakisuk.coremodel.ToiletRecord
import com.babymakisuk.coremodel.VaccineReminder
import com.babymakisuk.ui.components.BabyTopBar
import com.babymakisuk.ui.components.LocalDrawerState
import com.babymakisuk.ui.theme.BabyMakiSukTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel? = if (LocalInspectionMode.current) null else hiltViewModel(),
    onNavigateToGrowth: () -> Unit = {},
    onNavigateToMedical: () -> Unit = {},
    onNavigateToAi: (String?) -> Unit = {}
) {
    val uiState by if (viewModel == null) {
        remember { mutableStateOf(HomeUiState()) }
    } else {
        viewModel.uiState.collectAsStateWithLifecycle()
    }

    HomeScreenContent(
        uiState = uiState,
        onUpdateChild = { viewModel?.updateChild(it) },
        onLogToilet = { viewModel?.logToilet(it) ?: Unit },
        onNavigateToGrowth = onNavigateToGrowth,
        onNavigateToMedical = onNavigateToMedical,
        onNavigateToAi = onNavigateToAi
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onUpdateChild: (ChildProfile) -> Unit,
    onLogToilet: (Long) -> Unit = {},
    onNavigateToGrowth: () -> Unit,
    onNavigateToMedical: () -> Unit,
    onNavigateToAi: (String?) -> Unit = {}
) {
    var expandedGender by remember { mutableStateOf<Gender?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    val drawerState = LocalDrawerState.current
    val drawerScope = rememberCoroutineScope()

    BackHandler(enabled = expandedGender != null) {
        expandedGender = null
    }

    Scaffold(
        topBar = {
            BabyTopBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ChildCare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Baby Maki Suk",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                showSearch = true,
                showAi = true,
                showAdd = false,
                onMenuClick = { drawerScope.launch { drawerState.open() } },
                onAiClick = { onNavigateToAi("QUICK_CHAT") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (uiState.boy == null && uiState.girl == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ChildCare,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "尚未建立孩子資料",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "開始記錄寶寶的每一個成長時刻",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { /* TODO: 新增孩子流程 */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("新增孩子")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Girl Card (Top)
                    val girlWeight by animateFloatAsState(
                        targetValue = when (expandedGender) {
                            Gender.FEMALE -> 0.85f
                            Gender.MALE -> 0.15f
                            else -> 0.5f
                        }, label = "girlWeight"
                    )

                    if (girlWeight > 0.01f) {
                        Box(modifier = Modifier.weight(girlWeight).fillMaxWidth()) {
                            ChildHubCard(
                                child = uiState.girl ?: ChildProfile(name = "妹妹", gender = Gender.FEMALE, birthday = LocalDate.now()),
                                latestGrowth = uiState.girlLatestGrowth,
                                toiletRecords = uiState.girlToiletRecords,
                                nextVaccine = uiState.girlNextVaccine,
                                isExpanded = expandedGender == Gender.FEMALE,
                                accentColor = GirlPink,
                                onExpand = { expandedGender = Gender.FEMALE },
                                onCollapse = { expandedGender = null },
                                onEditClick = { isEditMode = true },
                                onLogToilet = onLogToilet,
                                onNavigateToGrowth = onNavigateToGrowth,
                                onNavigateToMedical = onNavigateToMedical
                            )
                        }
                    }

                    // Boy Card (Bottom)
                    val boyWeight by animateFloatAsState(
                        targetValue = when (expandedGender) {
                            Gender.MALE -> 0.85f
                            Gender.FEMALE -> 0.15f
                            else -> 0.5f
                        }, label = "boyWeight"
                    )

                    if (boyWeight > 0.01f) {
                        Box(modifier = Modifier.weight(boyWeight).fillMaxWidth()) {
                            ChildHubCard(
                                child = uiState.boy ?: ChildProfile(name = "弟弟", gender = Gender.MALE, birthday = LocalDate.now()),
                                latestGrowth = uiState.boyLatestGrowth,
                                toiletRecords = uiState.boyToiletRecords,
                                nextVaccine = uiState.boyNextVaccine,
                                isExpanded = expandedGender == Gender.MALE,
                                accentColor = BoyBlue,
                                onExpand = { expandedGender = Gender.MALE },
                                onCollapse = { expandedGender = null },
                                onEditClick = { isEditMode = true },
                                onLogToilet = onLogToilet,
                                onNavigateToGrowth = onNavigateToGrowth,
                                onNavigateToMedical = onNavigateToMedical
                            )
                        }
                    }
                }
            }

            // Edit Dialog
            if (isEditMode && expandedGender != null) {
                val childToEdit = if (expandedGender == Gender.FEMALE) uiState.girl else uiState.boy
                childToEdit?.let {
                    EditChildProfileDialog(
                        child = it,
                        accentColor = if (it.gender == Gender.FEMALE) GirlPink else BoyBlue,
                        onDismiss = { isEditMode = false },
                        onSave = { updated ->
                            onUpdateChild(updated)
                            isEditMode = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChildHubCard(
    child: ChildProfile,
    latestGrowth: GrowthRecord?,
    toiletRecords: List<ToiletRecord> = emptyList(),
    nextVaccine: VaccineReminder? = null,
    isExpanded: Boolean,
    accentColor: Color,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onEditClick: () -> Unit,
    onLogToilet: (Long) -> Unit = {},
    onNavigateToGrowth: () -> Unit,
    onNavigateToMedical: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(enabled = !isExpanded) { onExpand() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        if (!isExpanded) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxHeight < 160.dp) {
                    // 緊湊版小橫條
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                                contentDescription = null,
                                modifier = Modifier.padding(6.dp),
                                tint = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = child.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 0.dp, end = 0.dp)
                                .size(160.dp)
                                .clip(RoundedCornerShape(topStart = 80.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent),
                                        center = Offset(160f, 160f)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp).offset(x = 20.dp, y = 20.dp),
                                tint = accentColor.copy(alpha = 0.08f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = accentColor.copy(alpha = 0.1f),
                                    modifier = Modifier.size(86.dp)
                                ) {}
                                Surface(
                                    shape = CircleShape,
                                    color = accentColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                                        contentDescription = null,
                                        modifier = Modifier.padding(14.dp),
                                        tint = accentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = child.name,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                    color = accentColor
                                )
                                val age = Period.between(child.birthday, LocalDate.now())
                                Text(
                                    text = "${age.years}歲 ${age.months}個月",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    InfoBadge(icon = Icons.Default.Height, text = "${latestGrowth?.heightCm ?: "--"} cm", color = accentColor)
                                    InfoBadge(icon = Icons.Default.MonitorWeight, text = "${latestGrowth?.weightKg ?: "--"} kg", color = accentColor)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.8f))
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onCollapse) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White)
                        }
                    }
                    
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(90.dp),
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                                contentDescription = null,
                                modifier = Modifier.padding(18.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = child.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = child.birthday.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HubSection(
                        title = "最新成長數值",
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        accentColor = accentColor,
                        onClick = onNavigateToGrowth
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBox(label = "身高", value = "${latestGrowth?.heightCm ?: "--"}", unit = "cm", color = accentColor)
                            VerticalDivider(modifier = Modifier.height(40.dp).align(Alignment.CenterVertically), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            StatBox(label = "體重", value = "${latestGrowth?.weightKg ?: "--"}", unit = "kg", color = accentColor)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "點擊查看成長趨勢與百分位圖表",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    HubSection(
                        title = "就醫・疫苗",
                        icon = Icons.Default.MedicalServices,
                        accentColor = accentColor,
                        onClick = onNavigateToMedical
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("查看完整醫療病歷與預防接種紀錄", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (nextVaccine != null) {
                            Spacer(Modifier.height(8.dp))
                            val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                            Surface(
                                color = accentColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "💉 下次：${nextVaccine.name}（${dateFormat.format(java.util.Date(nextVaccine.scheduledDate))}）",
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }
                        }
                    }

                    HubSection(
                        title = "大號紀錄",
                        icon = Icons.Default.WaterDrop,
                        accentColor = accentColor,
                        onClick = {}
                    ) {
                        if (toiletRecords.isEmpty()) {
                            Text(
                                "尚無紀錄",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            toiletRecords.forEach { record ->
                                val elapsed = formatElapsed(record.timestamp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💩", fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(elapsed, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onLogToilet(child.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("記錄大號")
                        }

                        if (toiletRecords.size >= 2) {
                            val avgInterval = calculateAverageInterval(toiletRecords)
                            val sinceLastMinutes = (System.currentTimeMillis() - toiletRecords.first().timestamp) / 60000
                            val alertRatio = sinceLastMinutes.toFloat() / avgInterval
                            if (avgInterval > 0 && alertRatio >= 0.8f) {
                                Spacer(Modifier.height(8.dp))
                                val alertColor = if (alertRatio >= 1f) Color.Red else Color(0xFFFFA500)
                                Surface(
                                    color = alertColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (alertRatio >= 1f) "⚠️ 超過平均間隔，可帶去廁所！"
                                        else "🟡 接近平均間隔時間",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = alertColor
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
fun InfoBadge(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HubSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = accentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = color)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChildProfileDialog(
    child: ChildProfile,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (ChildProfile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "編輯 ${child.name} 的資料",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                var name by remember { mutableStateOf(child.name) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                var birthday by remember { mutableStateOf(child.birthday.toString()) }
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("生日 (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(child.copy(name = name, birthday = LocalDate.parse(birthday)))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("儲存")
                    }
                }
            }
        }
    }
}

fun formatElapsed(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    return when {
        hours >= 1 -> "${hours}小時${minutes % 60}分鐘前"
        else -> "${minutes}分鐘前"
    }
}

fun calculateAverageInterval(records: List<ToiletRecord>): Long {
    if (records.size < 2) return 0L
    val sorted = records.sortedByDescending { it.timestamp }
    val gaps = sorted.zipWithNext { a, b -> a.timestamp - b.timestamp }
    return gaps.average().toLong() / 60000
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val sampleBoy = ChildProfile(
        id = 1L,
        name = "小明",
        gender = Gender.MALE,
        birthday = LocalDate.now().minusMonths(6)
    )
    val sampleGirl = ChildProfile(
        id = 2L,
        name = "小美",
        gender = Gender.FEMALE,
        birthday = LocalDate.now().minusMonths(8)
    )

    val sampleUiState = HomeUiState(
        boy = sampleBoy,
        girl = sampleGirl,
        boyLatestGrowth = GrowthRecord(
            childId = 1L,
            heightCm = 68.0f,
            weightKg = 7.5f,
            date = LocalDate.now()
        ),
        girlLatestGrowth = GrowthRecord(
            childId = 2L,
            heightCm = 70.0f,
            weightKg = 8.2f,
            date = LocalDate.now()
        )
    )

    BabyMakiSukTheme {
        HomeScreenContent(
            uiState = sampleUiState,
            onUpdateChild = {},
            onNavigateToGrowth = {},
            onNavigateToMedical = {},
            onNavigateToAi = {}
        )
    }
}
