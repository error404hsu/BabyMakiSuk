package com.babymakisuk.coredata

import android.util.Log
import com.babymakisuk.coreai.AiContextInjector
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.MedicalVisit
import org.json.JSONObject
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
    private val aiDispatcher: AiDispatcher,
    private val aiContextInjector: AiContextInjector,
    private val medicalDao: MedicalDao
) {

    companion object {
        private const val TAG = "MedicalAiRepository"

        private const val PHARMACIST_SYSTEM_PROMPT = """
你是一位專業的兒科藥師 AI 助理。
請根據提供的個案背景資訊與處方箋原始文字，分析用藥內容，
確認劑量是否符合兒童體重與年齡，並標注任何潛在用藥疑慮。
回應使用繁體中文，條列式呈現，保持精確與專業。
""".trimIndent()
    }

    /**
     * 使用 AI 整理就醫紀錄，回傳填入 diagnosisSummary / prescriptions / careInstructions 的 MedicalVisit。
     * 失敗時 fallback 回原始 entity，不覆寫既有資料。
     */
    suspend fun summarizeMedicalVisit(childId: Long, visitId: Long): MedicalVisit {
        val allVisits = medicalDao.getAllOnce()
        val visitEntity = allVisits.firstOrNull { it.id == visitId }
            ?: return MedicalVisit(
                id = visitId, childId = childId,
                date = java.time.LocalDate.now(),
                hospital = "", department = "", diagnosis = "",
                notes = "", attachments = emptyList(),
                diagnosisSummary = "", prescriptions = "", careInstructions = "",
                imageStoragePath = null, aiPending = false
            )

        return try {
            val contextBlock = aiContextInjector.buildContext(childId)
            val systemPrompt = buildString {
                appendLine(contextBlock)
                appendLine()
                appendLine("你是一位資深兒科醫師 AI 助理，請根據上方個案背景資訊，整理以下就醫紀錄。")
            }
            val userPrompt = buildString {
                appendLine("請整理以下就醫紀錄，回傳 JSON 格式：")
                appendLine("{\"diagnosisSummary\": string, \"prescriptions\": [string], \"careInstructions\": string}")
                appendLine()
                appendLine("原始備註：${visitEntity.notes}")
                appendLine("診斷：${visitEntity.diagnosis}")
                appendLine("科別：${visitEntity.department}")
                appendLine("醫院：${visitEntity.hospital}")
            }

            val response = aiDispatcher.executeWithSystemPrompt(
                task = AiTask.MEDICAL_CONSULTATION,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            )

            // 解析 JSON，容忍 markdown code block 包裝
            val jsonStr = response
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
            val json = JSONObject(jsonStr)
            val summary = json.optString("diagnosisSummary", "")
            val prescriptionsArr = json.optJSONArray("prescriptions")
            val prescriptions = buildString {
                if (prescriptionsArr != null) {
                    for (i in 0 until prescriptionsArr.length()) {
                        if (i > 0) append("; ")
                        append(prescriptionsArr.getString(i))
                    }
                }
            }
            val careInstructions = json.optString("careInstructions", "")

            visitEntity.toDomain().copy(
                diagnosisSummary = summary.ifBlank { visitEntity.diagnosisSummary },
                prescriptions = prescriptions.ifBlank { visitEntity.prescriptions },
                careInstructions = careInstructions.ifBlank { visitEntity.careInstructions },
                aiPending = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "summarizeMedicalVisit failed, fallback to original: ${e.message}")
            visitEntity.toDomain()
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
}
