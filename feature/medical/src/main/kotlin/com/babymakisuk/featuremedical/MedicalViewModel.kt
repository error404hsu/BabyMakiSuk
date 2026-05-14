package com.babymakisuk.featuremedical

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.MedicalAiRepository
import com.babymakisuk.coredata.SettingsRepository
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.MedicalVisit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedicalViewModel @Inject constructor(
    private val childRepo: ChildRepository,
    private val medicalDao: MedicalDao,
    private val settingsRepo: SettingsRepository,
    private val medicalAiRepo: MedicalAiRepository
) : ViewModel() {

    private val _selectedChildId = MutableStateFlow<Long?>(null)

    val canEditData: StateFlow<Boolean> = settingsRepo.userRoleFlow
        .map { it.canEditData }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val canUseLocalAi: StateFlow<Boolean> = settingsRepo.userRoleFlow
        .map { it.canUseLocalAi }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<MedicalUiState> = combine(
        childRepo.observeAll(),
        _selectedChildId
    ) { children, selectedId ->
        children to selectedId
    }
        .flatMapLatest { (children, selectedId) ->
            if (children.isEmpty()) {
                return@flatMapLatest flowOf<MedicalUiState>(
                    MedicalUiState.Success(
                        children = emptyList(),
                        selectedChildId = -1L,
                        visits = emptyList()
                    )
                )
            }
            val effectiveId = selectedId
                ?: children.firstOrNull { it.gender == Gender.MALE }?.id
                ?: children.first().id
            medicalDao.observeByChild(effectiveId).map { entities ->
                MedicalUiState.Success(
                    children = children,
                    selectedChildId = effectiveId,
                    visits = entities.map { it.toDomain() }
                )
            }
        }
        .catch { emit(MedicalUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicalUiState.Loading)

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    private val _editingVisit = MutableStateFlow<MedicalVisit?>(null)
    val editingVisit: StateFlow<MedicalVisit?> = _editingVisit.asStateFlow()

    // ── AI 圖片分析狀態 ──────────────────────────────────────────────────────
    private val _aiAnalysisState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AiAnalysisState> = _aiAnalysisState.asStateFlow()

    fun selectChild(childId: Long) { _selectedChildId.value = childId }

    fun openForm() {
        _editingVisit.value = null
        _aiAnalysisState.value = AiAnalysisState.Idle
        _showForm.value = true
    }

    fun editVisit(visit: MedicalVisit) {
        _editingVisit.value = visit
        _aiAnalysisState.value = AiAnalysisState.Idle
        _showForm.value = true
    }

    fun closeForm() {
        _showForm.value = false
        _editingVisit.value = null
        _aiAnalysisState.value = AiAnalysisState.Idle
    }

    fun saveVisit(visit: MedicalVisit) {
        viewModelScope.launch {
            medicalDao.upsert(visit.toEntity())
            _showForm.value = false
            _aiAnalysisState.value = AiAnalysisState.Idle
        }
    }

    fun deleteVisit(visit: MedicalVisit) {
        viewModelScope.launch { medicalDao.delete(visit.toEntity()) }
    }

    /**
     * 藥單圖片 AI 分析。
     * 呼叫 [MedicalAiRepository.analyzePrescriptionImage] 並將結果寫入 [AiAnalysisState]。
     * UI 透過 [LaunchedEffect] 監聽 [AiAnalysisState.Success] 後自動填入三欄位。
     */
    fun analyzeImageWithAi(imageUri: Uri?, symptomText: String, childId: Long) {
        if (imageUri == null) return
        _aiAnalysisState.value = AiAnalysisState.Analyzing
        viewModelScope.launch {
            val child = childRepo.getById(childId)
            medicalAiRepo.analyzePrescriptionImage(
                imageUri    = imageUri,
                symptomHint = symptomText,
                ageMonths   = child?.ageMonths ?: 0,
                gender      = child?.gender?.name ?: "UNKNOWN",
                allergies   = child?.allergies ?: ""
            ).onSuccess { result ->
                _aiAnalysisState.value = AiAnalysisState.Success(
                    diagnosisSummary = result.diagnosisSummary,
                    prescriptions    = result.prescriptions.joinToString("・"),
                    careInstructions = result.careInstructions.joinToString("・"),
                    confidence       = result.confidence ?: 85
                )
            }.onFailure { e ->
                _aiAnalysisState.value = AiAnalysisState.Error(e.message ?: "AI 分析失敗")
            }
        }
    }

    /** 重置 AI 狀態（用戶移除圖片時呼叫）。 */
    fun resetAiState() { _aiAnalysisState.value = AiAnalysisState.Idle }

    fun triggerAiSummary(visit: MedicalVisit) {
        viewModelScope.launch {
            val child = childRepo.getById(visit.childId) ?: return@launch
            medicalAiRepo.summarizeMedicalVisit(
                visitId   = visit.id,
                rawNote   = visit.notes.ifBlank { visit.diagnosis },
                ageMonths = child.ageMonths,
                gender    = child.gender.name,
                allergies = child.allergies
            ).onSuccess { result ->
                medicalDao.updateAiFields(
                    id               = visit.id,
                    diagnosisSummary = result.diagnosisSummary,
                    prescriptions    = result.prescriptions.joinToString("・"),
                    careInstructions = result.careInstructions.joinToString("・"),
                    isUrgent         = result.safetyFlag == "urgent"
                )
            }
        }
    }

    fun updateAiFields(
        id: Long,
        diagnosisSummary: String,
        prescriptions: String,
        careInstructions: String,
        isUrgent: Boolean
    ) {
        viewModelScope.launch {
            medicalDao.updateAiFields(
                id               = id,
                diagnosisSummary = diagnosisSummary,
                prescriptions    = prescriptions,
                careInstructions = careInstructions,
                isUrgent         = isUrgent
            )
        }
    }
}
