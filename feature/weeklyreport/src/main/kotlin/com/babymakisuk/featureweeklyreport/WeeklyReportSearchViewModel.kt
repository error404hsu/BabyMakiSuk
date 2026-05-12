package com.babymakisuk.featureweeklyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.WeeklyReportDao
import com.babymakisuk.coredata.entity.WeeklyReportFts
import com.babymakisuk.coredata.entity.WeeklyReportEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * WeeklyReportSearchViewModel — Sprint 3
 *
 * 使用 FTS 全文搜尋週報，debounce 300ms 避免每次按鍵都觸發查詢。
 * 搜尋結果為 WeeklyReportEntity（含 aiSummary 供 Highlight 使用）。
 */
@HiltViewModel
class WeeklyReportSearchViewModel @Inject constructor(
    private val dao: WeeklyReportDao
) : ViewModel() {

    /** 目前由外部設定（例如從導航參數帶入），預設空字串顯示所有 */
    var currentChildId: String = ""

    /** 使用者輸入的搜尋關鍵字 */
    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")

    /**
     * FTS 搜尋結果，debounce 300ms。
     * 空字串時回傳空清單（避免 FTS MATCH 空字串報錯）。
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<WeeklyReportEntity>> = searchQuery
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
