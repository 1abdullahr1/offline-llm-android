package com.offline.llm.data.repository

import com.offline.llm.data.db.ChatDao
import com.offline.llm.data.model.ChatMessage
import com.offline.llm.data.model.ChatSession
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessages(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun getOrCreateDefaultSession(defaultModel: String): ChatSession {
        val existing = chatDao.getSessionById(1L)
        if (existing != null) {
            return existing
        }
        val newSession = ChatSession(
            id = 1L,
            title = "Offline Session",
            modelName = defaultModel
        )
        chatDao.insertSession(newSession)
        return newSession
    }

    suspend fun addMessage(message: ChatMessage): Long {
        val id = chatDao.insertMessage(message)
        val session = chatDao.getSessionById(message.sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun clearSession(sessionId: Long) {
        chatDao.deleteMessagesForSession(sessionId)
    }
}
