package com.babymakisuk.featurehome

import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.GrowthRecord
import com.babymakisuk.coremodel.MedicalVisit
import com.babymakisuk.coremodel.Memo
import com.babymakisuk.coremodel.ToiletRecord
import com.babymakisuk.coremodel.VaccineReminder

data class HomeUiState(
    val boy: ChildProfile? = null,
    val girl: ChildProfile? = null,
    val boyLatestGrowth: GrowthRecord? = null,
    val girlLatestGrowth: GrowthRecord? = null,
    val boyGrowthRecords: List<GrowthRecord> = emptyList(),
    val girlGrowthRecords: List<GrowthRecord> = emptyList(),
    val todayMemos: Map<Long, List<Memo>> = emptyMap(),
    val boyLatestMedical: MedicalVisit? = null,
    val girlLatestMedical: MedicalVisit? = null,
    val boyToiletRecords: List<ToiletRecord> = emptyList(),
    val girlToiletRecords: List<ToiletRecord> = emptyList(),
    val boyNextVaccine: VaccineReminder? = null,
    val girlNextVaccine: VaccineReminder? = null,
    val isLoading: Boolean = false
)
