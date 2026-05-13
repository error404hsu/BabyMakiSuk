# BabyMakiSuk — Sprint 4 Agent Prompt
# 目標：完善 Phase C AI 整合 ＋ Phase D 前置 ＋ Phase F 週報 UI ＋ Phase G-2 補齊

> 產出日期：2026-05-13  
> 撰寫依據：`TODO.md`、`core/ai` 現有源碼、`AiDispatcher` / `AiPromptBuilder` / `AiSystemConstraints` 架構

---

## 專案背景

- 幼兒管理 App，雙寶（雙胞胎）場景
- `core/ai` 架構：`AiDispatcher` → Fallback Chain → `GenerativeModel`
- 角色定義：`AiPreset` enum（5 個角色），含 `preferredModel` 與 `task` 對應
- Prompt 組裝：`AiPromptBuilder.buildSystemPromptWithContext()`
- 全域限制：`AiSystemConstraints.GLOBAL_CONSTRAINTS` 附加於所有 prompt 末尾
  - ⚠️ `GLOBAL_CONSTRAINTS` 明確禁止 Markdown 輸出（`**`、`#`、` ``` ` 等）
- `AiContextInjector` 位於 `core/data`（非 `core/ai`），避免循環依賴

---

## 通用注意事項（所有 Phase 皆須遵守）

1. **KSP 相容性**：新增 Room `@Entity` 或 DAO 修改後，確認 `kapt/ksp` 不衝突；若有新欄位必須同步新增 Room Migration，命名規則：`MIGRATION_X_Y`
2. **Hilt 模組**：新增 Repository 或 Client 時，確認 `di/` 資料夾有對應 `@Provides`
3. **kotlinx.serialization**：`data class` 加 `@Serializable`，`build.gradle.kts` 確認 `kotlin("plugin.serialization")` 已啟用（`core/model` 模組）
4. **禁止 hardcode API key**：`AiConfig` 已封裝，直接注入使用
5. **安全提示文字不可省略**：上架審核必要項目
6. **所有新 Composable 必須提供 `@Preview`**
7. **TODO.md 更新**：完成後將對應 `[ ]` 改為 `[x]`

---

## Phase G-2 補齊 — AndroidManifest FileProvider 設定

> **執行時機：無任何依賴，最先執行，零風險**

### Step 1：新增 `res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="backup_cache" path="." />
</paths>
```

### Step 2：`AndroidManifest.xml` 新增 `<provider>`

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

確認 `BackupManager.exportToShareIntent()` 中的 `authority` 字串與上方一致。

### 完成後 TODO.md 勾選

```
- [x] `AndroidManifest.xml` 新增 FileProvider `<provider>` 設定
- [x] `res/xml/file_paths.xml` 定義 cache-path
```

---

## Phase C 補完 — medical_note_summarizer 完整流程

> **依賴：無（純邏輯層，不依賴 UI）**

### Step 1：新增 `MedicalSummaryResult` data class

位置：`core/model/src/main/kotlin/com/babymakisuk/coremodel/MedicalSummaryResult.kt`

```kotlin
@Serializable
data class MedicalSummaryResult(
    val diagnosisSummary: String,
    val prescriptions: List<String>,
    val careInstructions: List<String>,
    val safetyFlag: String  // "normal" | "attention" | "urgent"
)
```

### Step 2：`AiPromptBuilder.kt` 新增 `buildMedicalSummaryPrompt()`

```kotlin
/**
 * 組合病歷摘要的 System Prompt + User Prompt。
 * 強制要求 LLM 輸出嚴格 JSON，不得有任何前綴文字或 Markdown。
 *
 * @return Pair(systemPrompt, userPrompt)
 */
