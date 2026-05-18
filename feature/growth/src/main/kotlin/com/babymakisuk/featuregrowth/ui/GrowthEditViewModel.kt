package com.babymakisuk.featuregrowth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.GrowthRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed interface GrowthEditUiEvent {
    data object Saved : GrowthEditUiEvent
    data class ValidationError(val message: String) : GrowthEditUiEvent
}

sealed interface GrowthEditUiState {
    data object Loading : GrowthEditUiState
    data class Ready(
        val childId: Long = -1L,
        val heightCm: String = "",
        val weightKg: String = "",
        val headCircumferenceCm: String = "",
        val note: String = "",
        val aiSuggestion: String = "",
        val date: LocalDate = LocalDate.now(),
        val dateStr: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy / MM / dd")),
        val heightError: Boolean = false,
        val weightError: Boolean = false
    ) : GrowthEditUiState
    data class Saving(val form: GrowthEditUiState.Ready) : GrowthEditUiState
}

@HiltViewModel
class GrowthEditViewModel @Inject constructor(
    private val growthDao: GrowthDao,
    private val childRepository: ChildRepository
) : ViewModel() {
    // TODO: Migrate from direct GrowthDao to GrowthRepository (currently lacks getById).

    private val _uiState = MutableStateFlow<GrowthEditUiState>(GrowthEditUiState.Loading)
    val uiState: StateFlow<GrowthEditUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GrowthEditUiEvent>()
    val events: SharedFlow<GrowthEditUiEvent> = _events.asSharedFlow()

    private var existingRecordId: Long = 0L

    fun initialize(recordId: Long, childId: Long) {
        viewModelScope.launch {
            if (recordId > 0L) {
                existingRecordId = recordId
                val entity = growthDao.getById(recordId)
                if (entity != null) {
                    val record = entity.toDomain()
                    _uiState.value = GrowthEditUiState.Ready(
                        childId = record.childId,
                        heightCm = record.heightCm.toString(),
                        weightKg = record.weightKg.toString(),
                        headCircumferenceCm = record.headCircumferenceCm?.toString() ?: "",
                        note = record.note,
                        aiSuggestion = record.aiSuggestion ?: "",
                        date = record.date,
                        dateStr = record.date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"))
                    )
                } else {
                    _uiState.value = GrowthEditUiState.Ready(childId = childId)
                }
            } else {
                existingRecordId = 0L
                _uiState.value = GrowthEditUiState.Ready(childId = childId)
            }
        }
    }

    fun updateHeight(v: String) {
        _uiState.update { (it as? GrowthEditUiState.Ready)?.copy(heightCm = v, heightError = false) ?: it }
    }
    fun updateWeight(v: String) {
        _uiState.update { (it as? GrowthEditUiState.Ready)?.copy(weightKg = v, weightError = false) ?: it }
    }
    fun updateHeadCircumference(v: String) {
        _uiState.update { (it as? GrowthEditUiState.Ready)?.copy(headCircumferenceCm = v) ?: it }
    }
    fun updateNote(v: String) {
        _uiState.update { (it as? GrowthEditUiState.Ready)?.copy(note = v) ?: it }
    }
    fun updateAiSuggestion(v: String) {
        _uiState.update { (it as? GrowthEditUiState.Ready)?.copy(aiSuggestion = v) ?: it }
    }

    fun updateDate(date: LocalDate) {
        _uiState.update {
            (it as? GrowthEditUiState.Ready)?.copy(
                date = date,
                dateStr = date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"))
            ) ?: it
        }
    }

    fun save() {
        val state = _uiState.value as? GrowthEditUiState.Ready ?: return
        val h = state.heightCm.toFloatOrNull()
        val w = state.weightKg.toFloatOrNull()

        if (h == null || h <= 0) {
            _uiState.update { (it as? GrowthEditUiState.Ready)?.copy(heightError = true) ?: it }
            return
        }
        if (w == null || w <= 0) {
            _uiState.update { (it as? GrowthEditUiState.Ready)?.copy(weightError = true) ?: it }
            return
        }

        _uiState.value = GrowthEditUiState.Saving(form = state)

        viewModelScope.launch {
            try {
                val record = GrowthRecord(
                    id = existingRecordId,
                    childId = state.childId,
                    date = state.date,
                    heightCm = h,
                    weightKg = w,
                    headCircumferenceCm = state.headCircumferenceCm.toFloatOrNull(),
                    note = state.note,
                    aiSuggestion = state.aiSuggestion
                )
                growthDao.upsert(record.toEntity())
                _events.emit(GrowthEditUiEvent.Saved)
            } catch (e: Exception) {
                _uiState.value = GrowthEditUiState.Ready(
                    childId = state.childId,
                    heightCm = state.heightCm,
                    weightKg = state.weightKg,
                    headCircumferenceCm = state.headCircumferenceCm,
                    note = state.note,
                    aiSuggestion = state.aiSuggestion,
                    date = state.date,
                    dateStr = state.dateStr,
                    heightError = state.heightError,
                    weightError = state.weightError
                )
                _events.emit(GrowthEditUiEvent.ValidationError(e.message ?: "儲存失敗"))
            }
        }
    }
}
