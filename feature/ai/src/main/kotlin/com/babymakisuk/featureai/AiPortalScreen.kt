package com.babymakisuk.featureai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiSystemConstraints
import com.babymakisuk.coreai.GeminiModel
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.ui.theme.BabyMakiSukTheme
import androidx.compose.material.icons.filled.Info
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiPortalScreen(
    navController: NavController,
    @Suppress("UnusedPrivateMember") presetHint: String? = null,
    viewModel: AiPortalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 顯示一般 UI 訊息（儲存成功、新對話等）
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    // 顯示 AI 錯誤訊息
    LaunchedEffect(uiState.aiError) {
        uiState.aiError?.let { snackbarHostState.showSnackbar(it) }
    }

    AiPortalScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = { navController.popBackStack() },
        onSummarizeToKnowledgeBase = { viewModel.summarizeToKnowledgeBase() },
        onClearModelOverride = { viewModel.clearModelOverride() },
        onOverrideModel = { viewModel.overrideModel(it) },
        onSwitchPreset = { viewModel.switchPreset(it) },
        onSendMessage = { viewModel.sendMessage(it) },
        onNewConversation = { viewModel.startNewConversation() },
        onSelectChild = { viewModel.selectChild(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPortalScreenContent(
    uiState: AiPortalUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBackClick: () -> Unit,
    onSummarizeToKnowledgeBase: () -> Unit,
    onClearModelOverride: () -> Unit,
    onOverrideModel: (GeminiModel) -> Unit,
    onSwitchPreset: (AiPreset) -> Unit,
    onSendMessage: (String) -> Unit,
    onNewConversation: () -> Unit,
    onSelectChild: (Long) -> Unit = {}
) {
    val isTestingMode = true
    var showModelMenu by remember { mutableStateOf(false) }
    var showNewConvDialog by remember { mutableStateOf(false) }
    var pendingChildId by remember { mutableStateOf<Long?>(null) }
    var showSwitchChildDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // 智慧捲動：只有當使用者本來就在底部附近時，才在有新訊息時自動捲動
    LaunchedEffect(uiState.messages.size, uiState.isGenerating) {
        if (uiState.messages.isNotEmpty()) {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val isNearBottom = lastVisibleItem != null &&
                               lastVisibleItem.index >= uiState.messages.size - 2
            if (isNearBottom || uiState.isGenerating) {
                listState.animateScrollToItem(
                    if (uiState.isGenerating) uiState.messages.size else uiState.messages.size - 1
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI 助手")
                        val modelLabel = if (uiState.isModelOverridden)
                            "${uiState.effectiveModel.displayName} ★強制"
                        else
                            "${uiState.effectiveModel.displayName} ·建議"
                        Text(
                            text  = "角色: ${uiState.selectedPreset.displayName} | $modelLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isModelOverridden)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { showNewConvDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "開啟新對話",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (uiState.isSummarizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp).size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        IconButton(onClick = onSummarizeToKnowledgeBase) {
                            Icon(Icons.Default.Book, contentDescription = "整理本次對話為知識庫")
                        }
                    }

                    if (isTestingMode) {
                        if (uiState.isModelOverridden) {
                            IconButton(onClick = onClearModelOverride) {
                                Icon(
                                    Icons.Default.RestartAlt,
                                    contentDescription = "回復為角色建議模型",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Box {
                            IconButton(onClick = { showModelMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "選擇模型")
                            }
                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false }
                            ) {
                                GeminiModel.entries.forEach { model ->
                                    val isRecommended = model == uiState.selectedPreset.preferredModel
                                    val isActive      = model == uiState.effectiveModel
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(model.displayName)
                                                if (isRecommended) {
                                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                        Text("建議")
                                                    }
                                                }
                                                Badge { Text(model.badge) }
                                            }
                                        },
                                        onClick = {
                                            onOverrideModel(model)
                                            showModelMenu = false
                                        },
                                        trailingIcon = if (isActive) {{
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "使用中",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }} else null
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 角色選擇列
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.sortedPresets) { preset ->
                    val roleIcon = when (preset.name) {
                        "PEDIATRIC_DOCTOR" -> "👨\u200d⚕️"
                        "PHARMACIST"       -> "💊"
                        "NUTRITIONIST"     -> "🥗"
                        "GROWTH_ANALYST"   -> "📊"
                        else               -> "💬"
                    }
                    FilterChip(
                        selected = uiState.selectedPreset == preset,
                        onClick  = { onSwitchPreset(preset) },
                        label    = { Text(roleIcon + " " + preset.displayName) },
                        leadingIcon = if (uiState.selectedPreset == preset) {{
                            Icon(Icons.Default.Check, contentDescription = "已選擇", modifier = Modifier.size(16.dp))
                        }} else null
                    )
                }
            }

            // 小孩選擇器
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isSelected = uiState.selectedChildId == -1L
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (uiState.messages.isNotEmpty() && !isSelected) {
                                pendingChildId = -1L
                                showSwitchChildDialog = true
                            } else {
                                onSelectChild(-1L)
                            }
                        },
                        label = { Text("👫 雙胞胎 (不指定)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            selectedLabelColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }

                items(uiState.children) { child ->
                    val isSelected = child.id == uiState.selectedChildId
                    val genderColor = if (child.gender == Gender.MALE) Color(0xFF4A90D9) else Color(0xFFE07BBD)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (uiState.messages.isNotEmpty() && !isSelected) {
                                pendingChildId = child.id
                                showSwitchChildDialog = true
                            } else {
                                onSelectChild(child.id)
                            }
                        },
                        label = {
                            Text("${if (child.gender == Gender.MALE) "👦" else "👧"} ${child.name}")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = genderColor.copy(alpha = 0.2f),
                            selectedLabelColor = genderColor
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // ── AI 呼叫 Loading 橫幅 ──────────────────────────────────────────
            if (uiState.isAiLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 聊天對話區
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    if (message.text.isNotEmpty() || message.role == Role.USER) {
                        ChatBubble(
                            message = message,
                            aiPresetName = if (message.role == Role.AI) uiState.selectedPreset.displayName else null,
                            aiPresetHint = if (message.role == Role.AI) uiState.selectedPreset.name else null
                        )
                    }
                }

                if (uiState.isGenerating) {
                    item(key = "typing_indicator") { ThinkingIndicator() }
                }
            }

            // 預設問題（初始空對話時顯示）
            if (uiState.isAwaitingInput && uiState.messages.isEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetQuestions = listOf(
                        "分析最新的生長數據",
                        "規劃 6 個月寶寶副食品",
                        "最近夜醒頻繁怎麼辦？"
                    )
                    items(presetQuestions) { question ->
                        AssistChip(
                            onClick = { onSendMessage(question) },
                            label   = { Text(question) },
                            leadingIcon = {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            ChatInputBar(
                onSend       = onSendMessage,
                isGenerating = uiState.isGenerating || uiState.isAiLoading
            )
        }
    }

    if (showSwitchChildDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchChildDialog = false },
            title = { Text("切換對象") },
            text = { Text("切換孩子對象將會清除目前的對話記錄並開始新對話，確定要切換嗎？") },
            confirmButton = {
                TextButton(onClick = {
                    showSwitchChildDialog = false
                    pendingChildId?.let { onSelectChild(it) }
                }) { Text("確定切換") }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchChildDialog = false }) { Text("取消") }
            }
        )
    }

    if (showNewConvDialog) {
        AlertDialog(
            onDismissRequest = { showNewConvDialog = false },
            title = { Text("開啟新對話") },
            text = { Text("確定要清除所有對話記錄並開始新對話嗎？") },
            confirmButton = {
                TextButton(onClick = {
                    showNewConvDialog = false
                    onNewConversation()
                }) { Text("確定") }
            },
            dismissButton = {
                TextButton(onClick = { showNewConvDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    aiPresetName: String? = null,
    aiPresetHint: String? = null
) {
    val isUser = message.role == Role.USER
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = timeFormatter.format(Date(message.timestampMs))

    val roleIcon = when {
        isUser -> "👤"
        aiPresetHint == "PEDIATRIC_DOCTOR" -> "👨\u200d⚕️"
        aiPresetHint == "PHARMACIST"       -> "💊"
        aiPresetHint == "NUTRITIONIST"     -> "🥗"
        aiPresetHint == "GROWTH_ANALYST"   -> "📊"
        else -> "🤖"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role label
        Row(
            modifier = Modifier.padding(
                start  = if (isUser) 0.dp else 8.dp,
                end    = if (isUser) 8.dp else 0.dp,
                bottom = 4.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$roleIcon ${if (isUser) "你" else (aiPresetName ?: "AI 助手")}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart    = 20.dp,
                    topEnd      = 20.dp,
                    bottomStart = if (isUser) 20.dp else 4.dp,
                    bottomEnd   = if (isUser) 4.dp else 20.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp),
                shadowElevation = if (isUser) 0.dp else 2.dp,
                tonalElevation  = if (isUser) 0.dp else 1.dp
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                )
            }
        }

        // ── REFERENCE_DISCLAIMER（僅 AI 訊息，有內容時顯示）──────────────────
        if (!isUser && message.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = AiSystemConstraints.REFERENCE_DISCLAIMER,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
            shadowElevation = 2.dp,
            tonalElevation  = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text  = "正在回覆...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    onSend: (String) -> Unit,
    isGenerating: Boolean
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value         = text,
            onValueChange = { text = it },
            modifier      = Modifier.weight(1f),
            placeholder   = { Text("輸入問題...") },
            enabled       = !isGenerating,
            shape         = RoundedCornerShape(24.dp),
            maxLines      = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (text.isNotBlank()) { onSend(text); text = "" }
            })
        )

        IconButton(
            onClick  = { if (text.isNotBlank()) { onSend(text); text = "" } },
            enabled  = text.isNotBlank() && !isGenerating,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (text.isNotBlank() && !isGenerating)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "發送",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onTertiary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiPortalScreenPreview() {
    val sampleMessages = listOf(
        ChatMessage(role = Role.USER, text = "你好，我想了解寶寶副食品"),
        ChatMessage(role = Role.AI, text = "你好！關於寶寶副食品，建議從 4-6 個月開始嘗試。")
    )
    val sampleUiState = AiPortalUiState(
        messages = sampleMessages,
        isGenerating = false,
        selectedPreset = AiPreset.default
    )
    BabyMakiSukTheme {
        AiPortalScreenContent(
            uiState = sampleUiState,
            onBackClick = {},
            onSummarizeToKnowledgeBase = {},
            onClearModelOverride = {},
            onOverrideModel = {},
            onSwitchPreset = {},
            onSendMessage = {},
            onNewConversation = {}
        )
    }
}
