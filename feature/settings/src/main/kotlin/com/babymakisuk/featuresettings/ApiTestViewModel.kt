package com.babymakisuk.featuresettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.GeminiModel
import com.google.ai.client.generativeai.GenerativeModel
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
    private val aiConfig: AiConfig,       // 供 UI 讀取 hasValidKey 與 apiKey
) : ViewModel() {

    val hasValidKey: Boolean get() = aiConfig.hasValidKey

    private val _uiState = MutableStateFlow<ApiTestUiState>(ApiTestUiState.Idle)
    val uiState: StateFlow<ApiTestUiState> = _uiState.asStateFlow()

    private val _selectedModel = MutableStateFlow(GeminiModel.default)
    val selectedModel: StateFlow<GeminiModel> = _selectedModel.asStateFlow()

    val availableModels: List<GeminiModel> = GeminiModel.entries

    private val testPrompt =
        """請以JSON格式回傳 {"status":"ok","message":"Gemini 連線正常"}"""

    fun selectModel(model: GeminiModel) {
        if (_selectedModel.value == model) return
        _selectedModel.value = model
        // 切換模型後清除上次結果，避免誤誤對比
        _uiState.value = ApiTestUiState.Idle
    }

    fun sendTestRequest() {
        if (_uiState.value is ApiTestUiState.Loading) return
        val model = _selectedModel.value
        viewModelScope.launch {
            _uiState.value = ApiTestUiState.Loading
            val start = System.currentTimeMillis()
            runCatching {
                // 測試頁面直接建立臨時 GenerativeModel，不經過生產用的 ServiceAiClient
                val genModel = GenerativeModel(
                    modelName = model.modelId,
                    apiKey    = aiConfig.apiKey,
                )
                val response = genModel.generateContent(testPrompt)
                response.text ?: "{}"
            }.fold(
                onSuccess = { response ->
                    val elapsed = System.currentTimeMillis() - start
                    _uiState.value = ApiTestUiState.Success(response, elapsed)
                }
            ) { error ->
                val elapsed = System.currentTimeMillis() - start
                _uiState.value = ApiTestUiState.Error(
                    error.localizedMessage ?: "未知錯誤", elapsed
                )
            }
        }
    }

    fun reset() { _uiState.value = ApiTestUiState.Idle }
}
