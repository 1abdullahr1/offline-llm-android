package com.offline.llm.engine

data class TokenChunk(
    val token: String,
    val isDone: Boolean = false,
    val timeToFirstTokenMs: Long = 0L,
    val tokensPerSecond: Double = 0.0,
    val totalTokens: Int = 0
)