fun buildMedicalSummaryPrompt(
    rawNote: String,
    ageMonths: Int,
    gender: String,
    allergies: String?
): Pair<String, String> {
    val system = buildString {
        appendLine("你是一位台灣兒科醫療摘要 AI，專門將家長輸入的就診備註結構化。")
        appendLine()
        appendLine("【當前個案】")
        appendLine("月齡：${ageMonths} 個月 | 性別：${gender} | 過敏史：${allergies ?: "無"}")
        appendLine()
        appendLine("【輸出規則 - 嚴格遵守】")
        appendLine("- 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、說明文字")
        appendLine("- 不使用 Markdown 包裝（禁止 ```json）")
        appendLine("- JSON schema（所有欄位必填，無資料填空字串或空陣列）：")
        appendLine("""
{
  "diagnosisSummary": "string（50字以內的診斷摘要）",
  "prescriptions": ["string（藥名 劑量 頻率）", ...],
  "careInstructions": ["string", ...],
  "safetyFlag": "normal | attention | urgent"
}
        """.trimIndent())
        appendLine()
        appendLine("- safetyFlag 判斷標準：")
        appendLine("  normal    = 一般就診，無特殊提醒")
        appendLine("  attention = 需持續觀察（如發燒超過3天、特殊藥物）")
        appendLine("  urgent    = 需立即就醫（如過敏反應、呼吸困難等描述）")
        append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
    }
    val user = "請摘要以下就診備註：\n$rawNote"
    return Pair(system, user)
}
```

### Step 3：更新 `MedicalAiRepository.summarizeMedicalVisit()`

```kotlin
suspend fun summarizeMedicalVisit(
    visitId: Int,
    rawNote: String,
    ageMonths: Int,
    gender: String,
    allergies: String?
): Result<MedicalSummaryResult> = runCatching {
    val (systemPrompt, userPrompt) = AiPromptBuilder.buildMedicalSummaryPrompt(
        rawNote, ageMonths, gender, allergies
    )
    val raw = aiDispatcher.executeWithSystemPrompt(
        task         = AiTask.MEDICAL_CONSULTATION,
        systemPrompt = systemPrompt,
        userPrompt   = userPrompt
    )
    try {
        Json { ignoreUnknownKeys = true }.decodeFromString<MedicalSummaryResult>(raw)
    } catch (e: SerializationException) {
        // Fallback：整段文字塞入 diagnosisSummary
        MedicalSummaryResult(
            diagnosisSummary  = raw.take(200),
            prescriptions     = emptyList(),
            careInstructions  = emptyList(),
            safetyFlag        = "normal"
        )
    }
}
```

### Step 4：Room Entity 補齊 + Migration

確認 `MedicalVisitEntity` 是否已有 `isUrgent: Boolean` 欄位：
- **若無**，新增欄位並加入 `MIGRATION_4_5`：
  ```sql
  ALTER TABLE medical_visits ADD COLUMN is_urgent INTEGER NOT NULL DEFAULT 0
  ```
- `prescriptions` 與 `careInstructions` 以 `"・"` 作為分隔符 join 後存入 Room

### Step 5：`MedicalViewModel` 新增 `triggerAiSummary()`

```kotlin
fun triggerAiSummary(visit: MedicalVisit) {
    viewModelScope.launch {
        val child = childRepository.getChildById(visit.childId) ?: return@launch
        medicalAiRepository.summarizeMedicalVisit(
            visitId   = visit.id,
            rawNote   = visit.notes ?: "",
            ageMonths = child.ageMonths,
            gender    = child.gender,
            allergies = child.allergies
        ).onSuccess { result ->
            medicalDao.updateAiFields(
                id               = visit.id,
                diagnosisSummary = result.diagnosisSummary,
                prescriptions    = result.prescriptions.joinToString("・"),
                careInstructions = result.careInstructions.joinToString("・"),
                isUrgent         = result.safetyFlag == "urgent"
            )
        }
    }
}
```

新增 DAO query：
```kotlin
@Query("""
    UPDATE medical_visits
    SET diagnosis_summary = :diagnosisSummary,
        prescriptions     = :prescriptions,
        care_instructions = :careInstructions,
        is_urgent         = :isUrgent
    WHERE id = :id
""")
suspend fun updateAiFields(
    id: Int,
    diagnosisSummary: String,
    prescriptions: String,
    careInstructions: String,
    isUrgent: Boolean
)
```

### Step 6：「AI 整理僅供參考」安全提示 + 手動編輯 UI

位置：`feature/medical` 的 `MedicalVisitCard` Composable

**安全提示 Banner（必須實作，不可省略）：**
```kotlin
// 僅在 AI 已整理時顯示
if (visit.diagnosisSummary.isNotBlank()) {
    val (bannerColor, bannerText) = if (visit.isUrgent) {
        MaterialTheme.colorScheme.errorContainer to
            "🚨 AI 偵測到緊急提示，請立即就醫或聯絡醫師"
    } else {
        MaterialTheme.colorScheme.tertiaryContainer to
            "⚕️ AI 整理僅供參考，請以醫師診斷為準"
    }
    Surface(color = bannerColor, shape = RoundedCornerShape(8.dp)) {
        Text(
            text     = bannerText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style    = MaterialTheme.typography.labelSmall
        )
    }
}
```

**手動編輯 UI（展開狀態）：**
- 三個 AI 欄位旁各加入「✏️」`IconButton`
- 點擊後切換為 `OutlinedTextField`（單行 / 多行依欄位決定）
- 儲存時呼叫 `MedicalViewModel.updateAiFields(visitId, ...)`
- 離開編輯狀態時還原為 `Text` 顯示

### 完成後 TODO.md 勾選

```
- [x] coreai / ServiceAI 真實 SDK 串接
- [x] medical_note_summarizer prompt schema
- [x] AI JSON 解析與寫入 MedicalVisit
- [x] 「AI 整理僅供參考」安全提示
- [x] AI 結果手動編輯 UI
```

---

## Phase D 前置 — `weekly_baby_log_summary` Prompt Schema

> **依賴：Phase C Step 1-2 完成（需要 kotlinx.serialization 確認可用）**

### Step 1：新增 `WeeklySummaryResult` data class

位置：`core/model/src/main/kotlin/com/babymakisuk/coremodel/WeeklySummaryResult.kt`

```kotlin
@Serializable
data class WeeklySummaryResult(
    val weekSummary: String,           // 整週總結，150字以內，溫馨語氣
    val highlights: List<String>,      // 本週亮點，3條以內
    val parentTips: List<String>,      // 給家長的具體建議，2條
    val searchKeywords: List<String>   // 3-5個關鍵字，供 FTS 搜尋
)
```

### Step 2：`AiPromptBuilder.kt` 新增 `buildWeeklyLogSummaryPrompt()`

```kotlin
/**
 * 組合每週成長總結的 System Prompt + User Prompt。
 *
 * @param childName      幼兒姓名
 * @param ageMonths      幼兒月齡
 * @param weekLabel      週次標籤，例："2026 年第 20 週（5/11–5/17）"
 * @param dailyLogsBlock 由 AiContextInjector 組裝的 7 天日誌文字
 * @param recentMedical  同期就診摘要（可為 null）
 * @return Pair(systemPrompt, userPrompt)
 */
