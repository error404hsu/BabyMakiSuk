package com.babymakisuk.featuresettings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coredata.DarkModeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val darkMode by viewModel.darkMode.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<android.net.Uri?>(null) }

    // 檔案選取器：用戶選完 JSON 備份檔
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { showImportConfirm = it } }

    // 監聽匯出就緒：展間 ShareSheet
    LaunchedEffect(backupState) {
        if (backupState is BackupUiState.ExportReady) {
            val intent = (backupState as BackupUiState.ExportReady).intent
            context.startActivity(android.content.Intent.createChooser(intent, "備份檔儲存至…"))
            viewModel.clearBackupState()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("設定") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // ── 外觀
            item { SettingsSectionHeader("外觀") }
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = darkMode.label,
                    onClick = { showDarkModeDialog = true }
                )
            }

            // ── 資料管理
            item { SettingsSectionHeader("資料管理") }
            item {
                SettingsItem(
                    icon = Icons.Default.Upload,
                    title = "匯出備份",
                    subtitle = "將所有資料儲存為 JSON 檔案",
                    enabled = backupState !is BackupUiState.Loading,
                    onClick = { viewModel.triggerExport() }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "匯入備份",
                    subtitle = "從 JSON 備份檔還原資料",
                    enabled = backupState !is BackupUiState.Loading,
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            // ── 關於
            item { SettingsSectionHeader("關於") }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "版本",
                    subtitle = "1.0.0",
                    onClick = {}
                )
            }
        }
    }

    // ── Loading 覆蓋層
    if (backupState is BackupUiState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    // ── 深色模式 Dialog
    if (showDarkModeDialog) {
        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { Text("深色模式") },
            text = {
                Column {
                    DarkModeOption.values().forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDarkMode(option); showDarkModeDialog = false }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = darkMode == option,
                                onClick = { viewModel.setDarkMode(option); showDarkModeDialog = false }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // ── 匯入確認 Dialog
    showImportConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            title = { Text("確認匯入") },
            text = { Text("將導入備份檔中的資料。\n相同 ID 的資料會被覆蓋，新資料會被新增。\n是否繼續？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.triggerImport(uri, merge = true)
                    showImportConfirm = null
                }) { Text("確認匯入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) { Text("取消") }
            }
        )
    }

    // ── 匯入成功 / 錯誤 Snackbar
    if (backupState is BackupUiState.ImportSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.clearBackupState() },
            title = { Text("匯入完成") },
            text = { Text("資料已成功還原。") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearBackupState() }) { Text("確定") }
            }
        )
    }

    if (backupState is BackupUiState.Error) {
        val msg = (backupState as BackupUiState.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.clearBackupState() },
            title = { Text("發生錯誤") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearBackupState() }) { Text("確定") }
            }
        )
    }
}

// ── 可複用子元件 ──────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(
                imageVector = icon, contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
