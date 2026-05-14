package com.babymakisuk.featurelibrary.shelf.memo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.featurelibrary.LibraryViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val BoyBlue = androidx.compose.ui.graphics.Color(0xFF4A90D9)
private val GirlPink = androidx.compose.ui.graphics.Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class)
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

    val primaryColor = MaterialTheme.colorScheme.primary
    val childColor = remember(uiState.childId, primaryColor) {
        if (uiState.childId == 1L) BoyBlue
        else if (uiState.childId == 2L) GirlPink
        else primaryColor
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (memoId == -1L) "新增 Memo" else "編輯 Memo",
                        fontWeight = FontWeight.Bold
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (children.isNotEmpty() && uiState.childId <= 0L) {
                Text("選擇孩子", style = MaterialTheme.typography.labelLarge, color = childColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach { child ->
                        val acc = if (child.gender == com.babymakisuk.coremodel.Gender.MALE) BoyBlue else GirlPink
                        val sel = child.id == uiState.childId
                        androidx.compose.material3.FilterChip(
                            selected = sel,
                            onClick = { viewModel.setChildId(child.id) },
                            label = { Text(child.name) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = acc.copy(alpha = 0.2f),
                                selectedLabelColor = acc
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("標題") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.updateContent(it) },
                label = { Text("內容") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10
            )

            OutlinedTextField(
                value = uiState.dateStr,
                onValueChange = {},
                readOnly = true,
                label = { Text("日期") },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        val dateStr = uiState.dateStr
                        if (dateStr.isNotBlank()) {
                            try {
                                val parts = dateStr.split("-")
                                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                            } catch (_: Exception) {}
                        }
                        DatePickerDialog(
                            ctx,
                            { _, y, m, d ->
                                viewModel.updateDate(LocalDate.of(y, m + 1, d))
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.DateRange, "選擇日期")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (uiState.reminderAt != null) childColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                OutlinedTextField(
                    value = uiState.reminderStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("設定提醒（可選填）") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.reminderAt != null) {
                            IconButton(onClick = { viewModel.clearReminder() }) {
                                Icon(Icons.Default.Clear, "清除提醒")
                            }
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        if (uiState.reminderAt != null) {
                            cal.timeInMillis = uiState.reminderAt!!
                        }
                        DatePickerDialog(
                            ctx,
                            { _, y, m, d ->
                                val now = Calendar.getInstance()
                                now.set(y, m, d)
                                val newMillis = now.timeInMillis
                                TimePickerDialog(
                                    ctx,
                                    { _, h, min ->
                                        val finalCal = Calendar.getInstance()
                                        finalCal.set(y, m, d, h, min, 0)
                                        viewModel.updateReminder(finalCal.timeInMillis)
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
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (uiState.reminderAt == null) "設定提醒時間" else "修改提醒時間"
                    )
                }

                if (uiState.reminderAt != null) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.clearReminder() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清除提醒")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.save()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.title.isNotBlank()
            ) {
                Text("儲存")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