fun buildWeeklyLogSummaryPrompt(
    childName: String,
    ageMonths: Int,
    weekLabel: String,
    dailyLogsBlock: String,
    recentMedical: String?
): Pair<String, String> {
    val system = buildString {
        appendLine("你是一位幼兒成長週報 AI 撰稿員，專門為家長生成溫馨且具體的每週成長總結。")
        appendLine()
        appendLine("【輸出規則】")
        appendLine("- 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、說明文字")
        appendLine("- 禁止 Markdown 包裝")
        appendLine("- JSON schema：")
        appendLine("""
{
  "weekSummary": "string（整週總結，150字以內，溫馨語氣）",
  "highlights": ["string（本週亮點，3條以內）"],
  "parentTips": ["string（給家長的具體建議，2條）"],
  "searchKeywords": ["string（3-5個關鍵字，供 FTS 搜尋）"]
}
        """.trimIndent())
        append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
    }
    val user = buildString {
        appendLine("請根據以下資料，為 ${childName}（${ageMonths}個月）生成 ${weekLabel} 的成長週報：")
        appendLine()
        appendLine("【本週日誌】")
        appendLine(dailyLogsBlock)
        if (!recentMedical.isNullOrBlank()) {
            appendLine()
            appendLine("【同期就診紀錄】")
            append(recentMedical)
        }
    }
    return Pair(system, user)
}
```

### Step 3：`WeeklyReportRepository` 整合新 Prompt

更新 `generateWeeklyReport()` 改呼叫 `buildWeeklyLogSummaryPrompt()`，並以 `Json.decodeFromString<WeeklySummaryResult>()` 解析後：
- `weekSummary` → `WeeklyReport.summary`
- `highlights` + `parentTips` → `WeeklyReport.content`（join 存入）
- `searchKeywords` → `WeeklyReportFts` 索引欄位

### 完成後 TODO.md 勾選

```
- [x] `core/ai`：`weekly_baby_log_summary` prompt schema（含 searchKeywords 萃取）
```

---

## Phase F 補完 — WeeklyReportScreen UI + ViewModel

> **依賴：Phase D 前置 Step 1-3 完成**

### Step 1：`WeeklyReportViewModel`

位置：`feature/weeklyreport/src/.../WeeklyReportViewModel.kt`

```kotlin
@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    private val weeklyReportRepository: WeeklyReportRepository,
    private val childRepository: ChildRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val childId: Int = savedStateHandle["childId"] ?: 0

    val reports: StateFlow<List<WeeklyReport>> =
        weeklyReportRepository.getReportsByChildId(childId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun generateReport() {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            weeklyReportRepository.generateWeeklyReport(childId)
                .onFailure { _errorMessage.value = it.message }
            _isGenerating.value = false
        }
    }

    fun deleteReport(reportId: Int) {
        viewModelScope.launch {
            weeklyReportRepository.deleteReport(reportId)
        }
    }
}
```

### Step 2：`WeeklyReportScreen` Composable

UI 結構規格：

```
WeeklyReportScreen
├── TopAppBar（「週報」標題 + 返回按鈕）
├── if (isGenerating) LinearProgressIndicator + 提示文字「AI 正在生成週報…」
├── ExtendedFloatingActionButton「✨ 生成本週週報」（右下角）
│   └── onClick → viewModel.generateReport()
└── LazyColumn
    ├── Empty State（無週報時）
    │   └── EmptyState（圖示 + 引導文字 + 生成按鈕）
    └── items(reports) { report →
            WeeklyReportCard(report)
        }

