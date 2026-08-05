package com.samroid.wled.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class WifiUiStatus(
    val ssid: String = "—",
    val ipHint: String = "—",      // مثلاً 192.168.1.x
    val configSaved: Boolean = false,
    val connected: Boolean = false
)

@Singleton
class WifiStatusRepository @Inject constructor() {

    private val _status = MutableStateFlow(WifiUiStatus())
    val status: StateFlow<WifiUiStatus> = _status.asStateFlow()

    fun setConfig(ssid: String, b1: Int, b2: Int, b3: Int) {
        _status.update {
            it.copy(
                ssid = ssid.ifBlank { "—" },
                ipHint = "$b1.$b2.$b3.x",
                configSaved = true
            )
        }
    }

    fun setConnected(connected: Boolean) {
        _status.update { it.copy(connected = connected) }
    }

    fun clear() {
        _status.value = WifiUiStatus()
    }
}