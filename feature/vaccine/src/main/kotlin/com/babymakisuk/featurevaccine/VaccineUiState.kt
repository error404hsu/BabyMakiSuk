package com.babymakisuk.featurevaccine

import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.VaccineReminder

data class VaccineUiState(
    val children: List<ChildProfile> = emptyList(),
    val reminders: List<VaccineReminder> = emptyList(),
    val isLoading: Boolean = false
) {
    val groupedReminders: List<GroupedVaccineReminder>
        get() = reminders.groupBy { "${it.name}|${it.scheduledDate}|${it.note}" }
            .map { (key, list) ->
                val first = list.first()
                GroupedVaccineReminder(
                    name = first.name,
                    scheduledDate = first.scheduledDate,
                    note = first.note,
                    childReminders = list.associateBy { it.childId }
                )
            }.sortedBy { it.scheduledDate }
}

data class GroupedVaccineReminder(
    val name: String,
    val scheduledDate: Long,
    val note: String,
    val childReminders: Map<Long, VaccineReminder> // childId -> reminder
) {
    val id: String get() = "${name}_${scheduledDate}_${note}"
    val isAllCompleted: Boolean get() = childReminders.values.all { it.isCompleted }
}
