package com.offline.llm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val role: MessageRole,
    val content: String,
    val tokensPerSecond: Double = 0.0,
    val timeToFirstTokenMs: Long = 0L,
    val totalTokens: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
