package com.babymakisuk.featurehome

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Boy
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Girl
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.GrowthRecord
import com.babymakisuk.coremodel.MedicalVisit
import com.babymakisuk.coremodel.Memo
import com.babymakisuk.coremodel.ToiletRecord
import com.babymakisuk.coremodel.VaccineReminder
import com.babymakisuk.ui.components.BabyTopBar
import com.babymakisuk.ui.components.LocalDrawerState
import com.babymakisuk.ui.theme.BabyMakiSukTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    onNavigateToAi: (String?) -> Unit = {},
    onNavigateToMemoEdit: (Long) -> Unit = {}
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
        onNavigateToAi = onNavigateToAi,
        onNavigateToMemoEdit = onNavigateToMemoEdit
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
    onNavigateToAi: (String?) -> Unit = {},
    onNavigateToMemoEdit: (Long) -> Unit = {}
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
                    val aiGradient = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    )
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clickable { onNavigateToAi("QUICK_CHAT") },
                        shape = RoundedCornerShape(21.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .background(aiGradient)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "問點什麼？ AI 小幫手為您解答",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                showSearch = false,
                showAi = false,
                showAdd = false,
                onMenuClick = { drawerScope.launch { drawerState.open() } },
                extraActions = {
                    IconButton(onClick = { /* 帳號設定導向 */ }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "帳號",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val girlWeight by animateFloatAsState(
                        targetValue = when (expandedGender) {
                            Gender.FEMALE -> 0.85f
                            Gender.MALE -> 0.15f
                            else -> 0.5f
                        }, label = "girlWeight"
                    )

                    if (girlWeight > 0.01f) {
                        Box(modifier = Modifier.weight(girlWeight).fillMaxWidth()) {
                            ChildSummaryCard(
                                child = uiState.girl ?: ChildProfile(name = "姊姊", gender = Gender.FEMALE, birthday = LocalDate.now()),
                                latestGrowth = uiState.girlLatestGrowth,
                                latestMedical = uiState.girlLatestMedical,
                                toiletRecords = uiState.girlToiletRecords,
                                nextVaccine = uiState.girlNextVaccine,
                                todayMemos = uiState.todayMemos[uiState.girl?.id] ?: emptyList(),
                                isExpanded = expandedGender == Gender.FEMALE,
                                accentColor = GirlPink,
                                onExpand = { expandedGender = Gender.FEMALE },
                                onCollapse = { expandedGender = null },
                                onEditClick = { isEditMode = true },
                                onLogToilet = onLogToilet,
                                onNavigateToGrowth = onNavigateToGrowth,
                                onNavigateToMedical = onNavigateToMedical,
                                onNavigateToMemoEdit = onNavigateToMemoEdit
                            )
                        }
                    }

                    val boyWeight by animateFloatAsState(
                        targetValue = when (expandedGender) {
                            Gender.MALE -> 0.85f
                            Gender.FEMALE -> 0.15f
                            else -> 0.5f
                        }, label = "boyWeight"
                    )

                    if (boyWeight > 0.01f) {
                        Box(modifier = Modifier.weight(boyWeight).fillMaxWidth()) {
                            ChildSummaryCard(
                                child = uiState.boy ?: ChildProfile(name = "弟弟", gender = Gender.MALE, birthday = LocalDate.now()),
                                latestGrowth = uiState.boyLatestGrowth,
                                latestMedical = uiState.boyLatestMedical,
                                toiletRecords = uiState.boyToiletRecords,
                                nextVaccine = uiState.boyNextVaccine,
                                todayMemos = uiState.todayMemos[uiState.boy?.id] ?: emptyList(),
                                isExpanded = expandedGender == Gender.MALE,
                                accentColor = BoyBlue,
                                onExpand = { expandedGender = Gender.MALE },
                                onCollapse = { expandedGender = null },
                                onEditClick = { isEditMode = true },
                                onLogToilet = onLogToilet,
                                onNavigateToGrowth = onNavigateToGrowth,
                                onNavigateToMedical = onNavigateToMedical,
                                onNavigateToMemoEdit = onNavigateToMemoEdit
                            )
                        }
                    }
                }
            }

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
fun ChildSummaryCard(
    child: ChildProfile,
    latestGrowth: GrowthRecord?,
    latestMedical: MedicalVisit? = null,
    toiletRecords: List<ToiletRecord> = emptyList(),
    nextVaccine: VaccineReminder? = null,
    todayMemos: List<Memo> = emptyList(),
    isExpanded: Boolean,
    accentColor: Color,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onEditClick: () -> Unit,
    onLogToilet: (Long) -> Unit = {},
    onNavigateToGrowth: () -> Unit,
    onNavigateToMedical: () -> Unit,
    onNavigateToMemoEdit: (Long) -> Unit = {}
) {
    val childPhoto = rememberChildPhoto(child.photoUri)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(enabled = !isExpanded) { onExpand() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxHeight < 160.dp) {
                CompactBar(child, childPhoto, accentColor)
            } else if (!isExpanded) {
                CollapsedContent(child, childPhoto, latestGrowth, toiletRecords, accentColor)
            } else {
                ExpandedContent(
                    child = child,
                    childPhoto = childPhoto,
                    latestGrowth = latestGrowth,
                    latestMedical = latestMedical,
                    nextVaccine = nextVaccine,
                    todayMemos = todayMemos,
                    toiletRecords = toiletRecords,
                    accentColor = accentColor,
                    onCollapse = onCollapse,
                    onEditClick = onEditClick,
                    onLogToilet = onLogToilet,
                    onNavigateToGrowth = onNavigateToGrowth,
                    onNavigateToMedical = onNavigateToMedical,
                    onNavigateToMemoEdit = onNavigateToMemoEdit
                )
            }
        }
    }
}

