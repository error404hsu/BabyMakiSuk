package com.babymakisuk.featuresettings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.babymakisuk.corefirebase.auth.FirebaseAuthRepository
import com.babymakisuk.corefirebase.storage.StorageRepository
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.dao.DailyLogDao
import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.repository.MedicalRepository
import com.babymakisuk.coredata.dao.MemoDao
import com.babymakisuk.coredata.dao.MonthlyReportDao
import com.babymakisuk.coredata.dao.SystemReminderDao
import com.babymakisuk.coredata.dao.ToiletDao
import com.babymakisuk.coredata.dao.VaccineReminderDao
import com.babymakisuk.coredata.repository.SettingsRepository
import com.babymakisuk.coredata.repository.MonthlyReportRepository
import com.babymakisuk.coredata.worker.DataRetentionWorker
import com.babymakisuk.coredata.worker.MemoReminderWorker
import com.babymakisuk.coremodel.UserRole
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** 匯出 / 匯入的 UI 狀態 */
sealed interface BackupUiState {
    object Idle : BackupUiState
    data class Loading(val progress: Float? = null, val message: String? = null) : BackupUiState
    data class ExportReady(val intent: Intent) : BackupUiState
    object ImportSuccess : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val monthlyReportRepository: MonthlyReportRepository,
    private val authRepository: FirebaseAuthRepository,
    private val storageRepository: StorageRepository,
    private val growthDao: GrowthDao,
    private val medicalRepo: MedicalRepository,
    private val memoDao: MemoDao,
    private val aiInsightDao: AiInsightDao,
    private val vaccineReminderDao: VaccineReminderDao,
    private val systemReminderDao: SystemReminderDao,
    private val dailyLogDao: DailyLogDao,
    private val toiletDao: ToiletDao,
    private val monthlyReportDao: MonthlyReportDao,
) : ViewModel() {

    val darkMode: StateFlow<DarkModeOption> = repository.darkModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DarkModeOption.SYSTEM
    )

    fun setDarkMode(option: DarkModeOption) {
        viewModelScope.launch { repository.setDarkMode(option) }
    }

    val userRole: StateFlow<UserRole> = repository.userRoleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserRole.NONE
    )

    fun setUserRole(role: UserRole) {
        viewModelScope.launch { repository.setUserRole(role) }
    }

    val aiCloudEnabled: StateFlow<Boolean> = repository.aiCloudEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    fun setAiCloudEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAiCloudEnabled(enabled) }
    }

    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    }

    // ── Auth 狀態 ─────────────────────────────────────────
    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(null)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    // ── Storage 配額 ──────────────────────────────────────
    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()

    val autoBackupEnabled: StateFlow<Boolean> = repository.autoBackupEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoBackupEnabled(enabled) }
    }

    val lastBackupTime: StateFlow<String?> = repository.lastBackupTime.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val developerModeEnabled: StateFlow<Boolean> = repository.developerModeEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private var clickCount = 0
    private var lastClickTime = 0L

    fun onVersionClick(onDevModeEnabled: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 2000) clickCount = 0
        clickCount++
        lastClickTime = currentTime
        if (clickCount >= 7 && !developerModeEnabled.value) {
            viewModelScope.launch {
                repository.setDeveloperModeEnabled(true)
                onDevModeEnabled()
            }
            clickCount = 0
        }
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDeveloperModeEnabled(enabled) }
    }

    // ── 初始化監聽 ─────────────────────────────────────────
    init {
        viewModelScope.launch {
            // ★ 先補一次當前值，避免 AuthStateListener 錯過已完成的匿名登入
            _firebaseUser.value = authRepository.getCurrentUser()

            authRepository.observeAuthState().collect { user ->
                _firebaseUser.value = user
            }
        }
        viewModelScope.launch {
            _storageUsedBytes.value = storageRepository.getUsedBytes()
        }
    }

    // ── Google 登入 / 登出 ───────────────────────────────
    suspend fun linkGoogleAccount(idToken: String): Result<FirebaseUser> =
        authRepository.linkWithGoogleCredential(idToken)

    fun signOutGoogle() {
        authRepository.signOut()
    }

    // ── 匯出 / 匯入 ───────────────────────────────────────
    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun triggerExport() {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading(message = "正在準備備份資料...")
            runCatching { repository.buildExportIntent() }
                .onSuccess {
                    _backupState.value = BackupUiState.ExportReady(it)
                    repository.updateLastBackupTime(
                        java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    )
                }
                .onFailure { _backupState.value = BackupUiState.Error(it.message ?: "匯出失敗") }
        }
    }

    fun triggerImport(uri: Uri, merge: Boolean = true) {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading(message = "正在分析備份檔...")
            runCatching { repository.importFromUri(uri, merge) }
                .onSuccess { _backupState.value = BackupUiState.ImportSuccess }
                .onFailure { _backupState.value = BackupUiState.Error(it.message ?: "匯入失敗") }
        }
    }

    fun clearBackupState() {
        _backupState.value = BackupUiState.Idle
    }

    // ── 測試功能 ─────────────────────────────────────────
    fun generateLastMonthReport() {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading(message = "正在產生上月合併月報...")
            runCatching {
                val lastMonth = java.time.YearMonth.now().minusMonths(1)
                monthlyReportRepository.generateMonthlyReport(lastMonth)
            }.onSuccess {
                _backupState.value = BackupUiState.Idle
            }.onFailure {
                _backupState.value = BackupUiState.Error(it.message ?: "產生失敗")
            }
        }
    }

    fun triggerMonthlyReminderTest() {
        viewModelScope.launch {
            monthlyReportRepository.checkAndCreateMonthlyReportReminder(force = true)
        }
    }

    fun testFirebaseConnection(onResult: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val pingData = hashMapOf(
            "timestamp" to Timestamp.now(),
            "device" to android.os.Build.MODEL,
            "os" to android.os.Build.VERSION.RELEASE
        )
        db.collection("debug_ping").add(pingData)
            .addOnSuccessListener { onResult("Firebase 連線成功！\n文件 ID: ${it.id}") }
            .addOnFailureListener { onResult("Firebase 連線失敗：${it.message}") }
    }

    // ── 開發者測試擴充 ────────────────────────────────────

    /** 讀取 Firebase Auth 目前登入狀態摘要 */
    fun getAuthStatusSummary(onResult: (String) -> Unit) {
        val user = _firebaseUser.value
        if (user == null) {
            onResult("❌ 未登入（FirebaseUser = null）")
        } else {
            val type = if (user.isAnonymous) "匿名帳號" else "Google 帳號"
            onResult("✅ 已登入\n類型：$type\nUID：${user.uid}\nEmail：${user.email ?: "無"}")
        }
    }

    /** 強制觸發 DataRetentionWorker（立即清理過期資料） */
    fun triggerDataRetentionNow(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val request = OneTimeWorkRequestBuilder<DataRetentionWorker>().build()
                WorkManager.getInstance(context).enqueue(request)
            }.onSuccess { onResult("✅ DataRetentionWorker 已排入執行佇列") }
             .onFailure { onResult("❌ 觸發失敗：${it.message}") }
        }
    }

    /** 強制觸發即時測試通知（繞過排程） */
    fun triggerTestNotification(context: Context) {
        viewModelScope.launch {
            val request = OneTimeWorkRequestBuilder<MemoReminderWorker>()
                .setInputData(
                    workDataOf(
                        "memo_id" to -1L,
                        "title" to "🧪 測試通知",
                        "content" to "這是開發者測試通知，請確認顯示正常"
                    )
                ).build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    /** Room DB 各 Table 筆數快照 */
    fun getDbRowCountSnapshot(onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val medCount = medicalRepo.count()
                buildString {
                    appendLine("📦 Room 資料庫快照")
                    appendLine("GrowthRecord：${growthDao.count()} 筆")
                    appendLine("MedicalVisit：${medCount} 筆")
                    appendLine("Memo：${memoDao.count()} 筆")
                    appendLine("AiInsight：${aiInsightDao.count()} 筆")
                    appendLine("VaccineReminder：${vaccineReminderDao.count()} 筆")
                    appendLine("SystemReminder：${systemReminderDao.count()} 筆")
                    appendLine("DailyLog：${dailyLogDao.count()} 筆")
                    appendLine("ToiletRecord：${toiletDao.count()} 筆")
                    append("MonthlyReport：${monthlyReportDao.count()} 筆")
                }
            }.onSuccess { onResult(it) }
             .onFailure { onResult("❌ 查詢失敗：${it.message}") }
        }
    }

    /** Firestore 離線模式切換（停用/啟用網路以測試離線持久化） */
    fun setFirestoreOfflineMode(offline: Boolean, onResult: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        viewModelScope.launch {
            runCatching {
                if (offline) db.disableNetwork().await() else db.enableNetwork().await()
            }.onSuccess {
                onResult(if (offline) "✅ Firestore 網路已停用（離線模式）" else "✅ Firestore 網路已恢復")
            }.onFailure { onResult("❌ 操作失敗：${it.message}") }
        }
    }
}
