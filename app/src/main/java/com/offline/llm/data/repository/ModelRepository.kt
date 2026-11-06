package com.offline.llm.data.repository

import android.content.Context
import android.net.Uri
import com.offline.llm.data.model.ModelInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelRepository(private val context: Context) {

    private val modelsDirectory: File by lazy {
        File(context.filesDir, "models").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    val storageDirectoryPath: String
        get() = modelsDirectory.absolutePath

    @Volatile
    private var isCancelled: Boolean = false

    private val curatedCatalog = listOf(
        CatalogItem(
            name = "SmolLM2 360M Instruct (Q4_0)",
            fileName = "smollm2-360m-instruct-q4_0.gguf",
            fileSizeBytes = 245L * 1024L * 1024L,
            quantization = "Q4_0",
            estimatedRamMb = 650,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_0.gguf",
            description = "Ultra-compact mobile model by Hugging Face. Very low memory and battery usage.",
            isDefault = true
        ),
        CatalogItem(
            name = "Llama 3.2 1B Instruct (Q4_K_M)",
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            fileSizeBytes = 740L * 1024L * 1024L,
            quantization = "Q4_K_M",
            estimatedRamMb = 1250,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            description = "Meta flagship edge LLM. High instruction-following capability.",
            isDefault = false
        ),
        CatalogItem(
            name = "Qwen 2.5 0.5B Instruct (Q4_0)",
            fileName = "qwen2.5-0.5b-instruct-q4_0.gguf",
            fileSizeBytes = 390L * 1024L * 1024L,
            quantization = "Q4_0",
            estimatedRamMb = 800,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_0.gguf",
            description = "Multilingual edge model by Alibaba Cloud. Runs smoothly on 3-4 GB RAM phones.",
            isDefault = false
        ),
        CatalogItem(
            name = "SmolLM2 135M Instruct (Q4_0)",
            fileName = "smollm2-135m-instruct-q4_0.gguf",
            fileSizeBytes = 95L * 1024L * 1024L,
            quantization = "Q4_0",
            estimatedRamMb = 450,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_0.gguf",
            description = "Micro model for instant generation and low-resource testing.",
            isDefault = false
        )
    )

    suspend fun getAvailableModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ModelInfo>()

        // 1. Scan disk files
        val diskFiles = modelsDirectory.listFiles { file ->
            file.isFile && (file.name.endsWith(".gguf", ignoreCase = true) || file.name.endsWith(".bin", ignoreCase = true)) && !file.name.endsWith(".tmp")
        } ?: emptyArray()

        val diskFileMap = diskFiles.associateBy { it.name }

        // 2. Add curated catalog items with downloaded status
        for (item in curatedCatalog) {
            val localFile = diskFileMap[item.fileName]
            val isDownloaded = localFile != null && localFile.length() > 1024L * 1024L // at least 1MB
            val actualPath = localFile?.absolutePath ?: File(modelsDirectory, item.fileName).absolutePath
            val actualSize = localFile?.length() ?: item.fileSizeBytes

            list.add(
                ModelInfo(
                    name = item.name,
                    fileName = item.fileName,
                    filePath = actualPath,
                    fileSizeBytes = actualSize,
                    quantization = item.quantization,
                    estimatedRamMb = item.estimatedRamMb,
                    downloadUrl = item.downloadUrl,
                    description = item.description,
                    isDownloaded = isDownloaded,
                    isDefault = item.isDefault
                )
            )
        }

        // 3. Add any other imported files on disk that aren't in the curated catalog
        for (file in diskFiles) {
            if (curatedCatalog.none { it.fileName == file.name }) {
                val quant = parseQuantizationFromName(file.name)
                val sizeMb = (file.length() / (1024 * 1024)).toInt()
                val estRam = (sizeMb * 1.15).toInt() + 400

                list.add(
                    ModelInfo(
                        name = file.name,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileSizeBytes = file.length(),
                        quantization = quant,
                        estimatedRamMb = estRam,
                        downloadUrl = null,
                        description = "Imported local model file",
                        isDownloaded = true,
                        isDefault = false
                    )
                )
            }
        }

        // Ensure default demo stub exists if none downloaded yet
        val defaultItem = list.firstOrNull { it.isDefault }
        if (defaultItem != null && !defaultItem.isDownloaded) {
            val demoFile = File(modelsDirectory, defaultItem.fileName)
            if (!demoFile.exists()) {
                try {
                    demoFile.writeBytes(byteArrayOf(0x47, 0x47, 0x55, 0x46, 0x03, 0x00, 0x00, 0x00))
                } catch (_: Exception) {}
            }
        }

        list
    }

    suspend fun downloadModel(
        urlStr: String,
        fileName: String,
        onProgress: (progress: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        isCancelled = false
        val targetFile = File(modelsDirectory, fileName)
        val tempFile = File(modelsDirectory, "$fileName.tmp")

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            var currentUrl = urlStr
            var redirects = 0
            while (redirects < 6) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "EdgeLLM-Android/1.0")

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    currentUrl = newUrl
                    connection.disconnect()
                    redirects++
                } else if (status == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    return@withContext Result.failure(IllegalStateException("Server returned HTTP $status"))
                }
            }

            val totalBytes = connection?.contentLengthLong ?: -1L
            inputStream = connection?.inputStream ?: return@withContext Result.failure(IllegalStateException("No input stream available"))
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(64 * 1024)
            var downloadedBytes = 0L
            var lastReportTime = 0L

            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) {
                    tempFile.delete()
                    return@withContext Result.failure(CancellationException("Download cancelled by user"))
                }

                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastReportTime > 200 || downloadedBytes == totalBytes) {
                    lastReportTime = now
                    val progress = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
                    onProgress(progress, downloadedBytes, totalBytes)
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Atomically rename .tmp to target file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    fun cancelDownload() {
        isCancelled = true
    }

    suspend fun deleteModel(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(modelsDirectory, fileName)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    suspend fun importModelFromUri(uri: Uri, fileName: String): Result<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            val destination = File(modelsDirectory, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destination).use { outputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not read from selected file URI."))

            val quant = parseQuantizationFromName(destination.name)
            val sizeMb = (destination.length() / (1024 * 1024)).toInt()
            val estRam = (sizeMb * 1.15).toInt() + 400

            Result.success(
                ModelInfo(
                    name = destination.name,
                    fileName = destination.name,
                    filePath = destination.absolutePath,
                    fileSizeBytes = destination.length(),
                    quantization = quant,
                    estimatedRamMb = estRam,
                    isDownloaded = true,
                    isDefault = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseQuantizationFromName(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.contains("q4_k_m") -> "Q4_K_M"
            lower.contains("q4_0") -> "Q4_0"
            lower.contains("q8_0") -> "Q8_0"
            lower.contains("q5_k_m") -> "Q5_K_M"
            lower.contains("f16") -> "F16"
            else -> "Q4_0"
        }
    }

    private data class CatalogItem(
        val name: String,
        val fileName: String,
        val fileSizeBytes: Long,
        val quantization: String,
        val estimatedRamMb: Int,
        val downloadUrl: String,
        val description: String,
        val isDefault: Boolean = false
    )
}
