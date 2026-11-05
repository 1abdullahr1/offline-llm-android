package com.offline.llm.ui.models

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offline.llm.data.model.ModelInfo
import com.offline.llm.data.repository.ModelRepository
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
}
