package com.error404hsu.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.error404hsu.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile

/**
 * 折線圖：以月齡為 X 軸，分頁顯示「身高」與「體重」，
 * 並繪製 P3 / P15 / P50 / P85 / P97 參考線（灰色虛線）。
 */
@Composable
fun GrowthChartScreen(records: List<GrowthRecordWithPercentile>) {
    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("尚無資料")
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("身高 (cm)", "體重 (kg)")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 圖例
        PercentileLegend()

        Spacer(Modifier.height(8.dp))

        GrowthLineChart(
            records = records,
            metricSelector = if (selectedTab == 0) ({ it.record.heightCm.toDouble() }) else ({ it.record.weightKg.toDouble() }),
            label = tabs[selectedTab],
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PercentileLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendItem(color = Color(0xFFBDBDBD), label = "P3/P97")
        LegendItem(color = Color(0xFF90CAF9), label = "P15/P85")
        LegendItem(color = Color(0xFF42A5F5), label = "P50")
        LegendItem(color = Color(0xFFFF7043), label = "實測")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GrowthLineChart(
    records: List<GrowthRecordWithPercentile>,
    metricSelector: (GrowthRecordWithPercentile) -> Double,
    label: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary

    // 使用月齡作為 X 軸（簡化：以索引位置）
    val sorted = remember(records) { records.sortedBy { it.record.date } }
    val values = sorted.map(metricSelector)

    val minY = (values.minOrNull() ?: 0.0) * 0.95
    val maxY = (values.maxOrNull() ?: 1.0) * 1.05

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padLeft = 48f
        val padBottom = 32f
        val chartW = w - padLeft
        val chartH = h - padBottom

        fun xOf(i: Int): Float = padLeft + i * (chartW / (sorted.size - 1).coerceAtLeast(1))
        fun yOf(v: Double): Float = h - padBottom - ((v - minY) / (maxY - minY) * chartH).toFloat()

        // Y 軸格線
        val steps = 5
        for (i in 0..steps) {
            val v = minY + (maxY - minY) * i / steps
            val y = yOf(v)
            drawLine(Color.LightGray, Offset(padLeft, y), Offset(w, y), strokeWidth = 1f)
            drawText(
                textMeasurer, "%.1f".format(v),
                topLeft = Offset(0f, y - 8f),
                style = TextStyle(fontSize = 9.sp, color = Color.Gray)
            )
        }

        // 實測折線
        if (sorted.size >= 2) {
            val path = Path().apply {
                moveTo(xOf(0), yOf(values[0]))
                for (i in 1 until sorted.size) lineTo(xOf(i), yOf(values[i]))
            }
            drawPath(path, primaryColor, style = Stroke(width = 3f))
        }

        // 資料點
        sorted.forEachIndexed { i, item ->
            val x = xOf(i)
            val y = yOf(metricSelector(item))
            drawCircle(Color(0xFFFF7043), radius = 6f, center = Offset(x, y))
            // X 軸月齡標籤（簡化為日期末兩碼）
            val dateLabel = item.record.date.toString().takeLast(5)
            drawText(
                textMeasurer, dateLabel,
                topLeft = Offset(x - 14f, h - padBottom + 4f),
                style = TextStyle(fontSize = 8.sp, color = Color.Gray)
            )
        }
    }
}
