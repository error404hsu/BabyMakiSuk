package com.babymakisuk.featureai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.AiDispatchException
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiTask
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

    private val _uiState = MutableStateFlow(
        AiPortalUiState(
            selectedPreset = AiPreset.fromHint(presetHint),
            sortedPresets  = buildSortedPresets(presetHint)
        )
    )
    val uiState: StateFlow<AiPortalUiState> = _uiState.asStateFlow()

    /** 切換角色：直接使用 core/ai AiPreset，無需任何字串轉換 */
    fun switchPreset(preset: AiPreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    /** 切換模型：使用強型別 GeminiModel，與 AiDispatcher 對齊 */
    fun switchModel(model: GeminiModel) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return

        // 1. 立即顯示使用者訊息，並將狀態設為生成中
        _uiState.update { state ->
            state.copy(
                messages      = state.messages + ChatMessage(role = Role.USER, text = prompt),
                isGenerating  = true,
                isAwaitingInput = false,
                errorMessage  = null
            )
        }

        // 2. 建立空的 AI 訊息容器，以 UUID 綁定，準備接收資料流
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

                val currentPreset = _uiState.value.selectedPreset

                // 結合 AiPreset.systemPrompt（core 定義）與孩子資訊
                val systemPrompt = """
                    ${currentPreset.systemPrompt}
                    目前諮詢對象資訊：
                    - 月齡：$ageMonths 個月
                    - 性別：$gender
                    ${allergies?.let { "- 過敏史：$it" } ?: ""}
                    請根據以上背景提供專業建議。
                """.trimIndent()

                // 使用 AiPreset 對應的 AiTask 呼叫 AiDispatcher
                val response = aiDispatcher.executeWithSystemPrompt(
                    task         = currentPreset.task,
                    systemPrompt = systemPrompt,
                    userPrompt   = prompt
                )

                // 模擬 Streaming 打字機效果
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

    /**
     * 依 presetHint 建立排序清單：hint 對應的角色置頂，其餘依原順序排列。
     * 直接使用 AiPreset.fromHint，不需要任何 when 手動對應。
     */
    private fun buildSortedPresets(hint: String?): List<AiPreset> {
        val hintPreset = AiPreset.fromHint(hint)
        val rest = AiPreset.entries.filter { it != hintPreset }
        return listOf(hintPreset) + rest
    }
}
