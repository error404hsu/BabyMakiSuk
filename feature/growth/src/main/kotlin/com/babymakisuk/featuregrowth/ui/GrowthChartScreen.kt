package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile
import com.babymakisuk.featuregrowth.domain.PercentileCalculator
import kotlin.math.max
import kotlin.math.min

@Composable
fun GrowthChartScreen(records: List<GrowthRecordWithPercentile>) {
    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("尚無資料")
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("身高(cm)", "體重(kg)", "頭圍(cm)")

    // 頭圍 tab 僅在至少一筆紀錄有頭圍資料時才可點擊
    val hasHeadCircData = records.any { it.record.headCircumferenceCm != null }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        if (index != 2 || hasHeadCircData) selectedTab = index
                    },
                    enabled = index != 2 || hasHeadCircData,
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        PercentileLegend()
        Spacer(Modifier.height(8.dp))

        val metric = when (selectedTab) {
            0 -> PercentileCalculator.Metric.HEIGHT
            1 -> PercentileCalculator.Metric.WEIGHT
            else -> PercentileCalculator.Metric.HEAD_CIRC
        }

        // 頭圍圖表過濾掉無頭圍資料的紀錄
        val chartRecords = if (metric == PercentileCalculator.Metric.HEAD_CIRC) {
            records.filter { it.record.headCircumferenceCm != null }
        } else {
            records
        }

        GrowthLineChart(
            records = chartRecords,
            metric = metric,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LegendItem(Color(0xFFD6E8FA), "P15-P85")
        LegendItem(Color(0xFF4A5568), "P3/P97", dashed = true)
        LegendItem(Color(0xFF3182CE), "P15/P85", dashed = true)
        LegendItem(Color(0xFF2B6CB0), "P50")
        LegendItem(Color(0xFFDD6B20), "實測")
    }
}

