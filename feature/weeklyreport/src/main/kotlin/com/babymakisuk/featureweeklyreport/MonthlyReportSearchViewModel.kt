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

    var currentChildId: String = ""

    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<MonthlyReportEntity>> = searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank() || currentChildId.isBlank()) {
                flowOf(emptyList())
            } else {
                dao.searchByKeyword(currentChildId, query)
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
