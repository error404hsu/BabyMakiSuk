package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile

@Composable
fun NewGrowthRecordDialog(
    initialRecord: GrowthRecordWithPercentile? = null,
    onDismiss: () -> Unit,
    onConfirm: (heightCm: Float, weightKg: Float, headCircCm: Float?, date: LocalDate, note: String) -> Unit,
) {
    var heightText   by remember { mutableStateOf(initialRecord?.record?.heightCm?.toString() ?: "") }
    var weightText   by remember { mutableStateOf(initialRecord?.record?.weightKg?.toString() ?: "") }
    var headCircText by remember { mutableStateOf(initialRecord?.record?.headCircumferenceCm?.toString() ?: "") }
    var noteText     by remember { mutableStateOf(initialRecord?.record?.note ?: "") }
    var heightError  by remember { mutableStateOf(value = false) }
    var weightError  by remember { mutableStateOf(value = false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRecord == null) "新增成長紀錄" else "編輯成長紀錄") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = "身高(cm)",
                    value = heightText,
                    isError = heightError,
                ) { heightText = it; heightError = false }
                NumberField(
                    label = "體重 (kg)",
                    value = weightText,
                    isError = weightError,
                ) { weightText = it; weightError = false }
                NumberField(
                    label = "頭圍 (cm，可選)",
                    value = headCircText,
                ) { headCircText = it }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = heightText.toFloatOrNull()
                    val w = weightText.toFloatOrNull()
                    if (h == null || h <= 0) {
                        heightError = true
                        return@TextButton
                    }
                    if (w == null || w <= 0) {
                        weightError = true
                        return@TextButton
                    }
                    onConfirm(h, w, headCircText.toFloatOrNull(), initialRecord?.record?.date ?: LocalDate.now(), noteText)
                }
            ) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    isError: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
