package com.offline.llm.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.offline.llm.data.model.ChatMessage
import com.offline.llm.data.model.MessageRole
import com.offline.llm.databinding.ItemChatMessageBinding

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            if (message.role == MessageRole.USER) {
                binding.layoutUser.visibility = View.VISIBLE
                binding.layoutAssistant.visibility = View.GONE
                binding.tvUserContent.text = message.content
            } else {
                binding.layoutUser.visibility = View.GONE
                binding.layoutAssistant.visibility = View.VISIBLE
                binding.tvAssistantContent.text = message.content

                if (message.tokensPerSecond > 0 || message.timeToFirstTokenMs > 0) {
                    binding.tvAssistantMetrics.visibility = View.VISIBLE
                    binding.tvAssistantMetrics.text = String.format(
                        "%.1f tok/s | TTFT: %d ms | Tokens: %d",
                        message.tokensPerSecond,
                        message.timeToFirstTokenMs,
                        message.totalTokens
                    )
                } else {
                    binding.tvAssistantMetrics.visibility = View.GONE
                }
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
