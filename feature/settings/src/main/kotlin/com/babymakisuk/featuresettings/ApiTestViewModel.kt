package com.babymakisuk.featuresettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.ServiceAiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 四種互斥 UI 狀態 */
sealed interface ApiTestUiState {
    data object Idle    : ApiTestUiState
    data object Loading : ApiTestUiState
    data class  Success(val response: String, val elapsedMs: Long) : ApiTestUiState
    data class  Error  (val message: String,  val elapsedMs: Long) : ApiTestUiState
}

@HiltViewModel
class ApiTestViewModel @Inject constructor(
    private val aiClient: ServiceAiClient,   // 介面，非 Cloud 實作
    val aiConfig: AiConfig                   // 供 UI 讀取 hasValidKey
) : ViewModel() {

    private val _uiState = MutableStateFlow<ApiTestUiState>(ApiTestUiState.Idle)
    val uiState: StateFlow<ApiTestUiState> = _uiState.asStateFlow()

    private val testPrompt =
        """請以JSON格式回傳 {"status":"ok","message":"Gemini 連線正常"}"""

    fun sendTestRequest() {
        if (_uiState.value is ApiTestUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = ApiTestUiState.Loading
            val start = System.currentTimeMillis()
            runCatching { aiClient.complete(testPrompt) }
                .fold(
                    onSuccess = { response ->
                        val elapsed = System.currentTimeMillis() - start
                        _uiState.value = ApiTestUiState.Success(response, elapsed)
                    },
                    onFailure = { error ->
                        val elapsed = System.currentTimeMillis() - start
                        _uiState.value = ApiTestUiState.Error(
                            error.localizedMessage ?: "未知錯誤", elapsed
                        )
                    }
                )
        }
    }

    fun reset() { _uiState.value = ApiTestUiState.Idle }
}
