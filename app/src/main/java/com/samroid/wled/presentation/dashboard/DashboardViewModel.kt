package com.samroid.wled.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.repository.LocalSettingsRepository
import com.samroid.wled.data.repository.WifiStatusRepository
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.DeviceResponse
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.domain.usecase.AutoConnectBluetoothUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transport: DeviceTransport,
    private val localSettings: LocalSettingsRepository,
    private val wifiStatus: WifiStatusRepository,
    private val autoConnectBluetooth: AutoConnectBluetoothUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private object AutoConnectGate {
        @Volatile var attempted = false
    }

    @Volatile private var postConnectHandled = false
    private var postConnectJob: Job? = null

    init {
        restoreWifiConfigUi()
        observeConnection()
        observeNodes()
        observeWifi()
        tryAutoConnect()
    }

    private fun restoreWifiConfigUi() {
        viewModelScope.launch {
            localSettings.getNetworkConfig()?.let { cfg ->
                if (cfg.ssid.isNotBlank()) {
                    wifiStatus.setConfig(cfg.ssid, cfg.baseIp1, cfg.baseIp2, cfg.baseIp3)
                }
            }
        }
    }

    private fun tryAutoConnect() {
        if (AutoConnectGate.attempted) return
        AutoConnectGate.attempted = true

        viewModelScope.launch {
            if (transport.transportConnectionState.value == TransportConnectionState.CONNECTED) {
                onBluetoothConnected()
                return@launch
            }
            val saved = localSettings.getLastBtDevice() ?: return@launch
            _uiState.update { it.copy(isAutoConnecting = true) }
            val ok = autoConnectBluetooth()
            _uiState.update {
                it.copy(
                    isAutoConnecting = false,
                    bluetoothName = if (ok) saved.name else it.bluetoothName
                )
            }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        bluetoothState = state,
                        bluetoothName = when (state) {
                            TransportConnectionState.CONNECTED ->
                                it.bluetoothName.takeIf { n -> n != "—" } ?: "Master"
                            TransportConnectionState.CONNECTING -> it.bluetoothName
                            else -> "—"
                        }
                    )
                }
                when (state) {
                    TransportConnectionState.CONNECTED -> onBluetoothConnected()
                    TransportConnectionState.DISCONNECTED,
                    TransportConnectionState.ERROR -> onBluetoothLost()
                    else -> Unit
                }
            }
        }
    }

    private fun onBluetoothConnected() {
        if (postConnectHandled) return
        postConnectHandled = true
        postConnectJob?.cancel()
        postConnectJob = viewModelScope.launch {
            delay(400)
            reapplyWifiFromStorage()
            refreshNodesInternal()
        }
    }

    private fun onBluetoothLost() {
        postConnectHandled = false
        postConnectJob?.cancel()
        postConnectJob = null
        wifiStatus.setConnected(false)
    }

    private suspend fun reapplyWifiFromStorage() {
        val cfg = localSettings.getNetworkConfig() ?: return
        if (cfg.ssid.isBlank()) return

        wifiStatus.setConfig(cfg.ssid, cfg.baseIp1, cfg.baseIp2, cfg.baseIp3)
        wifiStatus.setConnected(false)

        val baseIp = byteArrayOf(
            cfg.baseIp1.toByte(),
            cfg.baseIp2.toByte(),
            cfg.baseIp3.toByte()
        )
        val configOk = transport.networkConfig(cfg.ssid, cfg.password, baseIp)
        if (!configOk) {
            _uiState.update { it.copy(message = "NETWORK_CONFIG failed") }
            return
        }
        delay(500)
        val connectSent = transport.wifiConnect()
        if (!connectSent) {
            _uiState.update { it.copy(message = "WIFI_CONNECT send failed") }
            return
        }
        val response = withTimeoutOrNull(4_000) {
            transport.lastDeviceResponse.first { r ->
                r is DeviceResponse.Ack || r is DeviceResponse.Nack
            }
        }
        when (response) {
            is DeviceResponse.Ack -> wifiStatus.setConnected(true)
            is DeviceResponse.Nack -> {
                wifiStatus.setConnected(false)
                _uiState.update { it.copy(message = "WIFI_CONNECT NACK: ${response.errorMessage()}") }
            }
            null -> {
                wifiStatus.setConnected(false)
                _uiState.update { it.copy(message = "WIFI_CONNECT ACK timeout") }
            }
            else -> Unit
        }
    }

    private fun observeWifi() {
        viewModelScope.launch {
            wifiStatus.status.collect { wifi ->
                _uiState.update {
                    it.copy(
                        wifiSsid = wifi.ssid,
                        wifiIp = wifi.ipHint,
                        wifiConnected = wifi.connected
                    )
                }
            }
        }
    }

    private fun observeNodes() {
        viewModelScope.launch {
            transport.nodeList.collect { list ->
                val online = list.count { it.online }
                _uiState.update {
                    it.copy(
                        totalNodes = list.size,
                        onlineNodes = online,
                        offlineNodes = list.size - online,
                        isRefreshingNodes = false
                    )
                }
            }
        }
    }

    fun refreshNodes() {
        viewModelScope.launch { refreshNodesInternal() }
    }

    private suspend fun refreshNodesInternal() {
        if (transport.transportConnectionState.value != TransportConnectionState.CONNECTED) {
            _uiState.update {
                it.copy(isRefreshingNodes = false, message = "Bluetooth must be connected")
            }
            return
        }
        _uiState.update { it.copy(isRefreshingNodes = true, message = null) }
        val ok = transport.nodeListCmd()
        if (!ok) {
            _uiState.update {
                it.copy(isRefreshingNodes = false, message = "NODE_LIST failed")
            }
        }
        delay(5_000)
        if (_uiState.value.isRefreshingNodes) {
            _uiState.update { it.copy(isRefreshingNodes = false) }
        }
    }

    fun setAmbientEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(ambientEnabled = enabled) }
            transport.udpStreamEnable(enabled)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        postConnectJob?.cancel()
        super.onCleared()
    }
}