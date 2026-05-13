package com.babymakisuk.featureweeklyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.WeeklyReportDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.repository.WeeklyReportRepository
import com.babymakisuk.coremodel.WeeklyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    private val weeklyReportRepository: WeeklyReportRepository,
    private val weeklyReportDao: WeeklyReportDao
) : ViewModel() {

    private val _childId = MutableStateFlow("")

    fun setChildId(id: String) {
        _childId.value = id
    }

    val reports: StateFlow<List<WeeklyReport>> = _childId
        .flatMapLatest { childId ->
            if (childId.isBlank()) flowOf(emptyList())
            else weeklyReportDao.getRecentReports(childId, limit = 50).map { entities ->
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

            val today = LocalDate.now()
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            runCatching {
                weeklyReportRepository.generateWeeklyReport(childId, weekStart)
            }.onFailure {
                _errorMessage.value = it.message
            }

            _isGenerating.value = false
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            try {
                weeklyReportDao.getById(reportId)?.let { entity ->
                    weeklyReportDao.delete(entity)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
