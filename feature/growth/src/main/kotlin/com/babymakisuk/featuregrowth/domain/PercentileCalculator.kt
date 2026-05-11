package com.babymakisuk.featuregrowth.domain

import com.babymakisuk.coremodel.Gender
import kotlin.math.*

/**
 * WHO Child Growth Standards — LMS 計算工具
 * L = Box-Cox power, M = median, S = coefficient of variation
 * 參考來源: WHO (2006), 涵蓋 0-60 月
 * 注意: 目前為 stub 資料，正式版請替換成 WHO 官方完整 CSV → lmsData
 */
object PercentileCalculator {

    data class LmsPoint(val l: Double, val m: Double, val s: Double)

    // --- 身高 ---
    private val heightBoyTable: Map<Int, LmsPoint> = mapOf(
        0  to LmsPoint(1.0, 49.9, 0.0379),
        3  to LmsPoint(1.0, 62.4, 0.0353),
        6  to LmsPoint(1.0, 68.0, 0.0348),
        12 to LmsPoint(1.0, 75.7, 0.0347),
        18 to LmsPoint(1.0, 82.3, 0.0353),
        24 to LmsPoint(1.0, 87.8, 0.0357),
        36 to LmsPoint(1.0, 96.1, 0.0360),
        48 to LmsPoint(1.0, 103.3, 0.0364),
        60 to LmsPoint(1.0, 110.0, 0.0366)
    )
    private val heightGirlTable: Map<Int, LmsPoint> = mapOf(
        0  to LmsPoint(1.0, 49.1, 0.0379),
        3  to LmsPoint(1.0, 60.9, 0.0362),
        6  to LmsPoint(1.0, 66.6, 0.0354),
        12 to LmsPoint(1.0, 74.0, 0.0356),
        18 to LmsPoint(1.0, 80.7, 0.0363),
        24 to LmsPoint(1.0, 86.4, 0.0369),
        36 to LmsPoint(1.0, 95.1, 0.0372),
        48 to LmsPoint(1.0, 102.7, 0.0374),
        60 to LmsPoint(1.0, 109.4, 0.0375)
    )

    // --- 體重 ---
    private val weightBoyTable: Map<Int, LmsPoint> = mapOf(
        0  to LmsPoint(0.3487, 3.3464, 0.14602),
        3  to LmsPoint(0.2671, 6.3762, 0.11980),
        6  to LmsPoint(0.1714, 7.9340, 0.11590),
        12 to LmsPoint(0.2282, 9.6479, 0.11380),
        18 to LmsPoint(0.4053, 11.0946, 0.11200),
        24 to LmsPoint(0.5798, 12.5266, 0.11010),
        36 to LmsPoint(0.7814, 14.3131, 0.10940),
        48 to LmsPoint(0.9261, 16.3066, 0.10940),
        60 to LmsPoint(1.0421, 18.3407, 0.10800)
    )
    private val weightGirlTable: Map<Int, LmsPoint> = mapOf(
        0  to LmsPoint(0.3809, 3.2322, 0.14171),
        3  to LmsPoint(0.2281, 5.8458, 0.12960),
        6  to LmsPoint(0.1499, 7.2972, 0.12460),
        12 to LmsPoint(0.1883, 8.9481, 0.12250),
        18 to LmsPoint(0.3708, 10.2074, 0.12110),
        24 to LmsPoint(0.5490, 11.5534, 0.11900),
        36 to LmsPoint(0.7693, 13.9102, 0.11570),
        48 to LmsPoint(0.9349, 15.9658, 0.11480),
        60 to LmsPoint(1.0641, 18.2479, 0.11380)
    )

