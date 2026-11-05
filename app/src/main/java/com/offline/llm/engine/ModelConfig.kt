package com.offline.llm.engine

enum class KvCacheQuantization {
    FP16,
    Q8_0,
    Q4_0
}

enum class PromptTemplate {
    LLAMA_3,
    CHAT_ML,
    GEMMA_2,
    RAW
}

data class ModelConfig(
    val modelPath: String,
    val modelName: String,
    val contextLength: Int = 2048,
    val threads: Int = 4,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val useMmap: Boolean = true,
    val kvQuantization: KvCacheQuantization = KvCacheQuantization.Q8_0,
    val template: PromptTemplate = PromptTemplate.CHAT_ML
)
