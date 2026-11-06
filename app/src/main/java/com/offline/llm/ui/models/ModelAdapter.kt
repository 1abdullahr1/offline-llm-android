package com.offline.llm.ui.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.offline.llm.data.model.ModelInfo
import com.offline.llm.databinding.ItemModelCardBinding

class ModelAdapter(
    private val onSelectModel: (ModelInfo) -> Unit,
    private val onDownloadModel: (ModelInfo) -> Unit,
    private val onCancelDownload: (ModelInfo) -> Unit,
    private val onDeleteModel: (ModelInfo) -> Unit
) : ListAdapter<ModelInfo, ModelAdapter.ModelViewHolder>(ModelDiffCallback()) {

    private var activeModelPath: String? = null

    fun setActiveModelPath(path: String?) {
        activeModelPath = path
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val binding = ItemModelCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        val model = getItem(position)
        val isActive = model.filePath == activeModelPath
        holder.bind(model, isActive, onSelectModel, onDownloadModel, onCancelDownload, onDeleteModel)
    }

    class ModelViewHolder(private val binding: ItemModelCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            model: ModelInfo,
            isActive: Boolean,
            onSelectModel: (ModelInfo) -> Unit,
            onDownloadModel: (ModelInfo) -> Unit,
            onCancelDownload: (ModelInfo) -> Unit,
            onDeleteModel: (ModelInfo) -> Unit
        ) {
            binding.tvModelName.text = model.name
            binding.tvModelDescription.text = model.description
            binding.tvModelSize.text = "Size: ${model.formattedSize}"
            binding.tvModelQuant.text = model.quantization
            binding.tvEstimatedRam.text = "Estimated RAM: ~${model.estimatedRamMb} MB"

            if (model.isDownloading) {
                binding.layoutDownloadProgress.visibility = View.VISIBLE
                binding.layoutActions.visibility = View.GONE
                binding.progressBarDownload.progress = model.downloadProgress
                val downloadedMb = (model.downloadProgress.toLong() * model.fileSizeBytes / 100) / (1024 * 1024)
                binding.tvDownloadStatus.text = "Downloading: ${model.downloadProgress}% ($downloadedMb MB / ${model.formattedSize})"
                binding.btnCancelDownload.setOnClickListener {
                    onCancelDownload(model)
                }
            } else {
                binding.layoutDownloadProgress.visibility = View.GONE
                binding.layoutActions.visibility = View.VISIBLE

                if (!model.isDownloaded) {
                    binding.btnDownloadModel.visibility = View.VISIBLE
                    binding.btnDownloadModel.text = "Download OTA (${model.formattedSize})"
                    binding.btnDownloadModel.setOnClickListener {
                        onDownloadModel(model)
                    }
                    binding.btnSelectModel.visibility = View.GONE
                    binding.btnDeleteModel.visibility = View.GONE
                } else {
                    binding.btnDownloadModel.visibility = View.GONE
                    binding.btnSelectModel.visibility = View.VISIBLE

                    if (isActive) {
                        binding.btnSelectModel.text = "Active Model"
                        binding.btnSelectModel.isEnabled = false
                        binding.btnDeleteModel.visibility = View.GONE
                    } else {
                        binding.btnSelectModel.text = "Load Model"
                        binding.btnSelectModel.isEnabled = true
                        binding.btnSelectModel.setOnClickListener {
                            onSelectModel(model)
                        }
                        binding.btnDeleteModel.visibility = View.VISIBLE
                        binding.btnDeleteModel.setOnClickListener {
                            onDeleteModel(model)
                        }
                    }
                }
            }
        }
    }

    private class ModelDiffCallback : DiffUtil.ItemCallback<ModelInfo>() {
        override fun areItemsTheSame(oldItem: ModelInfo, newItem: ModelInfo): Boolean {
            return oldItem.fileName == newItem.fileName
        }

        override fun areContentsTheSame(oldItem: ModelInfo, newItem: ModelInfo): Boolean {
            return oldItem == newItem
        }
    }
}
