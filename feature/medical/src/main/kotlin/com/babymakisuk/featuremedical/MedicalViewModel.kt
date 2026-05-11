package com.babymakisuk.featuremedical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val medicalDao: MedicalDao
) : ViewModel() {

    private val _selectedChildId = MutableStateFlow<Long?>(null)

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

    fun selectChild(childId: Long) {
        _selectedChildId.value = childId
    }

    fun openForm()  {
        _editingVisit.value = null
        _showForm.value = true
    }

    fun editVisit(visit: MedicalVisit) {
        _editingVisit.value = visit
        _showForm.value = true
    }

    fun closeForm() {
        _showForm.value = false
        _editingVisit.value = null
    }

    fun saveVisit(visit: MedicalVisit) {
        viewModelScope.launch {
            medicalDao.upsert(visit.toEntity())
            _showForm.value = false
        }
    }

    fun deleteVisit(visit: MedicalVisit) {
        viewModelScope.launch { medicalDao.delete(visit.toEntity()) }
    }
}
