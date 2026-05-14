package com.babymakisuk.featuremedical

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.MedicalAiRepository
import com.babymakisuk.coredata.PrescriptionImagePreprocessor
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.MedicalVisit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MedicalEditUiState(
    val childId: Long = -1L,
    val hospital: String = "",
    val department: String = "",
    val diagnosis: String = "",
    val notes: String = "",
    val date: LocalDate = LocalDate.now(),
    val dateStr: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy / MM / dd")),
    val diagnosisSummary: String = "",
    val prescriptions: String = "",
    val careInstructions: String = "",
    val imageStoragePath: String? = null,
    val hospitalError: Boolean = false
)

@HiltViewModel
class MedicalEditViewModel @Inject constructor(
    private val medicalDao: MedicalDao,
    private val medicalAiRepo: MedicalAiRepository,
    private val childRepository: ChildRepository,
    private val preprocessor: PrescriptionImagePreprocessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalEditUiState())
    val uiState: StateFlow<MedicalEditUiState> = _uiState.asStateFlow()

    private val _aiAnalysisState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AiAnalysisState> = _aiAnalysisState.asStateFlow()

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private var existingVisitId: Long = 0L

    fun initialize(visitId: Long, childId: Long) {
        viewModelScope.launch {
            if (visitId > 0L) {
                existingVisitId = visitId
                val entity = medicalDao.getById(visitId)
                if (entity != null) {
                    val visit = entity.toDomain()
                    _uiState.update {
                        it.copy(
                            childId = visit.childId,
                            hospital = visit.hospital,
                            department = visit.department,
                            diagnosis = visit.diagnosis,
                            notes = visit.notes,
                            date = visit.date,
                            dateStr = visit.date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd")),
                            diagnosisSummary = visit.diagnosisSummary,
                            prescriptions = visit.prescriptions,
                            careInstructions = visit.careInstructions,
                            imageStoragePath = visit.imageStoragePath
                        )
                    }
                }
            } else {
                existingVisitId = 0L
                _uiState.update { it.copy(childId = childId) }
            }
        }
    }

    fun updateHospital(v: String) { _uiState.update { it.copy(hospital = v, hospitalError = false) } }
    fun updateDepartment(v: String) { _uiState.update { it.copy(department = v) } }
    fun updateDiagnosis(v: String) { _uiState.update { it.copy(diagnosis = v) } }
    fun updateNotes(v: String) { _uiState.update { it.copy(notes = v) } }
    fun updateDiagnosisSummary(v: String) { _uiState.update { it.copy(diagnosisSummary = v) } }
    fun updatePrescriptions(v: String) { _uiState.update { it.copy(prescriptions = v) } }
    fun updateCareInstructions(v: String) { _uiState.update { it.copy(careInstructions = v) } }

    fun updateDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                date = date,
                dateStr = date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"))
            )
        }
    }

    fun analyzeImageWithAi(imageUri: Uri?, symptomText: String) {
        if (imageUri == null) return
        val state = _uiState.value
        _aiAnalysisState.value = AiAnalysisState.Analyzing
        viewModelScope.launch {
            val child = childRepository.getById(state.childId)
            medicalAiRepo.analyzePrescriptionImage(
                imageUri = imageUri,
                symptomHint = symptomText,
                ageMonths = child?.ageMonths ?: 0,
                gender = child?.gender?.name ?: "UNKNOWN",
                allergies = child?.allergies ?: ""
            ).onSuccess { (result, imagePath) ->
                _aiAnalysisState.value = AiAnalysisState.Reviewing(
                    rawText = result.diagnosisSummary,
                    diagnosisSummary = result.diagnosisSummary,
                    prescriptions = result.prescriptions.joinToString("・"),
                    careInstructions = result.careInstructions.joinToString("・"),
                    confidence = result.confidence ?: 85,
                    imagePath = imagePath
                )
            }.onFailure { e ->
                _aiAnalysisState.value = AiAnalysisState.Error(e.message ?: "AI 分析失敗")
            }
        }
    }

    fun confirmAnalysis() {
        val reviewing = _aiAnalysisState.value as? AiAnalysisState.Reviewing ?: return
        _uiState.update {
            it.copy(
                diagnosisSummary = reviewing.diagnosisSummary,
                prescriptions = reviewing.prescriptions,
                careInstructions = reviewing.careInstructions,
                imageStoragePath = reviewing.imagePath
            )
        }
        _aiAnalysisState.value = AiAnalysisState.Success(
            diagnosisSummary = reviewing.diagnosisSummary,
            prescriptions = reviewing.prescriptions,
            careInstructions = reviewing.careInstructions,
            confidence = reviewing.confidence
        )
    }

    fun resetAiState() { _aiAnalysisState.value = AiAnalysisState.Idle }

    fun save() {
        val state = _uiState.value
        if (state.hospital.isBlank()) {
            _uiState.update { it.copy(hospitalError = true) }
            return
        }

        // 確保 childId 有效，避免 Room 外鍵約束導致閃退
        if (state.childId <= 0L) {
            _aiAnalysisState.value = AiAnalysisState.Error("無效的寶寶 ID，無法儲存。")
            return
        }

        viewModelScope.launch {
            try {
                val visit = MedicalVisit(
                    id = existingVisitId,
                    childId = state.childId,
                    date = state.date,
                    hospital = state.hospital.trim(),
                    department = state.department.trim(),
                    diagnosis = state.diagnosis.trim(),
                    notes = state.notes.trim(),
                    diagnosisSummary = state.diagnosisSummary.trim(),
                    prescriptions = state.prescriptions.trim(),
                    careInstructions = state.careInstructions.trim(),
                    isUrgent = false,
                    imageStoragePath = state.imageStoragePath,
                    aiPending = false
                )
                medicalDao.upsert(visit.toEntity())

                preprocessor.cleanupOldFiles()
                _savedEvent.emit(Unit)
            } catch (e: Exception) {
                _aiAnalysisState.value = AiAnalysisState.Error("儲存失敗：${e.message}")
            }
        }
    }
}
