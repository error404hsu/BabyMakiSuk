package com.babymakisuk.featuremedical.fever

import com.babymakisuk.coremodel.FeverRecord

sealed interface FeverUiState {
    data object Loading : FeverUiState
    data class Success(
        val episodes: List<FeverEpisode>,
        val currentChildId: Long
    ) : FeverUiState {
        val allRecords: List<FeverRecord> get() = episodes.flatMap { it.records }
        
        /** 最高熱 */
        val peakTemperature: Float?
            get() = allRecords.maxOfOrNull { it.temperatureCelsius }

        /** 總記錄數 */
        val totalRecordsCount: Int get() = allRecords.size
    }
    data class Error(val message: String) : FeverUiState
}

/**
 * 發燒病程：代表一段連續的發燒時間（相隔未超過 48 小時）。
 */
data class FeverEpisode(
    val id: String, // 可以用第一筆記錄的 ID
    val records: List<FeverRecord>,
    val startTime: Long,
    val endTime: Long
)
