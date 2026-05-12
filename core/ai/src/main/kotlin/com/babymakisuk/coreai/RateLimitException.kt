package com.babymakisuk.coreai

/**
 * 當 [RateLimiter.checkAndRecord] 回傳 false 時由 [AiDispatcher] 拋出。
 *
 * @param task             超限的任務類型
 * @param secondsRemaining 距離下次可用的剩餘秒數
 */
class RateLimitException(
    val task: AiTask,
    val secondsRemaining: Long
) : Exception("Rate limit exceeded for $task. Retry in ${secondsRemaining}s")
