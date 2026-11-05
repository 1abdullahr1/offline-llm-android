package com.offline.llm.ui.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.offline.llm.databinding.FragmentDiagnosticsBinding
import com.offline.llm.ui.chat.ChatViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DiagnosticsFragment : Fragment() {

    private var _binding: FragmentDiagnosticsBinding? = null
    private val binding get() = _binding!!

    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()
    private val chatViewModel: ChatViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiagnosticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRefreshDiagnostics.setOnClickListener {
            diagnosticsViewModel.refreshTelemetry()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            diagnosticsViewModel.memoryTelemetry.collectLatest { mem ->
                binding.tvRamUsage.text = String.format(
                    "Allocated JVM: %d MB / %d MB | System RAM: %d MB free of %d MB",
                    mem.allocatedHeapMb,
                    mem.maxHeapMb,
                    mem.availableSystemRamMb,
                    mem.totalSystemRamMb
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            diagnosticsViewModel.cpuTopology.collectLatest { topo ->
                binding.tvDiagCpuTopology.text = String.format(
                    "CPU Cores: %d total (%d Performance / %d Efficiency)",
                    topo.totalCores,
                    topo.performanceCores,
                    topo.efficiencyCores
                )
                binding.tvDiagCoreAffinity.text = String.format(
                    "Core Affinity: Thread pool restricted to %d Performance Cores",
                    topo.recommendedInferenceThreads
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            chatViewModel.thermalController.thermalLevel.collectLatest { level ->
                binding.tvDiagThermalStatus.text = "Thermal State: ${level.name} (Dynamic Governor Active)"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
