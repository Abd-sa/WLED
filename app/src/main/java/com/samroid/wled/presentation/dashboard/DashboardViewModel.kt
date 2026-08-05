package com.samroid.wled.presentation.dashboard


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.repository.LocalSettingsRepository
import com.samroid.wled.data.repository.WifiStatusRepository
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.domain.usecase.AutoConnectBluetoothUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
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
    private val _autoConnectAttempted = AtomicBoolean(false)
    object AutoConnectGate {
        @Volatile var attempted = false
    }
    init {
        observeConnection()
        observeNodes()
        tryAutoConnect()
        observeWifi()
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


    private fun tryAutoConnect() {
        if (AutoConnectGate.attempted) return
        AutoConnectGate.attempted = true

        viewModelScope.launch {
            if (transport.transportConnectionState.value == TransportConnectionState.CONNECTED) return@launch

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
        viewModelScope.launch {
            localSettings.getNetworkConfig()?.let { cfg ->
                if (cfg.ssid.isNotBlank()) {
                    wifiStatus.setConfig(cfg.ssid, cfg.baseIp1, cfg.baseIp2, cfg.baseIp3)
                    // connected را false بگذار مگر اینکه از فریمور وضعیت واقعی بگیری
                }
            }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        bluetoothState = state,
                        bluetoothName = if (state == TransportConnectionState.CONNECTED) {
                            "HC-05 / Master"
                        } else {
                            "—"
                        }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingNodes = true, message = null) }
            val ok = transport.nodeListCmd()
            if (!ok) {
                _uiState.update {
                    it.copy(
                        isRefreshingNodes = false,
                        message = "خطا در دریافت لیست نودها"
                    )
                }
            }
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
}