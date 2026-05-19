package com.babymakisuk.featureweeklyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.MonthlyReportDao
import com.babymakisuk.coredata.entity.MonthlyReportEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MonthlyReportSearchViewModel @Inject constructor(
    private val dao: MonthlyReportDao
) : ViewModel() {

    var currentChildId: Long = 0L

    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<MonthlyReportEntity>> = searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                // 月報目前採合併制，固定使用 0L 作為 childId 搜尋
                val safeQuery = query.filter { it !in setOf('*', '"', '-', '+', '(', ')') }
                dao.searchByKeyword(0L, safeQuery)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun onQueryChange(query: String) {
        searchQuery.value = query
    }
}
