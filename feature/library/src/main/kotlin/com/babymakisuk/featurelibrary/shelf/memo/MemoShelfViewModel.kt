package com.babymakisuk.featurelibrary.shelf.memo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.MemoDao
import com.babymakisuk.coredata.entity.MemoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemoShelfViewModel @Inject constructor(
    private val memoDao: MemoDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val childId: String = savedStateHandle.get<String>("childId") ?: ""

    val memos: StateFlow<List<MemoEntity>> = flowOf(childId)
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf(emptyList())
            else memoDao.getByChildId(cid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insert(title: String, content: String, tags: String) {
        viewModelScope.launch {
            val tagJson = tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .let { '[' + it.joinToString(",") { t -> "\"$t\"" } + ']' }
            val now = System.currentTimeMillis()
            memoDao.insert(
                MemoEntity(
                    id = UUID.randomUUID().toString(),
                    childId = childId,
                    title = title,
                    content = content,
                    tags = tagJson,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun update(id: String, title: String, content: String, tags: String, createdAt: Long) {
        viewModelScope.launch {
            val tagJson = tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .let { '[' + it.joinToString(",") { t -> "\"$t\"" } + ']' }
            memoDao.update(
                MemoEntity(
                    id = id,
                    childId = childId,
                    title = title,
                    content = content,
                    tags = tagJson,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteById(id: String) {
        viewModelScope.launch {
            memoDao.deleteById(id)
        }
    }
}
