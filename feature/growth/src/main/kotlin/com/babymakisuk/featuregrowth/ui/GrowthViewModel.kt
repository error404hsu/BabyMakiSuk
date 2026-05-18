package com.babymakisuk.featuregrowth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiError
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coredata.repository.SettingsRepository
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.entity.AiInsightEntity
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
import javax.inject.Inject

sealed interface GrowthListUiEvent {
    data class NavigateToEdit(val recordId: Long, val childId: Long) : GrowthListUiEvent
    data object RecordDeleted : GrowthListUiEvent
    data class ShowToast(val message: String) : GrowthListUiEvent
}

sealed interface GrowthListUiState {
    data object Loading : GrowthListUiState
    data class Success(
        val children: List<ChildProfile>,
        val selectedChildId: Long,
        val records: List<GrowthRecordWithPercentile>,
        val aiAnalysisText: String = "",
        val isAiLoading: Boolean = false,
        val aiError: String? = null,
        val isAnalyzing: Boolean = false
    ) : GrowthListUiState
    data class Error(val message: String) : GrowthListUiState
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
    private val _isAnalyzing    = MutableStateFlow(false)
    private val _isAiLoading    = MutableStateFlow(false)
    private val _aiError        = MutableStateFlow<String?>(null)

    val canEditData: StateFlow<Boolean> = settingsRepo.userRoleFlow
        .map { it.canEditData }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GrowthListUiState> = combine(
        childRepo.observeAll(),
        _selectedChildId,
        aiInsightDao.getAllFlow().map { insights ->
            insights.filter { it.id.startsWith("growth_analysis_") }
                .associate { (it.childId.toLongOrNull() ?: 0L) to it.content }
        },
        _isAnalyzing,
        _isAiLoading,
        _aiError
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        DataSnapshot(
            children   = array[0] as List<ChildProfile>,
            selectedId = array[1] as Long?,
            aiMap      = array[2] as Map<Long, String>,
            isAnalyzing = array[3] as Boolean,
            isAiLoading = array[4] as Boolean,
            aiError     = array[5] as String?
        )
    }.flatMapLatest { snapshot ->
        val children = snapshot.children
        if (children.isEmpty()) {
            return@flatMapLatest flowOf(
                GrowthListUiState.Success(
                    children = emptyList(),
                    selectedChildId = -1L,
                    records = emptyList()
                )
            )
        }
        val effectiveId = snapshot.selectedId
            ?: children.firstOrNull { it.gender == Gender.MALE }?.id
            ?: children.first().id

        observeGrowth(effectiveId).map<List<GrowthRecordWithPercentile>, GrowthListUiState> { records ->
            GrowthListUiState.Success(
                children        = children,
                selectedChildId = effectiveId,
                records         = records,
                aiAnalysisText  = snapshot.aiMap[effectiveId] ?: "",
                isAiLoading     = snapshot.isAiLoading,
                aiError         = snapshot.aiError,
                isAnalyzing     = snapshot.isAnalyzing
            )
        }
    }.catch { emit(GrowthListUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthListUiState.Loading)

    private val _events = MutableSharedFlow<GrowthListUiEvent>()
    val events: SharedFlow<GrowthListUiEvent> = _events.asSharedFlow()

    private data class DataSnapshot(
        val children: List<ChildProfile>,
        val selectedId: Long?,
        val aiMap: Map<Long, String>,
        val isAnalyzing: Boolean,
        val isAiLoading: Boolean,
        val aiError: String?
    )

    fun selectChild(childId: Long) { _selectedChildId.value = childId }

    fun onAddRecord() {
        val childId = (uiState.value as? GrowthListUiState.Success)?.selectedChildId ?: return
        viewModelScope.launch { _events.emit(GrowthListUiEvent.NavigateToEdit(recordId = -1L, childId = childId)) }
    }

    fun onEditRecord(record: GrowthRecordWithPercentile) {
        viewModelScope.launch {
            _events.emit(
                GrowthListUiEvent.NavigateToEdit(
                    recordId = record.record.id,
                    childId = record.record.childId
                )
            )
        }
    }

    fun deleteRecord(record: GrowthRecord) {
        viewModelScope.launch {
            deleteGrowth(record)
            _events.emit(GrowthListUiEvent.RecordDeleted)
        }
    }

    fun refreshAiAnalysis() {
        val currentState = uiState.value as? GrowthListUiState.Success ?: return
        val records = currentState.records
        if (records.isEmpty()) return

        _isAnalyzing.value = true
        _isAiLoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            val child = childRepo.getById(currentState.selectedChildId)
            val systemPrompt = AiPromptBuilder.buildSystemPrompt(
                preset    = AiPreset.GROWTH_ANALYST,
                ageMonths = child?.ageMonths ?: 0,
                gender    = child?.gender?.name ?: "未知",
                allergies = child?.allergies
            )

            val dataPrompt = buildString {
                appendLine("請分析以下幼兒的完整生長紀錄趨勢：")
                appendLine("小孩：${child?.name ?: "寶寶"}，月齡：${child?.ageMonths ?: 0}個月")
                appendLine()
                appendLine("【全部生長數據（由舊到新）】")
                records.forEach { item ->
                    val r = item.record
                    appendLine("${r.date} 身高：${r.heightCm}cm(P${item.heightPercentile}) 體重：${r.weightKg}kg(P${item.weightPercentile}) 頭圍：${r.headCircumferenceCm ?: "-"}(P${item.headCircPercentile ?: "-"})")
                }
                appendLine()
                appendLine("請分析生長趨勢、百分位變化、並給予建議。回答以繁體中文呈現，控制在 300 字以內。")
            }

            val result = aiDispatcher.executeWithSystemPrompt(
                task         = AiPreset.GROWTH_ANALYST.task,
                systemPrompt = systemPrompt,
                userPrompt   = dataPrompt
            )

            _isAiLoading.value = false

            result.fold(
                onSuccess = { response ->
                    val insight = AiInsightEntity(
                        id         = "growth_analysis_${currentState.selectedChildId}",
                        childId    = currentState.selectedChildId.toString(),
                        title      = "生長分析",
                        content    = response,
                        sourceDate = System.currentTimeMillis(),
                        createdAt  = System.currentTimeMillis()
                    )
                    aiInsightDao.insert(insight)
                },
                onFailure = { err ->
                    val errorMsg = when (err as? AiError) {
                        is AiError.RateLimited     -> "已達每分鐘上限，請稍後再試"
                        is AiError.AllModelsFailed -> "所有模型均無法使用"
                        is AiError.InvalidConfig   -> "AI 設定錯誤"
                        is AiError.Cancelled       -> "請求已取消"
                        else -> err.message ?: "AI 分析失敗"
                    }
                    _aiError.value = errorMsg
                }
            )

            _isAnalyzing.value = false
        }
    }
}
