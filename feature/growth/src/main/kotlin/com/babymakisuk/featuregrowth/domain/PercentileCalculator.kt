package com.babymakisuk.featuregrowth.domain

import com.babymakisuk.coremodel.Gender
import kotlin.math.*

/**
 * WHO Child Growth Standards 窶・邁｡蛹・LMS 譟･陦ｨ豕輔・
 * L = Box-Cox power, M = median, S = coefficient of variation
 * 雉・侭萓・ｺ・ WHO (2006), 萓晄怦鮨｡ 0-60 譛医・
 * 豁､轤ｺ stub 雉・侭・帶ｭ｣蠑冗沿隲句ｾ・WHO 螳俶婿 CSV 譖ｿ謠・lmsData縲・
 */
object PercentileCalculator {

    data class LmsPoint(val l: Double, val m: Double, val s: Double)

    // --- Stub tables: (ageMonths -> LmsPoint)・梧ｭ｣蠑冗沿譖ｿ謠帷ぜ螳梧紛 WHO 陦ｨ ---
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

    enum class Metric { HEIGHT, WEIGHT }

    /**
     * 蝗槫さ 0~100 逧・卆蛻・ｽ肴丙蛟ｼ・亥屁謐ｨ莠泌・蛻ｰ謨ｴ謨ｸ・峨・
     * @param ageMonths 譛磯ｽ｡
     * @param value 驥乗ｸｬ蛟ｼ (cm 謌・kg)
     */
    fun percentile(gender: Gender, metric: Metric, ageMonths: Int, value: Double): Int {
        val table = when (metric) {
            Metric.HEIGHT -> if (gender == Gender.MALE) heightBoyTable else heightGirlTable
            Metric.WEIGHT -> if (gender == Gender.MALE) weightBoyTable else weightGirlTable
        }
        val lms = interpolateLms(table, ageMonths) ?: return -1
        val z = calcZ(lms, value)
        return (normalCdf(z) * 100).roundToInt().coerceIn(0, 100)
    }

    /**
     * 蝗槫さ萓帛恂陦ｨ郢ｪ陬ｽ逧・純閠・卆蛻・ｽ咲ｷ壽丙蛟ｼ・・3 / P15 / P50 / P85 / P97・峨・
     */
    fun referenceValues(gender: Gender, metric: Metric, ageMonths: Int): Map<Int, Double> {
        val table = when (metric) {
            Metric.HEIGHT -> if (gender == Gender.MALE) heightBoyTable else heightGirlTable
            Metric.WEIGHT -> if (gender == Gender.MALE) weightBoyTable else weightGirlTable
        }
        val lms = interpolateLms(table, ageMonths) ?: return emptyMap()
        val zScores = mapOf(3 to -1.881, 15 to -1.036, 50 to 0.0, 85 to 1.036, 97 to 1.881)
        return zScores.mapValues { (_, z) -> lmsToValue(lms, z) }
    }

    // ---- 蜈ｧ驛ｨ險育ｮ・----

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

    /** Hart (1968) 霑台ｼｼ蛟ｼ・檎ｲｾ蠎ｦ ﾂｱ0.0005 */
    private fun normalCdf(z: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * abs(z))
        val poly = t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        val pdf = exp(-0.5 * z * z) / sqrt(2 * PI)
        val cdf = 1.0 - pdf * poly
        return if (z >= 0) cdf else 1.0 - cdf
    }
}
