package com.babymakisuk.featurelibrary.shelf.memo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coremodel.Gender
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoEditScreen(
    navController: NavController,
    memoId: Long = -1L,
    childId: Long = -1L,
    viewModel: MemoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val children by viewModel.children.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(memoId, childId) {
        viewModel.initialize(memoId, childId)
    }

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (memoId == -1L) "新增手動記事" else "編輯手動記事",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Child Selection (Multi-select for new, single for edit)
            Column {
                Text(
                    "記事對象",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    children.forEach { child ->
                        val isSelected = child.id in uiState.selectedChildIds
                        val color = if (child.gender == Gender.MALE) BoyBlue else GirlPink
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                if (memoId == -1L) {
                                    viewModel.toggleChildSelection(child.id)
                                } else {
                                    // 編輯模式：強制單選，不可取消，只能切換
                                    if (!isSelected) {
                                        viewModel.setSingleChildSelection(child.id)
                                    }
                                }
                            },
                            label = { Text(child.name) },
                            leadingIcon = {
                                Text(if (child.gender == Gender.MALE) "👦" else "👧")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color,
                                selectedLeadingIconColor = color
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("標題") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.updateContent(it) },
                label = { Text("詳細內容") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 5
            )

            // Date Picker
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.dateStr,
                    onValueChange = {},
                    label = { Text("記事日期") },
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
                        .clickable {
                            val cal = Calendar.getInstance()
                            // Try to parse current dateStr
                            try {
                                val parts = uiState.dateStr.split(" / ")
                                if (parts.size == 3) {
                                    cal.set(parts[0].trim().toInt(), parts[1].trim().toInt() - 1, parts[2].trim().toInt())
                                }
                            } catch (_: Exception) {}

                            DatePickerDialog(
                                ctx,
                                { _, y, m, d ->
                                    viewModel.updateDate(LocalDate.of(y, m + 1, d))
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )
            }

            // Reminder Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (uiState.reminderAt != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "提醒設定",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.weight(1f))
                    if (uiState.reminderAt != null) {
                        TextButton(onClick = { viewModel.clearReminder() }) {
                            Text("清除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                if (uiState.reminderAt == null) {
                    AdaptiveButton(
                        onClick = { showDateTimePicker(ctx, uiState.reminderAt) { viewModel.updateReminder(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.OUTLINED
                    ) {
                        Text("新增提醒時間")
                    }
                } else {
                    OutlinedCard(
                        onClick = { showDateTimePicker(ctx, uiState.reminderAt) { viewModel.updateReminder(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                uiState.reminderStr,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            AdaptiveButton(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.title.isNotBlank() && uiState.selectedChildIds.isNotEmpty()
            ) {
                Text("儲存記事", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AdaptiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    variant: ButtonVariant = ButtonVariant.FILLED,
    content: @Composable RowScope.() -> Unit
) {
    if (variant == ButtonVariant.FILLED) {
        androidx.compose.material3.Button(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, content = content)
    } else {
        androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, content = content)
    }
}

enum class ButtonVariant { FILLED, OUTLINED }

private fun showDateTimePicker(
    context: android.content.Context,
    initialMillis: Long?,
    onSelected: (Long) -> Unit
) {
    val cal = Calendar.getInstance()
    initialMillis?.let { cal.timeInMillis = it }
    
    DatePickerDialog(
        context,
        { _, y, m, d ->
            TimePickerDialog(
                context,
                { _, h, min ->
                    val result = Calendar.getInstance()
                    result.set(y, m, d, h, min, 0)
                    onSelected(result.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
