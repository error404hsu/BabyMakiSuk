package com.babymakisuk.corefirebase.firestore

import android.util.Log
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.repository.MedicalRepository
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.di.ApplicationScope
import com.babymakisuk.coremodel.ImageStoragePath
import com.babymakisuk.coremodel.MedicalVisit
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFirestoreMedicalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val medicalRepo: MedicalRepository,
    private val childRepository: ChildRepository,
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

    /**
     * 刪除 Firestore 中對應的就診紀錄。
     * 先從 Room 查詢 childId，再刪除 Firestore 子集合文件。
     * 若 Room 已刪除（查無 childId），則 skip 並記錄 warning。
     */
    override suspend fun deleteVisit(visitId: Long) {
        val entity = medicalRepo.getById(visitId)
        if (entity == null) {
            Log.w(TAG, "deleteVisit: visitId=$visitId not found in Room, skipping Firestore delete")
            return
        }
        val childId = entity.childId.toString()
        val visitIdStr = visitId.toString()
        runCatching {
            firestore.collection("children").document(childId)
                .collection("medicalVisits").document(visitIdStr)
                .delete().await()
            Log.d(TAG, "deleteVisit: Firestore document deleted (childId=$childId, visitId=$visitIdStr)")
        }.onFailure { e ->
            Log.e(TAG, "deleteVisit: Firestore delete failed for visitId=$visitId: ${e.message}")
        }
    }

    override fun observeVisitsWithAiPending(): Flow<List<MedicalVisit>> =
        medicalRepo.observeAiPending()

    override suspend fun markAiProcessed(visitId: Long) {
        medicalRepo.updateAiFields(visitId, "", "", "", false)
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
            val child = childRepository.getById(visit.childId)
            val ageMonths = child?.ageMonths ?: java.time.temporal.ChronoUnit.MONTHS.between(
                java.time.LocalDate.from(visit.date), java.time.LocalDate.now()
            ).toInt()
            val (systemPrompt, userPrompt) = AiPromptBuilder.buildMedicalSummaryPrompt(
                rawNote = visit.notes,
                ageMonths = ageMonths,
                gender = child?.gender?.name ?: "UNKNOWN",
                allergies = child?.allergies
            )
            val result = aiDispatcher.executeWithSystemPrompt(
                task = AiTask.SUMMARIZE_MEDICAL_VISIT,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            )

            result.fold(
                onSuccess = { raw ->
                    runCatching {
                        val jsonResult = JSONObject(raw)
                        val diagnosisSummary = jsonResult.optString("diagnosisSummary", "")
                        val prescriptions = jsonResult.optJSONArray("prescriptions")
                            ?.let { arr ->
                                (0 until arr.length()).map { arr.optString(it) }
                            }?.joinToString("; ") ?: ""
                        val careInstructions = jsonResult.optJSONArray("careInstructions")
                            ?.let { arr ->
                                (0 until arr.length()).map { arr.optString(it) }
                            }?.joinToString("; ") ?: ""
                        val isUrgent = jsonResult.optString("safetyFlag", "normal") == "urgent"

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

                        medicalRepo.updateAiFields(visit.id, diagnosisSummary, prescriptions, careInstructions, isUrgent)
                    }.onFailure { e ->
                        Log.w(TAG, "Failed to parse AI result for visit ${visit.id}: ${e.message}")
                        medicalRepo.updateAiFields(visit.id, "", "", "", false)
                    }
                },
                onFailure = { err ->
                    Log.w(TAG, "AI dispatch failed for visit ${visit.id}: ${err.message}")
                    medicalRepo.updateAiFields(visit.id, "", "", "", false)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "processAiPending error for visit ${visit.id}: ${e.message}")
            medicalRepo.updateAiFields(visit.id, "", "", "", false)
        }
    }
}
