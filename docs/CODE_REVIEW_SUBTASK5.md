# Code Review — 子任務 5：Firebase 整合與 Worker

> 審查日期：2026-05-19
> 審查範圍：Firebase Auth、Firestore、Storage、DataRetentionWorker、MemoReminderWorker、StorageCleanupWorker

---

## 🔴 嚴重問題

### 1. observeVisitsWithAiPending 傳入 childId=0L，永遠回傳空

**檔案：** `DefaultFirestoreMedicalRepository.kt:92-95`

```kotlin
override fun observeVisitsWithAiPending(): Flow<List<MedicalVisit>> =
    medicalDao.observeByChild(0L).map { list ->
        list.filter { it.aiPending }.map { it.toDomain() }
    }
```

`medicalDao.observeByChild(0L)` 查詢 `WHERE childId = 0`。但所有有效的 childId 從 1 起跳。因此 `observeAndDispatchAiPending()` 永遠不會撈到任何待處理的 `aiPending` 紀錄。

這意味著 **aiPending 自動摘要機制完全無法作用**。

**建議：** 新增一個 DAO 方法：
```kotlin
@Query("SELECT * FROM medical_visit WHERE aiPending = 1")
fun observeAiPending(): Flow<List<MedicalVisitEntity>>
```

---

### 2. StorageCleanupWorker 直接注入 DAO

**檔案：** `StorageCleanupWorker.kt:25`

```kotlin
class StorageCleanupWorker @AssistedInject constructor(
    ...
    private val medicalDao: MedicalDao,
```

違反 AGENTS.md Repository 模式。Worker 也應透過 Repository 操作資料。

**建議：** 注入 `MedicalRepository`（需先建立）或建立專用的 cleanup UseCase。

---

### 3. MemoReminderWorker 未使用 @HiltWorker

**檔案：** `MemoReminderWorker.kt:18-21`

```kotlin
class MemoReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams)
```

與 `DataRetentionWorker` 和 `StorageCleanupWorker`（均使用 `@HiltWorker` + `@AssistedInject`）不一致。此 Worker 無法注入任何 Hilt 依賴，需手動解析。

**建議：** 加上 `@HiltWorker` annotation、`@AssistedInject constructor`、`@Assisted` 參數，以符合專案規範。

---

## 🟡 架構與設計問題

### 4. StorageRepository.getUsedBytes 遞迴掃描效能災難

**檔案：** `StorageRepository.kt:35-47`

```kotlin
suspend fun getUsedBytes(): Long = runCatching {
    val listResult = ref.listAll().await()
    for (prefix in listResult.prefixes) {   // 子目錄
        val items = prefix.listAll().await()  // 每個子目錄再全掃
        for (item in items.items) {
            val metadata = item.metadata.await()  // 每個檔案一個 API call
            total += metadata.sizeBytes ?: 0L
        }
    }
}
```

- N+1 API calls: `listAll` → foreach prefix → another `listAll` → foreach item → `metadata` call
- 若 Storage 有大量檔案，可能耗費巨量網路請求與 Firebase 配額
- 若無管理員權限，`metadata.sizeBytes` 可能為 null

**建議：** 改用 Firebase Storage List All 的 `items.totalSize`（若 API 支援）或完全移除這個函式（目前未被任何程式碼呼叫）。

---

### 5. Firestore combine 邏輯有潛在循環更新

**檔案：** `DefaultFirestoreChildRepository.kt:54-60`

```kotlin
fun observeAllChildren(): Flow<List<ChildProfile>> =
    combine(local, remote) { local, remote ->
        if (remote.isNotEmpty()) remote else local
    }
```

同時 `observeFirestoreChildren()` 內部又將遠端資料寫回 Room：
```kotlin
appScope.launch { for (child in children) childRepository.save(merged) }
```

→ Firestore 資料更新 → callbackFlow 發出新值 → combine 發出 `remote` → UI 更新
→ 同時寫回 Room → Room Flow 變動 → observeAll() 發出 `local` → combine 重新合併

目前 `if (remote.isNotEmpty()) remote else local` 的邏輯可以防止 loop 影響 UI，但 Room 中仍會被反覆寫入，可能造成不必要的 UI 重組。

**建議：** 寫回 Room 前比對資料是否有實際變更（例如比較 `lastModified`）。

---

### 6. DefaultFirestoreMedicalRepository 使用 org.json.JSONObject 而非 kotlinx.serialization

**檔案：** `DefaultFirestoreMedicalRepository.kt:130,132`

```kotlin
val json = org.json.JSONObject(raw)
```

專案其他所有 JSON 解析都使用 `kotlinx.serialization.json.Json`（見 `MedicalAiRepository.kt:65`、`MonthlyReportRepository.kt:75`）。此處不一致。

**建議：** 統一使用 `kotlinx.serialization` 解析 AI 輸出 JSON。

---

## ⚠️ 穩定性

### 7. processAiPending 中 gender/allergies 為空/硬編碼

**檔案：** `DefaultFirestoreMedicalRepository.kt:118-119`

```kotlin
ageMonths = Period.between(visit.date, LocalDate.now()).toTotalMonths().toInt(),
gender = "",
allergies = null
```

`gender` 和 `allergies` 未從 child profile 查詢。AI prompt 中這兩個資訊為空會降低摘要品質。

**建議：** 調用 `childRepository.getById(visit.childId)` 取得正確的 gender 和 allergies。

---

### 8. StorageCleanupWorker 每年只執行一次

**檔案：** `StorageCleanupWorker.kt:39`

```kotlin
val request = PeriodicWorkRequestBuilder<StorageCleanupWorker>(365, TimeUnit.DAYS)
```

每年的 12/31 才會執行一次。若使用者有大量一年前的就診圖片，Storage 中可能存在大量已無對應就診紀錄的孤兒圖片。

**建議：** 改為每月或每季執行一次（30 或 90 天）。

---

## ✅ 做得好的地方

- **Firebase Auth 匿名登入 + Google 連結**：支援離線開始使用，後續可連結 Google 帳號保留資料
- **Firestore subcollection 設計**：`children/{childId}/medicalVisits/{visitId}` 結構合理，避免文件大小限制
- **Worker 的 retry 策略**：`if (runAttemptCount < 3) Result.retry()` 有指數退撐
- **StorageCleanupWorker 使用初始延遲**：`setInitialDelay` 確保在年底執行

---

**建議修復優先級：** #1 (aiPending sentinel) > #2 (DAO injection) > #3 (MemoReminderWorker) > #6 (JSON parser) > #8 (365-day interval)