WeeklyReportCard
├── 標題：report.weekLabel
├── 摘要：report.summary 前 60 字 + "…"（收合狀態）
├── 底部列：生成時間 + 展開/收合 Icon
├── 展開狀態：
│   ├── 完整摘要
│   ├── 「本週亮點」條列（highlights）
│   └── 「給家長的建議」條列（parentTips）
└── 長按 → 刪除確認 AlertDialog
```

### Step 3：確認 Navigation 路由

在 `BabyMakiSukNavHost` 確認以下路由已正確連結至 `WeeklyReportScreen`：

```kotlin
composable("weekly_report?childId={childId}") { backStackEntry ->
    val childId = backStackEntry.arguments?.getString("childId")?.toIntOrNull() ?: 0
    WeeklyReportScreen(
        childId   = childId,
        onBack    = { navController.popBackStack() }
    )
}
```

### 完成後 TODO.md 勾選

```
- [x] `feature/weeklyreport`：`WeeklyReportScreen` UI
- [x] `feature/weeklyreport`：`WeeklyReportViewModel`
```

---

## 執行順序總覽

| 順序 | Phase | 內容 | 依賴 |
|------|-------|------|------|
| 1 | **G-2** | AndroidManifest FileProvider 設定 | 無 |
| 2 | **C** | `MedicalSummaryResult` data class + `buildMedicalSummaryPrompt()` | 無 |
| 3 | **C** | `MedicalAiRepository` 解析寫回 + Room Migration | Step 2 |
| 4 | **C** | AI 安全提示 Banner + 手動編輯 UI | Step 3 |
| 5 | **D** | `WeeklySummaryResult` data class + `buildWeeklyLogSummaryPrompt()` | Step 2 確認 serialization 可用 |
| 6 | **D** | `WeeklyReportRepository` 整合新 Prompt | Step 5 |
| 7 | **F** | `WeeklyReportViewModel` + `WeeklyReportScreen` UI | Step 6 |
