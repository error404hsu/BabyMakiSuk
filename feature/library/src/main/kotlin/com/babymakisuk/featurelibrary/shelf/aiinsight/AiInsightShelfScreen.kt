package com.babymakisuk.featurelibrary.shelf.aiinsight

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coredata.entity.AiInsightEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AiInsightShelfScreen(
    navController: NavController,
    childId: String = "",
    viewModel: AiInsightShelfViewModel = hiltViewModel()
) {
    val insights by viewModel.insights.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var insightToDelete by remember { mutableStateOf<AiInsightEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = { Text("AI 精華", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (insights.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "尚無 AI 精華",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(insights, key = { it.id }) { insight ->
                    InsightCard(
                        insight = insight,
                        onLongClick = {
                            insightToDelete = insight
                            showDeleteDialog = true
                        }
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    if (showDeleteDialog && insightToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                insightToDelete = null
            },
            title = { Text("刪除確認") },
            text = { Text("確定要刪除「${insightToDelete!!.title}」嗎？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteInsight(insightToDelete!!.id)
                    showDeleteDialog = false
                    insightToDelete = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    insightToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InsightCard(
    insight: AiInsightEntity,
    onLongClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    var isExpanded by remember { mutableStateOf(false) }
    
    // 根據 childId 決定圖示與標籤
    val (childIcon, childLabel, childColor) = when {
        insight.childId == "twins" -> Triple("👫", "雙胞胎", MaterialTheme.colorScheme.tertiary)
        // 嘗試解析為 Long，如果成功則顯示寶寶標籤
        insight.childId.toLongOrNull() != null -> {
            // 目前暫時用 ID 判斷奇偶數來模擬性別圖示，或統一顯示寶寶圖示
            if (insight.childId.toLong() % 2 == 1L) 
                Triple("👦", "男寶寶", Color(0xFF4A90D9))
            else 
                Triple("👧", "女寶寶", Color(0xFFE07BBD))
        }
        else -> Triple("🤖", "AI 精華", MaterialTheme.colorScheme.primary)
    }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { isExpanded = !isExpanded },
                onLongClick = onLongClick
            )
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = childColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = childIcon, fontSize = 20.sp)
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(insight.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = insight.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                    overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = childColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = childLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = childColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                if (!isExpanded && insight.content.length > 100) {
                    Text(
                        text = "點擊展開更多...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
