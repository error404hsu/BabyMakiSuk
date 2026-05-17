package com.babymakisuk.featureai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiError
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
    private val contextChildId: Long? = savedStateHandle.get<Long>("contextChildId")
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
            val entities = withContext(Dispatchers.IO) { chatMessageDao.getAllOnce() }
            val effectiveChildId = contextChildId?.takeIf { it != -1L }
                ?: children.firstOrNull()?.id
                ?: -1L

            _uiState.update { state ->
                state.copy(
                    children        = children,
                    selectedChildId = effectiveChildId,
                    messages        = if (entities.isNotEmpty()) entities.map { it.toChatMessage() } else emptyList()
                )
            }
        }
    }

    fun selectChild(childId: Long) {
        if (_uiState.value.selectedChildId == childId) return
        _uiState.update { it.copy(selectedChildId = childId) }
        startNewConversation()
    }

    fun switchPreset(preset: AiPreset) {
        _uiState.update { state ->
            if (state.isModelOverridden) state.copy(selectedPreset = preset)
            else state.copy(selectedPreset = preset, selectedModel = preset.preferredModel)
        }
    }

    fun overrideModel(model: GeminiModel) {
        _uiState.update { it.copy(selectedModel = model, isModelOverridden = true) }
    }

    fun clearModelOverride() {
        _uiState.update { state ->
            state.copy(selectedModel = state.selectedPreset.preferredModel, isModelOverridden = false)
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
                isAiLoading     = true,
                isGenerating    = false,
                isAwaitingInput = false,
                aiError         = null
            )
        }

        val aiMessageId = UUID.randomUUID().toString()
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(id = aiMessageId, role = Role.AI, text = "")
            )
        }

        viewModelScope.launch {
            val selectedChildId = _uiState.value.selectedChildId
            val child = if (selectedChildId > 0) childRepository.getById(selectedChildId)
                        else childRepository.getChildren().firstOrNull()
            val ageMonths = child?.ageMonths ?: 0
            val gender    = child?.gender?.name ?: "未知"
            val allergies = child?.allergies
            val currentPreset = _uiState.value.selectedPreset

            val systemPrompt = buildSystemPromptWithContext(
                preset    = currentPreset,
                ageMonths = ageMonths,
                gender    = gender,
                allergies = allergies,
                childId   = selectedChildId
            )

            val result = aiDispatcher.executeWithSystemPrompt(
                task          = currentPreset.task,
                systemPrompt  = systemPrompt,
                userPrompt    = contextualizedPrompt,
                modelOverride = _uiState.value.effectiveModel
            )

            _uiState.update { it.copy(isAiLoading = false) }

            result.fold(
                onSuccess = { response ->
                    persistMessage(ChatMessage(id = aiMessageId, role = Role.AI, text = response))
                    _uiState.update { it.copy(isGenerating = true) }
                    val prefix = "【以 ${currentPreset.displayName} 的身分回答】\n"
                    for (char in (prefix + response)) {
                        delay(15)
                        updateAiMessage(aiMessageId, char.toString())
                    }
                    _uiState.update { it.copy(isGenerating = false, isAwaitingInput = true) }
                },
                onFailure = { err ->
                    val errorMsg = when (err as? AiError) {
                        is AiError.RateLimited -> {
                            val secs = (err as AiError.RateLimited).secondsRemaining
                            "已達每分鐘上限，請 $secs 秒後再試"
                        }
                        is AiError.AllModelsFailed -> "所有模型均無法使用，請稍後再試"
                        is AiError.InvalidConfig   -> "AI 設定錯誤，請聯絡開發者"
                        is AiError.Cancelled       -> "請求已取消"
                        else -> err.message ?: "AI 服務暫時無法使用"
                    }
                    updateAiMessage(aiMessageId, errorMsg)
                    _uiState.update {
                        it.copy(
                            aiError         = errorMsg,
                            isGenerating    = false,
                            isAwaitingInput = true
                        )
                    }
                }
            )
        }
    }

    fun summarizeToKnowledgeBase() {
        val currentMessages = _uiState.value.messages
        if (currentMessages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "尚無對話內容可整理") }
            return
        }

        _uiState.update { it.copy(isSummarizing = true, isAiLoading = true, aiError = null) }

        viewModelScope.launch {
            val selectedChildId = _uiState.value.selectedChildId
            val child = if (selectedChildId > 0) childRepository.getById(selectedChildId)
                        else childRepository.getChildren().firstOrNull()
            val childName = child?.name ?: "寶寶"

            val transcript = currentMessages.joinToString("\n") { msg ->
                val roleLabel = if (msg.role == Role.USER) "家長" else "AI"
                "$roleLabel：${msg.text}"
            }

            // 透過 AiPromptBuilder 建構 system prompt
            val systemPrompt = AiPromptBuilder.buildSummarySystemPrompt(childName)

            val result = aiDispatcher.executeWithSystemPrompt(
                task          = AiTask.CUSTOM_PRESET,
                systemPrompt  = systemPrompt,
                userPrompt    = "請將以下關於 ${childName} 的育兒對話整理成知識卡：\n$transcript",
                modelOverride = _uiState.value.effectiveModel
            )

            _uiState.update { it.copy(isAiLoading = false) }

            result.fold(
                onSuccess = { raw ->
                    val jsonStart = raw.indexOf('{')
                    val jsonEnd   = raw.lastIndexOf('}')
                    val cleanJson = if (jsonStart != -1 && jsonEnd > jsonStart)
                        raw.substring(jsonStart, jsonEnd + 1) else raw.trim()

                    val (title, content) = try {
                        val json = JSONObject(cleanJson)
                        val t = json.optString("title", "AI 對話摘要").take(15)
                        val c = json.optString("content", "").take(200)
                        if (c.isBlank()) throw Exception("Empty content")
                        t to c
                    } catch (_: Exception) {
                        "AI 對話摘要" to raw.take(200)
                    }

                    if (content.isBlank()) {
                        _uiState.update { it.copy(aiError = "AI 回傳內容為空", isSummarizing = false) }
                        return@fold
                    }

                    val childIdToSave = if (selectedChildId == -1L) "twins"
                                        else selectedChildId.toString()
                    val insight = AiInsightEntity(
                        id         = UUID.randomUUID().toString(),
                        childId    = childIdToSave,
                        title      = title,
                        content    = content,
                        sourceDate = System.currentTimeMillis(),
                        createdAt  = System.currentTimeMillis()
                    )
                    aiInsightDao.insert(insight)
                    _uiState.update {
                        it.copy(errorMessage = "已儲存至「AI 精華」書架", isSummarizing = false)
                    }
                },
                onFailure = { err ->
                    val errorMsg = when (err as? AiError) {
                        is AiError.RateLimited     -> "Rate Limit：請稍後再試"
                        is AiError.AllModelsFailed -> "所有模型均無法使用"
                        is AiError.InvalidConfig   -> "AI 設定錯誤"
                        is AiError.Cancelled       -> "請求已取消"
                        else -> "知識卡整理失敗：${err.message}"
                    }
                    _uiState.update { it.copy(aiError = errorMsg, isSummarizing = false) }
                }
            )
        }
    }

    fun startNewConversation() {
        _uiState.update { it.copy(messages = emptyList(), errorMessage = "已開始新對話") }
        viewModelScope.launch(Dispatchers.IO) { chatMessageDao.deleteAll() }
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
            val updated = state.messages.map { msg ->
                if (msg.id == messageId) msg.copy(text = msg.text + newText) else msg
            }
            state.copy(messages = updated)
        }
    }

    private suspend fun buildSystemPromptWithContext(
        preset: AiPreset,
        ageMonths: Int,
        gender: String,
        allergies: String?,
        childId: Long
    ): String {
        val basePrompt = if (childId == -1L) {
            val children = childRepository.getChildren()
            val twinsAgeInfo = if (children.isNotEmpty())
                "目前月齡約為 ${children.first().ageMonths} 個月" else ""
            """
            你是一位專業的育兒專家。目前對話對象是一對男女雙胞胎的家長。
            $twinsAgeInfo
            請以同時照顧兩位不同性別寶寶的觀點出發，提供平衡且具備雙胞胎家庭特性的建議。
            
            ${preset.systemPrompt}
            """.trimIndent()
        } else {
            AiPromptBuilder.buildSystemPrompt(preset, ageMonths, gender, allergies)
        }

        val child = if (childId > 0) childRepository.getById(childId) else null
        val defaultPrompt = child?.defaultAiPrompt
        val injectedContext = if (childId > 0) {
            try { aiContextInjector.buildContext(childId) } catch (_: Exception) { null }
        } else null

        return buildString {
            append(basePrompt)
            if (!defaultPrompt.isNullOrBlank()) {
                append("\n\n【小孩預設狀態與特質】\n")
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
        return listOf(hintPreset) + AiPreset.entries.filter { it != hintPreset }
    }
}

private fun ChatMessageEntity.toChatMessage() = ChatMessage(
    id          = id,
    role        = try { Role.valueOf(role) } catch (_: Exception) { Role.AI },
    text        = text,
    timestampMs = timestampMs
)
