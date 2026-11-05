package com.offline.llm.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.offline.llm.databinding.FragmentChatBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatAdapter = ChatAdapter()
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
            setHasFixedSize(true)
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessageInput.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessageInput.text.clear()
            }
        }

        binding.btnStop.setOnClickListener {
            viewModel.stopGeneration()
        }

        // Collect messages
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collectLatest { list ->
                chatAdapter.submitList(list) {
                    if (list.isNotEmpty()) {
                        binding.rvChatMessages.scrollToPosition(list.size - 1)
                        binding.tvEmptyState.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Collect active model name
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeModelName.collectLatest { modelName ->
                binding.tvActiveModelLabel.text = modelName
            }
        }

        // Collect live telemetry
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.liveMetrics.collectLatest { metrics ->
                binding.tvLiveMetrics.text = String.format(
                    "Speed: %.1f tok/s | TTFT: %d ms | Threads: %d (Zero-Copy mmap)",
                    metrics.tokensPerSecond,
                    metrics.timeToFirstTokenMs,
                    metrics.activeThreads
                )

                if (metrics.isGenerating) {
                    binding.btnSend.visibility = View.GONE
                    binding.btnStop.visibility = View.VISIBLE
                } else {
                    binding.btnSend.visibility = View.VISIBLE
                    binding.btnStop.visibility = View.GONE
                }
            }
        }

        // Collect thermal status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.thermalController.thermalLevel.collectLatest { level ->
                binding.tvThermalIndicator.text = "Thermal: ${level.name}"
                binding.tvThermalIndicator.setTextColor(
                    when (level) {
                        com.offline.llm.engine.ThermalLevel.NORMAL ->
                            resources.getColor(com.offline.llm.R.color.accent_green, null)
                        com.offline.llm.engine.ThermalLevel.MODERATE ->
                            resources.getColor(com.offline.llm.R.color.accent_amber, null)
                        com.offline.llm.engine.ThermalLevel.SEVERE,
                        com.offline.llm.engine.ThermalLevel.CRITICAL ->
                            resources.getColor(com.offline.llm.R.color.accent_rose, null)
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
