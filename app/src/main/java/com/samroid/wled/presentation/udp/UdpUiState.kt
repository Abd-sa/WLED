package com.samroid.wled.presentation.udp


data class UdpNodeMapUi(
    val nodeId: Int,
    val name: String,
    val enabled: Boolean = false,
    val startPixel: String = "0",
    val endPixel: String = "0",
    val processorId: Int = 0 // 0=Copy, 1=Average
)

data class UdpUiState(
    val streamEnabled: Boolean = false,
    val localIp: String = "192.168.1.100",
    val port: String = "7777",
    val nodes: List<UdpNodeMapUi> = emptyList(),
    val bluetoothConnected: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null
)