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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.babymakisuk.coredata.entity.MemoEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun jsonTagsToCsv(tags: String): String {
    return tags.removeSurrounding("[", "]")
        .split(",")
        .map { it.trim().removeSurrounding("\"") }
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemoShelfScreen(
    navController: NavController,
    childId: String = "",
    viewModel: MemoShelfViewModel = hiltViewModel()
) {
    val memos by viewModel.memos.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingMemo by remember { mutableStateOf<MemoEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var memoToDelete by remember { mutableStateOf<MemoEntity?>(null) }

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
                    editingMemo = null
                    showBottomSheet = true
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(memos, key = { it.id }) { memo ->
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    editingMemo = memo
                                    showBottomSheet = true
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
                                text = dateFormat.format(Date(memo.updatedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        val currentMemo = editingMemo
        MemoEditSheet(
            initialTitle = currentMemo?.title ?: "",
            initialContent = currentMemo?.content ?: "",
            initialTags = if (currentMemo != null) jsonTagsToCsv(currentMemo.tags) else "",
            onDismiss = {
                showBottomSheet = false
                editingMemo = null
            },
            onSave = { title, content, tags ->
                if (currentMemo != null) {
                    viewModel.update(
                        id = currentMemo.id,
                        title = title,
                        content = content,
                        tags = tags,
                        createdAt = currentMemo.createdAt
                    )
                } else {
                    viewModel.insert(title = title, content = content, tags = tags)
                }
                showBottomSheet = false
                editingMemo = null
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoEditSheet(
    initialTitle: String,
    initialContent: String,
    initialTags: String,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, tags: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var tags by remember { mutableStateOf(initialTags) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (initialTitle.isEmpty()) "新增 Memo" else "編輯 Memo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("標題") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("內容（Markdown 純文字）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("標籤（逗號分隔）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { onSave(title, content, tags) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("儲存")
            }
        }
    }
}
