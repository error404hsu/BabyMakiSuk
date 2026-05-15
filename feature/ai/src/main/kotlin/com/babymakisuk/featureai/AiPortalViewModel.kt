package com.babymakisuk.featureai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.AiDispatchException
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coreai.GeminiModel
import com.babymakisuk.coredata.ai.AiContextInjector
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.dao.ChatMessageDao
import com.babymakisuk.coredata.entity.AiInsightEntity
import com.babymakisuk.coredata.entity.ChatMessageEntity
import com.babymakisuk.coredata.repository.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiPortalViewModel @Inject constructor(
    private val aiDispatcher: AiDispatcher,
    @Suppress("UnusedPrivateMember") private val aiConfig: AiConfig,
    private val childRepository: ChildRepository,
    private val aiInsightDao: AiInsightDao,
    private val chatMessageDao: ChatMessageDao,
    private val aiContextInjector: AiContextInjector,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val presetHint: String? = savedStateHandle["presetHint"]
    private val contextChildId: Long? = (savedStateHandle.get<Long>("contextChildId"))

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

    init {
        viewModelScope.launch {
            val children = childRepository.observeAll().first()
            val entities = withContext(Dispatchers.IO) {
                chatMessageDao.getAllOnce()
            }
            val effectiveChildId = contextChildId?.takeIf { it != -1L }
                ?: children.firstOrNull()?.id
                ?: -1L

            _uiState.update { state ->
                state.copy(
                    children = children,
                    selectedChildId = effectiveChildId,
                    messages = if (entities.isNotEmpty()) entities.map { it.toChatMessage() } else emptyList()
                )
            }
        }
    }

    fun selectChild(childId: Long) {
        _uiState.update { it.copy(selectedChildId = childId) }
    }

    fun switchPreset(preset: AiPreset) {
        _uiState.update { state ->
            if (state.isModelOverridden) {
                state.copy(selectedPreset = preset)
            } else {
                state.copy(
                    selectedPreset = preset,
                    selectedModel  = preset.preferredModel
                )
            }
        }
    }

    fun overrideModel(model: GeminiModel) {
        _uiState.update {
            it.copy(
                selectedModel     = model,
                isModelOverridden = true
            )
        }
    }

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

        val contextualizedPrompt = buildContextualizedPrompt(prompt)

        val userMsg = ChatMessage(role = Role.USER, text = prompt)
        persistMessage(userMsg)

        _uiState.update { state ->
            state.copy(
                messages        = state.messages + userMsg,
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
                val selectedChildId = _uiState.value.selectedChildId
                val child = if (selectedChildId > 0) childRepository.getById(selectedChildId) else childRepository.getChildren().firstOrNull()
                val ageMonths = child?.ageMonths ?: 0
                val gender    = child?.gender?.name ?: "未知"
                val allergies = child?.allergies

                val currentPreset = _uiState.value.selectedPreset

                val systemPrompt = buildSystemPromptWithContext(
                    preset     = currentPreset,
                    ageMonths  = ageMonths,
                    gender     = gender,
                    allergies  = allergies,
                    childId    = selectedChildId
                )

                val response = aiDispatcher.executeWithSystemPrompt(
                    task          = currentPreset.task,
                    systemPrompt  = systemPrompt,
                    userPrompt    = contextualizedPrompt,
                    modelOverride = _uiState.value.effectiveModel
                )

                persistMessage(ChatMessage(id = aiMessageId, role = Role.AI, text = response))

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

    private fun persistMessage(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.insert(
                ChatMessageEntity(
                    id          = message.id,
                    role        = message.role.name,
                    text        = message.text,
                    timestampMs = message.timestampMs
                )
            )
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

    fun summarizeToKnowledgeBase() {
        val currentMessages = _uiState.value.messages
        if (currentMessages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "尚無對話內容可整理") }
            return
        }

        _uiState.update { it.copy(isSummarizing = true) }

        viewModelScope.launch {
            try {
                val transcript = currentMessages.joinToString("\n") { msg ->
                    val roleLabel = if (msg.role == Role.USER) "家長" else "AI"
                    "$roleLabel：${msg.text}"
                }
                val selectedChildId = _uiState.value.selectedChildId
                val child = if (selectedChildId > 0) childRepository.getById(selectedChildId) else childRepository.getChildren().firstOrNull()
                val childId = child?.id?.toString() ?: "unknown"
                val childName = child?.name ?: "寶寶"

                val systemPrompt = buildString {
                    appendLine("你是一位對話摘要 AI，專門將育兒對話整理成結構化知識卡。")
                    appendLine()
                    appendLine("【輸出規則 - 嚴格遵守】")
                    appendLine("- 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、說明文字")
                    appendLine("- 禁止使用 Markdown 包裝（禁止 ```json```）")
                    appendLine("- JSON schema：")
                    appendLine("""{ "title": "簡潔標題（15字內）", "content": "重點摘要整理（200字內，以繁體中文撰寫）" }""")
                }

                val raw = aiDispatcher.executeWithSystemPrompt(
                    task          = AiTask.CUSTOM_PRESET,
                    systemPrompt  = systemPrompt,
                    userPrompt    = "請將以下關於 ${childName} 的育兒對話整理成知識卡：\n$transcript",
                    modelOverride = _uiState.value.effectiveModel
                )

                val cleanJson = raw.substringAfter("```json")
                    .substringBefore("```")
                    .trim()
                    .ifBlank { raw }

                val json = JSONObject(cleanJson)
                val title = json.optString("title", "AI 對話摘要")
                    .take(15)
                val content = json.optString("content", raw.take(200))
                    .take(200)

                val insight = AiInsightEntity(
                    id         = UUID.randomUUID().toString(),
                    childId    = childId,
                    title      = title,
                    content    = content,
                    sourceDate = System.currentTimeMillis(),
                    createdAt  = System.currentTimeMillis()
                )
                aiInsightDao.insert(insight)

                _uiState.update { it.copy(errorMessage = "已儲存至「AI 精華」書架", isSummarizing = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "知識卡整理失敗：${e.message}", isSummarizing = false) }
            }
        }
    }

    fun startNewConversation() {
        _uiState.update { it.copy(messages = emptyList(), errorMessage = "已開始新對話") }
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.deleteAll()
        }
    }

    private suspend fun buildSystemPromptWithContext(
        preset: AiPreset,
        ageMonths: Int,
        gender: String,
        allergies: String?,
        childId: Long
    ): String {
        val child = if (childId > 0) childRepository.getById(childId) else null
        val basePrompt = AiPromptBuilder.buildSystemPrompt(preset, ageMonths, gender, allergies)

        val defaultPrompt = child?.defaultAiPrompt
        val injectedContext = if (childId > 0) {
            try {
                aiContextInjector.buildContext(childId)
            } catch (_: Exception) {
                null
            }
        } else null

        return buildString {
            append(basePrompt)
            if (!defaultPrompt.isNullOrBlank()) {
                append("\n\n【小孩預設狀態】\n")
                append(defaultPrompt)
            }
            if (!injectedContext.isNullOrBlank()) {
                append("\n\n$injectedContext")
            }
        }
    }

    private fun buildContextualizedPrompt(currentPrompt: String): String {
        val msgs = _uiState.value.messages
        if (msgs.isEmpty()) return currentPrompt

        val recent = msgs.takeLast(20)

        return buildString {
            appendLine("【對話紀錄】")
            recent.forEach { msg ->
                val roleLabel = if (msg.role == Role.USER) "家長" else "AI"
                appendLine("$roleLabel：${msg.text}")
            }
            appendLine()
            appendLine("【目前問題】")
            append(currentPrompt)
        }
    }

    private fun buildSortedPresets(hint: String?): List<AiPreset> {
        val hintPreset = AiPreset.fromHint(hint)
        val rest = AiPreset.entries.filter { it != hintPreset }
        return listOf(hintPreset) + rest
    }
}

private fun ChatMessageEntity.toChatMessage() = ChatMessage(
    id          = id,
    role        = try { Role.valueOf(role) } catch (_: Exception) { Role.AI },
    text        = text,
    timestampMs = timestampMs
)
