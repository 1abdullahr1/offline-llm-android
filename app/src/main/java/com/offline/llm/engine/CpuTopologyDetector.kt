package com.offline.llm.engine

import java.io.File

data class CpuTopology(
    val totalCores: Int,
    val performanceCores: Int,
    val efficiencyCores: Int,
    val recommendedInferenceThreads: Int
)

object CpuTopologyDetector {

    fun detect(): CpuTopology {
        val totalCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        var perfCount = 0
        var effCount = 0

        try {
            val cpuFrequencies = mutableListOf<Long>()
            for (i in 0 until totalCores) {
                val maxFreqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (maxFreqFile.exists() && maxFreqFile.canRead()) {
                    val freq = maxFreqFile.readText().trim().toLongOrNull() ?: 0L
                    cpuFrequencies.add(freq)
                }
            }

            if (cpuFrequencies.isNotEmpty()) {
                val medianFreq = cpuFrequencies.sorted()[cpuFrequencies.size / 2]
                for (freq in cpuFrequencies) {
                    if (freq >= medianFreq && freq > 0) {
                        perfCount++
                    } else {
                        effCount++
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback when sysfs is restricted by SELinux
        }

        if (perfCount == 0) {
            perfCount = when {
                totalCores >= 8 -> 4
                totalCores >= 6 -> 3
                totalCores >= 4 -> 2
                else -> totalCores
            }
            effCount = (totalCores - perfCount).coerceAtLeast(0)
        }

        // We target performance cores only to prevent lockstep BLAS slowdown on slow LITTLE cores
        val recommended = perfCount.coerceIn(1, 4)

        return CpuTopology(
            totalCores = totalCores,
            performanceCores = perfCount,
            efficiencyCores = effCount,
            recommendedInferenceThreads = recommended
        )
    }
}
