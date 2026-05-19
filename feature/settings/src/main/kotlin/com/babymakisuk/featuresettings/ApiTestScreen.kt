package com.babymakisuk.featuresettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTestScreen(
    onNavigateBack: () -> Unit,
    viewModel: ApiTestViewModel = hiltViewModel(),
) {
    val uiState       by viewModel.uiState.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val hasValidKey   = viewModel.hasValidKey

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 連線測試") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Key 狀態卡片 ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (hasValidKey)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (hasValidKey)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    },
                    headlineContent = { Text("Gemini API Key 狀態") },
                    supportingContent = {
                        Text(
                            text = if (hasValidKey) "✅ 已注入（BuildConfig）" else "❌ 未注入 / 為空白",
                            color = if (hasValidKey)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )
            }

            // ── 模型選擇區塊 ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "選擇模型",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                // 横向可滾動的 FilterChip 清單
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewModel.availableModels.forEach { model ->
                        val isSelected = model == selectedModel
                        FilterChip(
                            selected  = isSelected,
                            onClick   = { viewModel.selectModel(model) },
                            label     = { Text(model.displayName, style = MaterialTheme.typography.labelMedium) },
                            trailingIcon = {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        text = model.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // ── 發送按鈕 ──
            Button(
                onClick  = { viewModel.sendTestRequest() },
                enabled  = hasValidKey && (uiState !is ApiTestUiState.Loading),
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("發送測試請求（${selectedModel.displayName}）")
            }

            // ── 狀態顯示區 ──
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ApiTestState"
            ) { state ->
                when (state) {
                    is ApiTestUiState.Idle -> {}

                    is ApiTestUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    "送出中，等待 ${selectedModel.displayName} 回應…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is ApiTestUiState.Success -> {
                        ResultCard(
                            isSuccess = true,
                            label     = "回應成功（${selectedModel.displayName}）",
                            body      = state.response,
                            elapsedMs = state.elapsedMs,
                            onReset   = viewModel::reset
                        )
                    }

                    is ApiTestUiState.Error -> {
                        ResultCard(
                            isSuccess = false,
                            label     = "回應失敗（${selectedModel.displayName}）",
                            body      = state.message,
                            elapsedMs = state.elapsedMs,
                            onReset   = viewModel::reset
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultCard(
    isSuccess: Boolean,
    label: String,
    body: String,
    elapsedMs: Long,
    onReset: () -> Unit
) {
    val containerColor = if (isSuccess)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.errorContainer

    val contentColor = if (isSuccess)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onErrorContainer

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text  = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor
                )
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = {},
                    label   = { Text("$elapsedMs ms", style = MaterialTheme.typography.labelSmall) },
                    colors  = AssistChipDefaults.assistChipColors(
                        containerColor = containerColor.copy(alpha = 0.6f),
                        labelColor     = contentColor
                    )
                )
            }
            Surface(
                shape    = RoundedCornerShape(8.dp),
                color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text     = body,
                    style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color    = if (isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
            TextButton(
                onClick  = onReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("清除結果", color = contentColor)
            }
        }
    }
}
