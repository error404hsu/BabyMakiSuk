# Code Review — 子任務 1：核心資料層 (core/data)

> 審查日期：2026-05-19
> 審查範圍：Room Database、Entity、DAO、Repository、TypeConverter、Migration、PrescriptionImagePreprocessor

---

## 🔴 嚴重問題 (Bug/正確性)

### 1. MonthlyReport 寫死 childId = 0L 導致資料孤兒

**檔案：** `core/data/.../repository/MonthlyReportRepository.kt:241`
```kotlin
childId = 0L,  // 合併報表不屬任何孩子
```

`monthly_reports` 雖無 FK，但 `searchByKeyword()`、`getByYear()`、`getRecentReports()` 均以 `WHERE child_id = :childId` 過濾。childId=0L 的合併報表在一般查詢中永遠無法被撈出。

**建議：** 新增一個常數 `MonthlyReport.GLOBAL_CHILD_ID = -1L`，並在所有 DAO 查詢處處理「全體報表」情境（例如 `WHERE child_id = :childId OR child_id = -1`），或將合併報表另存一 table。

---

### 2. AiInsightEntity.childId 型別不一致

**檔案：** `core/data/.../repository/MedicalAiRepository.kt:100`
```kotlin
childId = childId.toString(),  // Long → String
```

其餘 Entity 的 childId 均為 `Long`，唯獨 `AiInsightEntity` 使用 `String`。跨 Entity JOIN 或統一查詢時無法直接關聯。

**建議：** 將 `AiInsightEntity.childId` 改為 `Long`，與整體設計一致。

---

### 3. FTS4 內容表缺少同步 Trigger

**檔案：** `core/data/.../db/AppDatabase.kt:427-434` (MIGRATION_13_14)

FTS4 content table (`monthly_reports_fts`) 需透過 SQLite trigger 與外部內容表同步。目前 `monthly_reports` 有 INSERT/UPDATE/DELETE 時，FTS 索引不會自動更新，導致搜尋回傳過時或空結果。

**建議：** 在重建 FTS 之後立即建立三個 trigger：
```sql
CREATE TRIGGER monthly_reports_ai AFTER INSERT ON monthly_reports BEGIN
    INSERT INTO monthly_reports_fts(docid, ai_summary, search_keywords)
    VALUES (new.rowid, new.ai_summary, new.search_keywords);
END;
-- 同理 AFTER DELETE、AFTER UPDATE
```

---

### 4. analyzePrescription 靜默吞錯誤

**檔案：** `core/data/.../repository/MedicalAiRepository.kt:126-129`

```kotlin
} catch (e: Exception) {
    Log.e(TAG, "analyzePrescription failed: ${e.message}")
    ""  // 回傳空字串，呼叫端無法區分失敗與空結果
}
```

**建議：** 回傳 `Result<String>` 而非 `String`，讓呼叫端明確感知失敗。

---

## 🟡 架構與設計問題

### 5. MedicalVisitEntity.imageStoragePath 可疑 nullability

**檔案：** `core/data/.../entity/MedicalVisitEntity.kt:27`

```kotlin
val imageStoragePath: ImageStoragePath? = ImageStoragePath.None,
```

宣告 nullable 但預設值非 null，`toDomain()` 又用 `?:` fallback。Converter 實作顯示 `ImageStoragePath.None` 對應 `null` column 值，造成設計矛盾。

**建議：** 宣告為 `ImageStoragePath`（non-null），並調整 Converter 讓 Room 正確處理 NOT NULL column。

---

### 6. ChildRepository shareIn 永遠活躍

**檔案：** `core/data/.../repository/ChildRepository.kt:33-36`

```kotlin
.shareIn(appScope, SharingStarted.WhileSubscribed(5_000), replay = 1)
```

綁定 `ApplicationScope` 意味著 Flow 永遠不會停止收集，即使無任何觀察者。

**建議：** 改為普通 Flow 不在 Repository 層做 shareIn，或在 ViewModel 層管理共享。

---

### 7. N+1 查詢 + 記憶體過濾 (generateMonthlyReport)

**檔案：** `core/data/.../repository/MonthlyReportRepository.kt:145-178`

使用 `getAllOnce()` 撈出全表後以 Kotlin `filter {}` 在記憶體中過濾。資料量增加時效能呈線性衰退。

**建議：** 使用 Room `@Query` 搭配 SQLite 日期範圍過濾，將壓力留給 SQLite 索引。

---

### 8. 合併報表 childId=0L 無對應策略

同上 #1。整體設計未定義「全體/合併」報表的 childId 處理策略。

---

## 🟠 可讀性與維護性

### 9. Migration 內部殘留大量開發註解

**檔案：** `core/data/.../db/AppDatabase.kt:49-61`

MIGRATION_14_15 包含長篇分析 identity hash 的英文開發註解，屬於開發過程筆記，不應留於 production 程式碼。

---

### 10. FTS 搜尋參數未轉義

**檔案：** `core/data/.../dao/MonthlyReportDao.kt:37`

```kotlin
SELECT ... FROM monthly_reports_fts WHERE monthly_reports_fts MATCH :keyword
```

FTS 的 MATCH 語法對特殊字元（`*`, `"`, `-`, `+`）敏感，直接傳入使用者輸入可能導致 crash 或異常行為。

**建議：** 在呼叫端對 keyword 做 FTS escaping（移除或跳脫特殊字元）。

---

## ⚠️ 效能

### 11. fixExifRotation 兩次讀取 Uri

**檔案：** `core/data/.../PrescriptionImagePreprocessor.kt:31-33,38`

content:// Uri 被 `openInputStream` 兩次（一次 Bitmap decode、一次 EXIF 讀取），浪費 I/O。

**建議：** Bitmap decode 後直接從 Bitmap 資訊或第一次 stream 中擷取 EXIF。

---

### 12. saveToInternal 無自動清理機制

**檔案：** `core/data/.../PrescriptionImagePreprocessor.kt:75-84`

圖片存放於 `filesDir/prescriptions/`，`cleanupOldFiles()` 有實作但未被任何排程呼叫。

**建議：** 在 `DataRetentionWorker` 中週期性呼叫 `cleanupOldFiles()`。

---

## ✅ 做得好的地方

- **Entity ↔ Domain 轉換**：全專案一致的 `toDomain()` / `toEntity()` 模式。
- **Migration 覆蓋率**：14 次 Migration，每步正確對應 schema 變更。
- **Repository 封裝**：ViewModel 不直接操作 DAO，遵守 MVI 規範。
- **Qualifier 使用**：`@IoDispatcher` + `@ApplicationScope` 一致注入，避免硬編碼 Dispatchers。

---

**建議修復優先級：** #1 (childId=0L) > #3 (FTS trigger) > #4 (錯誤吞沒) > #2 (型別不一致)