    // --- 頭圍 (WHO 2006 LMS stub, 0-60 月) ---
    private val headCircBoyTable: Map<Int, LmsPoint> = mapOf(
        0  to LmsPoint(1.0, 34.46, 0.03686),
        3  to LmsPoint(1.0, 40.50, 0.03132),
        6  to LmsPoint(1.0, 43.32, 0.02996),
        9  to LmsPoint(1.0, 45.00, 0.02956),
        12 to LmsPoint(1.0, 46.32, 0.02938),
        18 to LmsPoint(1.0, 47.99, 0.02892),
        24 to LmsPoint(1.0, 49.10, 0.02870),
        36 to LmsPoint(1.0, 50.39, 0.02843),
        48 to LmsPoint(1.0, 51.22, 0.02827),
        60 to LmsPoint(1.0, 51.92, 0.02816)
    )
    private val headCircGirlTable: Map<Int, LmsPoint> = mapOf(
        0  to LmsPoint(1.0, 33.81, 0.03533),
        3  to LmsPoint(1.0, 38.95, 0.03132),
        6  to LmsPoint(1.0, 42.00, 0.02996),
        9  to LmsPoint(1.0, 43.71, 0.02956),
        12 to LmsPoint(1.0, 44.95, 0.02938),
        18 to LmsPoint(1.0, 46.67, 0.02892),
        24 to LmsPoint(1.0, 47.82, 0.02870),
        36 to LmsPoint(1.0, 49.10, 0.02843),
        48 to LmsPoint(1.0, 49.95, 0.02827),
        60 to LmsPoint(1.0, 50.63, 0.02816)
    )

    enum class Metric { HEIGHT, WEIGHT, HEAD_CIRC }

    /**
     * 回傳 0~100 的百分位數值（Hart 近似常態累積分佈）
     * @param ageMonths 月齡
     * @param value 實測值 (cm 或 kg)
     */
    fun percentile(gender: Gender, metric: Metric, ageMonths: Int, value: Double): Int {
        val table = tableFor(gender, metric)
        val lms = interpolateLms(table, ageMonths) ?: return -1
        val z = calcZ(lms, value)
        return (normalCdf(z) * 100).roundToInt().coerceIn(0, 100)
    }

    /**
     * 回傳參考曲線指定百分位對應數值（P3 / P15 / P50 / P85 / P97）
     */
    fun referenceValues(gender: Gender, metric: Metric, ageMonths: Int): Map<Int, Double> {
        val table = tableFor(gender, metric)
        val lms = interpolateLms(table, ageMonths) ?: return emptyMap()
        val zScores = mapOf(3 to -1.881, 15 to -1.036, 50 to 0.0, 85 to 1.036, 97 to 1.881)
        return zScores.mapValues { (_, z) -> lmsToValue(lms, z) }
    }

    private fun tableFor(gender: Gender, metric: Metric): Map<Int, LmsPoint> = when (metric) {
        Metric.HEIGHT    -> if (gender == Gender.MALE) heightBoyTable else heightGirlTable
        Metric.WEIGHT    -> if (gender == Gender.MALE) weightBoyTable else weightGirlTable
        Metric.HEAD_CIRC -> if (gender == Gender.MALE) headCircBoyTable else headCircGirlTable
    }

    // ---- 內部計算 ----

    private fun interpolateLms(table: Map<Int, LmsPoint>, ageMonths: Int): LmsPoint? {
        val sorted = table.keys.sorted()
        val lo = sorted.lastOrNull { it <= ageMonths } ?: return table[sorted.first()]
        val hi = sorted.firstOrNull { it > ageMonths } ?: return table[sorted.last()]
        if (lo == hi) return table[lo]
        val t = (ageMonths - lo).toDouble() / (hi - lo)
        val a = table[lo]!!
        val b = table[hi]!!
        return LmsPoint(
            l = a.l + t * (b.l - a.l),
            m = a.m + t * (b.m - a.m),
            s = a.s + t * (b.s - a.s)
        )
    }

    private fun calcZ(lms: LmsPoint, value: Double): Double =
        if (abs(lms.l) < 1e-6) ln(value / lms.m) / lms.s
        else ((value / lms.m).pow(lms.l) - 1) / (lms.l * lms.s)

    private fun lmsToValue(lms: LmsPoint, z: Double): Double =
        if (abs(lms.l) < 1e-6) lms.m * exp(lms.s * z)
        else lms.m * (1 + lms.l * lms.s * z).pow(1.0 / lms.l)

    /** Hart (1968) 近似值，精度 ±0.0005 */
    private fun normalCdf(z: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * abs(z))
        val poly = t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        val pdf = exp(-0.5 * z * z) / sqrt(2 * PI)
        val cdf = 1.0 - pdf * poly
        return if (z >= 0) cdf else 1.0 - cdf
    }
}
