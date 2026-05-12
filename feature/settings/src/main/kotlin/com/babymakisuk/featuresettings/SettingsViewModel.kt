package com.babymakisuk.featuresettings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.CloudServiceAiClient
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coredata.SettingsRepository
import com.babymakisuk.coremodel.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 匯出 / 匯入的 UI 狀態 */
sealed interface BackupUiState {
    object Idle : BackupUiState
    object Loading : BackupUiState
    data class ExportReady(val intent: Intent) : BackupUiState
    object ImportSuccess : BackupUiState
    data class Error(val message: String) : BackupUiState
}

/** Gemini API Key 驗證的 UI 狀態 */
data class ApiKeyUiState(
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val cloudAiClient: CloudServiceAiClient   // 直接注入具體類別以呼叫 testApiKeyValid()
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

    // ── 匯出 / 匯入 ──────────────────────────────────────────
    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun triggerExport() {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading
            runCatching { repository.buildExportIntent() }
                .onSuccess { _backupState.value = BackupUiState.ExportReady(it) }
                .onFailure { _backupState.value = BackupUiState.Error(it.message ?: "匯出失敗") }
        }
    }

    fun triggerImport(uri: Uri, merge: Boolean = true) {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading
            runCatching { repository.importFromUri(uri, merge) }
                .onSuccess { _backupState.value = BackupUiState.ImportSuccess }
                .onFailure { _backupState.value = BackupUiState.Error(it.message ?: "匯入失敗") }
        }
    }

    fun clearBackupState() {
        _backupState.value = BackupUiState.Idle
    }

    // ── Gemini API Key 驗證 ──────────────────────────────────
    private val _apiKeyState = MutableStateFlow(ApiKeyUiState())
    val apiKeyState: StateFlow<ApiKeyUiState> = _apiKeyState.asStateFlow()

    /**
     * 以 testKey 呼叫 Gemini Ping 驗證，成功後儲存至 DataStore。
     * 驗證過程中鎖定按鈕；失敗時顯示錯誤訊息。
     */
    fun verifyAndSaveApiKey(key: String) {
        viewModelScope.launch {
            _apiKeyState.update { it.copy(isVerifying = true, isVerified = false, errorMessage = null) }

            val isValid = cloudAiClient.testApiKeyValid(key)

            if (isValid) {
                repository.setGeminiApiKey(key)
                _apiKeyState.update { it.copy(isVerifying = false, isVerified = true, errorMessage = null) }
            } else {
                _apiKeyState.update {
                    it.copy(
                        isVerifying = false,
                        isVerified = false,
                        errorMessage = "API Key 驗證失敗，請確認 Key 是否正確或已開啟 Gemini API 權限"
                    )
                }
            }
        }
    }

    /** 清除驗證狀態（離開頁面或重新輸入時呼叫） */
    fun clearApiKeyState() {
        _apiKeyState.value = ApiKeyUiState()
    }
}
