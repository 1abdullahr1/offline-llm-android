package com.offline.llm.ui.models

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.offline.llm.data.model.ModelInfo
import com.offline.llm.databinding.ItemModelCardBinding

class ModelAdapter(
    private val onSelectModel: (ModelInfo) -> Unit
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
        holder.bind(model, isActive, onSelectModel)
    }

    class ModelViewHolder(private val binding: ItemModelCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            model: ModelInfo,
            isActive: Boolean,
            onSelectModel: (ModelInfo) -> Unit
        ) {
            binding.tvModelName.text = model.name
            binding.tvModelSize.text = "Size: ${model.formattedSize}"
            binding.tvModelQuant.text = model.quantization
            binding.tvEstimatedRam.text = "Estimated RAM: ~${model.estimatedRamMb} MB"

            if (isActive) {
                binding.btnSelectModel.text = "Active Model"
                binding.btnSelectModel.isEnabled = false
            } else {
                binding.btnSelectModel.text = "Load Model"
                binding.btnSelectModel.isEnabled = true
                binding.btnSelectModel.setOnClickListener {
                    onSelectModel(model)
                }
            }
        }
    }

    private class ModelDiffCallback : DiffUtil.ItemCallback<ModelInfo>() {
        override fun areItemsTheSame(oldItem: ModelInfo, newItem: ModelInfo): Boolean {
            return oldItem.filePath == newItem.filePath
        }

        override fun areContentsTheSame(oldItem: ModelInfo, newItem: ModelInfo): Boolean {
            return oldItem == newItem
        }
    }
}
