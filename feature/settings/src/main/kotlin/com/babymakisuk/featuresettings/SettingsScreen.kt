package com.babymakisuk.featuresettings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coremodel.UserRole
import com.babymakisuk.ui.components.BabyTopBar
import com.babymakisuk.ui.components.LocalDrawerState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToApiTest: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val darkMode by viewModel.darkMode.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val aiCloudEnabled by viewModel.aiCloudEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState()
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val storageUsedBytes by viewModel.storageUsedBytes.collectAsState()

    var showDarkModeSheet by remember { mutableStateOf(false) }
    var showRoleSheet by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<android.net.Uri?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val drawerState = LocalDrawerState.current
    val drawerScope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { showImportConfirm = it } }

    // Google Sign-In setup
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            coroutineScope.launch {
                try {
                    val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val idToken = account.result?.idToken ?: return@launch
                    viewModel.linkGoogleAccount(idToken)
                        .onSuccess { snackbarHostState.showSnackbar("Google 帳號連結成功") }
                        .onFailure { snackbarHostState.showSnackbar("連結失敗：${it.message}") }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Google 登入失敗：${e.message}")
                }
            }
        }
    }

    val gso = remember {
        val clientId = runCatching {
            val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (id != 0) context.getString(id) else ""
        }.getOrDefault("")

        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .apply {
                if (clientId.isNotEmpty()) {
                    requestIdToken(clientId)
                }
            }
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

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
            BabyTopBar(
                title = {
                    Text(
                        "設定",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                showSearch = false,
                showAi = false,
                showAdd = false,
                onMenuClick = { drawerScope.launch { drawerState.open() } }
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

            // ── 帳號 ──────────────────────────────────────────
            item {
                SettingsSection(title = "帳號") {
                    GoogleSignInItem(
                        firebaseUser = firebaseUser,
                        onSignInClick = {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        onSignOutClick = {
                            viewModel.signOutGoogle()
                            googleSignInClient.signOut()
                        }
                    )
                }
            }

            // ── 個人化與偏好 ──────────────────────────────────
            item {
                SettingsSection(title = "個人化與偏好") {
                    SettingsItem(
                        icon = userRole.toIcon(),
                        title = "目前角色：${userRole.label}",
                        subtitle = userRole.description,
                        onClick = { showRoleSheet = true }
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "深色模式",
                        subtitle = darkMode.label,
                        onClick = { showDarkModeSheet = true }
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            IconBox(
                                icon = Icons.Default.Notifications,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            )
                        },
                        headlineContent = { Text("啟用通知") },
                        supportingContent = {
                            Text(
                                if (notificationsEnabled) "Memo 提醒通知已啟用" else "已關閉所有通知",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ── AI 功能 ──────────────────────────────────────
            item {
                SettingsSection(title = "AI 功能") {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            IconBox(
                                icon = Icons.Default.Cloud,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            )
                        },
                        headlineContent = { Text("啟用 Gemini 雲端分析") },
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
                        icon = Icons.Default.Sync,
                        title = "產生上月合併月報",
                        subtitle = "彙整所有孩子資料並由 AI 生成上月份總結",
                        onClick = { viewModel.generateLastMonthReport() }
                    )
                }
            }

            // ── 資料管理與備份 ──────────────────────────────
            item {
                SettingsSection(title = "資料管理與備份") {
                    // 雲端同步 + 配額
                    SettingsItem(
                        icon = Icons.Default.Sync,
                        title = "雲端同步",
                        subtitle = formatStorageBytes(storageUsedBytes),
                        onClick = {}
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // 自動備份
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            IconBox(
                                icon = Icons.Default.Backup,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            )
                        },
                        headlineContent = { Text("自動建立本地備份") },
                        supportingContent = { Text("每週自動產出備份檔至裝置儲存空間") },
                        trailingContent = {
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                            )
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    SettingsItem(
                        icon = Icons.Default.Upload,
                        title = "立即匯出備份 (.json)",
                        subtitle = lastBackupTime?.let { "上次匯出：$it" } ?: "尚未建立備份",
                        enabled = backupState !is BackupUiState.Loading,
                        onClick = { viewModel.triggerExport() }
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "匯入備份檔",
                        subtitle = "從檔案選擇器選取備份進行還原",
                        enabled = backupState !is BackupUiState.Loading,
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )
                }
            }

            if (developerModeEnabled) {
                item {
                    SettingsSection(title = "開發者選項") {
                        SettingsItem(
                            icon = Icons.Default.BugReport,
                            title = "API 連線測試",
                            subtitle = "驗證 Gemini API Key 是否可正常呼叫",
                            onClick = onNavigateToApiTest
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        SettingsItem(
                            icon = Icons.Default.Cloud,
                            title = "Firebase 連線測試",
                            subtitle = "發送 Ping 至 Firestore debug_ping 集合",
                            onClick = {
                                viewModel.testFirebaseConnection { message ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                }
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "測試月底提醒生成",
                            subtitle = "模擬月底最後一週觸發書庫提醒",
                            onClick = { viewModel.triggerMonthlyReminderTest() }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                IconBox(
                                    icon = Icons.Default.AdminPanelSettings,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            headlineContent = { Text("開發者模式") },
                            supportingContent = { Text("關閉後隱藏此分區") },
                            trailingContent = {
                                Switch(
                                    checked = developerModeEnabled,
                                    onCheckedChange = { viewModel.setDeveloperModeEnabled(it) }
                                )
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "關於") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "版本",
                        subtitle = "1.0.0",
                        onClick = {
                            viewModel.onVersionClick {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("開發者模式已開啟")
                                }
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

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
        val state = backupState as BackupUiState.Loading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.progress != null) {
                        CircularProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 6.dp
                        )
                        Text(
                            "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    }
                    Text(
                        text = state.message ?: "正在處理資料...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠ 請勿關閉 App 以免資料損毀",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

// ── Google 登入元件 ──────────────────────────────────────────
@Composable
private fun GoogleSignInItem(
    firebaseUser: FirebaseUser?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    if (firebaseUser != null) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                IconBox(
                    icon = Icons.Default.Person,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            },
            headlineContent = {
                Text(firebaseUser.email ?: firebaseUser.displayName ?: "已登入")
            },
            supportingContent = { Text("點按登出 Google 帳號") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSignOutClick)
                .padding(vertical = 4.dp)
        )
    } else {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                IconBox(
                    icon = Icons.Default.Login,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            },
            headlineContent = { Text("使用 Google 帳號登入") },
            supportingContent = { Text("連結後可跨裝置同步資料") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSignInClick)
                .padding(vertical = 4.dp)
        )
    }
}

// ── 工具函式 ──────────────────────────────────────────────
private fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0) return "已使用 0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return "已使用 ${"%.1f".format(mb)} MB"
}

@Composable
private fun IconBox(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color = color, shape = RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
    }
}

internal fun UserRole.toIcon(): ImageVector = when (this) {
    UserRole.DATA_MANAGER -> Icons.Default.ManageAccounts
    UserRole.AI_OPERATOR  -> Icons.Default.SmartToy
    UserRole.ADMIN        -> Icons.Default.AdminPanelSettings
    UserRole.NONE         -> Icons.Default.ManageAccounts
}

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
            IconBox(
                icon = icon,
                color = if (enabled) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                tint = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp)
    )
}
