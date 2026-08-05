package com.samroid.wled.presentation.dashboard


import com.samroid.wled.domain.model.TransportConnectionState

data class DashboardUiState(
    val bluetoothState: TransportConnectionState = TransportConnectionState.DISCONNECTED,
    val bluetoothName: String = "—",
    val wifiConnected: Boolean = false,
    val wifiSsid: String = "—",
    val wifiIp: String = "—",
    val totalNodes: Int = 0,
    val onlineNodes: Int = 0,
    val offlineNodes: Int = 0,
    val ambientEnabled: Boolean = false,
    val ambientEndpoint: String = "192.168.1.255:7777",
    val isRefreshingNodes: Boolean = false,
    val message: String? = null,
    val isAutoConnecting: Boolean = false
)