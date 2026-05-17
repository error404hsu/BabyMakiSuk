package com.babymakisuk.coreai

/**
 * AI 層統一錯誤模型。
 *
 * UI 層應透過 when(AiError) 分支處理，而非 try-catch 多個例外型別。
 *
 * ## 設計原則
 * - [RateLimited]：使用者操作過快，UI 顯示倒數並禁用按鈕。
 * - [AllModelsFailed]：所有 Fallback 均失敗，UI 顯示「AI 暫時無法使用」並提供重試。
 * - [InvalidConfig]：API Key 未設定，UI 顯示設定引導（Debug 用）。
 * - [Cancelled]：使用者主動取消，UI 靜默處理或還原狀態。
 *
 * ⚠️ 所有 AI 結果僅供參考，非醫療診斷依據。
 *
 * TODO (Step 2)：完成 feature/ 層遷移後，刪除舊的 AiDispatchException.kt 與 RateLimitException.kt。
 */
sealed class AiError : Exception() {

    /** 超出每分鐘配額，[waitSeconds] 為建議等待秒數。 */
    data class RateLimited(
        val task: AiTask,
        val waitSeconds: Long
    ) : AiError() {
        override val message: String
            get() = "[${task.displayName}] 請求過於頻繁，請等待 ${waitSeconds} 秒後再試。"
    }

    /** Fallback Chain 所有模型均失敗，[cause] 為最後一個錯誤。 */
    data class AllModelsFailed(
        val task: AiTask,
        override val cause: Throwable?
    ) : AiError() {
        override val message: String
            get() = "[${task.displayName}] AI 服務暫時無法使用，請稍後重試。"
    }

    /** API Key 無效或未設定（通常為 Debug 環境問題）。 */
    data object InvalidConfig : AiError() {
        override val message: String = "AI 設定不完整，請確認 API Key 已正確設定。"
    }

    /** 呼叫端透過 Job.cancel() 主動取消，UI 不需顯示錯誤。 */
    data object Cancelled : AiError() {
        override val message: String = "AI 任務已取消。"
    }
}
