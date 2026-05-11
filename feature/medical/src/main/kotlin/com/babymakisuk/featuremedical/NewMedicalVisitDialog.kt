package com.babymakisuk.featuremedical

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.babymakisuk.coremodel.MedicalVisit
import java.time.LocalDate

@Composable
fun NewMedicalVisitDialog(
    childId: Long,
    onDismiss: () -> Unit,
    onConfirm: (MedicalVisit) -> Unit
) {
    var hospital      by remember { mutableStateOf("") }
    var department    by remember { mutableStateOf("") }
    var diagnosis     by remember { mutableStateOf("") }
    var notes         by remember { mutableStateOf("") }
    var hospitalError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增就診紀錄") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it; hospitalError = false },
                    label = { Text("醫院名稱 *") },
                    isError = hospitalError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("科別") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("診斷") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (hospital.isBlank()) { hospitalError = true; return@TextButton }
                onConfirm(
                    MedicalVisit(
                        childId = childId,
                        date = LocalDate.now(),
                        hospital = hospital.trim(),
                        department = department.trim(),
                        diagnosis = diagnosis.trim(),
                        notes = notes.trim()
                    )
                )
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
