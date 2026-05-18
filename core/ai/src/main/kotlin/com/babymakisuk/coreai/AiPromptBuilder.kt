package com.babymakisuk.coreai

/**
 * AiPromptBuilder：組合動態 System Prompt，將個案資訊與全域限制注入角色 prompt。
 *
 * ## 組合順序
 * 1. [AiPreset.systemPrompt]（角色人格與專業定義）
 * 2. 個案資訊 / Context Block（月齡、性別、過敏史等）
 * 3. [AiSystemConstraints.GLOBAL_CONSTRAINTS]（語言、風格、安全、格式限制）
 *
 * 此順序確保角色定義在前、限制在後，LLM 傾向以最末端的 instruction 為優先。
 */
object AiPromptBuilder {

    /**
     * 依 [preset] 與幼兒個案資訊組合完整 system prompt。
     * 末尾自動附加 [AiSystemConstraints.GLOBAL_CONSTRAINTS]。
     *
     * - 若 [preset.systemPrompt] 為空白（CUSTOM 模式），仍附加全域限制。
     */
    fun buildSystemPrompt(
        preset: AiPreset,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): String {
        return buildString {
            if (preset.systemPrompt.isNotBlank()) {
                append(preset.systemPrompt)
                append("\n\n")
            }
            append("【當前個案資訊】\n")
            append("當前月齡：${ageMonths} 個月\n")
            append("性別：${gender}\n")
            append("過敏史：${allergies ?: "無"}")
            append("\n\n")
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
    }

    /**
     * 依 [preset] 與已組裝好的 [contextBlock] 合併 system prompt。
     * Sprint 3 overload：供 AiContextInjector 注入完整 RAG context 使用。
     * 末尾自動附加 [AiSystemConstraints.GLOBAL_CONSTRAINTS]。
     */
    fun buildSystemPromptWithContext(
        preset: AiPreset,
        contextBlock: String
    ): String {
        return buildString {
            if (preset.systemPrompt.isNotBlank()) {
                append(preset.systemPrompt)
                append("\n\n")
            }
            append(contextBlock)
            append("\n\n")
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
    }

    /**
     * 組合對話摘要知識卡的 System Prompt。
     * 強制要求 LLM 輸出嚴格 JSON（title + content），不得有任何前綴文字或 Markdown。
     *
     * @param childName 幼兒姓名，用於 prompt 個人化
     * @return systemPrompt String
     */
    fun buildSummarySystemPrompt(childName: String): String {
        return buildString {
            appendLine("你是一位對話摘要 AI，專門將關於 ${childName} 的育兒對話整理成結構化知識卡。")
            appendLine()
            appendLine("【輸出規則 - 嚴格遵守】")
            appendLine("- 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、說明文字")
            appendLine("- 禁止使用 Markdown 包裝（禁止 ```json```）")
            appendLine("- JSON schema：")
            appendLine("""{ "title": "簡潔標題（15字內，以繁體中文撰寫）", "content": "重點摘要整理（200字內，以繁體中文撰寫）" }""")
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
    }

    /**
     * 組合病歷摘要的 System Prompt + User Prompt。
     * 強制要求 LLM 輸出嚴格 JSON，不得有任何前綴文字或 Markdown。
     *
     * @return Pair(systemPrompt, userPrompt)
     */
    fun buildMedicalSummaryPrompt(
        rawNote: String,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): Pair<String, String> {
        val system = buildString {
            appendLine("你是一位台灣兒科醫療摘要 AI，專門將家長輸入的就診備註結構化。")
            appendLine()
            appendLine("【當前個案】")
            appendLine("月齡：${ageMonths} 個月 | 性別：${gender} | 過敏史：${allergies ?: "無"}")
            appendLine()
            appendLine("【輸出規則 - 嚴格遵守】")
            appendLine("- 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、說明文字")
            appendLine("- 不使用 Markdown 包裝（禁止 ```json```）")
            appendLine("- JSON schema（所有欄位必填，無資料填空字串或空陣列）：")
            appendLine(
                """
{
  "diagnosisSummary": "string（50字以內的診斷摘要，以繁體中文撰寫）",
  "prescriptions": ["string（藥名 劑量 頻率，以繁體中文撰寫）", ...],
  "careInstructions": ["string（照護建議，以繁體中文撰寫）", ...],
  "safetyFlag": "normal | attention | urgent"
}
                """.trimIndent()
            )
            appendLine()
            appendLine("- safetyFlag 判斷標準：")
            appendLine("  normal    = 一般就診，無特殊提醒")
            appendLine("  attention = 需持續觀察（如發燒超過3天、特殊藥物）")
            appendLine("  urgent    = 需立即就醫（如過敏反應、呼吸困難等描述）")
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
        val user = "請摘要以下就診備註：\n$rawNote"
        return Pair(system, user)
    }

    /**
     * 組合每週成長總結的 System Prompt + User Prompt。
     *
     * @param childName      幼兒姓名
     * @param ageMonths      幼兒月齡
     * @param weekLabel      週次標籤，例："2026 年第 20 週（5/11–5/17）"
     * @param dailyLogsBlock 由 AiContextInjector 組裝的 7 天日誌文字
     * @param recentMedical  同期就診摘要（可為 null）
     * @return Pair(systemPrompt, userPrompt)
     */
    fun buildWeeklyLogSummaryPrompt(
        childName: String,
        ageMonths: Int,
        weekLabel: String,
        dailyLogsBlock: String,
        recentMedical: String?
    ): Pair<String, String> {
        val system = buildString {
            appendLine("你是一位幼兒成長週報 AI 撰稿員，專門為家長生成溫馨且具體的每週成長總結。")
            appendLine()
            appendLine("【輸出規則】")
            appendLine("- 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、說明文字")
            appendLine("- 禁止 Markdown 包裝")
            appendLine("- JSON schema：")
            appendLine(
                """
{
  "weekSummary": "string（整週總結，150字以內，溫馨語氣，以繁體中文撰寫）",
  "highlights": ["string（本週亮點，3條以內，以繁體中文撰寫）"],
  "parentTips": ["string（給家長的具體建議，2條，以繁體中文撰寫）"],
  "searchKeywords": ["string（3-5個關鍵字，以繁體中文撰寫，供 FTS 搜尋）"]
}
                """.trimIndent()
            )
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
        val user = buildString {
            appendLine("請根據以下資料，為 ${childName}（${ageMonths}個月）生成 ${weekLabel} 的成長週報：")
            appendLine()
            appendLine("【本週日誌】")
            appendLine(dailyLogsBlock)
            if (!recentMedical.isNullOrBlank()) {
                appendLine()
                appendLine("【同期就診紀錄】")
                append(recentMedical)
            }
        }
        return Pair(system, user)
    }

    /**
     * 組合每月成長總結的 System Prompt + User Prompt。
     *
     * @param monthLabel         月份標籤，例："2026 年 5 月"
     * @param dailyLogsBlock     由 Repository 組裝的當月日誌文字
     * @param recentMedical      當月就診摘要（可為 null）
     * @param systemReminderBlock 當月系統提醒（可為 null）
     * @return Pair(systemPrompt, userPrompt)
     */
    fun buildMonthlyLogSummaryPrompt(
        monthLabel: String,
        dailyLogsBlock: String,
        recentMedical: String?,
        systemReminderBlock: String?
    ): Pair<String, String> {
        val system = buildString {
            appendLine("你是一位幼兒成長月報 AI 撰稿員，專門為家長生成溫馨且具體的每月成長總結。")
            appendLine("本月報為合併報告，可能包含多位孩子（雙胞胎）的資料，請在總結中均衡提到他們的狀況。")
            appendLine()
            appendLine("【輸出規則】")
            appendLine("- 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、說明文字")
            appendLine("- 禁止 Markdown 包裝")
            appendLine("- JSON schema：")
            appendLine(
                """
{
  "monthSummary": "string（整月總結，200字以內，溫馨語氣，以繁體中文撰寫）",
  "highlights": ["string（本月亮點，3條以內，以繁體中文撰寫）"],
  "parentTips": ["string（給家長的具體建議，2條，以繁體中文撰寫）"],
  "searchKeywords": ["string（3-5個關鍵字，以繁體中文撰寫，供 FTS 搜尋）"]
}
                """.trimIndent()
            )
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
        val user = buildString {
            appendLine("請根據以下資料，為孩子們生成 ${monthLabel} 的成長月報：")
            appendLine()
            appendLine("【本月日誌】")
            appendLine(dailyLogsBlock)
            if (!recentMedical.isNullOrBlank()) {
                appendLine()
                appendLine("【本月就診紀錄】")
                append(recentMedical)
            }
            if (!systemReminderBlock.isNullOrBlank()) {
                appendLine()
                appendLine("【本月系統提醒】")
                append(systemReminderBlock)
            }
        }
        return Pair(system, user)
    }

    /**
     * 組合處方箋圖片辨識的 System Prompt。
     */
    fun buildPrescriptionImagePrompt(
        ageMonths: Int,
        gender: String,
        allergies: String?,
        symptomHint: String
    ): String {
        return buildString {
            appendLine(AiPreset.PHARMACIST.systemPrompt)
            appendLine()
            appendLine("【個案資訊】")
            appendLine("年齡：${ageMonths}個月 | 性別：$gender | 過敏史：${allergies ?: "無"}")
            appendLine("使用者描述：$symptomHint")
            appendLine()
            appendLine("【OCR 輸出規範 - 嚴格遵守】")
            appendLine("1. 請先辨識圖片中的藥名、劑量、用法等文字資訊。")
            appendLine("2. 以藥師角度分析以下三項，並嚴格以 JSON 格式回傳（以繁體中文撰寫）：")
            appendLine(
                """
{
  "diagnosisSummary": "診斷摘要（50字內，以繁體中文撰寫）",
  "prescriptions": ["藥名 劑量 用法（以繁體中文撰寫）", ...],
  "careInstructions": ["居家照護建議（以繁體中文撰寫）", ...],
  "confidence": 0-100
}
                """.trimIndent()
            )
            appendLine("3. confidence 為你對此分析結果的信心分數（0-100），請據實評估。")
            appendLine("4. 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、Markdown 包裝或說明文字。")
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
    }
}
