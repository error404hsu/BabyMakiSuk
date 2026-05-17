package com.babymakisuk.featuremedical.fever

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.FeverDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.FeverRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeverViewModel @Inject constructor(
    private val feverDao: FeverDao,
    private val childRepository: ChildRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeverUiState>(FeverUiState.Loading)
    val uiState: StateFlow<FeverUiState> = _uiState.asStateFlow()

    fun init(childId: Long) {
        viewModelScope.launch {
            feverDao.observeByChildId(childId).collectLatest { entities ->
                val records = entities.map { it.toDomain() }
                val current = _uiState.value
                _uiState.value = FeverUiState.Success(
                    records = records,
                    currentChildId = childId,
                    timerRunning = (current as? FeverUiState.Success)?.timerRunning ?: false,
                    timerStartMs = (current as? FeverUiState.Success)?.timerStartMs
                )
            }
        }
    }

    fun addRecord(record: FeverRecord) {
        val childId = ((_uiState.value) as? FeverUiState.Success)?.currentChildId ?: return
        viewModelScope.launch {
            feverDao.insert(record.copy(childId = childId).toEntity())
        }
    }

    fun deleteRecord(record: FeverRecord) {
        viewModelScope.launch {
            feverDao.deleteById(record.id)
        }
    }

    /** 開始發燒計時器 */
    fun startTimer() {
        _uiState.update {
            if (it is FeverUiState.Success)
                it.copy(timerRunning = true, timerStartMs = System.currentTimeMillis())
            else it
        }
    }

    /** 停止計時器，回傳經過分鐘數供 Dialog 預填 */
    fun stopTimer(): Int {
        val state = _uiState.value as? FeverUiState.Success ?: return 0
        val elapsed = state.timerStartMs?.let {
            ((System.currentTimeMillis() - it) / 60_000).toInt().coerceAtLeast(1)
        } ?: 0
        _uiState.update { (it as? FeverUiState.Success)?.copy(timerRunning = false, timerStartMs = null) ?: it }
        return elapsed
    }
}
