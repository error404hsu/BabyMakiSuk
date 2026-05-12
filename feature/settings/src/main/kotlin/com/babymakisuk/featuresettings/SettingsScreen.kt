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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coremodel.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToApiTest: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current

    val darkMode by viewModel.darkMode.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val aiCloudEnabled by viewModel.aiCloudEnabled.collectAsState()

    var showDarkModeSheet by remember { mutableStateOf(false) }
    var showRoleSheet by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<android.net.Uri?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { showImportConfirm = it } }

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
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
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
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Text(
                            "設定",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    actions = {
                        if (userRole != UserRole.NONE) {
                            AssistChip(
                                onClick = { showRoleSheet = true },
                                label = {
                                    Text(
                                        userRole.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = userRole.toIcon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            // ── 裝置角色 ──
            item {
                SettingsSection(title = "裝置角色") {
                    SettingsItem(
                        icon = userRole.toIcon(),
                        title = "目前角色：${userRole.label}",
                        subtitle = userRole.description,
                        onClick = { showRoleSheet = true }
                    )
                }
            }

            // ── 外觀 ──
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

            // ── AI 雲端推論 ──
            item {
                SettingsSection(title = "AI 雲端推論") {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        },
                        headlineContent = {
                            Text("啟用 Gemini 雲端分析")
                        },
                        supportingContent = {
                            Text(
                                if (aiCloudEnabled) "雲端 AI 推論啟動中"
                                else "已關閉，將使用本地備用處理",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = aiCloudEnabled,
                                onCheckedChange = { viewModel.setAiCloudEnabled(it) }
                            )
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    SettingsItem(
                        icon = Icons.Default.BugReport,
                        title = "API 連線測試",
                        subtitle = "驗證 Gemini API Key 是否可正常呼叫",
                        onClick = onNavigateToApiTest
                    )
                }
            }

            // ── 資料管理 ──
            item {
                SettingsSection(title = "資料管理") {
                    SettingsItem(
                        icon = Icons.Default.Upload,
                        title = "匯出備份",
                        subtitle = "將所有資料儲存為 JSON 檔案",
                        enabled = backupState !is BackupUiState.Loading,
                        onClick = { viewModel.triggerExport() }
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "匯入備份",
                        subtitle = "從 JSON 備份檔還原資料",
                        enabled = backupState !is BackupUiState.Loading,
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )
                }
            }

            // ── 關於 ──
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

    // ── 角色選擇 BottomSheet ──
    if (showRoleSheet) {
        ModalBottomSheet(onDismissRequest = { showRoleSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "選擇裝置角色",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                UserRole.entries
                    .filter { it != UserRole.NONE }
                    .forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setUserRole(role)
                                    coroutineScope.launch { showRoleSheet = false }
                                }
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = userRole == role,
                                onClick = {
                                    viewModel.setUserRole(role)
                                    coroutineScope.launch { showRoleSheet = false }
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(role.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    role.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
            }
        }
    }

    // ── 深色模式 BottomSheet ──
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

    // ── 匯入確認對話框 ──
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

    // ── 載入遮罩 ──
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

// ── 角色對應 Icon ──────────────────────────────────────────
internal fun UserRole.toIcon(): ImageVector = when (this) {
    UserRole.DATA_MANAGER -> Icons.Default.ManageAccounts
    UserRole.AI_OPERATOR  -> Icons.Default.SmartToy
    UserRole.ADMIN        -> Icons.Default.AdminPanelSettings
    UserRole.NONE         -> Icons.Default.ManageAccounts
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
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                title,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        },
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
