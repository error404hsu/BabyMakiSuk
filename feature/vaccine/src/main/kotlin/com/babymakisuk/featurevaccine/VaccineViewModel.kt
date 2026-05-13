package com.babymakisuk.featurevaccine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.VaccineReminderRepository
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.VaccineReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VaccineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childRepo: ChildRepository,
    private val vaccineReminderRepo: VaccineReminderRepository
) : ViewModel() {

    private val _selectedChildId = MutableStateFlow<Long?>(savedStateHandle["childId"])

    private val _uiState = MutableStateFlow(VaccineUiState())
    val uiState: StateFlow<VaccineUiState> = _uiState.asStateFlow()

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    private val _editingReminder = MutableStateFlow<VaccineReminder?>(null)
    val editingReminder: StateFlow<VaccineReminder?> = _editingReminder.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                childRepo.observeAll(),
                _selectedChildId
            ) { children, selectedId ->
                children to selectedId
            }.flatMapLatest { (children, selectedId) ->
                if (children.isEmpty()) {
                    return@flatMapLatest flowOf(VaccineUiState(children = emptyList()))
                }
                val effectiveId = selectedId
                    ?: children.firstOrNull { it.gender == Gender.MALE }?.id
                    ?: children.first().id
                val child = children.find { it.id == effectiveId }
                if (child != null) {
                    val existing = vaccineReminderRepo.observeByChild(effectiveId).first()
                    if (existing.isEmpty()) {
                        vaccineReminderRepo.generateDefaultSchedule(child.birthday, effectiveId)
                    }
                }
                vaccineReminderRepo.observeByChild(effectiveId).map { reminders ->
                    VaccineUiState(
                        children = children,
                        selectedChildId = effectiveId,
                        reminders = reminders
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    fun selectChild(childId: Long) { _selectedChildId.value = childId }

    fun openForm() {
        _editingReminder.value = null
        _showForm.value = true
    }

    fun editReminder(reminder: VaccineReminder) {
        _editingReminder.value = reminder
        _showForm.value = true
    }

    fun closeForm() {
        _showForm.value = false
        _editingReminder.value = null
    }

    fun toggleCompleted(reminder: VaccineReminder) {
        viewModelScope.launch {
            vaccineReminderRepo.update(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun saveReminder(reminder: VaccineReminder) {
        viewModelScope.launch {
            if (reminder.id == 0) {
                vaccineReminderRepo.save(reminder)
            } else {
                vaccineReminderRepo.update(reminder)
            }
            _showForm.value = false
            _editingReminder.value = null
        }
    }

    fun deleteReminder(reminder: VaccineReminder) {
        viewModelScope.launch {
            vaccineReminderRepo.delete(reminder)
        }
    }
}
