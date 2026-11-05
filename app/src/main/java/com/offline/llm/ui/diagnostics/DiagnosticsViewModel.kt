package com.offline.llm.ui.diagnostics

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.offline.llm.engine.CpuTopology
import com.offline.llm.engine.CpuTopologyDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MemoryTelemetry(
    val allocatedHeapMb: Long,
    val maxHeapMb: Long,
    val availableSystemRamMb: Long,
    val totalSystemRamMb: Long,
    val isLowMemory: Boolean
)

class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {

    private val _cpuTopology = MutableStateFlow(CpuTopologyDetector.detect())
    val cpuTopology: StateFlow<CpuTopology> = _cpuTopology.asStateFlow()

    private val _memoryTelemetry = MutableStateFlow(readMemoryTelemetry())
    val memoryTelemetry: StateFlow<MemoryTelemetry> = _memoryTelemetry.asStateFlow()

    fun refreshTelemetry() {
        _cpuTopology.value = CpuTopologyDetector.detect()
        _memoryTelemetry.value = readMemoryTelemetry()
    }

    private fun readMemoryTelemetry(): MemoryTelemetry {
        val runtime = Runtime.getRuntime()
        val allocatedHeap = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxHeap = runtime.maxMemory() / (1024 * 1024)

        val activityManager =
            getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val availSys = memInfo.availMem / (1024 * 1024)
        val totalSys = memInfo.totalMem / (1024 * 1024)

        return MemoryTelemetry(
            allocatedHeapMb = allocatedHeap,
            maxHeapMb = maxHeap,
            availableSystemRamMb = availSys,
            totalSystemRamMb = totalSys,
            isLowMemory = memInfo.lowMemory
        )
    }
}
