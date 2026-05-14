package com.babymakisuk.featurelibrary.shelf.memo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.babymakisuk.coremodel.Memo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemoShelfScreen(
    navController: NavController,
    childId: String = "",
    viewModel: MemoShelfViewModel = hiltViewModel()
) {
    val memos by viewModel.memos.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var memoToDelete by remember { mutableStateOf<Memo?>(null) }

    val childIdLong = childId.toLongOrNull() ?: 0L
    val grouped = remember(memos) { memos.groupBy { it.date }.toSortedMap(reverseOrder()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手動 Memo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("library/memo/edit?memoId=-1&childId=$childIdLong")
                }
            ) {
                Icon(Icons.Default.Add, "新增")
            }
        }
    ) { innerPadding ->
        if (memos.isEmpty()) {
            Text(
                text = "尚無 Memo 紀錄",
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                grouped.forEach { (date, dayMemos) ->
                    item(key = "date_header_$date") {
                        val localDate = LocalDate.ofEpochDay(date)
                        Text(
                            text = localDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (E)")),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(dayMemos, key = { it.id }) { memo ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("library/memo/edit?memoId=${memo.id}&childId=${memo.childId}")
                                    },
                                    onLongClick = {
                                        memoToDelete = memo
                                        showDeleteDialog = true
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = memo.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (memo.content.length > 60)
                                                memo.content.take(60) + "..."
                                            else memo.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(memo.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            }
        }
    }

    if (showDeleteDialog && memoToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                memoToDelete = null
            },
            title = { Text("刪除確認") },
            text = { Text("確定要刪除「${memoToDelete!!.title}」嗎？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteById(memoToDelete!!.id)
                    showDeleteDialog = false
                    memoToDelete = null
                }) {
                    Text("刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    memoToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}
