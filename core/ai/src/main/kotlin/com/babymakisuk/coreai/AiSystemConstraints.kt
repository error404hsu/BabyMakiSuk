package com.babymakisuk.coreai

/**
 * AiSystemConstraints：所有 AI 角色共用的全域行為限制層。
 *
 * 此物件集中定義注入至每個角色 System Prompt 末尾的約束指令，
 * 確保無論選用哪個 AiPreset，AI 回覆行為皆符合 App 規範。
 *
 * ## 設計原則
 * - 本層僅由 [AiPromptBuilder] 統一附加，feature 層不需感知。
 * - 新增限制只需修改 [GLOBAL_CONSTRAINTS]，所有角色自動套用。
 * - 分四類管理：語言、風格、安全、格式。
 *
 * [REFERENCE_DISCLAIMER] 供 UI 層在 AI 結果旁邊顯示，不應省略。
 * 建議顯示為小字灰色文字，置於結果卡片底部。
 *
 * 範例（Compose）：
 * ```kotlin
 * Text(
 *     text = AiSystemConstraints.REFERENCE_DISCLAIMER,
 *     style = MaterialTheme.typography.labelSmall,
 *     color = MaterialTheme.colorScheme.onSurfaceVariant
 * )
 * ```
 */
object AiSystemConstraints {

    /**
     * 全域約束指令，附加於所有角色 System Prompt 末尾。
     */
    val GLOBAL_CONSTRAINTS: String = """
        
        【全域行為規範 - 請嚴格遵守】

        ▌語言規範
        - 必須以繁體中文回覆，禁止使用簡體中文或其他語言。
        - 醫學術語可附英文原文於括號內，例如：過敏（Allergy）。

        ▌回覆風格
        - 只說重點，禁止問候、寒暄、填充語（例如：「當然！」「很高興為您服務」）。
        - 禁止重複使用者已知的問題描述，直接進入建議。
        - 每則回覆長度以 300 字以內為原則，確有必要時可適當延伸。
        - 條列優先：可使用條列式（-）或編號，避免大段連續文字。

        ▌安全規範
        - 嚴禁給予具體劑量的用藥指示，請一律建議諮詢醫師或藥師。
        - 若問題超出本角色專業範疇，請明確說明並建議就醫或轉介。
        - 禁止回覆任何與育兒無關的話題（政治、娛樂、金融等）。

        ▌輸出格式
        - 若需分段，使用「▌」作為段落標題前綴。
        - 禁止使用 Markdown 語法（** # ``` 等），因為輸出會顯示在純文字 UI 中。
    """.trimIndent()

    /**
     * UI 層在顯示 AI 生成結果時，必須在結果旁附上此聲明。
     * 建議顯示為小字灰色文字，置於結果卡片底部。
     */
    const val REFERENCE_DISCLAIMER = "⚠️ AI 結果僅供參考，不構成醫療診斷建議，請諮詢專業兒科醫師。"
}
