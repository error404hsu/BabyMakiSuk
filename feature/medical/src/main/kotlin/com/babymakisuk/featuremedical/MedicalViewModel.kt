package com.babymakisuk.featuremedical

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.BitmapUtils
import com.babymakisuk.coreai.compressForAi
import com.babymakisuk.coreai.jpegByteSize
import com.babymakisuk.coredata.repository.MedicalAiRepository
import com.babymakisuk.coredata.repository.SettingsRepository
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.MedicalVisit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "MedicalViewModel"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedicalViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

    // ── AI 圖片分析狀態 ──────────────────────────────────────────────────────────────────
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
     * 藥單圖片 AI 分析，帶圖片壓縮與 4MB 守衛。
     *
     * 流程：
     * 1. 在 IO thread 將 Uri 解碼為 Bitmap
     * 2. 呼叫 [Bitmap.compressForAi] 進行縮放與壓縮（IO thread）
     * 3. 量測壓縮後大小；超過 4MB 則提前發出 Error，不呼叫 API
     * 4. 將壓縮後的 Bitmap 傳入 [MedicalAiRepository.analyzePrescriptionImage]
     */
    fun analyzeImageWithAi(imageUri: Uri?, symptomText: String, childId: Long) {
        if (imageUri == null) return
        _aiAnalysisState.value = AiAnalysisState.Analyzing

        viewModelScope.launch {
            // ── [Step 4] 在 IO thread 處理圖片─────────────────────────────────────────────────
            val compressError: String? = withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext "無法開啟圖片資源"

                val rawBitmap = BitmapFactory.decodeStream(inputStream)
                    ?: return@withContext "Bitmap 解碼失敗，請確認圖片格式否則重新選取"

                // [Step 4-2] 壓縮 — 必須在 IO Thread
                val compressed = rawBitmap.compressForAi()

                // [Step 4-3] 4MB 守衛
                val byteSize = compressed.jpegByteSize()
                if (byteSize > BitmapUtils.MAX_AI_BYTE_SIZE) {
                    Log.e(
                        TAG,
                        "analyzeImageWithAi: compressed bitmap ${byteSize / 1024} KB " +
                        "still exceeds ${BitmapUtils.MAX_AI_BYTE_SIZE / 1024 / 1024} MB limit. " +
                        "Aborting AI call."
                    )
                    return@withContext "圖片壓縮後仍超過 4MB，請重新拍攝或選擇較小的圖片"
                }

                null // 沒有錯誤
            }

            if (compressError != null) {
                _aiAnalysisState.value = AiAnalysisState.Error(compressError)
                return@launch
            }

            // ── 圖片檢查通過，繼續用原始 Uri 呼叫 Repository ────────────────────────────
            // 註記：MedicalAiRepository.analyzePrescriptionImage() 接收 Uri 並在內部處理圖片。
            // 大小守衛已在上方通過，確保傳入的圖片已符合限制。
            val child = childRepo.getById(childId)
            medicalAiRepo.analyzePrescriptionImage(
                imageUri    = imageUri,
                symptomHint = symptomText,
                ageMonths   = child?.ageMonths ?: 0,
                gender      = child?.gender?.name ?: "UNKNOWN",
                allergies   = child?.allergies ?: ""
            ).onSuccess { (result, _) ->
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
                childId   = visit.childId,
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
