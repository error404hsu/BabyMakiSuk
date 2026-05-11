package com.babymakisuk.featuremedical

import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.MedicalVisit

sealed interface MedicalUiState {
    data object Loading : MedicalUiState
    data class Success(
        val children: List<ChildProfile>,
        val selectedChildId: Long,
        val visits: List<MedicalVisit>
    ) : MedicalUiState
    data class Error(val message: String) : MedicalUiState
}
