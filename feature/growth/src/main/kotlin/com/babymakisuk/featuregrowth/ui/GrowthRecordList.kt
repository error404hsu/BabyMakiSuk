package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile

@Composable
fun GrowthRecordList(
    records: List<GrowthRecordWithPercentile>,
    onEdit: (GrowthRecordWithPercentile) -> Unit,
    onDelete: (GrowthRecordWithPercentile) -> Unit
) {
    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("尚無成長紀錄，點擊 + 新增")
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(records, key = { it.record.id }) { item ->
            GrowthRecordCard(
                item = item,
                onEdit = { onEdit(item) },
                onDelete = { onDelete(item) }
            )
        }
    }
}
