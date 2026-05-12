package com.babymakisuk.coreai

/**
 * AiPreset：Sprint 2 預設角色層。
 * 每個 entry 封裝角色的顯示名稱、描述、系統提示詞、偏好模型與對應的 AiTask。
 */
enum class AiPreset(
    val displayName: String,
    val description: String,
    val systemPrompt: String,
    val preferredModel: GeminiModel,
    val task: AiTask
) {
    PEDIATRIC_DOCTOR(
        displayName    = "兒科醫師",
        description    = "以專業但親切的口吻回答幼兒健康問題，並在適當時機建議就醫",
        systemPrompt   = "你是一位台灣兒科主治醫師，請以專業但親切的口吻回答家長關於幼兒健康的問題，並在適當時機建議就醫。",
        preferredModel = GeminiModel.GEMINI_3_FLASH,
        task           = AiTask.MEDICAL_CONSULTATION
    ),
    PHARMACIST(
        displayName    = "藥師",
        description    = "協助家長理解兒童用藥的成分、劑量與注意事項",
        systemPrompt   = "你是一位領有執照的台灣藥師，專門協助家長理解兒童用藥的成分、劑量與注意事項。",
        preferredModel = GeminiModel.GEMINI_3_FLASH,
        task           = AiTask.MEDICAL_OCR
    ),
    NUTRITIONIST(
        displayName    = "營養師",
        description    = "依據幼兒月齡提供副食品建議與飲食規劃",
        systemPrompt   = "你是一位專精嬰幼兒營養的台灣營養師，請依據幼兒月齡提供副食品建議與飲食規劃。",
        preferredModel = GeminiModel.GEMINI_31_FLASH_LITE,
        task           = AiTask.QUICK_CHAT
    ),
    GROWTH_ANALYST(
        displayName    = "發育分析師",
        description    = "根據身高體重數據解釋百分位意義與發育趨勢",
        systemPrompt   = "你是一位專注於嬰幼兒生長發育的分析師，請根據提供的身高體重數據，解釋百分位意義與發育趨勢。",
        preferredModel = GeminiModel.GEMMA_4_31B,
        task           = AiTask.QUICK_CHAT
    ),
    CUSTOM(
        displayName    = "自由模式",
        description    = "輸入任何問題，AI 自由回答",
        systemPrompt   = "",
        preferredModel = GeminiModel.GEMINI_31_FLASH_LITE,
        task           = AiTask.CUSTOM_PRESET
    );

    companion object {
        val default: AiPreset = CUSTOM

        fun fromHint(hint: String?): AiPreset =
            values().find { it.name == hint } ?: CUSTOM
    }
}
