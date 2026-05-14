package com.babymakisuk.featuregrowth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.GrowthRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class GrowthEditUiState(
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
)

@HiltViewModel
class GrowthEditViewModel @Inject constructor(
    private val growthDao: GrowthDao,
    private val childRepository: ChildRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GrowthEditUiState())
    val uiState: StateFlow<GrowthEditUiState> = _uiState.asStateFlow()

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private var existingRecordId: Long = 0L

    fun initialize(recordId: Long, childId: Long) {
        viewModelScope.launch {
            if (recordId > 0L) {
                existingRecordId = recordId
                val entity = growthDao.getById(recordId)
                if (entity != null) {
                    val record = entity.toDomain()
                    _uiState.update {
                        it.copy(
                            childId = record.childId,
                            heightCm = record.heightCm.toString(),
                            weightKg = record.weightKg.toString(),
                            headCircumferenceCm = record.headCircumferenceCm?.toString() ?: "",
                            note = record.note,
                            aiSuggestion = record.aiSuggestion,
                            date = record.date,
                            dateStr = record.date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"))
                        )
                    }
                }
            } else {
                existingRecordId = 0L
                _uiState.update { it.copy(childId = childId) }
            }
        }
    }

    fun updateHeight(v: String) { _uiState.update { it.copy(heightCm = v, heightError = false) } }
    fun updateWeight(v: String) { _uiState.update { it.copy(weightKg = v, weightError = false) } }
    fun updateHeadCircumference(v: String) { _uiState.update { it.copy(headCircumferenceCm = v) } }
    fun updateNote(v: String) { _uiState.update { it.copy(note = v) } }
    fun updateAiSuggestion(v: String) { _uiState.update { it.copy(aiSuggestion = v) } }

    fun updateDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                date = date,
                dateStr = date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"))
            )
        }
    }

    fun save() {
        val state = _uiState.value
        val h = state.heightCm.toFloatOrNull()
        val w = state.weightKg.toFloatOrNull()

        if (h == null || h <= 0) {
            _uiState.update { it.copy(heightError = true) }
            return
        }
        if (w == null || w <= 0) {
            _uiState.update { it.copy(weightError = true) }
            return
        }

        viewModelScope.launch {
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
            _savedEvent.emit(Unit)
        }
    }
}
