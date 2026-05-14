package com.babymakisuk.featurelibrary.shelf.memo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.MemoRepository
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Memo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemoShelfViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val childRepository: ChildRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val entryChildId: Long = savedStateHandle.get<String>("childId")?.toLongOrNull() ?: 0L

    // 觀察所有孩子資料，用於卡片上的身份標籤
    val children: StateFlow<List<ChildProfile>> = childRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 邏輯修正：如果從主選單(entryChildId=0)進入，顯示全部；如果從特定孩子進入，顯示該孩子記事
    // 但根據需求「全部列出」，這裡改為優先顯示全部，或由 UI 控制過濾
    val memos: StateFlow<List<Memo>> = memoRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteById(id: Long) {
        viewModelScope.launch {
            memoRepository.deleteById(id)
        }
    }
}
