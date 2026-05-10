package com.error404hsu.babymakisuk.featuremedical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.error404hsu.babymakisuk.coredata.dao.MedicalDao
import com.error404hsu.babymakisuk.coredata.entity.toDomain
import com.error404hsu.babymakisuk.coredata.entity.toEntity
import com.error404hsu.babymakisuk.coredata.repository.ChildRepository
import com.error404hsu.babymakisuk.coremodel.Gender
import com.error404hsu.babymakisuk.coremodel.MedicalVisit
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
            val defaultId = _selectedChildId.value
                ?: children.firstOrNull { it.gender == Gender.MALE }?.id
                ?: children.firstOrNull()?.id
                ?: return@flatMapLatest flowOf(MedicalUiState.Loading)

            if (_selectedChildId.value == null) _selectedChildId.value = defaultId

            medicalDao.observeByChild(defaultId).map { entities ->
                MedicalUiState.Success(
                    children = children,
                    selectedChildId = defaultId,
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
