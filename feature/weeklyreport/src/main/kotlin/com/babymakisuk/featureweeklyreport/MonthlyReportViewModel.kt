package com.babymakisuk.featureweeklyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.DataRetentionRepository
import com.babymakisuk.coredata.repository.MonthlyReportRepository
import com.babymakisuk.coremodel.MonthlyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

sealed interface ReportGenerationState {
    data object Idle : ReportGenerationState
    data object Generating : ReportGenerationState
    data object NoNetwork : ReportGenerationState
    data class Error(val message: String) : ReportGenerationState
}

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val monthlyReportRepository: MonthlyReportRepository,
    private val retentionRepository: DataRetentionRepository,
) : ViewModel() {

    private val _childId = MutableStateFlow(0L)

    fun setChildId(id: Long) {
        _childId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val reports: StateFlow<List<MonthlyReport>> = _childId
        .flatMapLatest { childId ->
            monthlyReportRepository.getRecentReports(childId, limit = 50)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _generationState = MutableStateFlow<ReportGenerationState>(ReportGenerationState.Idle)
    val generationState: StateFlow<ReportGenerationState> = _generationState.asStateFlow()

    fun generateReport() {
        viewModelScope.launch {
            val yearMonth = YearMonth.now()

            _isGenerating.value = true
            _errorMessage.value = null
            _generationState.value = ReportGenerationState.Generating

            runCatching {
                monthlyReportRepository.generateMonthlyReport(yearMonth)
            }.onSuccess {
                retentionRepository.cleanRawDataForMonth(yearMonth)
            }.onFailure {
                val message = it.message ?: "生成失敗"
                _errorMessage.value = message
                _generationState.value = ReportGenerationState.Error(message)
            }

            _isGenerating.value = false
            if (_generationState.value is ReportGenerationState.Generating) {
                _generationState.value = ReportGenerationState.Idle
            }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            try {
                monthlyReportRepository.deleteReport(reportId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
