package com.babymakisuk.corefirebase.firestore

import com.babymakisuk.coremodel.MedicalVisit
import kotlinx.coroutines.flow.Flow

interface FirestoreMedicalRepository {
    suspend fun upsertVisit(visit: MedicalVisit)
    suspend fun deleteVisit(visitId: Long)
    fun observeVisitsWithAiPending(): Flow<List<MedicalVisit>>
    suspend fun markAiProcessed(visitId: Long)
}
