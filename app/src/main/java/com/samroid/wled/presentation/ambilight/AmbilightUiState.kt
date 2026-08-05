package com.samroid.wled.presentation.ambilight

data class AmbientTargetUi(
    val nodeId: Int,
    val name: String,
    val ip: String,
    val online: Boolean,
    val selected: Boolean = true,
    val startPixel: Int = 0,
    val endPixel: Int = 99,
    val processorId: Int = 0, // 0 = Copy, 1 = Average
    val ledCount: Int = 0
)

data class AmbilightUiState(
    val isRunning: Boolean = false,
    val isPreparing: Boolean = false,
    val bluetoothConnected: Boolean = false,
    val needsProjection: Boolean = false,

    val protocol: String = "DDP", // DDP | UDP_RAW
    val colorOrder: String = "GRB",
    val fps: Int = 30,
    val quality: String = "Medium", // Low | Medium | High | Ultra
    val smoothingEnabled: Boolean = true,
    val smoothingPercent: Float = 50f,
    val averageColor: Boolean = false,

    val targets: List<AmbientTargetUi> = emptyList(),
    val isLoadingTargets: Boolean = false,
    val message: String? = null
)

val FPS_OPTIONS = listOf(15, 20, 30, 60)
val QUALITY_OPTIONS = listOf("Low", "Medium", "High", "Ultra")
val PROTOCOL_OPTIONS = listOf("DDP", "UDP_RAW")
val COLOR_ORDER_OPTIONS = listOf("RGB", "RBG", "GRB", "GBR", "BRG", "BGR")

fun qualityToPx(quality: String): Int = when (quality) {
    "Low" -> 64
    "Medium" -> 128
    "High" -> 256
    "Ultra" -> 512
    else -> 128
}

fun protocolDefaultPort(protocol: String): Int =
    if (protocol == "UDP_RAW") 19446 else 4048