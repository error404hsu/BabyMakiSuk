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

    private val _editingGroup = MutableStateFlow<GroupedVaccineReminder?>(null)
    val editingGroup: StateFlow<GroupedVaccineReminder?> = _editingGroup.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                childRepo.observeAll(),
                vaccineReminderRepo.observeAll()
            ) { children, reminders ->
                VaccineUiState(
                    children = children,
                    reminders = reminders,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun selectChild(childId: Long) { /* Removed filter */ }

    fun openForm() {
        _editingGroup.value = null
        _showForm.value = true
    }

    fun editGroup(group: GroupedVaccineReminder) {
        _editingGroup.value = group
        _showForm.value = true
    }

    fun closeForm() {
        _showForm.value = false
        _editingGroup.value = null
    }

    fun toggleGroupCompleted(group: GroupedVaccineReminder) {
        viewModelScope.launch {
            val nextState = !group.isAllCompleted
            group.childReminders.values.forEach { reminder ->
                vaccineReminderRepo.update(reminder.copy(isCompleted = nextState))
            }
        }
    }

    fun saveGroupedReminder(
        name: String,
        date: Long,
        note: String,
        isCompleted: Boolean,
        childIds: List<Long>,
        originalGroup: GroupedVaccineReminder?
    ) {
        viewModelScope.launch {
            if (originalGroup == null) {
                // New
                childIds.forEach { childId ->
                    vaccineReminderRepo.save(
                        VaccineReminder(
                            childId = childId,
                            name = name,
                            scheduledDate = date,
                            isCompleted = isCompleted,
                            note = note
                        )
                    )
                }
            } else {
                // Edit: Delete those not in childIds, update existing, add new
                val oldChildIds = originalGroup.childReminders.keys
                
                // Remove removed
                oldChildIds.filter { it !in childIds }.forEach { id ->
                    originalGroup.childReminders[id]?.let { vaccineReminderRepo.delete(it) }
                }
                
                // Update/Add
                childIds.forEach { childId ->
                    val existing = originalGroup.childReminders[childId]
                    if (existing != null) {
                        vaccineReminderRepo.update(
                            existing.copy(
                                name = name,
                                scheduledDate = date,
                                note = note,
                                isCompleted = isCompleted
                            )
                        )
                    } else {
                        vaccineReminderRepo.save(
                            VaccineReminder(
                                childId = childId,
                                name = name,
                                scheduledDate = date,
                                isCompleted = isCompleted,
                                note = note
                            )
                        )
                    }
                }
            }
            _showForm.value = false
            _editingGroup.value = null
        }
    }

    fun deleteGroup(group: GroupedVaccineReminder) {
        viewModelScope.launch {
            group.childReminders.values.forEach {
                vaccineReminderRepo.delete(it)
            }
        }
    }
}
