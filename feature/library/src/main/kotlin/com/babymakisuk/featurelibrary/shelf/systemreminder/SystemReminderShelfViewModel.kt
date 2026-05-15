package com.babymakisuk.featurelibrary.shelf.systemreminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.SystemReminderRepository
import com.babymakisuk.coremodel.SystemReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SystemReminderShelfViewModel @Inject constructor(
    private val systemReminderRepository: SystemReminderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val childId: Long = savedStateHandle.get<String>("childId")?.toLongOrNull() ?: 0L

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd (E)", Locale.getDefault())

    val reminders: StateFlow<List<SystemReminder>> = if (childId == 0L) {
        MutableStateFlow(emptyList())
    } else {
        systemReminderRepository.getByChildId(childId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    }

    val groupedByDate: StateFlow<Map<String, List<SystemReminder>>> = reminders.map { list ->
        list.groupBy { reminder ->
            dateFormat.format(Date(reminder.createdAt))
        }.toSortedMap(reverseOrder())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    fun markResolved(id: String) {
        viewModelScope.launch {
            try {
                systemReminderRepository.markResolved(id)
            } catch (_: Exception) {
            }
        }
    }
}
