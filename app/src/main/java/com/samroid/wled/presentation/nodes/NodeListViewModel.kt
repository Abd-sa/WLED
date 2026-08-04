package com.samroid.wled.presentation.nodes


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class NodeListViewModel @Inject constructor(
    private val transport: DeviceTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeListUiState())
    val uiState: StateFlow<NodeListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                _uiState.update {
                    it.copy(bluetoothConnected = state == TransportConnectionState.CONNECTED)
                }
            }
        }
        viewModelScope.launch {
            transport.nodeList.collect { list ->
                _uiState.update {
                    it.copy(nodes = list, isLoading = false)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_uiState.value.bluetoothConnected) {
                _uiState.update { it.copy(message = "Bluetooth is not connected") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, message = null) }
            val ok = transport.nodeListCmd()
            if (!ok) {
                _uiState.update {
                    it.copy(isLoading = false, message = "Error in detecting Nodes")
                }
            }
        }
    }
}