package com.babymakisuk.featurevaccine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import com.babymakisuk.coremodel.VaccineReminder
import com.babymakisuk.ui.components.BabyTopBar
import com.babymakisuk.ui.components.LocalDrawerState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineScreen(
    viewModel: VaccineViewModel = hiltViewModel(),
    onNavigateToAi: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val showForm by viewModel.showForm.collectAsState()
    val editingGroup by viewModel.editingGroup.collectAsState()

    val drawerState = LocalDrawerState.current
    val drawerScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            BabyTopBar(
                title = {
                    Text(
                        "健護提醒",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                showSearch = true,
                showAi = true,
                showAdd = true,
                onMenuClick = { drawerScope.launch { drawerState.open() } },
                onAiClick = { onNavigateToAi("PEDIATRIC_DOCTOR") },
                onAddClick = viewModel::openForm
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (uiState.groupedReminders.isEmpty() && !uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "尚無健護提醒",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "點擊上方 + 按鈕新增提醒",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.groupedReminders, key = { it.id }) { group ->
                        VaccineReminderCard(
                            group = group,
                            children = uiState.children,
                            onEdit = { viewModel.editGroup(group) },
                            onToggleCompleted = { viewModel.toggleGroupCompleted(group) },
                            onDelete = { viewModel.deleteGroup(group) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showForm) {
        VaccineReminderDialog(
            availableChildren = uiState.children,
            initialGroup = editingGroup,
            onDismiss = viewModel::closeForm,
            onConfirm = { name, date, note, isCompleted, childIds ->
                viewModel.saveGroupedReminder(name, date, note, isCompleted, childIds, editingGroup)
            }
        )
    }
}

@Composable
private fun VaccineReminderCard(
    group: GroupedVaccineReminder,
    children: List<ChildProfile>,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val daysUntil = ((group.scheduledDate - now) / 86400000).toInt()
    val dateFormat = SimpleDateFormat("yyyy / MM / dd", Locale.getDefault())

    val statusColor = when {
        group.isAllCompleted -> Color(0xFF4CAF50)
        daysUntil < 0 -> Color.Red
        daysUntil <= 7 -> Color(0xFFFFA500)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (group.isAllCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Child Indicators
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                children.filter { it.id in group.childReminders.keys }.forEach { child ->
                    val color = if (child.gender == Gender.MALE) BoyBlue else GirlPink
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f))
                            .border(1.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (child.gender == Gender.MALE) "👦" else "👧", fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (group.isAllCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = statusColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = dateFormat.format(Date(group.scheduledDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                    if (daysUntil in 0..7 && !group.isAllCompleted) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "剩 $daysUntil 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFA500),
                            modifier = Modifier
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (group.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = group.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onToggleCompleted, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (group.isAllCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "切換狀態",
                        tint = if (group.isAllCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "編輯",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "刪除",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun VaccineReminderDialog(
    availableChildren: List<ChildProfile>,
    initialGroup: GroupedVaccineReminder?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String, Boolean, List<Long>) -> Unit
) {
    var name by remember { mutableStateOf(initialGroup?.name ?: "") }
    var selectedDate by remember {
        mutableStateOf(
            if (initialGroup != null) {
                Instant.ofEpochMilli(initialGroup.scheduledDate)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
            } else LocalDate.now()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    var note by remember { mutableStateOf(initialGroup?.note ?: "") }
    var isCompleted by remember { mutableStateOf(initialGroup?.isAllCompleted ?: false) }
    var selectedChildIds by remember { 
        mutableStateOf(initialGroup?.childReminders?.keys?.toList() ?: availableChildren.map { it.id }) 
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("確認") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialGroup == null) "新增 疫苗/就醫 提醒" else "編輯 疫苗/就醫 提醒",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("提醒名稱 (如：B肝第二劑)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate.format(DateTimeFormatter.ofPattern("yyyy / MM / dd")),
                        onValueChange = {},
                        label = { Text("預計日期") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.CalendarToday, null)
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker = true }
                    )
                }

                Column {
                    Text("提醒對象", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableChildren.forEach { child ->
                            val isSelected = child.id in selectedChildIds
                            val color = if (child.gender == Gender.MALE) BoyBlue else GirlPink
                            
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedChildIds = if (isSelected) {
                                        selectedChildIds - child.id
                                    } else {
                                        selectedChildIds + child.id
                                    }
                                },
                                label = { Text(child.name) },
                                leadingIcon = {
                                    Text(if (child.gender == Gender.MALE) "👦" else "👧")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isCompleted = !isCompleted }
                ) {
                    Checkbox(checked = isCompleted, onCheckedChange = { isCompleted = it })
                    Text("已完成", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || selectedChildIds.isEmpty()) return@Button
                    
                    val date = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onConfirm(name, date, note, isCompleted, selectedChildIds)
                },
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && selectedChildIds.isNotEmpty()
            ) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
