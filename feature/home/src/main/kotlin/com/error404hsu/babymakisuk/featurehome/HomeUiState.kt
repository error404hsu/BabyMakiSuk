package com.error404hsu.babymakisuk.featurehome

import com.error404hsu.babymakisuk.coremodel.ChildProfile
import com.error404hsu.babymakisuk.coremodel.DailyLog
import com.error404hsu.babymakisuk.coremodel.GrowthRecord
import com.error404hsu.babymakisuk.coremodel.VaccineRecord

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
