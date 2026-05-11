package com.babymakisuk.featuresettings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coredata.DarkModeOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current

    val darkMode by viewModel.darkMode.collectAsState()
    val backupState by viewModel.backupState.collectAsState()

    // UI 狀態控制
    var showDarkModeSheet by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<android.net.Uri?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { showImportConfirm = it } }

    // ── 處理沉浸式滿版 (Edge-to-Edge) 與狀態列顏色 ──
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        DarkModeOption.DARK -> true
        DarkModeOption.LIGHT -> false
        DarkModeOption.SYSTEM -> isSystemDark
        else -> isSystemDark
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // 1. 讓畫面延伸到系統列底下 (滿版)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                // 2. 將狀態列與導覽列背景設為透明
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                // 3. 自動控制頂部(時間電量)與底部(導覽列)圖示為深色或淺色
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupUiState.ExportReady -> {
                context.startActivity(android.content.Intent.createChooser(state.intent, "備份檔儲存至…"))
                viewModel.clearBackupState()
            }
            is BackupUiState.ImportSuccess -> {
                snackbarHostState.showSnackbar("資料已成功還原", withDismissAction = true)
                viewModel.clearBackupState()
            }
            is BackupUiState.Error -> {
                snackbarHostState.showSnackbar("發生錯誤：${state.message}", withDismissAction = true)
                viewModel.clearBackupState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // 讓頂部不再是突兀的白色，完全融入背景
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 統一將底色改為基礎 background，視覺最和諧
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                SettingsSection(title = "外觀") {
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "深色模式",
                        subtitle = darkMode.label,
                        onClick = { showDarkModeSheet = true }
                    )
                }
            }

            item {
                SettingsSection(title = "資料管理") {
                    SettingsItem(
                        icon = Icons.Default.Upload,
                        title = "匯出備份",
                        subtitle = "將所有資料儲存為 JSON 檔案",
                        enabled = backupState !is BackupUiState.Loading,
                        onClick = { viewModel.triggerExport() }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "匯入備份",
                        subtitle = "從 JSON 備份檔還原資料",
                        enabled = backupState !is BackupUiState.Loading,
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )
                }
            }

            item {
                SettingsSection(title = "關於") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "版本",
                        subtitle = "1.0.0",
                        onClick = {}
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showDarkModeSheet) {
        ModalBottomSheet(onDismissRequest = { showDarkModeSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "選擇深色模式",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                DarkModeOption.values().forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setDarkMode(option)
                                coroutineScope.launch { showDarkModeSheet = false }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = darkMode == option,
                            onClick = {
                                viewModel.setDarkMode(option)
                                coroutineScope.launch { showDarkModeSheet = false }
                            }
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    showImportConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            title = { Text("確認匯入") },
            text = { Text("將導入備份檔中的資料。\n相同 ID 的資料會被覆蓋，新資料會被新增。\n\n是否繼續？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.triggerImport(uri, merge = true)
                    showImportConfirm = null
                }) { Text("確認匯入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) { Text("取消") }
            }
        )
    }

    if (backupState is BackupUiState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(enabled = false, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(Modifier.padding(32.dp)) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// ── 可複用子元件 ──────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        // 將卡片顏色改為 Elevated 顏色，與背景做出層次感區別
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
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
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (enabled) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp)
    )
}