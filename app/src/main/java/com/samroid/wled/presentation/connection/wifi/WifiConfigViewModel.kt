package com.samroid.wled.presentation.connection.wifi


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiConfigViewModel @Inject constructor(
    private val transport: DeviceTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiConfigUiState())
    val uiState: StateFlow<WifiConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                _uiState.update {
                    it.copy(bluetoothConnected = state == TransportConnectionState.CONNECTED)
                }
            }
        }
        viewModelScope.launch {
            transport.lastResponse.collect { msg ->
                if (msg != null) {
                    _uiState.update { it.copy(lastMessage = msg) }
                }
            }
        }
    }

    fun onSsidChange(value: String) {
        _uiState.update { it.copy(ssid = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onBaseIp1Change(value: String) {
        _uiState.update { it.copy(baseIp1 = value.filter { c -> c.isDigit() }.take(3)) }
    }

    fun onBaseIp2Change(value: String) {
        _uiState.update { it.copy(baseIp2 = value.filter { c -> c.isDigit() }.take(3)) }
    }

    fun onBaseIp3Change(value: String) {
        _uiState.update { it.copy(baseIp3 = value.filter { c -> c.isDigit() }.take(3)) }
    }

    fun sendNetworkConfig() {
        val state = _uiState.value
        if (!state.bluetoothConnected) {
            _uiState.update { it.copy(lastMessage = "Please, Connect Bluetooth") }
            return
        }
        if (state.ssid.isBlank()) {
            _uiState.update { it.copy(lastMessage = "SSID Is Empty") }
            return
        }

        val b1 = state.baseIp1.toIntOrNull()
        val b2 = state.baseIp2.toIntOrNull()
        val b3 = state.baseIp3.toIntOrNull()
        if (b1 == null || b2 == null || b3 == null ||
            b1 !in 0..255 || b2 !in 0..255 || b3 !in 0..255
        ) {
            _uiState.update { it.copy(lastMessage = "Invalid Base IP") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingConfig = true, lastMessage = null) }
            val baseIp = byteArrayOf(b1.toByte(), b2.toByte(), b3.toByte())
            val ok = transport.networkConfig(state.ssid.trim(), state.password, baseIp)
            _uiState.update {
                it.copy(
                    isSendingConfig = false,
                    configSent = ok,
                    lastMessage = if (ok) "NETWORK_CONFIG sent" else "Error in sending"
                )
            }
        }
    }

    fun connectWifi() {
        val state = _uiState.value
        if (!state.bluetoothConnected) {
            _uiState.update { it.copy(lastMessage = "Please, Connect Bluetooth") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isConnectingWifi = true, lastMessage = null) }
            val ok = transport.wifiConnect()
            _uiState.update {
                it.copy(
                    isConnectingWifi = false,
                    wifiConnected = ok, // با Fake بعد از ACK true می‌شود؛ واقعی بعداً از پاسخ دقیق‌تر
                    lastMessage = if (ok) "WIFI_CONNECT sent" else "Error in WIFI_CONNECT"
                )
            }
        }
    }
}