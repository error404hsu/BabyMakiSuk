package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GrowthAiTab(
    aiAnalysisText: String,
    isAnalyzing: Boolean,
    onRefresh: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (aiAnalysisText.isBlank() && !isAnalyzing) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "尚無 AI 分析",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "點擊下方按鈕讓 AI 分析所有生長紀錄趨勢",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83E\uDD16", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI 生長分析",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (isAnalyzing) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                aiAnalysisText,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "⚠️ AI 整理僅供參考，如有疑慮請諮詢兒科醫師",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnalyzing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isAnalyzing) "分析中..." else "開始分析")
        }
    }
}
