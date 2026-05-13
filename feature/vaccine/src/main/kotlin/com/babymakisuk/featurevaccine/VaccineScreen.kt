package com.babymakisuk.featurevaccine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
    val editingReminder by viewModel.editingReminder.collectAsState()

    val drawerState = LocalDrawerState.current
    val drawerScope = rememberCoroutineScope()

    val selectedChildColor = uiState.children.find { it.id == uiState.selectedChildId }?.let {
        if (it.gender == Gender.MALE) BoyBlue else GirlPink
    } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = selectedChildColor.copy(alpha = 0.05f),
        topBar = {
            BabyTopBar(
                title = {
                    if (uiState.children.isNotEmpty()) {
                        VaccineChildSelectorRow(
                            children = uiState.children,
                            selectedChildId = uiState.selectedChildId,
                            onSelectChild = { viewModel.selectChild(it) }
                        )
                    }
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
            if (uiState.reminders.isEmpty() && !uiState.isLoading) {
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
                            "尚無疫苗提醒",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "點擊上方 + 按鈕新增疫苗提醒",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.reminders, key = { it.id }) { reminder ->
                        VaccineReminderCard(
                            reminder = reminder,
                            accentColor = selectedChildColor,
                            onEdit = { viewModel.editReminder(reminder) },
                            onToggleCompleted = { viewModel.toggleCompleted(reminder) },
                            onDelete = { viewModel.deleteReminder(reminder) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showForm) {
        VaccineReminderDialog(
            childId = uiState.selectedChildId,
            initialReminder = editingReminder,
            onDismiss = viewModel::closeForm,
            onConfirm = viewModel::saveReminder
        )
    }
}

@Composable
private fun VaccineChildSelectorRow(
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
private fun VaccineReminderCard(
    reminder: VaccineReminder,
    accentColor: Color,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val daysUntil = ((reminder.scheduledDate - now) / 86400000).toInt()
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    val statusColor = when {
        reminder.isCompleted -> Color(0xFF4CAF50)
        daysUntil < 0 -> Color.Red
        daysUntil <= 7 -> Color(0xFFFFA500)
        else -> Color.Gray
    }

    val statusEmoji = when {
        reminder.isCompleted -> "✅"
        daysUntil < 0 -> "⚠️"
        daysUntil <= 7 -> "⏰"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (statusEmoji.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(statusEmoji, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (reminder.isCompleted) "已完成" else dateFormat.format(Date(reminder.scheduledDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (reminder.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reminder.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = reminder.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onToggleCompleted, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (reminder.isCompleted) "取消完成" else "標記完成",
                    tint = if (reminder.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "編輯",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "刪除",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaccineReminderDialog(
    childId: Long,
    initialReminder: VaccineReminder?,
    onDismiss: () -> Unit,
    onConfirm: (VaccineReminder) -> Unit
) {
    var name by remember { mutableStateOf(initialReminder?.name ?: "") }
    var dateText by remember {
        mutableStateOf(
            if (initialReminder != null) {
                val date = LocalDate.ofInstant(Instant.ofEpochMilli(initialReminder.scheduledDate), ZoneId.systemDefault())
                date.toString()
            } else ""
        )
    }
    var note by remember { mutableStateOf(initialReminder?.note ?: "") }
    var isCompleted by remember { mutableStateOf(initialReminder?.isCompleted ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialReminder == null) "新增疫苗提醒" else "編輯疫苗提醒",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("疫苗名稱") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("預計日期 (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCompleted, onCheckedChange = { isCompleted = it })
                    Text("已完成")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val date = try {
                        LocalDate.parse(dateText).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    onConfirm(
                        VaccineReminder(
                            id = initialReminder?.id ?: 0,
                            childId = childId,
                            name = name,
                            scheduledDate = date,
                            isCompleted = isCompleted,
                            note = note
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
