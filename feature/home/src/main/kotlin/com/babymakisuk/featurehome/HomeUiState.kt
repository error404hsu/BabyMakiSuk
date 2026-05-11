package com.babymakisuk.featurehome

import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.DailyLog
import com.babymakisuk.coremodel.GrowthRecord
import com.babymakisuk.coremodel.VaccineRecord

data class HomeUiState(
    val boy: ChildProfile? = null,
    val girl: ChildProfile? = null,
    val boyLatestGrowth: GrowthRecord? = null,
    val girlLatestGrowth: GrowthRecord? = null,
    val boyGrowthRecords: List<GrowthRecord> = emptyList(),
    val girlGrowthRecords: List<GrowthRecord> = emptyList(),
    val boyTodayLog: DailyLog? = null,
    val girlTodayLog: DailyLog? = null,
    val upcomingVaccines: List<Pair<ChildProfile, VaccineRecord>> = emptyList(),
    val isLoading: Boolean = false
)
