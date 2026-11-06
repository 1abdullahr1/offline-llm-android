package com.offline.llm.ui.models

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offline.llm.data.model.ModelInfo
import com.offline.llm.data.repository.ModelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ModelRepository(application)

    private val _models = MutableStateFlow<List<ModelInfo>>(emptyList())
    val models: StateFlow<List<ModelInfo>> = _models.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var downloadJob: Job? = null
    private var currentlyDownloadingFileName: String? = null

    val storageDirectoryPath: String get() = repository.storageDirectoryPath

    init {
        loadModels()
    }

    fun loadModels() {
        viewModelScope.launch {
            _isLoading.value = true
            _models.value = repository.getAvailableModels()
            _isLoading.value = false
        }
    }

    fun startDownload(model: ModelInfo, onFinished: (Boolean, String?) -> Unit) {
        val url = model.downloadUrl ?: return
        if (downloadJob != null && downloadJob?.isActive == true) {
            onFinished(false, "Another download is already in progress.")
            return
        }

        currentlyDownloadingFileName = model.fileName
        downloadJob = viewModelScope.launch {
            updateDownloadState(model.fileName, isDownloading = true, progress = 0)

            val result = repository.downloadModel(url, model.fileName) { progress, downloaded, total ->
                updateDownloadState(model.fileName, isDownloading = true, progress = progress)
            }

            result.fold(
                onSuccess = {
                    currentlyDownloadingFileName = null
                    loadModels()
                    onFinished(true, null)
                },
                onFailure = { error ->
                    currentlyDownloadingFileName = null
                    updateDownloadState(model.fileName, isDownloading = false, progress = 0)
                    loadModels()
                    onFinished(false, error.message)
                }
            )
        }
    }

    fun startDownloadFromCustomUrl(url: String, onFinished: (Boolean, String?) -> Unit) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || !cleanUrl.startsWith("http", ignoreCase = true)) {
            onFinished(false, "Invalid URL. Please enter a valid HTTP/HTTPS link.")
            return
        }

        val rawFileName = cleanUrl.substringAfterLast("/").substringBefore("?")
        val targetFileName = if (rawFileName.endsWith(".gguf", ignoreCase = true)) rawFileName else "custom_model.gguf"

        val tempModel = ModelInfo(
            name = targetFileName,
            fileName = targetFileName,
            filePath = "${repository.storageDirectoryPath}/$targetFileName",
            fileSizeBytes = 500L * 1024L * 1024L,
            quantization = "Custom",
            estimatedRamMb = 1000,
            downloadUrl = cleanUrl,
            description = "Custom remote GGUF model",
            isDownloaded = false,
            isDownloading = true,
            downloadProgress = 0
        )

        val currentList = _models.value.filter { it.fileName != targetFileName }
        _models.value = listOf(tempModel) + currentList

        startDownload(tempModel, onFinished)
    }

    fun cancelDownload() {
        repository.cancelDownload()
        downloadJob?.cancel()
        downloadJob = null
        currentlyDownloadingFileName?.let { fileName ->
            updateDownloadState(fileName, isDownloading = false, progress = 0)
        }
        currentlyDownloadingFileName = null
        loadModels()
    }

    fun deleteModel(model: ModelInfo, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val deleted = repository.deleteModel(model.fileName)
            loadModels()
            onFinished(deleted)
        }
    }

    fun importModel(uri: Uri, fileName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.importModelFromUri(uri, fileName)
            result.fold(
                onSuccess = {
                    loadModels()
                    onResult(true, null)
                },
                onFailure = { err ->
                    _isLoading.value = false
                    onResult(false, err.message)
                }
            )
        }
    }

    private fun updateDownloadState(fileName: String, isDownloading: Boolean, progress: Int) {
        _models.value = _models.value.map { item ->
            if (item.fileName == fileName) {
                item.copy(isDownloading = isDownloading, downloadProgress = progress)
            } else {
                item
            }
        }
    }
}
