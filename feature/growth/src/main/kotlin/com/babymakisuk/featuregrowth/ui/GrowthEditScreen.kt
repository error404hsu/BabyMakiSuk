package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthEditScreen(
    navController: NavController,
    recordId: Long = -1L,
    childId: Long = -1L,
    viewModel: GrowthEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(recordId, childId) {
        viewModel.initialize(recordId, childId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GrowthEditUiEvent.Saved -> navController.popBackStack()
                is GrowthEditUiEvent.ValidationError -> {
                    // future: showSnackbar with event.message
                }
            }
        }
    }

    when (uiState) {
        is GrowthEditUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is GrowthEditUiState.Ready -> {
            EditFormContent(
                state = uiState as GrowthEditUiState.Ready,
                isSaving = false,
                recordId = recordId,
                onBack = { navController.popBackStack() },
                onUpdateHeight = viewModel::updateHeight,
                onUpdateWeight = viewModel::updateWeight,
                onUpdateHeadCircumference = viewModel::updateHeadCircumference,
                onUpdateNote = viewModel::updateNote,
                onUpdateAiSuggestion = viewModel::updateAiSuggestion,
                onUpdateDate = viewModel::updateDate,
                onSave = viewModel::save
            )
        }
        is GrowthEditUiState.Saving -> {
            EditFormContent(
                state = (uiState as GrowthEditUiState.Saving).form,
                isSaving = true,
                recordId = recordId,
                onBack = { navController.popBackStack() },
                onUpdateHeight = viewModel::updateHeight,
                onUpdateWeight = viewModel::updateWeight,
                onUpdateHeadCircumference = viewModel::updateHeadCircumference,
                onUpdateNote = viewModel::updateNote,
                onUpdateAiSuggestion = viewModel::updateAiSuggestion,
                onUpdateDate = viewModel::updateDate,
                onSave = viewModel::save
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFormContent(
    state: GrowthEditUiState.Ready,
    isSaving: Boolean,
    recordId: Long,
    onBack: () -> Unit,
    onUpdateHeight: (String) -> Unit,
    onUpdateWeight: (String) -> Unit,
    onUpdateHeadCircumference: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
    onUpdateAiSuggestion: (String) -> Unit,
    onUpdateDate: (LocalDate) -> Unit,
    onSave: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onUpdateDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("確認") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recordId > 0L) "編輯成長紀錄" else "新增成長紀錄") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !isSaving) {
                        Text(if (isSaving) "儲存中..." else "儲存")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.dateStr,
                    onValueChange = {},
                    label = { Text("記錄日期") },
                    readOnly = true,
                    enabled = !isSaving,
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "選擇日期")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = !isSaving) { showDatePicker = true }
                )
            }

            OutlinedTextField(
                value = state.heightCm,
                onValueChange = onUpdateHeight,
                label = { Text("身高 (cm)") },
                isError = state.heightError,
                enabled = !isSaving,
                supportingText = if (state.heightError) {{ Text("請輸入有效的身高") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.weightKg,
                onValueChange = onUpdateWeight,
                label = { Text("體重 (kg)") },
                isError = state.weightError,
                enabled = !isSaving,
                supportingText = if (state.weightError) {{ Text("請輸入有效的體重") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.headCircumferenceCm,
                onValueChange = onUpdateHeadCircumference,
                label = { Text("頭圍 (cm，可選)") },
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.note,
                onValueChange = onUpdateNote,
                label = { Text("備註") },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.aiSuggestion,
                onValueChange = onUpdateAiSuggestion,
                label = { Text("AI 建議") },
                enabled = !isSaving,
                placeholder = { Text("由 AI 分析後自動填入，亦可手動編輯") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("儲存")
            }
        }
    }
}
