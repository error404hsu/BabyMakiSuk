package com.babymakisuk.featurelibrary.shelf.monthly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.MonthlyReportDao
import com.babymakisuk.coredata.entity.MonthlyReportEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MonthlyShelfViewModel @Inject constructor(
    private val monthlyReportDao: MonthlyReportDao
) : ViewModel() {

    val reports: StateFlow<List<MonthlyReportEntity>> = monthlyReportDao.getRecentReports("merged", 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
