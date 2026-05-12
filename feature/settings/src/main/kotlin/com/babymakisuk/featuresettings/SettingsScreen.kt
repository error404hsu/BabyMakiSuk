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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coremodel.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current

    val darkMode by viewModel.darkMode.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val apiKeyState by viewModel.apiKeyState.collectAsState()

    var showDarkModeSheet by remember { mutableStateOf(false) }
    var showRoleSheet by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<android.net.Uri?>(null) }
    var inputApiKey by remember { mutableStateOf("") }

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
            TopAppBar(
                title = { Text("設定", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (userRole != UserRole.NONE) {
                        AssistChip(
                            onClick = { showRoleSheet = true },
                            label = { Text(userRole.label, style = MaterialTheme.typography.labelMedium) },
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
                    } else {
                        AssistChip(
                            onClick = { showRoleSheet = true },
                            label = { Text("請設定角色", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
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

            // ── AI 雲端推論設定 ── (新增區塊)
            item {
                SettingsSection(title = "AI 雲端推論設定") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Gemini API Key",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = inputApiKey,
                            onValueChange = {
                                inputApiKey = it
                                // 重新輸入時清除先前的驗證狀態
                                if (apiKeyState.isVerified || apiKeyState.errorMessage != null) {
                                    viewModel.clearApiKeyState()
                                }
                            },
                            label = { Text("貼上您的 API Key") },
                            placeholder = { Text("AIzaSy...") },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            isError = apiKeyState.errorMessage != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors()
                        )

                        Button(
                            onClick = { viewModel.verifyAndSaveApiKey(inputApiKey) },
                            enabled = inputApiKey.isNotBlank() && !apiKeyState.isVerifying,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (apiKeyState.isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("驗證中...")
                            } else {
                                Text("驗證並儲存")
                            }
                        }

                        // 驗證結果回饋
                        when {
                            apiKeyState.isVerified -> {
                                Text(
                                    text = "✅ 驗證成功，API Key 已安全儲存",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            apiKeyState.errorMessage != null -> {
                                Text(
                                    text = "❌ ${apiKeyState.errorMessage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Text(
                            text = "Key 僅儲存於本機 DataStore，不會上傳至任何伺服器。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
