package com.babymakisuk.featureweeklyreport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coredata.entity.WeeklyReportEntity

/**
 * WeeklyReportSearchScreen — Sprint 3
 *
 * FTS 全文搜尋週報，支援關鍵字高亮（AnnotatedString 標黃）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportSearchScreen(
    navController: NavController,
    childId: String = "",
    viewModel: WeeklyReportSearchViewModel = hiltViewModel()
) {
    // 設定 childId
    LaunchedEffect(childId) {
        viewModel.currentChildId = childId
    }

    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜尋週報") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // ── 搜尋列 ──────────────────────────────────────────────────────
            SearchBar(
                query = query,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearch = { /* FTS 即時搜尋，無需手動觸發 */ },
                active = false,
                onActiveChange = {},
                placeholder = { Text("輸入關鍵字搜尋週報…") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "搜尋")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {}

            // ── 結果列表 / 空狀態 ────────────────────────────────────────────
            if (query.isNotBlank() && results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "找不到相關週報，試試其他關鍵字",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(results, key = { it.id }) { report ->
                        WeeklyReportSearchCard(
                            report = report,
                            keyword = query
                        )
                    }
                }
            }
        }
    }
}

// ── 週報搜尋結果卡片 ─────────────────────────────────────────────────────────

@Composable
private fun WeeklyReportSearchCard(
    report: WeeklyReportEntity,
    keyword: String
) {
    val summaryPreview = report.aiSummary.take(100).let {
        if (report.aiSummary.length > 100) "$it…" else it
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 週報日期區間
            Text(
                text = "${report.weekStart} ～ ${report.weekEnd}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            // 摘要前 100 字 + 關鍵字高亮
            Text(
                text = highlightKeyword(summaryPreview, keyword),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 以 AnnotatedString 將摘要中的關鍵字標為黃色粗體。
 */
private fun highlightKeyword(text: String, keyword: String) = buildAnnotatedString {
    if (keyword.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    var start = 0
    val lower = text.lowercase()
    val keyLower = keyword.lowercase()
    while (true) {
        val idx = lower.indexOf(keyLower, start)
        if (idx == -1) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, idx))
        withStyle(SpanStyle(background = Color(0xFFFFEE58), fontWeight = FontWeight.Bold)) {
            append(text.substring(idx, idx + keyword.length))
        }
        start = idx + keyword.length
    }
}
