package com.babymakisuk.featureai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coreai.GeminiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPortalScreen(
    navController: NavController,
    @Suppress("UnusedPrivateMember") presetHint: String? = null,
    viewModel: AiPortalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val isTestingMode = true
    var showModelMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI 助手")
                        // TopBar 副標題顯示有效模型，並以文字區分建議/強制狀態
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 整理為知識庫
                    IconButton(onClick = { viewModel.summarizeToKnowledgeBase() }) {
                        Icon(Icons.Default.Book, contentDescription = "整理本次對話為知識庫")
                    }

                    if (isTestingMode) {
                        // 強制 override 時顯示「重置為建議」按鈕
                        if (uiState.isModelOverridden) {
                            IconButton(onClick = { viewModel.clearModelOverride() }) {
                                Icon(
                                    Icons.Default.RestartAlt,
                                    contentDescription = "回復為角色建議模型",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        // 模型選單：從 GeminiModel.entries 動態產生，標示建議/強制狀態
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
                                                // 角色建議模型標示
                                                if (isRecommended) {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                    ) { Text("建議") }
                                                }
                                                Badge { Text(model.badge) }
                                            }
                                        },
                                        onClick = {
                                            viewModel.overrideModel(model)
                                            showModelMenu = false
                                        },
                                        trailingIcon = if (isActive) {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "使用中",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
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
            // 角色選擇列（Preset Selector）
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.sortedPresets) { preset ->
                    FilterChip(
                        selected = uiState.selectedPreset == preset,
                        onClick  = { viewModel.switchPreset(preset) },
                        label    = { Text(preset.displayName) },
                        leadingIcon = if (uiState.selectedPreset == preset) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "已選擇",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // 聊天對話區
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
            }

            // 預設問題（僅在初始狀態且沒有對話時顯示）
            if (uiState.isAwaitingInput && uiState.messages.isEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetQuestions = listOf(
                        "分析最新的生長數據",
                        "規劃 6 個月寶寶副食品",
                        "最近夜醒頻繁怎麼辦？"
                    )
                    items(presetQuestions) { question ->
                        AssistChip(
                            onClick = { viewModel.sendMessage(question) },
                            label   = { Text(question) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            ChatInputBar(
                onSend       = { viewModel.sendMessage(it) },
                isGenerating = uiState.isGenerating
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart    = 16.dp,
                topEnd      = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd   = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text     = message.text,
                modifier = Modifier.padding(12.dp),
                color    = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                style    = MaterialTheme.typography.bodyLarge
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            })
        )

        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            },
            enabled  = text.isNotBlank() && !isGenerating,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (text.isNotBlank() && !isGenerating)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "發送",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
