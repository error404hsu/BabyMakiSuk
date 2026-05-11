package com.babymakisuk.featuregrowth

import com.babymakisuk.coremodel.Gender
import com.babymakisuk.featuregrowth.domain.PercentileCalculator
import org.junit.Assert.*
import org.junit.Test

class PercentileCalculatorTest {

    @Test
    fun `median height boy 12m should be near P50`() {
        // WHO median height for boy at 12m = 75.7 cm 竊・P50
        val pct = PercentileCalculator.percentile(
            Gender.MALE, PercentileCalculator.Metric.HEIGHT, 12, 75.7
        )
        assertTrue("Expected P45-P55, got P$pct", pct in 45..55)
    }

    @Test
    fun `very low height should return low percentile`() {
        val pct = PercentileCalculator.percentile(
            Gender.MALE, PercentileCalculator.Metric.HEIGHT, 12, 68.0
        )
        assertTrue("Expected P < 10, got P$pct", pct < 10)
    }

    @Test
    fun `very high height should return high percentile`() {
        val pct = PercentileCalculator.percentile(
            Gender.MALE, PercentileCalculator.Metric.HEIGHT, 12, 84.0
        )
        assertTrue("Expected P > 90, got P$pct", pct > 90)
    }

    @Test
    fun `negative value should be clamped to 0`() {
        val pct = PercentileCalculator.percentile(
            Gender.FEMALE, PercentileCalculator.Metric.WEIGHT, 6, 0.1
        )
        assertEquals(0, pct)
    }

    @Test
    fun `reference values should include 5 percentile levels`() {
        val refs = PercentileCalculator.referenceValues(
            Gender.MALE, PercentileCalculator.Metric.HEIGHT, 12
        )
        assertEquals(5, refs.size)
        assertTrue(refs[3]!! < refs[50]!!)
        assertTrue(refs[50]!! < refs[97]!!)
    }
}
