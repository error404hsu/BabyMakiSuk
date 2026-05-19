package com.babymakisuk.coreai

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory 滑動視窗限流器，依 [AiTask] 獨立計算配額。
 *
 * **修正**：原版使用 `@Synchronized` 會在 coroutine suspend 點阻塞執行緒池。
 * 改用 [Mutex] 確保 coroutine-safe 且不阻塞執行緒。
 *
 * 視窗大小：60 秒（[WINDOW_MS]）。
 * 各任務每分鐘上限見 [LIMITS]。
 *
 * App 重啟後自動重置，不依賴 DataStore 或本地儲存。
 * TODO (Step 5 選做)：考慮改為持久化，使重啟後限流狀態不被重置。
 */
@Singleton
class RateLimiter @Inject constructor() {

    private val WINDOW_MS = 60_000L

    /** 各 AiTask 每分鐘請求上限 */
    private val LIMITS: Map<AiTask, Int> = mapOf(
        AiTask.MEDICAL_CONSULTATION  to 5,
        AiTask.MEDICAL_OCR           to 3,
        AiTask.VOICE_INPUT           to 10,
        AiTask.MONTHLY_REPORT        to 2,
        AiTask.QUICK_CHAT            to 10,
        AiTask.CUSTOM_PRESET         to 5,
        AiTask.SUMMARIZE_MEDICAL_VISIT to 3
    )

    private val mutex = Mutex()

    /** 各 task 獨立的滑動視窗時間戳佇列 */
    private val timestamps: Map<AiTask, ArrayDeque<Long>> =
        AiTask.entries.associateWith { ArrayDeque() }

    /**
     * 檢查 [task] 是否在配額內。
     * - 未超限：記錄時間戳，回傳 `true`。
     * - 已超限：回傳 `false`，不記錄。
     *
     * 此函式為 `suspend`，使用 [Mutex] 而非 Java `synchronized`，
     * 確保在 coroutine 環境下不阻塞執行緒。
     */
    suspend fun checkAndRecord(task: AiTask): Boolean = mutex.withLock {
        val now   = System.currentTimeMillis()
        val deque = timestamps.getValue(task)
        val limit = LIMITS[task] ?: Int.MAX_VALUE

        deque.evictExpired(now)

        if (deque.size >= limit) return@withLock false
        deque.addLast(now)
        true
    }

    /**
     * 回傳 [task] 下次可用的等待秒數（已在配額內則回傳 0）。
     */
    suspend fun secondsUntilAvailable(task: AiTask): Long = mutex.withLock {
        val now   = System.currentTimeMillis()
        val deque = timestamps.getValue(task)
        val limit = LIMITS[task] ?: Int.MAX_VALUE

        deque.evictExpired(now)

        if (deque.size < limit) return@withLock 0L
        val availableAt = deque.first() + WINDOW_MS
        maxOf(0L, (availableAt - now + 999) / 1000)
    }

    private fun ArrayDeque<Long>.evictExpired(now: Long) {
        while (isNotEmpty() && now - first() >= WINDOW_MS) removeFirst()
    }
}
