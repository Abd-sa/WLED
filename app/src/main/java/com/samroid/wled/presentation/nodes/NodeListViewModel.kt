package com.samroid.wled.presentation.nodes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.R
import com.samroid.wled.data.repository.LocalNodeRepository
import com.samroid.wled.data.transport.DeviceTransport
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
class NodeListViewModel @Inject constructor(
    private val transport: DeviceTransport,
    @ApplicationContext private val context: Context,
    private val localNodes: LocalNodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeListUiState())
    val uiState: StateFlow<NodeListUiState> = _uiState.asStateFlow()

    private var refreshTimeoutJob: Job? = null

    init {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                val connected = state == TransportConnectionState.CONNECTED
                _uiState.update { it.copy(bluetoothConnected = connected) }
                if (connected && _uiState.value.nodes.isEmpty()) {
                    refresh()
                }
            }
        }

        // Single source: Room cache (filled from transport)
        viewModelScope.launch {
            localNodes.observeNodes().collect { list ->
                _uiState.update {
                    it.copy(
                        nodes = list,
                        isLoading = false
                    )
                }
            }
        }

        // Live list from device → cache
        viewModelScope.launch {
            transport.nodeList.collect { list ->
                localNodes.cacheList(list)
                _uiState.update {
                    it.copy(nodes = list, isLoading = false)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_uiState.value.bluetoothConnected) {
                _uiState.update {
                    it.copy(message = context.getString(R.string.bluetooth_is_not_connected))
                }
                return@launch
            }

            refreshTimeoutJob?.cancel()
            _uiState.update { it.copy(isLoading = true, message = null) }

            val ok = transport.nodeListCmd()
            if (!ok) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = context.getString(R.string.error_in_detecting_nodes)
                    )
                }
                return@launch
            }

            // Stop loading if response is slow (cache still shown)
            refreshTimeoutJob = viewModelScope.launch {
                delay(4_000)
                _uiState.update { state ->
                    if (state.isLoading) state.copy(isLoading = false) else state
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        refreshTimeoutJob?.cancel()
        super.onCleared()
    }
}