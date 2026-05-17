package com.babymakisuk.featureweeklyreport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import com.babymakisuk.coredata.entity.MonthlyReportEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportSearchScreen(
    navController: NavController,
    childId: Long = 0L,
    viewModel: MonthlyReportSearchViewModel = hiltViewModel()
) {
    LaunchedEffect(childId) {
        viewModel.currentChildId = childId
    }

    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜尋月報") },
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
            SearchBar(
                query = query,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearch = { /* FTS 即時搜尋，無需手動觸發 */ },
                active = false,
                onActiveChange = {},
                placeholder = { Text("輸入關鍵字搜尋月報…") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "搜尋")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {}

            if (query.isBlank() && results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "搜尋月報內容",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "輸入關鍵字查找每月 AI 摘要與成長分析",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "找不到相關月報",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "試試其他關鍵字",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(results, key = { it.id }) { report ->
                        MonthlyReportSearchCard(
                            report = report,
                            keyword = query
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyReportSearchCard(
    report: MonthlyReportEntity,
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
            Text(
                text = "${report.monthStart} ～ ${report.monthEnd}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = highlightKeyword(summaryPreview, keyword),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

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
