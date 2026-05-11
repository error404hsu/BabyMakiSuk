package com.babymakisuk.featuregrowth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coremodel.GrowthRecord
import com.babymakisuk.featuregrowth.domain.DeleteGrowthRecord
import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile
import com.babymakisuk.featuregrowth.domain.ObserveGrowthWithPercentile
import com.babymakisuk.featuregrowth.domain.SaveGrowthRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface GrowthUiState {
    data object Loading : GrowthUiState
    data class Success(
        val records: List<GrowthRecordWithPercentile>,
        val showChart: Boolean = false
    ) : GrowthUiState
    data class Error(val message: String) : GrowthUiState
}

@HiltViewModel
class GrowthViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeGrowth: ObserveGrowthWithPercentile,
    private val saveGrowth: SaveGrowthRecord,
    private val deleteGrowth: DeleteGrowthRecord
) : ViewModel() {

    // childId 逕ｱ Navigation 蛯ｳ蜈･・幃占ｨｭ 1L 萓・HomeScreen 蠢ｫ騾滓ｸｬ隧ｦ
    private val childId: Long = savedStateHandle["childId"] ?: 1L

    val uiState: StateFlow<GrowthUiState> = observeGrowth(childId)
        .map<List<GrowthRecordWithPercentile>, GrowthUiState> { GrowthUiState.Success(it) }
        .catch { emit(GrowthUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthUiState.Loading)

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    fun openForm() { _showForm.value = true }
    fun closeForm() { _showForm.value = false }

    fun saveRecord(
        heightCm: Float,
        weightKg: Float,
        headCircCm: Float?,
        date: LocalDate,
        note: String
    ) {
        viewModelScope.launch {
            saveGrowth(
                GrowthRecord(
                    childId = childId,
                    date = date,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    headCircumferenceCm = headCircCm,
                    note = note
                )
            )
            _showForm.value = false
        }
    }

    fun deleteRecord(record: GrowthRecord) {
        viewModelScope.launch { deleteGrowth(record) }
    }
}
