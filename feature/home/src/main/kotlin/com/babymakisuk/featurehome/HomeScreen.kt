package com.babymakisuk.featurehome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.GrowthRecord
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel? = if (LocalInspectionMode.current) null else hiltViewModel(),
    onNavigateToGrowth: () -> Unit = {},
    onNavigateToMedical: () -> Unit = {}
) {
    val uiState by if (viewModel == null) {
        remember { mutableStateOf(HomeUiState()) }
    } else {
        viewModel.uiState.collectAsStateWithLifecycle()
    }

    HomeScreenContent(
        uiState = uiState,
        onUpdateChild = { viewModel?.updateChild(it) },
        onNavigateToGrowth = onNavigateToGrowth,
        onNavigateToMedical = onNavigateToMedical
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onUpdateChild: (ChildProfile) -> Unit,
    onNavigateToGrowth: () -> Unit,
    onNavigateToMedical: () -> Unit
) {
    var expandedGender by remember { mutableStateOf<Gender?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    BackHandler(enabled = expandedGender != null) {
        expandedGender = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Girl Card (Top)
            val girlWeight by animateFloatAsState(
                targetValue = when (expandedGender) {
                    Gender.FEMALE -> 1f
                    Gender.MALE -> 0f
                    else -> 0.5f
                }, label = "girlWeight"
            )

            if (girlWeight > 0.01f) {
                Box(modifier = Modifier.weight(girlWeight).fillMaxWidth()) {
                    ChildHubCard(
                        child = uiState.girl ?: ChildProfile(name = "妹妹", gender = Gender.FEMALE, birthday = LocalDate.now()),
                        latestGrowth = uiState.girlLatestGrowth,
                        isExpanded = expandedGender == Gender.FEMALE,
                        accentColor = GirlPink,
                        onExpand = { expandedGender = Gender.FEMALE },
                        onCollapse = { expandedGender = null },
                        onEditClick = { isEditMode = true },
                        onNavigateToGrowth = onNavigateToGrowth,
                        onNavigateToMedical = onNavigateToMedical
                    )
                }
            }

            // Boy Card (Bottom)
            val boyWeight by animateFloatAsState(
                targetValue = when (expandedGender) {
                    Gender.MALE -> 1f
                    Gender.FEMALE -> 0f
                    else -> 0.5f
                }, label = "boyWeight"
            )

            if (boyWeight > 0.01f) {
                Box(modifier = Modifier.weight(boyWeight).fillMaxWidth()) {
                    ChildHubCard(
                        child = uiState.boy ?: ChildProfile(name = "弟弟", gender = Gender.MALE, birthday = LocalDate.now()),
                        latestGrowth = uiState.boyLatestGrowth,
                        isExpanded = expandedGender == Gender.MALE,
                        accentColor = BoyBlue,
                        onExpand = { expandedGender = Gender.MALE },
                        onCollapse = { expandedGender = null },
                        onEditClick = { isEditMode = true },
                        onNavigateToGrowth = onNavigateToGrowth,
                        onNavigateToMedical = onNavigateToMedical
                    )
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

@Composable
fun ChildHubCard(
    child: ChildProfile,
    latestGrowth: GrowthRecord?,
    isExpanded: Boolean,
    accentColor: Color,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onEditClick: () -> Unit,
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
            // --- Summary View (Dashboard Mode) ---
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Photo Watermark (Subtle)
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
                    // Left: Large Icon with subtle ring
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

                    // Right: Stats
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
        } else {
            // --- Hub View (Full Profile Hub) ---
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Top Visual (More sophisticated header)
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
                    // Back and Edit Buttons
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

                // Details Content (Interactive Dashboard)
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Growth Section (Interactive)
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

                    // Medical Section (Interactive)
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
