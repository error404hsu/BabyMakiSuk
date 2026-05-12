package com.babymakisuk.featureai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiDispatchException
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.RateLimitException
import com.babymakisuk.data.repository.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiPortalViewModel @Inject constructor(
    private val aiDispatcher: AiDispatcher,
    @Suppress("UnusedPrivateMember") private val aiConfig: AiConfig,
    private val childRepository: ChildRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 由 Navigation 傳入的情境提示（e.g., "PEDIATRIC_DOCTOR"） */
    val presetHint: String? = savedStateHandle["presetHint"]

    private val _uiState = MutableStateFlow<AiPortalUiState>(AiPortalUiState.Idle)
    val uiState: StateFlow<AiPortalUiState> = _uiState.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _selectedPreset = MutableStateFlow(AiPreset.fromHint(presetHint))
    val selectedPreset: StateFlow<AiPreset> = _selectedPreset.asStateFlow()

    /**
     * 情境感知排序：presetHint 對應的 preset 置頂，其餘維持原 enum 順序。
     */
    private val _sortedPresets = MutableStateFlow(buildSortedPresets(presetHint))
    val sortedPresets: StateFlow<List<AiPreset>> = _sortedPresets.asStateFlow()

    // -------------------------------------------------------------------------

    fun selectPreset(preset: AiPreset) {
        _selectedPreset.update { preset }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return

        _chatHistory.update { it + ChatMessage(isUser = true, text = userInput) }
        _uiState.update { AiPortalUiState.Loading }

        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            try {
                // 取得第一個 child profile；若無資料使用安全預設值
                val child     = childRepository.getChildren().firstOrNull()
                val ageMonths = child?.ageMonths ?: 0
                val gender    = child?.gender?.name ?: "未知"
                val allergies = child?.allergies

                val preset       = _selectedPreset.value
                val systemPrompt = AiPromptBuilder.buildSystemPrompt(preset, ageMonths, gender, allergies)

                val response = if (systemPrompt.isBlank()) {
                    aiDispatcher.execute(preset.task, userInput)
                } else {
                    aiDispatcher.executeWithSystemPrompt(preset.task, systemPrompt, userInput)
                }

                val elapsed = System.currentTimeMillis() - startMs
                _chatHistory.update { it + ChatMessage(isUser = false, text = response) }
                _uiState.update { AiPortalUiState.Success(response, elapsed) }

            } catch (e: RateLimitException) {
                _uiState.update {
                    AiPortalUiState.Error("已達每分鐘上限，請 ${e.secondsRemaining} 秒後再試")
                }
            } catch (e: AiDispatchException) {
                _uiState.update {
                    AiPortalUiState.Error(e.message ?: "AI 服務暫時無法使用")
                }
            }
        }
    }

    // -------------------------------------------------------------------------

    private fun buildSortedPresets(hint: String?): List<AiPreset> {
        val hintPreset = AiPreset.fromHint(hint)
        val rest = AiPreset.values().filter { it != hintPreset }
        return listOf(hintPreset) + rest
    }
}
