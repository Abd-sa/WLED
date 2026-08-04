package com.samroid.wled.presentation.dashboard


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
class DashboardViewModel @Inject constructor(
    private val transport: DeviceTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeConnection()
        observeNodes()
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