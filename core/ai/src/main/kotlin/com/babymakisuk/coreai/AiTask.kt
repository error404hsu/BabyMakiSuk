package com.babymakisuk.coreai

/**
 * 定義 AiDispatcher 支援的所有 AI 任務類型。
 * 每個任務對應獨立的 RateLimiter 配額與 Fallback Chain。
 *
 * [displayName] 供 UI 顯示「AI 正在處理...」的狀態文字用，
 * 不應用於商業邏輯判斷。
 */
enum class AiTask(val displayName: String) {
    /** 就醫諮詢：針對病症與醫療問題進行 AI 解答 */
    MEDICAL_CONSULTATION("就醫諮詢"),
    /** 藥單圖片辨識：OCR + 結構化藥品資訊萃取 */
    MEDICAL_OCR("藥單辨識"),
    /** 語音輸入前處理（STT 佔位，尚未實作 STT 邏輯） */
    VOICE_INPUT("語音輸入"),
    /** 月報彙整：整合當月日誌產出摘要 */
    MONTHLY_REPORT("月報彙整"),
    /** 單輪快速聊天：關閉視窗即清空對話記憶 */
    QUICK_CHAT("快速問答"),
    /** 自定義 System Prompt：由呼叫端注入任意 system instruction */
    CUSTOM_PRESET("自定義助理")
}
