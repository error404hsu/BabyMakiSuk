package com.babymakisuk.coreai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory 滑動視窗限流器，依 [AiTask] 獨立計算配額。
 * App 重啟後自動重置，不依賴 DataStore 或本地儲存。
 *
 * 視窗大小：60 秒（[WINDOW_MS]）。
 * 各任務每分鐘上限見 [LIMITS]。
 */
@Singleton
class RateLimiter @Inject constructor() {

    private val WINDOW_MS = 60_000L

    /** 各 AiTask 每分鐘請求上限 */
    private val LIMITS: Map<AiTask, Int> = mapOf(
        AiTask.MEDICAL_CONSULTATION to 5,
        AiTask.MEDICAL_OCR          to 3,
        AiTask.VOICE_INPUT          to 10,
        AiTask.WEEKLY_REPORT        to 2,
        AiTask.QUICK_CHAT           to 10,
        AiTask.CUSTOM_PRESET        to 5
    )

    /** 滑動視窗：每個 task 保存最近 N 次請求的時間戳（毫秒） */
    private val timestamps: MutableMap<AiTask, ArrayDeque<Long>> =
        AiTask.entries.associateWith { ArrayDeque<Long>() }.toMutableMap()

    /**
     * 檢查 [task] 是否仍在配額內。
     * - 若未超限：記錄當前時間戳並回傳 `true`。
     * - 若已超限：回傳 `false`，不記錄時間戳。
     */
    @Synchronized
    fun checkAndRecord(task: AiTask): Boolean {
        val now   = System.currentTimeMillis()
        val deque = timestamps.getOrPut(task) { ArrayDeque() }
        val limit = LIMITS[task] ?: Int.MAX_VALUE

        // 清除已超出視窗的舊時間戳
        while (deque.isNotEmpty() && now - deque.first() >= WINDOW_MS) {
            deque.removeFirst()
        }

        if (deque.size >= limit) return false

        deque.addLast(now)
        return true
    }

    /**
     * 回傳距離 [task] 下次可發送請求的等待秒數（已在配額內則回傳 0）。
     */
    @Synchronized
    fun secondsUntilAvailable(task: AiTask): Long {
        val now   = System.currentTimeMillis()
        val deque = timestamps.getOrPut(task) { ArrayDeque() }
        val limit = LIMITS[task] ?: Int.MAX_VALUE

        // 清除已超出視窗的舊時間戳
        while (deque.isNotEmpty() && now - deque.first() >= WINDOW_MS) {
            deque.removeFirst()
        }

        if (deque.size < limit) return 0L

        // 最舊的時間戳 + 視窗大小 = 下次可用時間點
        val availableAt = deque.first() + WINDOW_MS
        return maxOf(0L, (availableAt - now + 999) / 1000)  // 無條件進位至秒
    }
}
