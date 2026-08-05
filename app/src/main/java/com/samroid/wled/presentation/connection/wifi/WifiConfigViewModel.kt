package com.samroid.wled.presentation.connection.wifi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.R
import com.samroid.wled.data.repository.LocalSettingsRepository
import com.samroid.wled.data.repository.WifiStatusRepository
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.DeviceResponse
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiConfigViewModel @Inject constructor(
    private val transport: DeviceTransport,
    @ApplicationContext private val context: Context,
    private val localSettings: LocalSettingsRepository,
    private val wifiStatus: WifiStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiConfigUiState())
    val uiState: StateFlow<WifiConfigUiState> = _uiState.asStateFlow()

    private var awaitingWifiConnectAck = false
    private var wifiAckTimeoutJob: Job? = null

    init {
        // Restore form from Room
        viewModelScope.launch {
            localSettings.getNetworkConfig()?.let { cfg ->
                _uiState.update {
                    it.copy(
                        ssid = cfg.ssid,
                        password = cfg.password,
                        baseIp1 = cfg.baseIp1.toString(),
                        baseIp2 = cfg.baseIp2.toString(),
                        baseIp3 = cfg.baseIp3.toString(),
                        configSaved = cfg.ssid.isNotBlank()
                    )
                }
            }
        }

        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                _uiState.update {
                    it.copy(bluetoothConnected = state == TransportConnectionState.CONNECTED)
                }
            }
        }

        // Shared status → sheet + dashboard stay in sync
        viewModelScope.launch {
            wifiStatus.status.collect { wifi ->
                _uiState.update {
                    it.copy(
                        wifiConnected = wifi.connected,
                        configSaved = wifi.configSaved || it.configSaved
                    )
                }
            }
        }

        viewModelScope.launch {
            transport.lastDeviceResponse.collect { response ->
                if (!awaitingWifiConnectAck || response == null) return@collect
                when (response) {
                    is DeviceResponse.Ack -> onWifiConnectAck()
                    is DeviceResponse.Nack -> onWifiConnectNack(response.errorMessage())
                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            transport.lastResponse.collect { msg ->
                if (msg == null) return@collect
                if (awaitingWifiConnectAck) {
                    val m = msg.uppercase()
                    when {
                        m.contains("NACK") -> onWifiConnectNack(msg)
                        m.contains("ACK") -> onWifiConnectAck()
                        else -> _uiState.update { it.copy(lastMessage = msg) }
                    }
                } else {
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

    /** NETWORK_CONFIG (0x12) */
    fun sendNetworkConfig() {
        val state = _uiState.value
        if (!state.bluetoothConnected) {
            _uiState.update { it.copy(lastMessage = context.getString(R.string.please_connect_bluetooth)) }
            return
        }
        if (state.ssid.isBlank()) {
            _uiState.update { it.copy(lastMessage = context.getString(R.string.ssid_is_empty)) }
            return
        }
        val b1 = state.baseIp1.toIntOrNull()
        val b2 = state.baseIp2.toIntOrNull()
        val b3 = state.baseIp3.toIntOrNull()
        if (b1 == null || b2 == null || b3 == null ||
            b1 !in 0..255 || b2 !in 0..255 || b3 !in 0..255
        ) {
            _uiState.update { it.copy(lastMessage = context.getString(R.string.invalid_base_ip)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingConfig = true, lastMessage = null) }
            val baseIp = byteArrayOf(b1.toByte(), b2.toByte(), b3.toByte())
            val ok = transport.networkConfig(state.ssid.trim(), state.password, baseIp)
            if (ok) {
                localSettings.saveNetworkConfig(
                    ssid = state.ssid.trim(),
                    password = state.password,
                    b1 = b1, b2 = b2, b3 = b3
                )
                wifiStatus.setConfig(state.ssid.trim(), b1, b2, b3)
                wifiStatus.setConnected(false)
            }
            _uiState.update {
                it.copy(
                    isSendingConfig = false,
                    configSent = ok,
                    configSaved = ok || it.configSaved,
                    wifiConnected = false,
                    lastMessage = if (ok) {
                        context.getString(R.string.network_config_sent)
                    } else {
                        context.getString(R.string.error_in_sending)
                    }
                )
            }
        }
    }

    /** WIFI_CONNECT (0x11) — connected only after ACK */
    fun connectWifi() {
        val state = _uiState.value
        if (!state.bluetoothConnected) {
            _uiState.update { it.copy(lastMessage = context.getString(R.string.please_connect_bluetooth)) }
            return
        }

        viewModelScope.launch {
            wifiAckTimeoutJob?.cancel()
            awaitingWifiConnectAck = true
            wifiStatus.setConnected(false)
            _uiState.update {
                it.copy(isConnectingWifi = true, wifiConnected = false, lastMessage = null)
            }

            val sent = transport.wifiConnect()
            if (!sent) {
                awaitingWifiConnectAck = false
                _uiState.update {
                    it.copy(
                        isConnectingWifi = false,
                        lastMessage = context.getString(R.string.error_in_wifi_connect)
                    )
                }
                return@launch
            }

            wifiAckTimeoutJob = viewModelScope.launch {
                delay(4_000)
                if (awaitingWifiConnectAck) {
                    awaitingWifiConnectAck = false
                    _uiState.update {
                        it.copy(
                            isConnectingWifi = false,
                            wifiConnected = false,
                            lastMessage = context.getString(R.string.wifi_connect_ack_timeout)
                        )
                    }
                }
            }
        }
    }

    private fun onWifiConnectAck() {
        if (!awaitingWifiConnectAck) return
        awaitingWifiConnectAck = false
        wifiAckTimeoutJob?.cancel()
        wifiStatus.setConnected(true)
        _uiState.update {
            it.copy(
                isConnectingWifi = false,
                wifiConnected = true,
                lastMessage = context.getString(R.string.wifi_master_connected_ack)
            )
        }
    }

    private fun onWifiConnectNack(detail: String) {
        if (!awaitingWifiConnectAck) return
        awaitingWifiConnectAck = false
        wifiAckTimeoutJob?.cancel()
        wifiStatus.setConnected(false)
        _uiState.update {
            it.copy(
                isConnectingWifi = false,
                wifiConnected = false,
                lastMessage = context.getString(R.string.wifi_connect_nack, detail)
            )
        }
    }

    override fun onCleared() {
        wifiAckTimeoutJob?.cancel()
        awaitingWifiConnectAck = false
        super.onCleared()
    }
}