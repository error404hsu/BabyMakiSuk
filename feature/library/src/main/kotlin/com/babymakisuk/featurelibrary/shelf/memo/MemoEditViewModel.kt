package com.babymakisuk.featurelibrary.shelf.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.MemoRepository
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Memo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MemoEditUiState(
    val selectedChildIds: List<Long> = emptyList(),
    val title: String = "",
    val content: String = "",
    val date: Long = LocalDate.now().toEpochDay(),
    val dateStr: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy / MM / dd")),
    val reminderAt: Long? = null,
    val reminderStr: String = ""
)

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val childRepository: ChildRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoEditUiState())
    val uiState: StateFlow<MemoEditUiState> = _uiState.asStateFlow()

    private val _children = MutableStateFlow<List<ChildProfile>>(emptyList())
    val children: StateFlow<List<ChildProfile>> = _children.asStateFlow()

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private var existingMemoId: Long = -1L

    fun initialize(memoId: Long, childId: Long) {
        existingMemoId = memoId
        viewModelScope.launch {
            val allChildren = childRepository.observeAll().first()
            _children.value = allChildren

            if (memoId > 0L) {
                // Edit mode
                val memos = memoRepository.observeByChildId(childId).first()
                val memo = memos.find { it.id == memoId }
                memo?.let {
                    _uiState.value = MemoEditUiState(
                        selectedChildIds = listOf(it.childId),
                        title = it.title,
                        content = it.content,
                        date = it.date,
                        dateStr = LocalDate.ofEpochDay(it.date).format(DateTimeFormatter.ofPattern("yyyy / MM / dd")),
                        reminderAt = it.reminderAt,
                        reminderStr = it.reminderAt?.let { formatReminder(it) } ?: ""
                    )
                }
            } else {
                // New mode
                val initialIds = if (childId > 0L) listOf(childId) else allChildren.map { it.id }
                _uiState.update { it.copy(selectedChildIds = initialIds) }
            }
        }
    }

    fun toggleChildSelection(id: Long) {
        _uiState.update { state ->
            val newIds = if (id in state.selectedChildIds) {
                // 不允許全部取消選擇，至少保留一個
                if (state.selectedChildIds.size <= 1) state.selectedChildIds
                else state.selectedChildIds - id
            } else {
                state.selectedChildIds + id
            }
            state.copy(selectedChildIds = newIds)
        }
    }

    fun setSingleChildSelection(id: Long) {
        _uiState.update { it.copy(selectedChildIds = listOf(id)) }
    }

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateContent(content: String) { _uiState.update { it.copy(content = content) } }

    fun updateDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                date = date.toEpochDay(),
                dateStr = date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"))
            )
        }
    }

    fun updateReminder(millis: Long) {
        _uiState.update {
            it.copy(
                reminderAt = millis,
                reminderStr = formatReminder(millis)
            )
        }
    }

    fun clearReminder() {
        _uiState.update { it.copy(reminderAt = null, reminderStr = "") }
    }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank() || state.selectedChildIds.isEmpty()) return
        
        viewModelScope.launch {
            if (existingMemoId > 0L) {
                // Update existing (only one)
                memoRepository.update(
                    Memo(
                        id = existingMemoId,
                        childId = state.selectedChildIds.first(),
                        title = state.title,
                        content = state.content,
                        date = state.date,
                        reminderAt = state.reminderAt,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                // Create new for each selected child
                state.selectedChildIds.forEach { cid ->
                    memoRepository.save(
                        Memo(
                            childId = cid,
                            title = state.title,
                            content = state.content,
                            date = state.date,
                            reminderAt = state.reminderAt,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            _savedEvent.emit(Unit)
        }
    }

    private fun formatReminder(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}
