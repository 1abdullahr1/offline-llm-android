package com.offline.llm.data.model

data class ModelInfo(
    val name: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val quantization: String,
    val estimatedRamMb: Int,
    val downloadUrl: String? = null,
    val description: String = "",
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val isDefault: Boolean = false
) {
    val formattedSize: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
