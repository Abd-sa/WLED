package com.samroid.wled.presentation.connection.bluetooth


import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.domain.model.TransportDevice

data class BluetoothConnectionUiState(
    val connectionState: TransportConnectionState = TransportConnectionState.DISCONNECTED,
    val devices: List<TransportDevice> = emptyList(),
    val isScanning: Boolean = false,
    val lastResponse: String? = null,
    val logLines: List<String> = emptyList(),
    val connectingDeviceId: String? = null,
    val showEnableLocationPrompt: Boolean = false
)