package com.error404hsu.babymakisuk.featuremedical

import com.error404hsu.babymakisuk.coremodel.ChildProfile
import com.error404hsu.babymakisuk.coremodel.MedicalVisit

sealed interface MedicalUiState {
    data object Loading : MedicalUiState
    data class Success(
        val children: List<ChildProfile>,
        val selectedChildId: Long,
        val visits: List<MedicalVisit>
    ) : MedicalUiState
    data class Error(val message: String) : MedicalUiState
}
