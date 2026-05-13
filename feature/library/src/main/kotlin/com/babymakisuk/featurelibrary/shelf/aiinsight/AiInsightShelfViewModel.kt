package com.babymakisuk.featurelibrary.shelf.aiinsight

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.entity.AiInsightEntity
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
class AiInsightShelfViewModel @Inject constructor(
    private val aiInsightDao: AiInsightDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val childId: String = savedStateHandle.get<String>("childId") ?: ""

    val insights: StateFlow<List<AiInsightEntity>> = flowOf(childId)
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf(emptyList())
            else aiInsightDao.getByChildId(cid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteInsight(id: String) {
        viewModelScope.launch {
            aiInsightDao.deleteById(id)
        }
    }
}
