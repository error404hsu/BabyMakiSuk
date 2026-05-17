package com.babymakisuk.featuregrowth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coredata.repository.SettingsRepository
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.GrowthRecord
import com.babymakisuk.featuregrowth.domain.DeleteGrowthRecord
import com.babymakisuk.featuregrowth.domain.GrowthRecordWithPercentile
import com.babymakisuk.featuregrowth.domain.ObserveGrowthWithPercentile
import com.babymakisuk.featuregrowth.domain.SaveGrowthRecord
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.entity.AiInsightEntity
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
        val aiAnalysisText: String = "",
        val isAnalyzing: Boolean = false
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
    private val aiInsightDao: AiInsightDao
) : ViewModel() {

    private val _selectedChildId = MutableStateFlow<Long?>(savedStateHandle["childId"])

    private val _isAnalyzing = MutableStateFlow(false)

    val canEditData: StateFlow<Boolean> = settingsRepo.userRoleFlow
        .map { it.canEditData }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GrowthUiState> = combine(
        childRepo.observeAll(),
        _selectedChildId,
        aiInsightDao.getAllFlow().map { insights ->
            insights.filter { it.id.startsWith("growth_analysis_") }
                .associate { (it.childId.toLongOrNull() ?: 0L) to it.content }
        },
        _isAnalyzing
    ) { children, selectedId, aiMap, isAnalyzing ->
        DataSnapshot(children, selectedId, aiMap, isAnalyzing)
    }.flatMapLatest { snapshot ->
        val children = snapshot.children
        if (children.isEmpty()) {
            return@flatMapLatest flowOf(
                GrowthUiState.Success(
                    children = emptyList(),
                    selectedChildId = -1L,
                    records = emptyList()
                )
            )
        }
        val effectiveId = snapshot.selectedId
            ?: children.firstOrNull { it.gender == Gender.MALE }?.id
            ?: children.first().id

        observeGrowth(effectiveId).map<List<GrowthRecordWithPercentile>, GrowthUiState> { records ->
            GrowthUiState.Success(
                children = children,
                selectedChildId = effectiveId,
                records = records,
                aiAnalysisText = snapshot.aiMap[effectiveId] ?: "",
                isAnalyzing = snapshot.isAnalyzing
            )
        }
    }.catch { emit(GrowthUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthUiState.Loading)

    private data class DataSnapshot(
        val children: List<ChildProfile>,
        val selectedId: Long?,
        val aiMap: Map<Long, String>,
        val isAnalyzing: Boolean
    )

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

    fun deleteRecord(record: GrowthRecord) {
        viewModelScope.launch { deleteGrowth(record) }
    }

    fun refreshAiAnalysis() {
        val currentState = uiState.value as? GrowthUiState.Success ?: return
        val records = currentState.records
        if (records.isEmpty()) return

        _isAnalyzing.value = true

        viewModelScope.launch {
            try {
                val child = childRepo.getById(currentState.selectedChildId)
                val systemPrompt = AiPromptBuilder.buildSystemPrompt(
                    preset = AiPreset.GROWTH_ANALYST,
                    ageMonths = child?.ageMonths ?: 0,
                    gender = child?.gender?.name ?: "未知",
                    allergies = child?.allergies
                )

                val dataPrompt = buildString {
                    appendLine("請分析以下幼兒的完整生長紀錄趨勢：")
                    appendLine("小孩：${child?.name ?: "寶寶"}，月齡：${child?.ageMonths ?: 0}個月")
                    appendLine()
                    appendLine("【全部生長數據（由舊到新）】")
                    records.forEach { item ->
                        val r = item.record
                        val dateStr = r.date.toString()
                        appendLine("$dateStr 身高：${r.heightCm}cm(P${item.heightPercentile}) 體重：${r.weightKg}kg(P${item.weightPercentile}) 頭圍：${r.headCircumferenceCm ?: "-"}(P${item.headCircPercentile ?: "-"})")
                    }
                    appendLine()
                    appendLine("請分析生長趨勢、百分位變化、並給予建議。回答以繁體中文呈現，控制在 300 字以內。")
                }

                val response = aiDispatcher.executeWithSystemPrompt(
                    task = AiPreset.GROWTH_ANALYST.task,
                    systemPrompt = systemPrompt,
                    userPrompt = dataPrompt
                )

                val insight = AiInsightEntity(
                    id = "growth_analysis_${currentState.selectedChildId}",
                    childId = currentState.selectedChildId.toString(),
                    title = "生長分析",
                    content = response,
                    sourceDate = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                aiInsightDao.insert(insight)
            } catch (_: Exception) {
                // 如果失敗不更新，或可以考慮更新一個錯誤訊息到資料庫
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
}