@Composable
private fun LegendItem(color: Color, label: String, dashed: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            if (dashed) {
                drawCircle(color, radius = size.minDimension / 3)
                drawCircle(Color.Transparent, radius = size.minDimension / 2)
                drawArc(color, 0f, 90f, false, style = Stroke(width = 2f))
            } else {
                drawCircle(color)
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GrowthLineChart(
    records: List<GrowthRecordWithPercentile>,
    metric: PercentileCalculator.Metric,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("尚無頭圍資料", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val sorted = remember(records) { records.sortedBy { it.ageMonths } }
    val gender = sorted.first().gender

    val measuredValues = sorted.map {
        when (metric) {
            PercentileCalculator.Metric.HEIGHT    -> it.record.heightCm.toDouble()
            PercentileCalculator.Metric.WEIGHT    -> it.record.weightKg.toDouble()
            PercentileCalculator.Metric.HEAD_CIRC -> it.record.headCircumferenceCm?.toDouble() ?: 0.0
        }
    }

    val monthMin = sorted.minOf { it.ageMonths }
    val monthMaxRaw = sorted.maxOf { it.ageMonths }
    val monthMax = max(monthMaxRaw, monthMin + 1)
    val months = (monthMin..monthMax).toList()

    val ref3  = remember(sorted, metric) { buildReferenceMap(gender, metric, months, 3) }
    val ref15 = remember(sorted, metric) { buildReferenceMap(gender, metric, months, 15) }
    val ref50 = remember(sorted, metric) { buildReferenceMap(gender, metric, months, 50) }
    val ref85 = remember(sorted, metric) { buildReferenceMap(gender, metric, months, 85) }
    val ref97 = remember(sorted, metric) { buildReferenceMap(gender, metric, months, 97) }

    val referenceAll = buildList {
        addAll(ref3.values); addAll(ref15.values); addAll(ref50.values)
        addAll(ref85.values); addAll(ref97.values)
    }

    val minRef = referenceAll.minOrNull() ?: measuredValues.minOrNull() ?: 0.0
    val maxRef = referenceAll.maxOrNull() ?: measuredValues.maxOrNull() ?: 1.0
    val rawMin = min(measuredValues.minOrNull() ?: minRef, minRef)
    val rawMax = max(measuredValues.maxOrNull() ?: maxRef, maxRef)

    val minY = if (rawMin > 1.0) rawMin * 0.95 else 0.0
    val maxY = rawMax * 1.05

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padLeft = 52f
        val padRight = 8f
        val padTop = 16f
        val padBottom = 36f
        val chartW = w - padLeft - padRight
        val chartH = h - padTop - padBottom

        fun xOf(month: Int): Float {
            val ratio = (month - monthMin).toFloat() / (monthMax - monthMin).coerceAtLeast(1)
            return padLeft + ratio * chartW
        }
        fun yOf(value: Double): Float {
            val ratio = ((value - minY) / (maxY - minY)).toFloat()
            return h - padBottom - ratio * chartH
        }

        drawAxes(monthMin, monthMax, minY, maxY, ::xOf, ::yOf, textMeasurer)
        drawReferenceBand(months, ref85, ref15, ::xOf, ::yOf, Color(0xFFD6E8FA))
        drawReferenceLine(months, ref3,  ::xOf, ::yOf, Color(0xFF4A5568), dashed = true)
        drawReferenceLine(months, ref15, ::xOf, ::yOf, Color(0xFF63A4D0), dashed = true)
        drawReferenceLine(months, ref50, ::xOf, ::yOf, Color(0xFF2B6CB0), dashed = false)
        drawReferenceLine(months, ref85, ::xOf, ::yOf, Color(0xFF63A4D0), dashed = true)
        drawReferenceLine(months, ref97, ::xOf, ::yOf, Color(0xFF4A5568), dashed = true)
        drawMeasuredLine(sorted, metric, ::xOf, ::yOf)

        val label = when (metric) {
            PercentileCalculator.Metric.HEIGHT    -> "WHO 身高參考線 + 實測"
            PercentileCalculator.Metric.WEIGHT    -> "WHO 體重參考線 + 實測"
            PercentileCalculator.Metric.HEAD_CIRC -> "WHO 頭圍參考線 + 實測"
        }
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(padLeft, 0f),
            style = TextStyle(fontSize = 12.sp, color = Color.Gray)
        )
    }
}

private fun buildReferenceMap(
    gender: Gender,
    metric: PercentileCalculator.Metric,
    months: List<Int>,
    percentile: Int
): Map<Int, Double> =
    months.associateWith { month ->
        PercentileCalculator.referenceValues(gender, metric, month)[percentile] ?: 0.0
    }

private fun DrawScope.drawAxes(
    monthMin: Int,
    monthMax: Int,
    minY: Double,
    maxY: Double,
    xOf: (Int) -> Float,
    yOf: (Double) -> Float,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    val padBottom = 36f
    val padLeft = 52f

    drawLine(color = Color.Gray, start = Offset(padLeft, h - padBottom), end = Offset(w, h - padBottom), strokeWidth = 2f)
    drawLine(color = Color.Gray, start = Offset(padLeft, 16f), end = Offset(padLeft, h - padBottom), strokeWidth = 2f)

    for (i in 0..5) {
        val value = minY + (maxY - minY) * i / 5
        val y = yOf(value)
        drawLine(color = Color(0xFFE0E0E0), start = Offset(padLeft, y), end = Offset(w, y), strokeWidth = 1f)
        drawText(textMeasurer, "%.1f".format(value), Offset(0f, y - 8f), TextStyle(fontSize = 9.sp, color = Color.Gray))
    }

    val span = monthMax - monthMin
    val xStep = when { span <= 12 -> 2; span <= 24 -> 3; else -> 6 }
    var month = monthMin
    while (month <= monthMax) {
        val x = xOf(month)
        drawLine(color = Color(0xFFF0F0F0), start = Offset(x, 16f), end = Offset(x, h - padBottom), strokeWidth = 1f)
        drawText(textMeasurer, "${month}m", Offset(x - 10f, h - padBottom + 6f), TextStyle(fontSize = 9.sp, color = Color.Gray))
        month += xStep
    }
}

private fun DrawScope.drawReferenceBand(
    months: List<Int>, upper: Map<Int, Double>, lower: Map<Int, Double>,
    xOf: (Int) -> Float, yOf: (Double) -> Float, color: Color
) {
    if (months.size < 2) return
    val path = Path().apply {
        moveTo(xOf(months.first()), yOf(upper.getValue(months.first())))
        months.drop(1).forEach { lineTo(xOf(it), yOf(upper.getValue(it))) }
        months.asReversed().forEach { lineTo(xOf(it), yOf(lower.getValue(it))) }
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawReferenceLine(
    months: List<Int>, values: Map<Int, Double>,
    xOf: (Int) -> Float, yOf: (Double) -> Float, color: Color, dashed: Boolean
) {
    if (months.size < 2) return
    val path = Path().apply {
        moveTo(xOf(months.first()), yOf(values.getValue(months.first())))
        months.drop(1).forEach { lineTo(xOf(it), yOf(values.getValue(it))) }
    }
    drawPath(path, color, style = Stroke(width = 2f, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null))
}

private fun DrawScope.drawMeasuredLine(
    records: List<GrowthRecordWithPercentile>,
    metric: PercentileCalculator.Metric,
    xOf: (Int) -> Float,
    yOf: (Double) -> Float
) {
    if (records.isEmpty()) return
    val path = Path().apply {
        val first = records.first()
        val firstVal = first.metricValue(metric)
        moveTo(xOf(first.ageMonths), yOf(firstVal))
        records.drop(1).forEach { lineTo(xOf(it.ageMonths), yOf(it.metricValue(metric))) }
    }
    drawPath(path, Color(0xFFDD6B20), style = Stroke(width = 4f))
    records.forEach { item ->
        val v = item.metricValue(metric)
        drawCircle(Color.White,  radius = 7f, center = Offset(xOf(item.ageMonths), yOf(v)))
        drawCircle(Color(0xFFDD6B20), radius = 5f, center = Offset(xOf(item.ageMonths), yOf(v)))
    }
}

private fun GrowthRecordWithPercentile.metricValue(metric: PercentileCalculator.Metric): Double =
    when (metric) {
        PercentileCalculator.Metric.HEIGHT    -> record.heightCm.toDouble()
        PercentileCalculator.Metric.WEIGHT    -> record.weightKg.toDouble()
        PercentileCalculator.Metric.HEAD_CIRC -> record.headCircumferenceCm?.toDouble() ?: 0.0
    }
