package com.babymakisuk.featurelibrary.shelf.weekly

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.WeeklyReportDao
import com.babymakisuk.coredata.entity.WeeklyReportEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeeklyShelfViewModel @Inject constructor(
    private val weeklyReportDao: WeeklyReportDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val childId: String = savedStateHandle.get<String>("childId") ?: ""

    val reports: StateFlow<List<WeeklyReportEntity>> = flowOf(childId)
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf(emptyList())
            else weeklyReportDao.getRecentReports(cid, 20)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
