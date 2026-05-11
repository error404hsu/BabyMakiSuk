package com.babymakisuk.featuregrowth.domain

import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.GrowthRepository
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.GrowthRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class GrowthRecordWithPercentile(
    val record: GrowthRecord,
    val ageMonths: Int,
    val gender: Gender,
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
        val ageMonths = ChronoUnit.MONTHS.between(child.birthday, date)
            .toInt()
            .coerceAtLeast(0)

        val hp = PercentileCalculator.percentile(
            gender = child.gender,
            metric = PercentileCalculator.Metric.HEIGHT,
            ageMonths = ageMonths,
            value = heightCm.toDouble()
        )

        val wp = PercentileCalculator.percentile(
            gender = child.gender,
            metric = PercentileCalculator.Metric.WEIGHT,
            ageMonths = ageMonths,
            value = weightKg.toDouble()
        )

        return GrowthRecordWithPercentile(
            record = this,
            ageMonths = ageMonths,
            gender = child.gender,
            heightPercentile = hp,
            weightPercentile = wp
        )
    }
}

class SaveGrowthRecord @Inject constructor(
    private val repo: GrowthRepository
) {
    suspend operator fun invoke(record: GrowthRecord): Long = repo.save(record)
}

class DeleteGrowthRecord @Inject constructor(
    private val repo: GrowthRepository
) {
    suspend operator fun invoke(record: GrowthRecord) = repo.delete(record)
}
