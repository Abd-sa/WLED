package com.samroid.wled.presentation.ambilight


data class AmbilightUiState(
    val isRunning: Boolean = false,
    val captureSource: String = "Screen",      // Screen / ...
    val fps: Int = 30,
    val quality: String = "High",              // Low / Medium / High
    val smoothing: Float = 50f,                // 0..100
    val targetHost: String = "192.168.1.255",
    val targetPort: Int = 7777,
    val message: String? = null,
    val bluetoothConnected: Boolean = false
)

val FPS_OPTIONS = listOf(15, 20, 30, 60)
val QUALITY_OPTIONS = listOf("Low", "Medium", "High")
val SOURCE_OPTIONS = listOf("Screen")