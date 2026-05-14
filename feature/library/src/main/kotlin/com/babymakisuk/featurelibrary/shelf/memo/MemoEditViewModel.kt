package com.babymakisuk.featurelibrary.shelf.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.MemoRepository
import com.babymakisuk.coremodel.ChildProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MemoEditUiState(
    val childId: Long = -1L,
    val title: String = "",
    val content: String = "",
    val date: Long = LocalDate.now().toEpochDay(),
    val dateStr: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
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

    private var existingMemoId: Long = -1L

    fun initialize(memoId: Long, childId: Long) {
        existingMemoId = memoId
        viewModelScope.launch {
            _children.value = childRepository.observeAll().first()

            if (memoId > 0L) {
                val memos = memoRepository.observeByChildId(childId).first()
                val memo = memos.find { it.id == memoId }
                memo?.let {
                    _uiState.value = MemoEditUiState(
                        childId = it.childId,
                        title = it.title,
                        content = it.content,
                        date = it.date,
                        dateStr = LocalDate.ofEpochDay(it.date).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        reminderAt = it.reminderAt,
                        reminderStr = it.reminderAt?.let { formatReminder(it) } ?: ""
                    )
                }
            } else if (childId > 0L) {
                _uiState.update { it.copy(childId = childId) }
            } else if (_children.value.isNotEmpty()) {
                _uiState.update { it.copy(childId = _children.value.first().id) }
            }
        }
    }

    fun setChildId(id: Long) { _uiState.update { it.copy(childId = id) } }
    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateContent(content: String) { _uiState.update { it.copy(content = content) } }

    fun updateDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                date = date.toEpochDay(),
                dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
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
        if (state.title.isBlank() || state.childId <= 0L) return
        viewModelScope.launch {
            if (existingMemoId > 0L) {
                memoRepository.update(
                    com.babymakisuk.coremodel.Memo(
                        id = existingMemoId,
                        childId = state.childId,
                        title = state.title,
                        content = state.content,
                        date = state.date,
                        reminderAt = state.reminderAt,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                memoRepository.save(
                    com.babymakisuk.coremodel.Memo(
                        childId = state.childId,
                        title = state.title,
                        content = state.content,
                        date = state.date,
                        reminderAt = state.reminderAt,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    companion object {
        fun formatReminder(millis: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(millis))
        }
    }
}
