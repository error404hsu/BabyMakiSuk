# 未完成項目追蹤

> 最後更新：2026-05-19

---

## 🔴 保留且未修復的嚴重問題

### 1. AiInsightEntity.childId 型別不一致 (String vs Long)

**原始報告：** SubTask 1 - #2
**風險：** 跨 Entity JOIN/比對時型別不匹配
**未修復原因：** 修改需 Entity + DAO + Migration + 所有 call site 同步變更，範圍過大。`AiInsightEntity` 是 Sprint 3 加入的獨立模組，目前不與其他 Entity 做 JOIN。
**建議做法：** 若未來需要跨 Entity 查詢（如「列出孩子的所有 AI 精華」），則優先修正。

### 2. DefaultFirestoreMedicalRepository 仍使用 org.json 而非 kotlinx.serialization

**原始報告：** SubTask 5 - #6
**風險：** 不一致的工具庫
**未修復原因：** `core:firebase` 模組未依賴 `kotlinx-serialization`，加入依賴需要版號管理和測試。`org.json` 為 Android SDK 內建，無功能差異。
**建議做法：** 若決定全專案統一為 kotlinx.serialization，可為 `core:firebase/build.gradle.kts` 加入 `implementation(libs.kotlinx.serialization.json)`。

### 3. Firestore combine 潛在循環更新

**原始報告：** SubTask 5 - #5
**風險：** Firestore 寫回 Room → Room Flow 變動 → combine 重新合併 → UI 重組
**未修復原因：** 需加入 `lastModified` 比較或版本號機制，涉及雙向同步架構。
**建議做法：** 在 `observeFirestoreChildren()` 寫入前比對 `existing.lastModified >= remote.lastModified`。

---

## 🟡 設計改善建議 (非 bug)

### 4. AiDispatcher 每次呼叫建立新的 GenerativeModel

**原始報告：** SubTask 2 - #4
**建議：** LRU cache `(GeminiModel, systemPrompt)` → `GenerativeModel`

### 5. MonthlyReportRepository N+1 查詢

**原始報告：** SubTask 1 - #7
**建議：** `getAllOnce()` 改為 `@Query` 日期範圍過濾，利用 SQLite 索引

### 6. ChildRepository shareIn(ApplicationScope)

**原始報告：** SubTask 1 - #6
**建議：** 移除 Repository 層的 shareIn，改由 ViewModel 管理共享

### 7. HomeViewModel God Constructor (7+ params)

**原始報告：** SubTask 4 - #4
**建議：** 萃取 HomeDataAggregator UseCase 或 HomeRepository

---

## ⚠️ 低優先級 / 可選

| 原始編號 | 說明 |
|----------|------|
| S1-5 | MedicalVisitEntity.imageStoragePath nullability 矛盾 |
| S1-9 | MIGRATION_14_15 開發註解 |
| S1-11 | PrescriptionImagePreprocessor 雙重讀取 Uri |
| S2-3 | BitmapUtils compressForAi 無意義的重新編碼 |
| S2-7 | AiDispatcher.execute() deprecated path |
| S2-8 | GeminiModel.modelId magic string |
| S2-9 | RateLimiter 重啟後重置 |
| S3-2 | BottomBar 判別邏輯脆弱 |
| S3-6 | BackHandler 不完整 |
| S3-7 | Preview 包含完整 NavHost |
| S3-8 | 通知權限拒絕後無 UI 回饋 |
| S5-4 | StorageRepository.getUsedBytes 遞迴效能 |
| S5-8 | StorageCleanupWorker 365 天執行間隔 |
