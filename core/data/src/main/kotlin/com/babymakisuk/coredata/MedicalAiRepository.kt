package com.babymakisuk.coredata

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.babymakisuk.coredata.ai.AiContextInjector
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coremodel.MedicalSummaryResult
import com.babymakisuk.coremodel.PrescriptionAnalysisResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MedicalAiRepository — Sprint 3
 *
 * 結合 AiDispatcher + AiContextInjector，
 * 提供就醫紀錄 AI 整理與處方箋分析功能。
 * 所有 AI 呼叫失敗均 fallback 回原始資料，不讓 UI 進入永久錯誤狀態。
 */
@Singleton
class MedicalAiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiDispatcher: AiDispatcher,
    private val aiContextInjector: AiContextInjector,
    private val medicalDao: MedicalDao
) {

    companion object {
        private const val TAG = "MedicalAiRepository"

        private val PHARMACIST_SYSTEM_PROMPT = """
你是一位專業的兒科藥師 AI 助理。
請根據提供的個案背景資訊與處方箋原始文字，分析用藥內容，
確認劑量是否符合兒童體重與年齡，並標注任何潛在用藥疑慮。
回應使用繁體中文，條列式呈現，保持精確與專業。
""".trimIndent()
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 使用 AI 整理就醫紀錄備註，回傳結構化 MedicalSummaryResult。
     * 失敗時 fallback：整段文字塞入 diagnosisSummary。
     */
    suspend fun summarizeMedicalVisit(
        visitId: Long,
        rawNote: String,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): Result<MedicalSummaryResult> = runCatching {
        val (systemPrompt, userPrompt) = AiPromptBuilder.buildMedicalSummaryPrompt(
            rawNote, ageMonths, gender, allergies
        )
        val raw = aiDispatcher.executeWithSystemPrompt(
            task         = AiTask.MEDICAL_CONSULTATION,
            systemPrompt = systemPrompt,
            userPrompt   = userPrompt
        )
        try {
            json.decodeFromString<MedicalSummaryResult>(raw)
        } catch (_: SerializationException) {
            MedicalSummaryResult(
                diagnosisSummary  = raw.take(200),
                prescriptions     = emptyList(),
                careInstructions  = emptyList(),
                safetyFlag        = "normal"
            )
        }
    }

    /**
     * 分析處方箋原始文字，回傳 AI 藥師解析文字。
     * 失敗時回傳空字串，不拋例外。
     */
    suspend fun analyzePrescription(childId: Long, rawText: String): String {
        return try {
            val contextBlock = aiContextInjector.buildContext(childId)
            val systemPrompt = buildString {
                appendLine(PHARMACIST_SYSTEM_PROMPT)
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

    /**
     * 藥單圖片多模態分析。
     * 讀取 Uri 轉換為 Bitmap 後，呼叫 AiDispatcher.executeWithImage。
     */
    suspend fun analyzePrescriptionImage(
        imageUri: Uri,
        symptomHint: String,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): Result<PrescriptionAnalysisResult> = runCatching {
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalArgumentException("無法讀取圖片 Uri: $imageUri")

        val systemPrompt = buildString {
            appendLine(PHARMACIST_SYSTEM_PROMPT)
            appendLine("\n[個案資訊]")
            appendLine("年齡：${ageMonths}個月")
            appendLine("性別：$gender")
            appendLine("過敏史：${allergies ?: "無"}")
            appendLine("使用者描述症狀：$symptomHint")
            appendLine("\n[輸出規範]")
            appendLine("請分析圖片中的處方箋藥物，並嚴格以 JSON 格式回傳：")
            appendLine("""{ "diagnosisSummary": "...", "prescriptions": ["藥名1", "藥名2"], "careInstructions": ["建議1"], "confidence": 90 }""")
        }

        val raw = aiDispatcher.executeWithImage(
            task = AiTask.MEDICAL_OCR,
            systemPrompt = systemPrompt,
            userPrompt = "請辨識並分析這張處方箋圖片。",
            image = bitmap
        )

        try {
            // 嘗試解析 JSON，Gemini 有時會帶 ```json ... ``` 需要處理
            val cleanJson = raw.substringAfter("```json").substringBefore("```").trim()
            val jsonToDecode = cleanJson.ifBlank { raw }
            json.decodeFromString<PrescriptionAnalysisResult>(jsonToDecode)
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse failed, fallback to raw text: ${e.message}")
            PrescriptionAnalysisResult(
                diagnosisSummary = raw.take(500),
                prescriptions = emptyList(),
                careInstructions = emptyList(),
                confidence = 40
            )
        }
    }
}