@Composable
private fun CompactBar(child: ChildProfile, childPhoto: androidx.compose.ui.graphics.ImageBitmap?, accentColor: Color) {
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
            if (childPhoto != null) {
                Image(bitmap = childPhoto, contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).alpha(0.25f),
                    contentScale = ContentScale.Crop)
            }
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
}

@Composable
private fun CollapsedContent(
    child: ChildProfile,
    childPhoto: androidx.compose.ui.graphics.ImageBitmap?,
    latestGrowth: GrowthRecord?,
    toiletRecords: List<ToiletRecord>,
    accentColor: Color
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景層：照片或漸層
        if (childPhoto != null) {
            Image(
                bitmap = childPhoto,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.2f
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(accentColor.copy(alpha = 0.05f), accentColor.copy(alpha = 0.15f))
                        )
                    )
            )
            Icon(
                imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .alpha(0.05f),
                tint = accentColor
            )
        }

        // 內容層
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上部：姓名與月齡
            Column {
                Text(
                    text = child.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = accentColor
                )
                val age = Period.between(child.birthday, LocalDate.now())
                Text(
                    text = "${age.years}歲 ${age.months}個月",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 下部：數據摘要
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickStatPill(
                    icon = Icons.Default.MonitorWeight,
                    label = "${latestGrowth?.weightKg ?: "--"} kg",
                    isDark = false,
                    accentColor = accentColor
                )
                Spacer(Modifier.width(10.dp))
                QuickStatPill(
                    icon = Icons.Default.Height,
                    label = "${latestGrowth?.heightCm ?: "--"} cm",
                    isDark = false,
                    accentColor = accentColor
                )
                
                if (toiletRecords.isNotEmpty()) {
                    Spacer(Modifier.weight(1f)) // 將大號紀錄推至右下角
                    val lastPoop = toiletRecords.first().timestamp
                    val diffHours = (System.currentTimeMillis() - lastPoop) / 3600000
                    val isWarning = diffHours >= 24

                    QuickStatPill(
                        icon = Icons.Default.WaterDrop,
                        label = formatElapsed(lastPoop),
                        isDark = false,
                        accentColor = accentColor,
                        isEmoji = true,
                        warningColor = if (isWarning) Color(0xFFE57373) else null // 超過 24h 顯示淡紅色提示
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatPill(
    icon: ImageVector, 
    label: String, 
    isDark: Boolean, 
    accentColor: Color, 
    isEmoji: Boolean = false,
    warningColor: Color? = null
) {
    val bgColor = warningColor?.copy(alpha = if (isDark) 0.5f else 0.2f) 
        ?: if (isDark) Color.White.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.1f)
    
    val contentColor = warningColor ?: if (isDark) Color.White else accentColor

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEmoji) {
                Text("💩", fontSize = 14.sp)
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
private fun ExpandedContent(
    child: ChildProfile,
    childPhoto: androidx.compose.ui.graphics.ImageBitmap?,
    latestGrowth: GrowthRecord?,
    latestMedical: MedicalVisit?,
    nextVaccine: VaccineReminder?,
    todayMemos: List<Memo>,
    toiletRecords: List<ToiletRecord>,
    accentColor: Color,
    onCollapse: () -> Unit,
    onEditClick: () -> Unit,
    onLogToilet: (Long) -> Unit,
    onNavigateToGrowth: () -> Unit,
    onNavigateToMedical: () -> Unit,
    onNavigateToMemoEdit: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.8f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "收合", tint = Color.White)
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(54.dp),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    if (childPhoto != null) {
                        Image(
                            bitmap = childPhoto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    val age = Period.between(child.birthday, LocalDate.now())
                    Text(
                        text = "${age.years}歲 ${age.months}個月",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Outlined.Edit, "編輯", tint = Color.White)
                }
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
                title = "就醫紀錄",
                icon = Icons.Default.MedicalServices,
                accentColor = accentColor,
                onClick = onNavigateToMedical
            ) {
                // 上欄：最近一次 (讀取實際數據)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "最近一次",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    if (latestMedical != null) {
                        Text(
                            text = "${latestMedical.date.toString().replace("-", "/")} · ${latestMedical.hospital}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "尚無就醫紀錄",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // 下欄：預約排程
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "預約排程",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    if (nextVaccine != null) {
                        val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                        Text(
                            text = "${dateFormat.format(java.util.Date(nextVaccine.scheduledDate))} · ${nextVaccine.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "尚無排程記錄",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            HubSection(
                title = "本日 Memo",
                icon = Icons.Outlined.Edit,
                accentColor = accentColor,
                onClick = { onNavigateToMemoEdit(child.id) }
            ) {
                if (todayMemos.isEmpty()) {
                    Text(
                        "尚無本日紀錄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    todayMemos.take(3).forEach { memo ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = accentColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = memo.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (memo.content.length > 80) memo.content.take(80) + "..."
                                    else memo.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onNavigateToMemoEdit(child.id) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("前往編輯", color = accentColor)
                }
            }

            HubSection(
                title = "大號紀錄",
                icon = Icons.Default.WaterDrop,
                accentColor = accentColor,
                onClick = {}
            ) {
                if (toiletRecords.isEmpty()) {
                    Text("尚無紀錄", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val timeFormatter = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                    toiletRecords.forEach { record ->
                        val formattedTime = timeFormatter.format(java.util.Date(record.timestamp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💩", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(formattedTime, style = MaterialTheme.typography.bodyMedium)
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
    var selectedUri by remember { mutableStateOf(child.photoUri) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    val path = copyPhotoToInternal(context, child.id, it)
                    withContext(Dispatchers.Main) {
                        if (path != null) selectedUri = path
                    }
                }
            }
        }
    )
    val selectedPhoto = rememberChildPhoto(selectedUri)

    androidx.compose.material3.AlertDialog(
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

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(80.dp)
                    ) {}
                    if (selectedPhoto != null) {
                        Image(
                            bitmap = selectedPhoto,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).padding(16.dp),
                            tint = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("選擇照片")
                    }
                    if (selectedUri != null) {
                        TextButton(onClick = { selectedUri = null }) {
                            Text("清除照片")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                var aiPrompt by remember { mutableStateOf(child.defaultAiPrompt ?: "") }
                OutlinedTextField(
                    value = aiPrompt,
                    onValueChange = { aiPrompt = it },
                    label = { Text("AI 助手提示 (孩子特質/過敏等)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4
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
                            onSave(child.copy(
                                name = name, 
                                birthday = LocalDate.parse(birthday), 
                                photoUri = selectedUri,
                                defaultAiPrompt = aiPrompt
                            ))
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

@Composable
private fun rememberChildPhoto(photoUri: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(photoUri) {
        bitmap = if (photoUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val file = java.io.File(photoUri)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                } catch (_: Exception) { null }
            }
        } else null
    }
    return bitmap?.asImageBitmap()
}

private fun copyPhotoToInternal(context: Context, childId: Long, sourceUri: Uri): String? {
    val dir = java.io.File(context.filesDir, "child_photos").apply { mkdirs() }
    val file = java.io.File(dir, "$childId.jpg")
    return try {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            java.io.FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (_: Exception) { null }
}
