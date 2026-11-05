package com.offline.llm.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offline.llm.data.db.AppDatabase
import com.offline.llm.data.model.ChatMessage
import com.offline.llm.data.model.ChatSession
import com.offline.llm.data.model.MessageRole
import com.offline.llm.data.repository.ChatRepository
import com.offline.llm.data.repository.ModelRepository
import com.offline.llm.engine.CpuTopologyDetector
import com.offline.llm.engine.ModelConfig
import com.offline.llm.engine.NativeLlmBridge
import com.offline.llm.engine.PromptFormatter
import com.offline.llm.engine.PromptTemplate
import com.offline.llm.engine.ThermalController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveMetrics(
    val tokensPerSecond: Double = 0.0,
    val timeToFirstTokenMs: Long = 0L,
    val totalTokens: Int = 0,
    val activeThreads: Int = 4,
    val isGenerating: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatRepository = ChatRepository(db.chatDao())
    private val modelRepository = ModelRepository(application)
    val thermalController = ThermalController(application)

    val engine = NativeLlmBridge(application, thermalController)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activeSession = MutableStateFlow<ChatSession?>(null)
    val activeSession: StateFlow<ChatSession?> = _activeSession.asStateFlow()

    private val _liveMetrics = MutableStateFlow(LiveMetrics())
    val liveMetrics: StateFlow<LiveMetrics> = _liveMetrics.asStateFlow()

    private val _activeModelName = MutableStateFlow("No model loaded")
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    private var currentSessionId: Long = 1L
    private var generationJob: Job? = null

    init {
        thermalController.startMonitoring()
        viewModelScope.launch {
            val session = chatRepository.getOrCreateDefaultSession("smollm2-360m-instruct-q4_0.gguf")
            currentSessionId = session.id
            _activeSession.value = session
            _activeModelName.value = session.modelName

            chatRepository.getMessages(session.id).collect { list ->
                _messages.value = list
            }
        }

        // Auto-initialize default model on launch
        viewModelScope.launch {
            val models = modelRepository.getAvailableModels()
            val defaultModel = models.firstOrNull()
            if (defaultModel != null) {
                val topology = CpuTopologyDetector.detect()
                val config = ModelConfig(
                    modelPath = defaultModel.filePath,
                    modelName = defaultModel.name,
                    threads = topology.recommendedInferenceThreads,
                    template = PromptTemplate.CHAT_ML
                )
                engine.loadModel(config)
                _activeModelName.value = defaultModel.name
                _liveMetrics.value = _liveMetrics.value.copy(activeThreads = topology.recommendedInferenceThreads)
            }
        }
    }

    fun sendMessage(userText: String) {
        val query = userText.trim()
        if (query.isEmpty() || _liveMetrics.value.isGenerating) return

        viewModelScope.launch {
            val userMsg = ChatMessage(
                sessionId = currentSessionId,
                role = MessageRole.USER,
                content = query
            )
            chatRepository.addMessage(userMsg)

            // Prepare prompt template
            val historyPairs = _messages.value.map {
                val roleStr = if (it.role == MessageRole.USER) "user" else "assistant"
                roleStr to it.content
            }
            val formattedPrompt = PromptFormatter.format(
                template = engine.currentConfig?.template ?: PromptTemplate.CHAT_ML,
                systemPrompt = "You are a helpful, concise AI assistant running completely offline on an Android device.",
                history = historyPairs,
                userInput = query
            )

            // Start generation
            _liveMetrics.value = _liveMetrics.value.copy(isGenerating = true)

            var assistantContent = ""
            var lastTtft = 0L
            var lastSpeed = 0.0
            var lastTokens = 0

            // Placeholder message in memory while streaming
            val tempId = System.currentTimeMillis()

            generationJob = launch {
                try {
                    engine.generateStream(formattedPrompt).collect { chunk ->
                        if (!chunk.isDone) {
                            assistantContent += chunk.token
                            lastTtft = chunk.timeToFirstTokenMs
                            lastSpeed = chunk.tokensPerSecond
                            lastTokens = chunk.totalTokens

                            _liveMetrics.value = _liveMetrics.value.copy(
                                tokensPerSecond = chunk.tokensPerSecond,
                                timeToFirstTokenMs = chunk.timeToFirstTokenMs,
                                totalTokens = chunk.totalTokens
                            )

                            // Update local streaming preview
                            val currentList = _messages.value.filter { it.id != tempId }
                            _messages.value = currentList + ChatMessage(
                                id = tempId,
                                sessionId = currentSessionId,
                                role = MessageRole.ASSISTANT,
                                content = assistantContent,
                                tokensPerSecond = chunk.tokensPerSecond,
                                timeToFirstTokenMs = chunk.timeToFirstTokenMs,
                                totalTokens = chunk.totalTokens
                            )
                        } else {
                            // Completed
                            val finalAssistantMsg = ChatMessage(
                                sessionId = currentSessionId,
                                role = MessageRole.ASSISTANT,
                                content = assistantContent.ifEmpty { "Model generated empty response." },
                                tokensPerSecond = chunk.tokensPerSecond,
                                timeToFirstTokenMs = lastTtft,
                                totalTokens = chunk.totalTokens
                            )
                            chatRepository.addMessage(finalAssistantMsg)
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = ChatMessage(
                        sessionId = currentSessionId,
                        role = MessageRole.ASSISTANT,
                        content = "Inference error: ${e.message}"
                    )
                    chatRepository.addMessage(errorMsg)
                } finally {
                    _liveMetrics.value = _liveMetrics.value.copy(isGenerating = false)
                }
            }
        }
    }

    fun stopGeneration() {
        engine.stopGeneration()
        generationJob?.cancel()
        _liveMetrics.value = _liveMetrics.value.copy(isGenerating = false)
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearSession(currentSessionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        thermalController.stopMonitoring()
        engine.unloadModel()
    }
}
