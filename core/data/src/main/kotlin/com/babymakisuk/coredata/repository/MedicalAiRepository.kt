package com.babymakisuk.coredata.repository

import android.net.Uri
import android.util.Log
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.PrescriptionImagePreprocessor
import com.babymakisuk.coredata.ai.AiContextInjector
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.AiInsightEntity
import com.babymakisuk.coremodel.MedicalSummaryResult
import com.babymakisuk.coremodel.PrescriptionAnalysisResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

/**
 * MedicalAiRepository — Sprint 3
 *
 * 結合 AiDispatcher + AiContextInjector，
 * 提供就醫紀錄 AI 整理與處方箋分析功能。
 */
interface MedicalAiRepository {
    suspend fun summarizeMedicalVisit(
        visitId: Long,
        childId: Long,
        rawNote: String,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): Result<MedicalSummaryResult>

    suspend fun analyzePrescription(childId: Long, rawText: String): String

    suspend fun analyzePrescriptionImage(
        imageUri: Uri,
        symptomHint: String,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): Result<Pair<PrescriptionAnalysisResult, String>>
}

@Singleton
class DefaultMedicalAiRepository @Inject constructor(
    private val aiDispatcher: AiDispatcher,
    private val aiContextInjector: AiContextInjector,
    private val aiInsightDao: AiInsightDao,
    private val preprocessor: PrescriptionImagePreprocessor,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MedicalAiRepository {

    companion object {
        private const val TAG = "MedicalAiRepository"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun summarizeMedicalVisit(
        visitId: Long,
        childId: Long,
        rawNote: String,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): Result<MedicalSummaryResult> = withContext(ioDispatcher) {
        runCatching {
            val (systemPrompt, userPrompt) = AiPromptBuilder.buildMedicalSummaryPrompt(
                rawNote, ageMonths, gender, allergies
            )
            val raw = aiDispatcher.executeWithSystemPrompt(
                task         = AiTask.MEDICAL_CONSULTATION,
                systemPrompt = systemPrompt,
                userPrompt   = userPrompt
            ).getOrThrow()
            val result = try {
                json.decodeFromString<MedicalSummaryResult>(raw)
            } catch (_: SerializationException) {
                MedicalSummaryResult(
                    diagnosisSummary  = raw.take(200),
                    prescriptions     = emptyList(),
                    careInstructions  = emptyList(),
                    safetyFlag        = "normal"
                )
            }

            // 自動產出 AI 精華卡
            try {
                val insight = AiInsightEntity(
                    id         = "medical_${visitId}_${System.currentTimeMillis()}",
                    childId    = childId.toString(),
                    title      = "就醫摘要",
                    content    = result.diagnosisSummary.take(200),
                    sourceDate = System.currentTimeMillis(),
                    createdAt  = System.currentTimeMillis()
                )
                aiInsightDao.insert(insight)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create AiInsight from medical summary: ${e.message}")
            }

            result
        }
    }

    override suspend fun analyzePrescription(childId: Long, rawText: String): String = withContext(ioDispatcher) {
        try {
            val contextBlock = aiContextInjector.buildContext(childId)
            val systemPrompt = AiPromptBuilder.buildSystemPromptWithContext(
                preset = AiPreset.PHARMACIST,
                contextBlock = contextBlock
            )
            aiDispatcher.executeWithSystemPrompt(
                task = AiTask.MEDICAL_OCR,
                systemPrompt = systemPrompt,
                userPrompt = rawText
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e(TAG, "analyzePrescription failed: ${e.message}")
            ""
        }
    }

    override suspend fun analyzePrescriptionImage(
        imageUri: Uri,
        symptomHint: String,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): Result<Pair<PrescriptionAnalysisResult, String>> = withContext(ioDispatcher) {
        runCatching {
            val bitmap = preprocessor.process(imageUri)

            // 留存圖片至內部儲存，7 天後自動清理
            val savedFile = preprocessor.saveToInternal(bitmap)
            val imagePath = savedFile.absolutePath

            val systemPrompt = AiPromptBuilder.buildPrescriptionImagePrompt(
                ageMonths = ageMonths,
                gender = gender,
                allergies = allergies,
                symptomHint = symptomHint
            )

            val raw = aiDispatcher.executeWithImage(
                task = AiTask.MEDICAL_OCR,
                systemPrompt = systemPrompt,
                userPrompt = "請辨識並分析這張處方箋圖片。",
                image = bitmap
            ).getOrThrow()

            val result = try {
                val cleanJson = raw.substringAfter("```json")
                    .substringBefore("```")
                    .trim()
                    .ifBlank { raw }
                json.decodeFromString<PrescriptionAnalysisResult>(cleanJson)
            } catch (e: Exception) {
                Log.w(TAG, "JSON parse failed, fallback to raw text: ${e.message}")
                PrescriptionAnalysisResult(
                    diagnosisSummary = raw.take(500),
                    prescriptions = emptyList(),
                    careInstructions = emptyList(),
                    confidence = 40
                )
            }

            Pair(result, imagePath)
        }
    }
}
