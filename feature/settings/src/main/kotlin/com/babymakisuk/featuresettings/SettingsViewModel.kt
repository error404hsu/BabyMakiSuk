package com.babymakisuk.featuresettings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coredata.SettingsRepository
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
    object Loading : BackupUiState
    data class ExportReady(val intent: Intent) : BackupUiState
    object ImportSuccess : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
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

    // ── 匯出 / 匯入 ──────────────────────────────────────────
    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    /** 觸發匯出：建立備份 JSON 並出備 ShareSheet Intent */
    fun triggerExport() {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading
            runCatching { repository.buildExportIntent() }
                .onSuccess { _backupState.value = BackupUiState.ExportReady(it) }
                .onFailure { _backupState.value = BackupUiState.Error(it.message ?: "匯出失敗") }
        }
    }

    /** 用戶選完檔後，從 Uri 匯入 */
    fun triggerImport(uri: Uri, merge: Boolean = true) {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading
            runCatching { repository.importFromUri(uri, merge) }
                .onSuccess { _backupState.value = BackupUiState.ImportSuccess }
                .onFailure { _backupState.value = BackupUiState.Error(it.message ?: "匯入失敗") }
        }
    }

    /** UI 處理完狀態後重置 */
    fun clearBackupState() {
        _backupState.value = BackupUiState.Idle
    }
}
