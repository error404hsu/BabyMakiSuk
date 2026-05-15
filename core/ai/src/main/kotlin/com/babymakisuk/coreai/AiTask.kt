package com.babymakisuk.coreai

/**
 * 定義 AiDispatcher 支援的所有 AI 任務類型。
 * 每個任務對應獨立的 RateLimiter 配額與 Fallback Chain。
 */
enum class AiTask {
    /** 就醫諮詢：針對病症與醫療問題進行 AI 解答 */
    MEDICAL_CONSULTATION,
    /** 藥單圖片辨識：OCR + 結構化藥品資訊萃取 */
    MEDICAL_OCR,
    /** 語音輸入前處理（STT 佔位，尚未實作 STT 邏輯） */
    VOICE_INPUT,
    /** 月報彙整：整合當月日誌產出摘要 */
    MONTHLY_REPORT,
    /** 單輪快速聊天：關閉視窗即清空對話記憶 */
    QUICK_CHAT,
    /** 自定義 System Prompt：由呼叫端注入任意 system instruction */
    CUSTOM_PRESET
}
