package com.babymakisuk.coreai

/**
 * 當 [AiDispatcher] 的所有 Fallback 模型均失敗時拋出。
 *
 * @param task    失敗的任務類型
 * @param message 最後一個錯誤的描述
 * @param cause   最後一個錯誤的原始例外
 */
class AiDispatchException(
    val task: AiTask,
    message: String,
    cause: Throwable? = null
) : Exception("AiDispatch failed [$task]: $message", cause)
