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
import com.babymakisuk.coredata.repository.MedicalRepository
import com.babymakisuk.coredata.repository.SettingsRepository
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.MedicalVisit
import com.babymakisuk.corefirebase.firestore.FirestoreMedicalRepository
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
    private val medicalRepo: MedicalRepository,
    private val settingsRepo: SettingsRepository,
    private val medicalAiRepo: MedicalAiRepository,
    private val firestoreMedicalRepo: FirestoreMedicalRepository
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
            medicalRepo.observeByChild(effectiveId).map { visits ->
                MedicalUiState.Success(
                    children = children,
                    selectedChildId = effectiveId,
                    visits = visits
                )
            }
        }
        .catch { emit(MedicalUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicalUiState.Loading)

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    private val _editingVisit = MutableStateFlow<MedicalVisit?>(null)
    val editingVisit: StateFlow<MedicalVisit?> = _editingVisit.asStateFlow()

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
            medicalRepo.upsert(visit)
            // 同步上傳 Firestore
            runCatching { firestoreMedicalRepo.upsertVisit(visit) }
                .onFailure { Log.w(TAG, "saveVisit: Firestore upsert failed: ${it.message}") }
            _showForm.value = false
            _aiAnalysisState.value = AiAnalysisState.Idle
        }
    }

    /**
     * 刪除就診紀錄。
     * 順序：先呼叫 Firestore delete（此時 Room 仍有 childId 可查詢），
     * 再從 Room 刪除，確保雲端同步正確執行。
     */
    fun deleteVisit(visit: MedicalVisit) {
        viewModelScope.launch {
            // ① 先同步刪除 Firestore（Room entity 尚在，getById 可查到 childId）
            runCatching { firestoreMedicalRepo.deleteVisit(visit.id) }
                .onFailure { Log.w(TAG, "deleteVisit: Firestore delete failed: ${it.message}") }
            // ② 再刪除本機 Room
            medicalRepo.delete(visit)
        }
    }

    /**
     * 藥單圖片 AI 分析，帶圖片壓縮與 4MB 守衛。
     */
    fun analyzeImageWithAi(imageUri: Uri?, symptomText: String, childId: Long) {
        if (imageUri == null) return
        _aiAnalysisState.value = AiAnalysisState.Analyzing

        viewModelScope.launch {
            val compressError: String? = withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext "無法開啟圖片資源"

                val rawBitmap = BitmapFactory.decodeStream(inputStream)
                    ?: return@withContext "Bitmap 解碼失敗，請確認圖片格式否則重新選取"

                val compressed = rawBitmap.compressForAi()

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

                null
            }

            if (compressError != null) {
                _aiAnalysisState.value = AiAnalysisState.Error(compressError)
                return@launch
            }

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
                    prescriptions    = result.prescriptions.joinToString("\u30fb"),
                    careInstructions = result.careInstructions.joinToString("\u30fb"),
                    confidence       = result.confidence ?: 85
                )
            }.onFailure { e ->
                _aiAnalysisState.value = AiAnalysisState.Error(e.message ?: "AI 分析失敗")
            }
        }
    }

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
                medicalRepo.updateAiFields(
                    id               = visit.id,
                    diagnosisSummary = result.diagnosisSummary,
                    prescriptions    = result.prescriptions.joinToString("\u30fb"),
                    careInstructions = result.careInstructions.joinToString("\u30fb"),
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
            medicalRepo.updateAiFields(
                id               = id,
                diagnosisSummary = diagnosisSummary,
                prescriptions    = prescriptions,
                careInstructions = careInstructions,
                isUrgent         = isUrgent
            )
        }
    }
}
