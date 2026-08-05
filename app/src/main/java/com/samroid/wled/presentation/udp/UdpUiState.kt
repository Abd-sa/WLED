package com.samroid.wled.presentation.udp

data class UdpNodeMapUi(
    val nodeId: Int,
    val name: String,
    val ip: String = "",
    val enabled: Boolean = false,
    val startPixel: String = "0",
    val endPixel: String = "99",
    val processorId: Int = 0 // 0=Copy, 1=Average
)

data class UdpUiState(
    val streamEnabled: Boolean = false,
    val nodes: List<UdpNodeMapUi> = emptyList(),
    val bluetoothConnected: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null
)