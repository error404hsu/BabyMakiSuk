package com.babymakisuk.coredata.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.PrescriptionImagePreprocessor
import com.babymakisuk.coredata.ai.AiContextInjector
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.AiInsightEntity
import com.babymakisuk.coremodel.MedicalSummaryResult
import com.babymakisuk.coremodel.PrescriptionAnalysisResult
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val aiDispatcher: AiDispatcher,
    private val aiContextInjector: AiContextInjector,
    private val medicalDao: MedicalDao,
    private val aiInsightDao: AiInsightDao,
    private val preprocessor: PrescriptionImagePreprocessor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MedicalAiRepository {

    companion object {
        private const val TAG = "MedicalAiRepository"

        /** OCR 專屬輸出規範，與 AiPreset.PHARMACIST.systemPrompt 分離管理 */
        private val OCR_OUTPUT_SPEC = """
            
            【OCR 輸出規範 - 嚴格遵守】
            
            ▌請先辨識圖片中的藥名、劑量、用法等文字資訊。
            ▌接著以藥師角度分析以下三項，並嚴格以 JSON 格式回傳：
            { "diagnosisSummary": "診斷摘要（50字內）", "prescriptions": ["藥名 劑量 用法", ...], "careInstructions": ["居家照護建議", ...], "confidence": 0-100 }
            ▌confidence 為你對此分析結果的信心分數（0-100），請據實評估。
            ▌只輸出 JSON 物件，不得有任何前綴、後綴、Markdown 包裝或說明文字。
        """.trimIndent()
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
            )
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
            val systemPrompt = buildString {
                appendLine(AiPreset.PHARMACIST.systemPrompt)
                appendLine()
                append(contextBlock)
            }
            aiDispatcher.executeWithSystemPrompt(
                task = AiTask.MEDICAL_OCR,
                systemPrompt = systemPrompt,
                userPrompt = rawText
            )
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

            val systemPrompt = buildString {
                appendLine(AiPreset.PHARMACIST.systemPrompt)
                appendLine()
                appendLine("【個案資訊】")
                appendLine("年齡：${ageMonths}個月")
                appendLine("性別：$gender")
                appendLine("過敏史：${allergies ?: "無"}")
                appendLine("使用者描述症狀：$symptomHint")
                append(OCR_OUTPUT_SPEC)
            }

            val raw = aiDispatcher.executeWithImage(
                task = AiTask.MEDICAL_OCR,
                systemPrompt = systemPrompt,
                userPrompt = "請辨識並分析這張處方箋圖片。",
                image = bitmap
            )

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
