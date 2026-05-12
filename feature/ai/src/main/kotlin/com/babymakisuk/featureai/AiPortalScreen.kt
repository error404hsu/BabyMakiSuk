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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPortalScreen(
    navController: NavController,
    @Suppress("UnusedPrivateMember") presetHint: String? = null,
    viewModel: AiPortalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 控制測試中模型選擇選單的顯示
    val isTestingMode = true 
    var showModelMenu by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()

    // 當新訊息產生或打字機效果進行時，自動捲動到底部
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
                        Text(
                            text = "角色: ${uiState.selectedPersona.title} | ${uiState.selectedModel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 功能：整理為知識庫
                    IconButton(onClick = { viewModel.summarizeToKnowledgeBase() }) {
                        Icon(Icons.Default.Book, contentDescription = "整理本次對話為知識庫")
                    }
                    
                    // 功能：強制使用模型的選項
                    if (isTestingMode) {
                        Box {
                            IconButton(onClick = { showModelMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "選擇模型")
                            }
                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Gemini 1.5 Flash") },
                                    onClick = { viewModel.switchModel("gemini-1.5-flash"); showModelMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gemini Pro") },
                                    onClick = { viewModel.switchModel("gemini-pro"); showModelMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gemma 4 (Local LiteRT)") },
                                    onClick = { viewModel.switchModel("gemma-4-local"); showModelMenu = false }
                                )
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
            // UI/UX: 頂部角色選擇列 (Persona Selector)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.sortedPersonas) { persona ->
                    FilterChip(
                        selected = uiState.selectedPersona == persona,
                        onClick = { viewModel.switchPersona(persona) },
                        label = { Text(persona.title) },
                        leadingIcon = if (uiState.selectedPersona == persona) {
                            { Icon(Icons.Default.Check, contentDescription = "已選擇", modifier = Modifier.size(16.dp)) }
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

            // 預設問題 (僅在初始狀態且沒有對話時顯示)
            if (uiState.isAwaitingInput && uiState.messages.isEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetQuestions = listOf("分析最新的生長數據", "規劃 6 個月寶寶副食品", "最近夜醒頻繁怎麼辦？")
                    items(presetQuestions) { question ->
                        AssistChip(
                            onClick = { viewModel.sendMessage(question) },
                            label = { Text(question) },
                            leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            // 底部輸入框與發送按鈕
            ChatInputBar(
                onSend = { viewModel.sendMessage(it) },
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
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
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
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("輸入問題...") },
            enabled = !isGenerating,
            shape = RoundedCornerShape(24.dp),
            maxLines = 3,
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
            enabled = text.isNotBlank() && !isGenerating,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (text.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "發送",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
