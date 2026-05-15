package com.babymakisuk.featuresettings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coredata.SettingsRepository
import com.babymakisuk.coredata.repository.MonthlyReportRepository
import com.babymakisuk.coremodel.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 匯出 / 匯入的 UI 狀態 */
sealed interface BackupUiState {
    object Idle : BackupUiState
    /**
     * 載入中狀態
     * @param progress 目前進度 (0.0 ~ 1.0)，若為 null 則顯示不確定進度
     * @param message 正在進行的具體步驟描述
     */
    data class Loading(val progress: Float? = null, val message: String? = null) : BackupUiState
    data class ExportReady(val intent: Intent) : BackupUiState
    object ImportSuccess : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val monthlyReportRepository: MonthlyReportRepository
) : ViewModel() {

    // ── 深色模式 ──────────────────────────────────────────
    val darkMode: StateFlow<DarkModeOption> = repository.darkModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DarkModeOption.SYSTEM
    )

    fun setDarkMode(option: DarkModeOption) {
        viewModelScope.launch { repository.setDarkMode(option) }
    }

    // ── 使用者角色 ──────────────────────────────────────────
    val userRole: StateFlow<UserRole> = repository.userRoleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserRole.NONE
    )

    fun setUserRole(role: UserRole) {
        viewModelScope.launch { repository.setUserRole(role) }
    }

    // ── 雲端 AI 開關 ──────────────────────────────────────────
    val aiCloudEnabled: StateFlow<Boolean> = repository.aiCloudEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    fun setAiCloudEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAiCloudEnabled(enabled) }
    }

    // ── 通知開關 ──────────────────────────────────────────

    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    }

    // ── 資料備份設定 ─────────────────────────────────────

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

    // ── 匯出 / 匯入 ──────────────────────────────────────────
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

    // ── 測試功能 ──────────────────────────────────────────

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
}
