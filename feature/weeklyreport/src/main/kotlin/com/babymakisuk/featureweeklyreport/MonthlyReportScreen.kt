package com.babymakisuk.featureweeklyreport

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coremodel.MonthlyReport
import com.babymakisuk.ui.theme.BabyMakiSukTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    childId: Long = 0L,
    onBack: () -> Unit = {},
    viewModel: MonthlyReportViewModel = hiltViewModel()
) {
    LaunchedEffect(childId) {
        viewModel.setChildId(childId)
    }

    val reports by viewModel.reports.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    title = {
                        Text(
                            "月報",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.generateReport() },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("生成 本月月報")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isGenerating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "AI 正在生成月報…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (reports.isEmpty() && !isGenerating) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "尚無月報",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "點擊下方按鈕生成第一份成長月報",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports, key = { it.id }) { report ->
                        MonthlyReportCard(
                            report = report,
                            onDelete = { viewModel.deleteReport(report.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthlyReportCard(
    report: MonthlyReport,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("確認刪除") },
            text = { Text("確定要刪除這份月報嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { showDeleteDialog = true }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${report.monthStart} ~ ${report.monthEnd}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            if (!expanded && report.aiSummary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = report.aiSummary.take(60) + if (report.aiSummary.length > 60) "…" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                if (report.aiSummary.isNotBlank()) {
                    Text(
                        text = report.aiSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                if (report.searchKeywords.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.searchKeywords.forEach { keyword ->
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(keyword, style = MaterialTheme.typography.labelSmall)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthlyReportCardPreview() {
    BabyMakiSukTheme {
        MonthlyReportCard(
            report = MonthlyReport(
                id = "1_2026-M05",
                childId = 1L,
                monthStart = "2026-05-01",
                monthEnd = "2026-05-31",
                aiSummary = "本月寶寶食慾良好，體重穩定成長。月中曾有一次輕微感冒已自行痊癒。建議多注意天氣變化適時添衣。",
                growthSnapshot = null,
                medicalCount = 1,
                systemReminderCount = 0,
                searchKeywords = listOf("食慾", "體重", "感冒"),
                driveFileId = null,
                syncedAt = System.currentTimeMillis()
            ),
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthlyReportScreenPreview() {
    BabyMakiSukTheme {
        MonthlyReportScreen(
            childId = 1L,
            onBack = {}
        )
    }
}
