package com.babymakisuk.featureweeklyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.MonthlyReportDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.repository.MonthlyReportRepository
import com.babymakisuk.coremodel.MonthlyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val monthlyReportRepository: MonthlyReportRepository,
    private val monthlyReportDao: MonthlyReportDao
) : ViewModel() {

    private val _childId = MutableStateFlow("")

    fun setChildId(id: String) {
        _childId.value = id
    }

    val reports: StateFlow<List<MonthlyReport>> = _childId
        .flatMapLatest { childId ->
            if (childId.isBlank()) flowOf(emptyList())
            else monthlyReportDao.getRecentReports(childId, limit = 50).map { entities ->
                entities.map { it.toDomain() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun generateReport() {
        viewModelScope.launch {
            val childIdStr = _childId.value
            if (childIdStr.isBlank()) return@launch
            val childId = childIdStr.toLongOrNull() ?: return@launch

            _isGenerating.value = true
            _errorMessage.value = null

            val yearMonth = YearMonth.now()

            runCatching {
                monthlyReportRepository.generateMonthlyReport(childId, yearMonth)
            }.onFailure {
                _errorMessage.value = it.message
            }

            _isGenerating.value = false
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            try {
                monthlyReportDao.getById(reportId)?.let { entity ->
                    monthlyReportDao.delete(entity)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
