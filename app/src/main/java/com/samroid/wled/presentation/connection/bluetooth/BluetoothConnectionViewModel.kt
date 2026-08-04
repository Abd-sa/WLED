package com.samroid.wled.presentation.connection.bluetooth


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothConnectionViewModel @Inject constructor(
    private val transport: DeviceTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothConnectionUiState())
    val uiState: StateFlow<BluetoothConnectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                transport.transportConnectionState,
                transport.devices,
                transport.isScanning,
                transport.lastResponse,
                transport.log
            ) { conn, devices, scanning, last, log ->
                BluetoothConnectionUiState(
                    connectionState = conn,
                    devices = devices,
                    isScanning = scanning,
                    lastResponse = last,
                    logLines = log.takeLast(12),
                    connectingDeviceId = if (conn == TransportConnectionState.CONNECTING) {
                        _uiState.value.connectingDeviceId
                    } else null
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startScan() = transport.startScan()

    fun stopScan() = transport.stopScan()

    fun connect(deviceId: String) {
        _uiState.update { it.copy(connectingDeviceId = deviceId) }
        transport.connect(deviceId)
    }

    fun disconnect() = transport.disconnect()

    fun ping() {
        viewModelScope.launch { transport.ping() }
    }

    fun clearLog() = transport.clearLog()
}