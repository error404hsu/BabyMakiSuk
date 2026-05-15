package com.babymakisuk.featurelibrary.shelf.memo

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Notifications
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
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.Memo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val BoyBlue = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemoShelfScreen(
    navController: NavController,
    childId: String = "",
    viewModel: MemoShelfViewModel = hiltViewModel(),
    onNavigateToAi: (String?) -> Unit = {}
) {
    val memos by viewModel.memos.collectAsState()
    val childrenProfiles by viewModel.children.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var memoToDelete by remember { mutableStateOf<Memo?>(null) }

    val entryChildIdLong = childId.toLongOrNull() ?: 0L

    val grouped = remember(memos) {
        memos.groupBy { it.date }
            .toSortedMap(reverseOrder())
            .mapValues { (_, dayMemos) ->
                dayMemos.groupBy { "${it.title}|${it.content}|${it.date}|${it.reminderAt}" }
                    .values.toList()
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = { Text("手動 Memo", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("library/memo/edit?memoId=-1&childId=$entryChildIdLong")
                    }) {
                        Icon(Icons.Default.Add, "新增")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (memos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "尚無 Memo 紀錄",
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
                grouped.forEach { (date, dayGroups) ->
                    item(key = "date_header_$date") {
                        val localDate = LocalDate.ofEpochDay(date)
                        Text(
                            text = localDate.format(DateTimeFormatter.ofPattern("yyyy / MM / dd (E)")),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(dayGroups, key = { it.first().id }) { group ->
                        val firstMemo = group.first()
                        val involvedChildIds = group.map { it.childId }
                        
                        MemoCard(
                            memo = firstMemo,
                            childIds = involvedChildIds,
                            allChildren = childrenProfiles,
                            onClick = {
                                navController.navigate("library/memo/edit?memoId=${firstMemo.id}&childId=${firstMemo.childId}")
                            },
                            onLongClick = {
                                memoToDelete = firstMemo
                                showDeleteDialog = true
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
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
            text = { Text("確定要刪除這則記事嗎？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteById(memoToDelete!!.id)
                        showDeleteDialog = false
                        memoToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoCard(
    memo: Memo,
    childIds: List<Long>,
    allChildren: List<ChildProfile>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(CircleShape)
            ) {
                childIds.forEach { id ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(if (id == 1L) BoyBlue else GirlPink)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = memo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timeFormat.format(java.util.Date(memo.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allChildren.filter { it.id in childIds }.forEach { child ->
                        val color = if (child.gender == Gender.MALE) BoyBlue else GirlPink
                        Surface(
                            color = color.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${if (child.gender == Gender.MALE) "👦" else "👧"} ${child.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (memo.content.isNotBlank()) {
                    Text(
                        text = memo.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (memo.reminderAt != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        val reminderFormat = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                        Text(
                            text = "提醒：${reminderFormat.format(java.util.Date(memo.reminderAt!!))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
