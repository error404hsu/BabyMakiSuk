package com.babymakisuk.corefirebase.firestore

import android.util.Log
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.di.ApplicationScope
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coremodel.ImageStoragePath
import com.babymakisuk.coremodel.MedicalVisit
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFirestoreMedicalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val medicalDao: MedicalDao,
    private val aiDispatcher: AiDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : FirestoreMedicalRepository {

    companion object {
        private const val TAG = "FirestoreMedicalRepo"
    }

    init {
        observeAndDispatchAiPending()
    }

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

    private fun observeAndDispatchAiPending() {
        appScope.launch {
            observeVisitsWithAiPending()
                .filter { it.isNotEmpty() }
                .collect { pendingVisits ->
                    for (visit in pendingVisits) {
                        appScope.launch { processAiPending(visit) }
                    }
                }
        }
    }

    private suspend fun processAiPending(visit: MedicalVisit) {
        try {
            val (systemPrompt, userPrompt) = AiPromptBuilder.buildMedicalSummaryPrompt(
                rawNote = visit.notes,
                ageMonths = Period.between(visit.date, LocalDate.now()).toTotalMonths().toInt(),
                gender = "",
                allergies = null
            )
            val result = aiDispatcher.executeWithSystemPrompt(
                task = AiTask.SUMMARIZE_MEDICAL_VISIT,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            )

            result.fold(
                onSuccess = { raw ->
                    runCatching {
                        val json = org.json.JSONObject(raw)
                        val diagnosisSummary = json.optString("diagnosisSummary", "")
                        val prescriptions = json.optJSONArray("prescriptions")
                            ?.let { arr ->
                                (0 until arr.length()).map { arr.optString(it) }
                            }?.joinToString("; ") ?: ""
                        val careInstructions = json.optJSONArray("careInstructions")
                            ?.let { arr ->
                                (0 until arr.length()).map { arr.optString(it) }
                            }?.joinToString("; ") ?: ""
                        val isUrgent = json.optString("safetyFlag", "normal") == "urgent"

                        // Write AI results to Firestore
                        val childId = visit.childId.toString()
                        val visitId = visit.id.toString()
                        val aiData = mapOf(
                            "diagnosisSummary" to diagnosisSummary,
                            "prescriptions" to prescriptions,
                            "careInstructions" to careInstructions,
                            "isUrgent" to isUrgent,
                            "aiPending" to false,
                            "lastModified" to System.currentTimeMillis()
                        )
                        firestore.collection("children").document(childId)
                            .collection("medicalVisits").document(visitId)
                            .set(aiData, SetOptions.merge()).await()

                        // Update local Room
                        medicalDao.updateAiFields(visit.id, diagnosisSummary, prescriptions, careInstructions, isUrgent)
                    }.onFailure { e ->
                        Log.w(TAG, "Failed to parse AI result for visit ${visit.id}: ${e.message}")
                        medicalDao.updateAiFields(visit.id, "", "", "", false)
                    }
                },
                onFailure = { err ->
                    Log.w(TAG, "AI dispatch failed for visit ${visit.id}: ${err.message}")
                    medicalDao.updateAiFields(visit.id, "", "", "", false)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "processAiPending error for visit ${visit.id}: ${e.message}")
            medicalDao.updateAiFields(visit.id, "", "", "", false)
        }
    }


}
