package com.samroid.wled.presentation.udp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.R
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
class UdpViewModel @Inject constructor(
    private val transport: DeviceTransport,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(UdpUiState())
    val uiState: StateFlow<UdpUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                val connected = state == TransportConnectionState.CONNECTED
                _uiState.update { it.copy(bluetoothConnected = connected) }
                if (connected) refreshNodes()
            }
        }
        viewModelScope.launch {
            transport.nodeList.collect { list ->
                _uiState.update { current ->
                    val mapped = list.map { n ->
                        val old = current.nodes.find { it.nodeId == n.nodeId }
                        old?.copy(
                            name = n.nodeName.ifBlank { "Node${n.nodeId}" },
                        ) ?: UdpNodeMapUi(
                            nodeId = n.nodeId,
                            name = n.nodeName.ifBlank { "Node${n.nodeId}" },
                            endPixel = "99"
                        )
                    }
                    current.copy(nodes = mapped)
                }
                // Enrich IP / map from NODE_INFO
                if (list.isNotEmpty() && _uiState.value.bluetoothConnected) {
                    enrichFromNodeInfo(list.map { it.nodeId })
                }
            }
        }
        viewModelScope.launch {
            transport.lastResponse.collect { msg ->
                if (msg != null) _uiState.update { it.copy(message = msg) }
            }
        }
    }

    private suspend fun enrichFromNodeInfo(ids: List<Int>) {
        for (id in ids) {
            transport.nodeInfoCmd(id)
            val info = withTimeoutOrNull(2_000) {
                transport.nodeInfo.first { it != null && it.nodeId == id }
            } ?: continue
            _uiState.update { state ->
                state.copy(
                    nodes = state.nodes.map {
                        if (it.nodeId != id) it
                        else it.copy(
                            ip = info.ip,
                            startPixel = info.startPixel.toString(),
                            endPixel = info.endPixel.toString(),
                            processorId = info.processorId.coerceIn(0, 1),
                            enabled = info.udpEnabled
                        )
                    }
                )
            }
            delay(60)
        }
    }

    fun refreshNodes() {
        viewModelScope.launch {
            if (!_uiState.value.bluetoothConnected) {
                _uiState.update {
                    it.copy(message = context.getString(R.string.bluetooth_is_not_connected))
                }
                return@launch
            }
            transport.nodeListCmd()
        }
    }

    fun setStreamEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.udpStreamEnable(enabled)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    streamEnabled = if (ok) enabled else it.streamEnabled,
                    message = if (ok) {
                        if (enabled) context.getString(R.string.udp_stream_on)
                        else context.getString(R.string.udp_stream_off)
                    } else context.getString(R.string.udp_stream_failed)
                )
            }
        }
    }

    fun onStartPixelChange(nodeId: Int, value: String) {
        updateNode(nodeId) {
            it.copy(startPixel = value.filter { c -> c.isDigit() }.take(3))
        }
    }

    fun onEndPixelChange(nodeId: Int, value: String) {
        updateNode(nodeId) {
            it.copy(endPixel = value.filter { c -> c.isDigit() }.take(3))
        }
    }

    fun onProcessorChange(nodeId: Int, processorId: Int) {
        updateNode(nodeId) { it.copy(processorId = processorId.coerceIn(0, 1)) }
    }

    fun applyMapForNode(nodeId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = applyMap(nodeId)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = if (ok) {
                        context.getString(R.string.udp_map_set_ok, nodeId)
                    } else context.getString(R.string.udp_map_set_failed, nodeId)
                )
            }
        }
    }

    fun toggleNodeUdp(nodeId: Int, enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = if (enabled) {
                applyMap(nodeId) && transport.udpStart(nodeId)
            } else {
                transport.udpStop(nodeId)
            }
            if (ok) updateNode(nodeId) { it.copy(enabled = enabled) }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = when {
                        !ok -> context.getString(R.string.udp_node_failed, nodeId)
                        enabled -> context.getString(R.string.udp_node_started, nodeId)
                        else -> context.getString(R.string.udp_node_stopped, nodeId)
                    }
                )
            }
        }
    }

    private suspend fun applyMap(nodeId: Int): Boolean {
        val node = _uiState.value.nodes.find { it.nodeId == nodeId } ?: return false
        val start = node.startPixel.toIntOrNull() ?: return false
        val end = node.endPixel.toIntOrNull() ?: return false
        if (start !in 0..300 || end !in 0..300) return false
        return transport.udpMapSet(nodeId, node.processorId, start, end)
    }

    private fun updateNode(nodeId: Int, block: (UdpNodeMapUi) -> UdpNodeMapUi) {
        _uiState.update { state ->
            state.copy(nodes = state.nodes.map { if (it.nodeId == nodeId) block(it) else it })
        }
    }
}