package com.babymakisuk.featuremedical.fever

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.FeverDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.FeverRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeverViewModel @Inject constructor(
    private val feverDao: FeverDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeverUiState>(FeverUiState.Loading)
    val uiState: StateFlow<FeverUiState> = _uiState.asStateFlow()

    fun init(childId: Long) {
        viewModelScope.launch {
            feverDao.observeByChildId(childId).collectLatest { entities ->
                val records = entities.map { it.toDomain() }.sortedBy { it.measuredAt }
                val episodes = groupRecordsIntoEpisodes(records)
                _uiState.value = FeverUiState.Success(
                    episodes = episodes.reversed(), // 最新病程在前
                    currentChildId = childId
                )
            }
        }
    }

    private fun groupRecordsIntoEpisodes(records: List<FeverRecord>): List<FeverEpisode> {
        if (records.isEmpty()) return emptyList()

        val episodes = mutableListOf<FeverEpisode>()
        var currentBatch = mutableListOf<FeverRecord>()

        for (record in records) {
            if (currentBatch.isEmpty()) {
                currentBatch.add(record)
            } else {
                val lastTime = currentBatch.last().measuredAt
                // 如果兩筆記錄相隔超過 48 小時，則開啟新病程
                if (record.measuredAt - lastTime > 48 * 60 * 60 * 1000L) {
                    episodes.add(createEpisode(currentBatch))
                    currentBatch = mutableListOf(record)
                } else {
                    currentBatch.add(record)
                }
            }
        }
        if (currentBatch.isNotEmpty()) {
            episodes.add(createEpisode(currentBatch))
        }
        return episodes
    }

    private fun createEpisode(records: List<FeverRecord>): FeverEpisode {
        return FeverEpisode(
            id = records.first().id.toString(),
            records = records,
            startTime = records.first().measuredAt,
            endTime = records.last().measuredAt
        )
    }

    fun addRecord(record: FeverRecord) {
        val childId = (uiState.value as? FeverUiState.Success)?.currentChildId ?: return
        viewModelScope.launch {
            feverDao.insert(record.copy(childId = childId).toEntity())
        }
    }

    fun deleteRecord(record: FeverRecord) {
        viewModelScope.launch {
            feverDao.deleteById(record.id)
        }
    }
}
