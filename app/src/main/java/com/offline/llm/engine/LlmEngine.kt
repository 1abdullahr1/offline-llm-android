package com.offline.llm.engine

import kotlinx.coroutines.flow.Flow

interface LlmEngine {
    val isLoaded: Boolean
    val currentConfig: ModelConfig?

    suspend fun loadModel(config: ModelConfig): Result<Unit>
    fun generateStream(prompt: String): Flow<TokenChunk>
    fun stopGeneration()
    fun unloadModel()
}
