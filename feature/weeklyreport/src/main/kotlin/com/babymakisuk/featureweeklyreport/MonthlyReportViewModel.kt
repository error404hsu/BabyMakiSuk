package com.babymakisuk.featureweeklyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.MonthlyReportRepository
import com.babymakisuk.coremodel.MonthlyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val monthlyReportRepository: MonthlyReportRepository
) : ViewModel() {

    private val _childId = MutableStateFlow(0L)

    fun setChildId(id: Long) {
        _childId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val reports: StateFlow<List<MonthlyReport>> = _childId
        .flatMapLatest { childId ->
            // 不分孩子，一律顯示合併月報 (使用 0L 作為標記)
            monthlyReportRepository.getRecentReports(0L, limit = 50)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun generateReport() {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null

            val yearMonth = YearMonth.now()

            runCatching {
                monthlyReportRepository.generateMonthlyReport(yearMonth)
            }.onFailure {
                _errorMessage.value = it.message
            }

            _isGenerating.value = false
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
