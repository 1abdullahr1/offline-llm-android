package com.offline.llm.engine

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThermalLevel {
    NORMAL,
    MODERATE,
    SEVERE,
    CRITICAL
}

class ThermalController(private val context: Context) {

    private val _thermalLevel = MutableStateFlow(ThermalLevel.NORMAL)
    val thermalLevel: StateFlow<ThermalLevel> = _thermalLevel.asStateFlow()

    private val powerManager: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                val level = when (status) {
                    PowerManager.THERMAL_STATUS_NONE,
                    PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.NORMAL
                    PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.MODERATE
                    PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.SEVERE
                    PowerManager.THERMAL_STATUS_CRITICAL,
                    PowerManager.THERMAL_STATUS_EMERGENCY,
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalLevel.CRITICAL
                    else -> ThermalLevel.NORMAL
                }
                _thermalLevel.value = level
            }
            thermalListener = listener
            try {
                powerManager.addThermalStatusListener(context.mainExecutor, listener)
            } catch (_: Exception) {
                // Ignore if permission or executor unavailable
            }
        }
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
            try {
                powerManager.removeThermalStatusListener(thermalListener!!)
            } catch (_: Exception) {
            }
            thermalListener = null
        }
    }

    fun computeThrottledThreads(baseThreads: Int): Int {
        return when (_thermalLevel.value) {
            ThermalLevel.NORMAL -> baseThreads
            ThermalLevel.MODERATE -> (baseThreads - 1).coerceAtLeast(2)
            ThermalLevel.SEVERE -> 2.coerceAtMost(baseThreads)
            ThermalLevel.CRITICAL -> 1
        }
    }

    fun computePacingDelayMs(): Long {
        return when (_thermalLevel.value) {
            ThermalLevel.NORMAL -> 0L
            ThermalLevel.MODERATE -> 2L
            ThermalLevel.SEVERE -> 8L
            ThermalLevel.CRITICAL -> 20L
        }
    }
}
