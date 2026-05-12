package com.babymakisuk.featureai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiDispatchException
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.RateLimitException
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

    private val presetHint: String? = savedStateHandle["presetHint"]

    private val _uiState = MutableStateFlow(
        AiPortalUiState(
            selectedPersona = mapHintToPersona(presetHint),
            sortedPersonas = buildSortedPersonas(presetHint)
        )
    )
    val uiState: StateFlow<AiPortalUiState> = _uiState.asStateFlow()

    fun switchPersona(persona: Persona) {
        _uiState.update { it.copy(selectedPersona = persona) }
    }

    fun switchModel(modelName: String) {
        _uiState.update { it.copy(selectedModel = modelName) }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return

        // 1. 立即顯示使用者訊息，並將狀態設為生成中
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(role = Role.USER, text = prompt),
                isGenerating = true,
                isAwaitingInput = false,
                errorMessage = null
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
                // 取得內容感知所需的資訊
                val child     = childRepository.getChildren().firstOrNull()
                val ageMonths = child?.ageMonths ?: 0
                val gender    = child?.gender?.name ?: "未知"
                val allergies = child?.allergies

                val currentPersona = _uiState.value.selectedPersona
                
                // 結合 Persona 的 System Instruction 與 孩子資訊
                val systemPrompt = """
                    ${currentPersona.systemInstruction}
                    目前諮詢對象資訊：
                    - 月齡：$ageMonths 個月
                    - 性別：$gender
                    ${allergies?.let { "- 過敏史：$it" } ?: ""}
                    請根據以上背景提供專業建議。
                """.trimIndent()

                // 呼叫 AI 服務 (使用自定義任務以支援動態 System Prompt)
                val response = aiDispatcher.executeWithSystemPrompt(
                    com.babymakisuk.coreai.AiTask.CUSTOM_PRESET,
                    systemPrompt, 
                    prompt
                )

                // 模擬 Streaming 的打字機效果 (包含角色名稱回饋)
                val prefix = "【以 ${currentPersona.title} 的身分回答】\n"
                for (char in (prefix + response)) {
                    delay(15)
                    updateAiMessage(aiMessageId, char.toString())
                }

            } catch (e: RateLimitException) {
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
                        isGenerating = false,
                        isAwaitingInput = true
                    )
                }
            }
        }
    }

    private fun updateAiMessage(messageId: String, newText: String) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(text = msg.text + newText)
                } else msg
            }
            state.copy(messages = updatedMessages)
        }
    }

    fun summarizeToKnowledgeBase() {
        val currentMessages = _uiState.value.messages
        if (currentMessages.isEmpty()) return
        
        viewModelScope.launch {
            // TODO: 提取知識點並寫入 Database
        }
    }

    private fun mapHintToPersona(hint: String?): Persona {
        return when (hint) {
            "PEDIATRIC_DOCTOR" -> Persona.DOCTOR
            "PHARMACIST" -> Persona.PHARMACIST
            "NUTRITIONIST" -> Persona.NUTRITIONIST
            else -> Persona.ASSISTANT
        }
    }

    private fun buildSortedPersonas(hint: String?): List<Persona> {
        val hintPersona = mapHintToPersona(hint)
        val rest = Persona.entries.filter { it != hintPersona }
        return listOf(hintPersona) + rest
    }
}
