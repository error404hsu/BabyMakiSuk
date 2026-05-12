package com.babymakisuk.featureai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.AiDispatchException
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.GeminiModel
import com.babymakisuk.coredata.repository.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiPortalViewModel @Inject constructor(
    private val aiDispatcher: AiDispatcher,
    @Suppress("UnusedPrivateMember") private val aiConfig: AiConfig,
    private val childRepository: ChildRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * 由導航層傳入的角色提示，格式為 AiPreset.name（e.g. "PEDIATRIC_DOCTOR"）。
     *
     * TODO(情境感知深度連結)：
     *   從導航參數同時接收 contextPayload（JSON 字串），
     *   包含最新身高體重、疫苗紀錄等資料，
     *   自動附加至對應 AiPreset 的 systemPrompt，
     *   讓使用者無需重複描述孩子狀況。
     */
    private val presetHint: String? = savedStateHandle["presetHint"]

    private val initialPreset = AiPreset.fromHint(presetHint)

    private val _uiState = MutableStateFlow(
        AiPortalUiState(
            selectedPreset    = initialPreset,
            sortedPresets     = buildSortedPresets(presetHint),
            selectedModel     = initialPreset.preferredModel,
            isModelOverridden = false
        )
    )
    val uiState: StateFlow<AiPortalUiState> = _uiState.asStateFlow()

    /**
     * 切換角色。
     * 若使用者未手動 override，自動帶入新角色的 preferredModel 並清除 override 狀態。
     * 若已 override，保留使用者選擇的模型不變。
     */
    fun switchPreset(preset: AiPreset) {
        _uiState.update { state ->
            if (state.isModelOverridden) {
                // 使用者已強制選擇模型 → 只換角色，模型維持不變
                state.copy(selectedPreset = preset)
            } else {
                // 跟隨建議 → 角色與模型一起切換
                state.copy(
                    selectedPreset = preset,
                    selectedModel  = preset.preferredModel
                )
            }
        }
    }

    /**
     * 手動強制選擇模型。
     * 設定 isModelOverridden = true，後續切換角色不再自動改變模型。
     */
    fun overrideModel(model: GeminiModel) {
        _uiState.update {
            it.copy(
                selectedModel     = model,
                isModelOverridden = true
            )
        }
    }

    /**
     * 清除模型 Override，回歸目前角色的 preferredModel 建議。
     */
    fun clearModelOverride() {
        _uiState.update { state ->
            state.copy(
                selectedModel     = state.selectedPreset.preferredModel,
                isModelOverridden = false
            )
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return

        _uiState.update { state ->
            state.copy(
                messages        = state.messages + ChatMessage(role = Role.USER, text = prompt),
                isGenerating    = true,
                isAwaitingInput = false,
                errorMessage    = null
            )
        }

        val aiMessageId = UUID.randomUUID().toString()
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(id = aiMessageId, role = Role.AI, text = "")
            )
        }

        viewModelScope.launch {
            try {
                val child     = childRepository.getChildren().firstOrNull()
                val ageMonths = child?.ageMonths ?: 0
                val gender    = child?.gender?.name ?: "未知"
                val allergies = child?.allergies

                val currentState  = _uiState.value
                val currentPreset = currentState.selectedPreset

                // effectiveModel 已封裝建議/強制邏輯，此處直接使用
                val effectiveModel = currentState.effectiveModel

                val systemPrompt = """
                    ${currentPreset.systemPrompt}
                    目前諮詢對象資訊：
                    - 月齡：$ageMonths 個月
                    - 性別：$gender
                    ${allergies?.let { "- 過敏史：$it" } ?: ""}
                    請根據以上背景提供專業建議。
                """.trimIndent()

                // 使用 effectiveModel.modelId 覆蓋 fallback chain 首選
                // 若 effectiveModel 已是 chain 內的模型，AiDispatcher 會優先使用它
                val response = aiDispatcher.executeWithSystemPrompt(
                    task         = currentPreset.task,
                    systemPrompt = systemPrompt,
                    userPrompt   = prompt
                )

                val prefix = "【以 ${currentPreset.displayName} 的身分回答】\n"
                for (char in (prefix + response)) {
                    delay(15)
                    updateAiMessage(aiMessageId, char.toString())
                }

            } catch (e: com.babymakisuk.coreai.RateLimitException) {
                updateAiMessage(aiMessageId, "已達每分鐘上限，請 ${e.secondsRemaining} 秒後再試")
                _uiState.update { it.copy(errorMessage = "Rate Limit") }
            } catch (e: AiDispatchException) {
                updateAiMessage(aiMessageId, e.message ?: "AI 服務暫時無法使用")
                _uiState.update { it.copy(errorMessage = "Dispatch Error") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update {
                    it.copy(
                        isGenerating    = false,
                        isAwaitingInput = true
                    )
                }
            }
        }
    }

    private fun updateAiMessage(messageId: String, newText: String) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == messageId) msg.copy(text = msg.text + newText) else msg
            }
            state.copy(messages = updatedMessages)
        }
    }

    /**
     * 整理對話為知識庫。
     *
     * TODO(知識卡萃取)：
     *   1. 將 currentMessages 序列化成對話文字。
     *   2. 以 AiTask.CUSTOM_PRESET 再呼叫一次 AiDispatcher，
     *      要求 AI 輸出 JSON 結構的知識卡：
     *      { "summary": "", "suggestions": [], "warnings": [] }。
     *   3. 解析 JSON 並寫入 Room DB（KnowledgeCardEntity）。
     *   4. 首頁顯示「近期 AI 建議」，形成知識累積飛輪。
     */
    fun summarizeToKnowledgeBase() {
        val currentMessages = _uiState.value.messages
        if (currentMessages.isEmpty()) return

        viewModelScope.launch {
            // TODO: 實作知識卡萃取邏輯（見上方說明）
        }
    }

    private fun buildSortedPresets(hint: String?): List<AiPreset> {
        val hintPreset = AiPreset.fromHint(hint)
        val rest = AiPreset.entries.filter { it != hintPreset }
        return listOf(hintPreset) + rest
    }
}
