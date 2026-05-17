package com.babymakisuk.featuremedical.fever

import com.babymakisuk.coremodel.FeverRecord

sealed interface FeverUiState {
    data object Loading : FeverUiState
    data class Success(
        val records: List<FeverRecord>,
        val currentChildId: Long,
        /** 計時器是否運行中 */
        val timerRunning: Boolean = false,
        /** 計時器起始時刻（epoch millis） */
        val timerStartMs: Long? = null
    ) : FeverUiState {
        /** 最高熱 */
        val peakTemperature: Float?
            get() = records.maxOfOrNull { it.temperatureCelsius }

        /** 累計發燒分鐘 */
        val totalDurationMinutes: Int
            get() = records.sumOf { it.durationMinutes ?: 0 }
    }
    data class Error(val message: String) : FeverUiState
}
