package com.babymakisuk.featurelibrary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.ui.components.LocalDrawerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class ShelfItem(
    val title: String,
    val subtitle: String,
    val route: String,
    val emoji: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    onNavigateToAi: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val selectedChildId by viewModel.selectedChildId.collectAsState()
    val weeklyUpdated by viewModel.weeklyLastUpdated.collectAsState()
    val aiUpdated by viewModel.aiInsightLastUpdated.collectAsState()
    val memoUpdated by viewModel.memoLastUpdated.collectAsState()
    val systemReminderUpdated by viewModel.systemReminderLastUpdated.collectAsState()

    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    val shelves = listOf(
        ShelfItem("月報書架", "AI 生成的每月綜合報告", "library/monthly", "📅"),
        ShelfItem("AI 精華", "智能摘錄的重要記錄", "library/aiinsight", "🤖"),
        ShelfItem("手動 Memo", "隨手記錄育兒筆記", "library/memo", "📝"),
        ShelfItem("系統提醒", "自動記錄事項（如排便提醒）", "library/system-reminder", "🔔")
    )

    val lastUpdatedMap = mapOf(
        "library/monthly" to weeklyUpdated,
        "library/aiinsight" to aiUpdated,
        "library/memo" to memoUpdated,
        "library/system-reminder" to systemReminderUpdated
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "書庫",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, "搜尋")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shelves) { shelf ->
                val lastUpdated = lastUpdatedMap[shelf.route]
                ShelfCard(
                    title = "${shelf.emoji} ${shelf.title}",
                    subtitle = shelf.subtitle,
                    lastUpdated = lastUpdated,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("${shelf.route}?childId=$selectedChildId")
                    }
                )
            }
        }
    }
}

@Composable
private fun ShelfCard(
    title: String,
    subtitle: String,
    lastUpdated: Long?,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lastUpdated != null && lastUpdated > 0L) {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    Text(
                        text = "最後更新：${dateFormat.format(Date(lastUpdated))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
