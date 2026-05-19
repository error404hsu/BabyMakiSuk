package com.babymakisuk.featurelibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.dao.MonthlyReportDao
import com.babymakisuk.coredata.dao.SystemReminderDao
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.MemoRepository
import com.babymakisuk.coremodel.Gender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val childRepo: ChildRepository,
    private val monthlyReportDao: MonthlyReportDao,
    private val aiInsightDao: AiInsightDao,
    private val systemReminderDao: SystemReminderDao,
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _selectedChildId = MutableStateFlow<Long?>(null)

    val selectedChildId: StateFlow<Long> = combine(
        childRepo.observeAll(),
        _selectedChildId
    ) { children, selectedId ->
        if (children.isEmpty()) -1L
        else selectedId
            ?: children.firstOrNull { it.gender == Gender.MALE }?.id
            ?: children.first().id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1L)

    private val selectedChildIdStr = selectedChildId.map { it.toString() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val weeklyLastUpdated: StateFlow<Long?> = selectedChildId.flatMapLatest { childId ->
        if (childId <= 0L) flowOf(null)
        else monthlyReportDao.getRecentReports(childId, 1).map { list ->
            list.firstOrNull()?.syncedAt
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _showMonthlyReportBadge = MutableStateFlow(false)
    val showMonthlyReportBadge: StateFlow<Boolean> = _showMonthlyReportBadge.asStateFlow()

    val aiInsightLastUpdated: StateFlow<Long?> = selectedChildIdStr.flatMapLatest { childId ->
        if (childId.isBlank() || childId == "-1") {
            aiInsightDao.getAllFlow().map { list -> list.firstOrNull()?.createdAt }
        } else {
            aiInsightDao.getByChildId(childId).map { list -> list.firstOrNull()?.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val systemReminderLastUpdated: StateFlow<Long?> = selectedChildId.flatMapLatest { childId ->
        if (childId <= 0L) flowOf(null)
        else systemReminderDao.getByChildIdIncludingGlobal(childId).map { list ->
            list.firstOrNull()?.createdAt
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val memoLastUpdated: StateFlow<Long?> = selectedChildId.flatMapLatest { childId ->
            if (childId <= 0L) flowOf(null)
            else memoRepository.observeByChildId(childId).map { list ->
                list.firstOrNull()?.createdAt
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        checkOverdueMonthlyReport()
    }

    private fun checkOverdueMonthlyReport() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val targetMonth = if (today.dayOfMonth > 7) {
                YearMonth.from(today).minusMonths(1)
            } else {
                YearMonth.from(today).minusMonths(2)
            }
            val exists = monthlyReportDao.existsForMonth(targetMonth.toString())
            _showMonthlyReportBadge.value = !exists
        }
    }

    fun selectChild(childId: Long) {
        _selectedChildId.value = childId
    }
}
