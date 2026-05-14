package com.babymakisuk.featuregrowth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coredata.SettingsRepository
import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.GrowthRecord
import com.babymakisuk.featuregrowth.domain.DeleteGrowthRecord
import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile
import com.babymakisuk.featuregrowth.domain.ObserveGrowthWithPercentile
import com.babymakisuk.featuregrowth.domain.SaveGrowthRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface GrowthUiState {
    data object Loading : GrowthUiState
    data class Success(
        val children: List<ChildProfile>,
        val selectedChildId: Long,
        val records: List<GrowthRecordWithPercentile>,
        val showChart: Boolean = false
    ) : GrowthUiState
    data class Error(val message: String) : GrowthUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GrowthViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childRepo: ChildRepository,
    private val observeGrowth: ObserveGrowthWithPercentile,
    private val saveGrowth: SaveGrowthRecord,
    private val deleteGrowth: DeleteGrowthRecord,
    private val settingsRepo: SettingsRepository,
    private val aiDispatcher: AiDispatcher,
    private val growthDao: GrowthDao
) : ViewModel() {

    private val _selectedChildId = MutableStateFlow<Long?>(savedStateHandle["childId"])

    // 角色旗標
    val canEditData: StateFlow<Boolean> = settingsRepo.userRoleFlow
        .map { it.canEditData }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<GrowthUiState> = combine(
        childRepo.observeAll(),
        _selectedChildId
    ) { children, selectedId ->
        children to selectedId
    }
        .flatMapLatest { (children, selectedId) ->
            if (children.isEmpty()) {
                return@flatMapLatest flowOf<GrowthUiState>(
                    GrowthUiState.Success(
                        children = emptyList(),
                        selectedChildId = -1L,
                        records = emptyList()
                    )
                )
            }
            val effectiveId = selectedId
                ?: children.firstOrNull { it.gender == Gender.MALE }?.id
                ?: children.first().id
            observeGrowth(effectiveId).map { records ->
                GrowthUiState.Success(
                    children = children,
                    selectedChildId = effectiveId,
                    records = records
                )
            }
        }
        .catch { emit(GrowthUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthUiState.Loading)

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    private val _editingRecord = MutableStateFlow<GrowthRecordWithPercentile?>(null)
    val editingRecord: StateFlow<GrowthRecordWithPercentile?> = _editingRecord.asStateFlow()

    fun selectChild(childId: Long) { _selectedChildId.value = childId }

    fun openForm() {
        _editingRecord.value = null
        _showForm.value = true
    }

    fun editRecord(record: GrowthRecordWithPercentile) {
        _editingRecord.value = record
        _showForm.value = true
    }

    fun closeForm() {
        _showForm.value = false
        _editingRecord.value = null
    }

    fun saveRecord(
        heightCm: Float,
        weightKg: Float,
        headCircCm: Float?,
        date: LocalDate,
        note: String
    ) {
        val selectedId = (uiState.value as? GrowthUiState.Success)?.selectedChildId ?: return
        val existingId = _editingRecord.value?.record?.id ?: 0L
        viewModelScope.launch {
            saveGrowth(
                GrowthRecord(
                    id = existingId,
                    childId = selectedId,
                    date = date,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    headCircumferenceCm = headCircCm,
                    note = note
                )
            )
            _showForm.value = false
            _editingRecord.value = null
        }
    }

    private val _aiSuggestingIds = MutableStateFlow<Set<Long>>(emptySet())
    val aiSuggestingIds: StateFlow<Set<Long>> = _aiSuggestingIds.asStateFlow()

    fun deleteRecord(record: GrowthRecord) {
        viewModelScope.launch { deleteGrowth(record) }
    }

    fun triggerAiSuggestion(item: GrowthRecordWithPercentile) {
        val record = item.record
        if (record.id == 0L || _aiSuggestingIds.value.contains(record.id)) return
        _aiSuggestingIds.update { it + record.id }
        viewModelScope.launch {
            try {
                val child = childRepo.getById(record.childId)
                val systemPrompt = AiPromptBuilder.buildSystemPrompt(
                    preset    = AiPreset.GROWTH_ANALYST,
                    ageMonths = item.ageMonths,
                    gender    = item.gender.name,
                    allergies = child?.allergies
                )

                val dataPrompt = buildString {
                    appendLine("請分析以下幼兒生長數據：")
                    appendLine("身高：${record.heightCm} cm（P${item.heightPercentile}）")
                    appendLine("體重：${record.weightKg} kg（P${item.weightPercentile}）")
                    record.headCircumferenceCm?.let {
                        appendLine("頭圍：$it cm（P${item.headCircPercentile ?: -1}）")
                    }
                    if (record.note.isNotBlank()) {
                        appendLine("備註：${record.note}")
                    }
                }

                val response = aiDispatcher.executeWithSystemPrompt(
                    task          = AiPreset.GROWTH_ANALYST.task,
                    systemPrompt  = systemPrompt,
                    userPrompt    = dataPrompt
                )

                growthDao.updateAiSuggestion(record.id, response)
            } catch (_: Exception) {
                // 靜默失敗，不影響 UI
            } finally {
                _aiSuggestingIds.update { it - record.id }
            }
        }
    }
}
