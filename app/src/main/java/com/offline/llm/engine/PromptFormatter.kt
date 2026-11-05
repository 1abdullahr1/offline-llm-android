package com.offline.llm.engine

object PromptFormatter {

    fun format(
        template: PromptTemplate,
        systemPrompt: String?,
        history: List<Pair<String, String>>,
        userInput: String
    ): String {
        return when (template) {
            PromptTemplate.LLAMA_3 -> formatLlama3(systemPrompt, history, userInput)
            PromptTemplate.CHAT_ML -> formatChatML(systemPrompt, history, userInput)
            PromptTemplate.GEMMA_2 -> formatGemma2(systemPrompt, history, userInput)
            PromptTemplate.RAW -> userInput
        }
    }

    private fun formatChatML(
        systemPrompt: String?,
        history: List<Pair<String, String>>,
        userInput: String
    ): String {
        val sb = StringBuilder()
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("<|im_start|>system\n").append(systemPrompt.trim()).append("<|im_end|>\n")
        }
        for ((role, content) in history) {
            sb.append("<|im_start|>").append(role).append("\n")
                .append(content.trim())
                .append("<|im_end|>\n")
        }
        sb.append("<|im_start|>user\n").append(userInput.trim()).append("<|im_end|>\n")
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun formatLlama3(
        systemPrompt: String?,
        history: List<Pair<String, String>>,
        userInput: String
    ): String {
        val sb = StringBuilder("<|begin_of_text|>")
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("<|start_header_id|>system<|end_header_id|>\n\n")
                .append(systemPrompt.trim())
                .append("<|eot_id|>")
        }
        for ((role, content) in history) {
            sb.append("<|start_header_id|>").append(role).append("<|end_header_id|>\n\n")
                .append(content.trim())
                .append("<|eot_id|>")
        }
        sb.append("<|start_header_id|>user<|end_header_id|>\n\n")
            .append(userInput.trim())
            .append("<|eot_id|>")
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }

    private fun formatGemma2(
        systemPrompt: String?,
        history: List<Pair<String, String>>,
        userInput: String
    ): String {
        val sb = StringBuilder()
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("<start_of_turn>user\n").append("System: ").append(systemPrompt.trim()).append("\n")
        }
        for ((role, content) in history) {
            sb.append("<start_of_turn>").append(role).append("\n")
                .append(content.trim())
                .append("<end_of_turn>\n")
        }
        sb.append("<start_of_turn>user\n").append(userInput.trim()).append("<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }
}
