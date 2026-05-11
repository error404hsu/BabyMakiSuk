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

    val uiState: StateFlow<MedicalUiState> = childRepo.observeAll()
        .flatMapLatest { children ->
            if (children.isEmpty()) {
                return@flatMapLatest flowOf<MedicalUiState>(
                    MedicalUiState.Success(
                        children = emptyList(),
                        selectedChildId = -1L,
                        visits = emptyList()
                    )
                )
            }

            val selectedId = _selectedChildId.value
                ?: children.firstOrNull { it.gender == Gender.MALE }?.id
                ?: children.first().id

            if (_selectedChildId.value == null) _selectedChildId.value = selectedId

            medicalDao.observeByChild(selectedId).map { entities ->
                MedicalUiState.Success(
                    children = children,
                    selectedChildId = selectedId,
                    visits = entities.map { it.toDomain() }
                )
            }
        }
        .catch { emit(MedicalUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicalUiState.Loading)

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    fun selectChild(childId: Long) {
        _selectedChildId.value = childId
    }

    fun openForm()  { _showForm.value = true }
    fun closeForm() { _showForm.value = false }

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
