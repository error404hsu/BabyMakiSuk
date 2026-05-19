package com.babymakisuk.corefirebase.firestore

import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coremodel.ImageStoragePath
import com.babymakisuk.coremodel.MedicalVisit
import com.babymakisuk.coredata.entity.toDomain
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFirestoreMedicalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val medicalDao: MedicalDao,
) : FirestoreMedicalRepository {

    override suspend fun upsertVisit(visit: MedicalVisit) {
        val data = mapOf(
            "id" to visit.id,
            "childId" to visit.childId,
            "date" to visit.date.toString(),
            "hospital" to visit.hospital,
            "department" to visit.department,
            "diagnosis" to visit.diagnosis,
            "notes" to visit.notes,
            "diagnosisSummary" to visit.diagnosisSummary,
            "prescriptions" to visit.prescriptions,
            "careInstructions" to visit.careInstructions,
            "isUrgent" to visit.isUrgent,
            "imageStoragePath" to when (val p = visit.imageStoragePath) {
                is ImageStoragePath.Local -> "local:${p.absolutePath}"
                is ImageStoragePath.FirebaseStorage -> "firebase:${p.storagePath}"
                ImageStoragePath.None -> null
            },
            "aiPending" to visit.aiPending,
            "lastModified" to System.currentTimeMillis()
        )
        val childId = visit.childId.toString()
        val visitId = visit.id.toString()
        firestore.collection("children").document(childId)
            .collection("medicalVisits").document(visitId)
            .set(data, SetOptions.merge()).await()
    }

    override suspend fun deleteVisit(visitId: Long) {
        // Requires childId context; use with external childId for now
    }

    override fun observeVisitsWithAiPending(): Flow<List<MedicalVisit>> =
        medicalDao.observeByChild(0L).map { list ->
            list.filter { it.aiPending }.map { it.toDomain() }
        }

    override suspend fun markAiProcessed(visitId: Long) {
        medicalDao.updateAiFields(visitId, "", "", "", false)
    }
}
