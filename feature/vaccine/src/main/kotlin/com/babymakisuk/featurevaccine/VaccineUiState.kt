package com.babymakisuk.featurevaccine

import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.VaccineReminder

data class VaccineUiState(
    val children: List<ChildProfile> = emptyList(),
    val selectedChildId: Long = -1L,
    val reminders: List<VaccineReminder> = emptyList(),
    val isLoading: Boolean = false
)
