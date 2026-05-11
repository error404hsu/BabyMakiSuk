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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coredata.DarkModeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onExportData: () -> Unit = {},
    onImportData: (uri: android.net.Uri) -> Unit = {}
) {
    val darkMode by viewModel.darkMode.collectAsState()
    var showDarkModeDialog by remember { mutableStateOf(false) }

    // 匯入檔案選擇器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onImportData(it) } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("設定") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 外觀 ──────────────────────────────────────
            item { SettingsSectionHeader(title = "外觀") }

            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = darkMode.label,
                    onClick = { showDarkModeDialog = true }
                )
            }

            // ── 資料管理 ──────────────────────────────────
            item { SettingsSectionHeader(title = "資料管理") }

            item {
                SettingsItem(
                    icon = Icons.Default.Upload,
                    title = "匯出備份",
                    subtitle = "將所有資料儲存為 JSON 檔案",
                    onClick = onExportData
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "匯入備份",
                    subtitle = "從 JSON 備份檔還原資料",
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            // ── 關於 ──────────────────────────────────────
            item { SettingsSectionHeader(title = "關於") }

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

    // 深色模式選擇 Dialog
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
                                .clickable {
                                    viewModel.setDarkMode(option)
                                    showDarkModeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = darkMode == option,
                                onClick = {
                                    viewModel.setDarkMode(option)
                                    showDarkModeDialog = false
                                }
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
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
