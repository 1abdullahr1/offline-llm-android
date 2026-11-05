package com.offline.llm.ui.models

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.offline.llm.data.model.ModelInfo
import com.offline.llm.databinding.FragmentModelsBinding
import com.offline.llm.engine.CpuTopologyDetector
import com.offline.llm.engine.ModelConfig
import com.offline.llm.engine.PromptTemplate
import com.offline.llm.ui.chat.ChatViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ModelsFragment : Fragment() {

    private var _binding: FragmentModelsBinding? = null
    private val binding get() = _binding!!

    private val modelsViewModel: ModelsViewModel by viewModels()
    private val chatViewModel: ChatViewModel by activityViewModels()

    private lateinit var modelAdapter: ModelAdapter

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryFileName(uri) ?: "imported_model.gguf"
            modelsViewModel.importModel(uri, fileName) { success, errorMsg ->
                if (success) {
                    Toast.makeText(requireContext(), "Model imported: $fileName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Import failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvStoragePath.text = "Path: ${modelsViewModel.storageDirectoryPath}"

        modelAdapter = ModelAdapter { selectedModel ->
            loadSelectedModel(selectedModel)
        }

        binding.rvModels.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = modelAdapter
            setHasFixedSize(true)
        }

        binding.btnImportModel.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            modelsViewModel.models.collectLatest { list ->
                modelAdapter.submitList(list)
                if (list.isNotEmpty()) {
                    binding.tvEmptyModels.visibility = View.GONE
                } else {
                    binding.tvEmptyModels.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            chatViewModel.activeModelName.collectLatest { name ->
                val current = modelsViewModel.models.value.find { it.name == name }
                modelAdapter.setActiveModelPath(current?.filePath)
            }
        }
    }

    private fun loadSelectedModel(model: ModelInfo) {
        viewLifecycleOwner.lifecycleScope.launch {
            val topology = CpuTopologyDetector.detect()
            val template = when {
                model.name.contains("llama", ignoreCase = true) -> PromptTemplate.LLAMA_3
                model.name.contains("gemma", ignoreCase = true) -> PromptTemplate.GEMMA_2
                else -> PromptTemplate.CHAT_ML
            }

            val config = ModelConfig(
                modelPath = model.filePath,
                modelName = model.name,
                threads = topology.recommendedInferenceThreads,
                template = template
            )

            val result = chatViewModel.engine.loadModel(config)
            if (result.isSuccess) {
                modelAdapter.setActiveModelPath(model.filePath)
                Toast.makeText(requireContext(), "Loaded ${model.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to load model: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
