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

/**
 * 月報書庫 ViewModel
 *
 * 月報為「合併式」設計（reportId = merged_yyyy-Mmm），涵蓋所有孩子的當月紀錄，
 * 因此列表應顯示全部月報，不依 childId 篩選。
 */
@HiltViewModel
class MonthlyShelfViewModel @Inject constructor(
    private val monthlyReportDao: MonthlyReportDao
) : ViewModel() {

    val reports: StateFlow<List<MonthlyReportEntity>> = monthlyReportDao.getAllRecent(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
