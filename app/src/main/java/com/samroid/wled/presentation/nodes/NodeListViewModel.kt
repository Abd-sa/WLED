package com.samroid.wled.presentation.nodes


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.R
import com.samroid.wled.data.repository.LocalNodeRepository
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    val nodes = localNodes.observeNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
        viewModelScope.launch {
            localNodes.observeNodes().collect { list ->
                _uiState.update { it.copy(nodes = list, isLoading = false) }
            }
        }
        viewModelScope.launch {
            transport.nodeList.collect { list ->
                localNodes.cacheList(list)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_uiState.value.bluetoothConnected) {
                _uiState.update { it.copy(message = context.getString(R.string.bluetooth_is_not_connected) ) }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, message = null) }
            val ok = transport.nodeListCmd()
            if (!ok) {
                _uiState.update {
                    it.copy(isLoading = false, message = context.getString(R.string.error_in_detecting_nodes))
                }
            }
        }
    }
}