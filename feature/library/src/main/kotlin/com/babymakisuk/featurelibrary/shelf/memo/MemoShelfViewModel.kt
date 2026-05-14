package com.babymakisuk.featurelibrary.shelf.memo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.MemoRepository
import com.babymakisuk.coremodel.Memo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemoShelfViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val childId: Long = savedStateHandle.get<String>("childId")?.toLongOrNull() ?: 0L

    val memos: StateFlow<List<Memo>> = flowOf(childId)
        .flatMapLatest { cid ->
            if (cid <= 0L) flowOf(emptyList())
            else memoRepository.observeByChildId(cid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insert(title: String, content: String, date: Long, reminderAt: Long? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            memoRepository.save(
                Memo(
                    childId = childId,
                    title = title,
                    content = content,
                    date = date,
                    reminderAt = reminderAt,
                    createdAt = now
                )
            )
        }
    }

    fun update(id: Long, title: String, content: String, date: Long, reminderAt: Long?, createdAt: Long) {
        viewModelScope.launch {
            memoRepository.update(
                Memo(
                    id = id,
                    childId = childId,
                    title = title,
                    content = content,
                    date = date,
                    reminderAt = reminderAt,
                    createdAt = createdAt
                )
            )
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            memoRepository.deleteById(id)
        }
    }
}
