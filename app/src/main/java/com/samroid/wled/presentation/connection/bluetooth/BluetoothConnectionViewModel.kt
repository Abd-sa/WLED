package com.samroid.wled.presentation.connection.bluetooth


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.repository.LocalSettingsRepository
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothConnectionViewModel @Inject constructor(
    private val transport: DeviceTransport,
    @ApplicationContext private val context: Context,
    private val localSettings: LocalSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothConnectionUiState())
    val uiState: StateFlow<BluetoothConnectionUiState> = _uiState.asStateFlow()
    private var pendingSaveId: String? = null
    private var pendingSaveName: String? = null

    init {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
//                if (state == TransportConnectionState.CONNECTED) {
//                    val id = pendingSaveId
//                    val name = pendingSaveName
//                    if (!id.isNullOrBlank()) {
//                        localSettings.saveLastBtDevice(id, name ?: id)
//                        pendingSaveId = null
//                        pendingSaveName = null
//                    }
//                }
                    val id = pendingSaveId
                    val name = pendingSaveName
                    if (!id.isNullOrBlank()) {
                        localSettings.saveLastBtDevice(id, name ?: id)
                        pendingSaveId = null
                        pendingSaveName = null
                    }
            }
        }
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
                    } else null,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startScan() {
        if (
//            LocationHelper.isLocationRequiredForBleScan()
//            &&
            !LocationHelper.isLocationEnabled(context)
        ) {
            // UI: show dialog / update state — do NOT start scan yet
            _uiState.update {
                it.copy(showEnableLocationPrompt = true)
            }
            return
        }
        transport.startScan()
    }
    fun dismissLocationPrompt(){
        _uiState.update {
            it.copy(showEnableLocationPrompt = false)
        }
    }

    fun stopScan() = transport.stopScan()

    fun connect(deviceId: String) {
//        _uiState.update { it.copy(connectingDeviceId = deviceId) }
//        transport.connect(deviceId)
//        viewModelScope.launch {
//            transport.transportConnectionState.collect { state ->
//                if (state == TransportConnectionState.CONNECTED) {
//                    val name = _uiState.value.devices.find { it.id == deviceId }?.name ?: deviceId
//                    pendingSaveId = deviceId
//                    pendingSaveName = name
//
//                }
//            }
//        }
        val name = _uiState.value.devices.find { it.id == deviceId }?.name ?: deviceId
        pendingSaveId = deviceId
        pendingSaveName = name
        _uiState.update { it.copy(connectingDeviceId = deviceId) }
        transport.connect(deviceId)


    }

    fun disconnect() {
        viewModelScope.launch { localSettings.clearLastBtDevice() }
        transport.disconnect()
    }

    fun ping() {
        viewModelScope.launch { transport.ping() }
    }

    fun clearLog() = transport.clearLog()
}