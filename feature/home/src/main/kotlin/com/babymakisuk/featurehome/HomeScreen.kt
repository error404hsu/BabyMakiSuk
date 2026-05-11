package com.babymakisuk.featurehome

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    viewModel: HomeViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val uiState by if (viewModel == null) {
        remember { mutableStateOf(HomeUiState()) }
    } else {
        viewModel.uiState.collectAsStateWithLifecycle()
    }

    HomeScreenContent(
        uiState = uiState,
        onUpdateChild = { viewModel?.updateChild(it) }
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onUpdateChild: (ChildProfile) -> Unit
) {
    var expandedGender by remember { mutableStateOf<Gender?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

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
                        onEditClick = { isEditMode = true }
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
                        onEditClick = { isEditMode = true }
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
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isExpanded) 0.dp else 12.dp)
            .clickable(enabled = !isExpanded) { onExpand() },
        shape = if (isExpanded) RoundedCornerShape(0.dp) else RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = if (isExpanded) CardDefaults.cardElevation(0.dp) else CardDefaults.cardElevation(6.dp)
    ) {
        if (!isExpanded) {
            // --- Summary View (Dashboard Mode) ---
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.05f), Color.White))
            )) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Large Icon/Photo
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp),
                            tint = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Right: Stats
                    Column(modifier = Modifier.weight(1f)) {
                        Text(child.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = accentColor)
                        val age = Period.between(child.birthday, LocalDate.now())
                        Text("${age.years}歲 ${age.months}個月", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
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
                // Top Visual
                Box(modifier = Modifier.fillMaxWidth().height(260.dp).background(accentColor)) {
                    // Back and Edit Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onCollapse) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White)
                        }
                    }
                    
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(100.dp)) {
                            Icon(
                                imageVector = if (child.gender == Gender.FEMALE) Icons.Default.Girl else Icons.Default.Boy,
                                contentDescription = null,
                                modifier = Modifier.padding(20.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(child.name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(child.birthday.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")), color = Color.White.copy(alpha = 0.8f))
                    }
                }

                // Details Content
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    HubSection(title = "最新成長數值", icon = Icons.Default.ShowChart, accentColor = accentColor) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBox(label = "身高", value = "${latestGrowth?.heightCm ?: "--"}", unit = "cm", color = accentColor)
                            StatBox(label = "體重", value = "${latestGrowth?.weightKg ?: "--"}", unit = "kg", color = accentColor)
                        }
                    }

                    HubSection(title = "醫院・疫苗", icon = Icons.Default.MedicalServices, accentColor = accentColor) {
                        Text("點擊查看完整就醫與疫苗記錄", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
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
fun HubSection(title: String, icon: ImageVector, accentColor: Color, content: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = accentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8F9FA),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = color)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
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
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    Text("編輯 ${child.name} 的資料", style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.height(32.dp))

                var name by remember { mutableStateOf(child.name) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                var birthday by remember { mutableStateOf(child.birthday.toString()) }
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("生日 (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        onSave(child.copy(name = name, birthday = LocalDate.parse(birthday)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("確認儲存")
                }
            }
        }
    }
}
