package com.error404hsu.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.error404hsu.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile

@Composable
fun GrowthListScreen(
    records: List<GrowthRecordWithPercentile>,
    onDelete: (GrowthRecordWithPercentile) -> Unit,
) {
    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("尚無成長紀錄，點擊 + 開始新增")
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(records, key = { it.record.id }) { item ->
            GrowthRecordCard(item) { onDelete(item) }
        }
    }
}

@Composable
private fun GrowthRecordCard(
    item: GrowthRecordWithPercentile,
    onDelete: () -> Unit
) {
    val r = item.record
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(r.date.toString(), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Metric(label = "身高", value = "${r.heightCm} cm", pct = item.heightPercentile)
                    Metric(label = "體重", value = "${r.weightKg} kg", pct = item.weightPercentile)
                    r.headCircumferenceCm?.let {
                        Metric(label = "頭圍", value = "$it cm", pct = -1)
                    }
                }
                if (r.note.isNotBlank()) Text(r.note, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除")
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, pct: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        if (pct >= 0) {
            Text(
                text = "P$pct",
                style = MaterialTheme.typography.labelSmall,
                color = percentileColor(pct)
            )
        }
    }
}

@Composable
private fun percentileColor(pct: Int) = when {
    pct < 3  -> MaterialTheme.colorScheme.error
    pct < 15 -> MaterialTheme.colorScheme.tertiary
    pct > 97 -> MaterialTheme.colorScheme.error
    pct > 85 -> MaterialTheme.colorScheme.tertiary
    else     -> MaterialTheme.colorScheme.primary
}
