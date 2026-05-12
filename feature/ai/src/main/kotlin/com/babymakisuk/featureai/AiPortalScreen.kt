package com.babymakisuk.featureai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coreai.AiPreset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPortalScreen(
    navController: NavController,
    presetHint: String? = null,
    viewModel: AiPortalViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val chatHistory    by viewModel.chatHistory.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val sortedPresets  by viewModel.sortedPresets.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()

    // 有新訊息時自動滾到底部
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 助理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Preset Selector ──────────────────────────────────────────────
            LazyRow(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedPresets) { preset ->
                    PresetChip(
                        preset     = preset,
                        isSelected = preset == selectedPreset,
                        onClick    = { viewModel.selectPreset(preset) }
                    )
                }
            }

            // ── Chat History ─────────────────────────────────────────────────
            LazyColumn(
                state                 = listState,
                modifier              = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                contentPadding        = PaddingValues(vertical = 8.dp)
            ) {
                items(chatHistory) { message ->
                    ChatBubble(message = message)
                }
            }

            // Loading indicator
            if (uiState is AiPortalUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // ── Input Row ─────────────────────────────────────────────────────
            val isRateLimit  = uiState is AiPortalUiState.Error &&
                               (uiState as AiPortalUiState.Error).message.contains("上限")
            val errorMessage = if (uiState is AiPortalUiState.Error)
                                   (uiState as AiPortalUiState.Error).message
                               else null

            Column(modifier = Modifier.padding(8.dp)) {
                if (isRateLimit && errorMessage != null) {
                    Text(
                        text     = errorMessage,
                        color    = MaterialTheme.colorScheme.error,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    val placeholder = if (selectedPreset == AiPreset.CUSTOM)
                        "輸入你想聊的話題..."
                    else
                        "問 ${selectedPreset.displayName} 任何問題..."

                    OutlinedTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        placeholder   = { Text(placeholder) },
                        modifier      = Modifier.weight(1f),
                        singleLine    = false,
                        maxLines      = 4,
                        isError       = isRateLimit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                scope.launch {
                                    val size = chatHistory.size
                                    if (size > 0) listState.animateScrollToItem(size - 1)
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && uiState !is AiPortalUiState.Loading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "傳送")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PresetChip(
    preset: AiPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title            = { Text(preset.displayName) },
            text             = { Text(preset.description) },
            confirmButton    = {
                TextButton(onClick = { showDialog = false }) { Text("關閉") }
            }
        )
    }

    if (isSelected) {
        FilledTonalButton(
            onClick        = onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(preset.displayName)
        }
    } else {
        OutlinedButton(
            onClick        = onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(preset.displayName)
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape    = MaterialTheme.shapes.medium,
            color    = if (message.isUser)
                           MaterialTheme.colorScheme.primaryContainer
                       else
                           MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text     = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style    = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
