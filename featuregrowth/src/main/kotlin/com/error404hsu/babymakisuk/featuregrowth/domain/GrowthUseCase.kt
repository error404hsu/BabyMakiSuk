package com.error404hsu.babymakisuk.featuregrowth.domain

import com.error404hsu.babymakisuk.coredata.repository.ChildRepository
import com.error404hsu.babymakisuk.coredata.repository.GrowthRepository
import com.error404hsu.babymakisuk.coremodel.ChildProfile
import com.error404hsu.babymakisuk.coremodel.GrowthRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class GrowthRecordWithPercentile(
    val record: GrowthRecord,
    val heightPercentile: Int,
    val weightPercentile: Int
)

class ObserveGrowthWithPercentile @Inject constructor(
    private val growthRepo: GrowthRepository,
    private val childRepo: ChildRepository
) {
    operator fun invoke(childId: Long): Flow<List<GrowthRecordWithPercentile>> =
        combine(
            growthRepo.observeByChild(childId),
            childRepo.observeAll()
        ) { records, children ->
            val child = children.firstOrNull { it.id == childId } ?: return@combine emptyList()
            records.map { it.withPercentile(child) }
        }

    private fun GrowthRecord.withPercentile(child: ChildProfile): GrowthRecordWithPercentile {
        val ageMonths = ChronoUnit.MONTHS.between(child.birthday, date).toInt().coerceAtLeast(0)
        val hp = PercentileCalculator.percentile(
            child.gender, PercentileCalculator.Metric.HEIGHT, ageMonths, heightCm.toDouble()
        )
        val wp = PercentileCalculator.percentile(
            child.gender, PercentileCalculator.Metric.WEIGHT, ageMonths, weightKg.toDouble()
        )
        return GrowthRecordWithPercentile(this, hp, wp)
    }
}

class SaveGrowthRecord @Inject constructor(private val repo: GrowthRepository) {
    suspend operator fun invoke(record: GrowthRecord): Long = repo.save(record)
}

class DeleteGrowthRecord @Inject constructor(private val repo: GrowthRepository) {
    suspend operator fun invoke(record: GrowthRecord) = repo.delete(record)
}
