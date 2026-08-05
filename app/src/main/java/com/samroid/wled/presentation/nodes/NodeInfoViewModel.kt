package com.samroid.wled.presentation.nodes

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.R
import com.samroid.wled.data.repository.LocalNodeRepository
import com.samroid.wled.data.transport.DeviceTransport
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
class NodeInfoViewModel @Inject constructor(
    private val transport: DeviceTransport,
    private val localNodes: LocalNodeRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val nodeId: Int = savedStateHandle.get<Int>("nodeId") ?: 0

    private val _uiState = MutableStateFlow(
        NodeInfoUiState(nodeId = nodeId, isLoading = true)
    )
    val uiState: StateFlow<NodeInfoUiState> = _uiState.asStateFlow()

    private var timeoutJob: Job? = null

    init {
        // 1) Show cache immediately (no infinite loading)
        viewModelScope.launch {
            localNodes.observeNodeInfo(nodeId).collect { cached ->
                if (cached != null) {
                    _uiState.update {
                        it.copy(info = cached, isLoading = false)
                    }
                }
            }
        }

        // 2) Live NODE_INFO from device
        viewModelScope.launch {
            transport.nodeInfo.collect { info ->
                if (info != null && info.nodeId == nodeId) {
                    localNodes.cacheInfo(info, online = true)
                    timeoutJob?.cancel()
                    _uiState.update {
                        it.copy(info = info, isLoading = false, message = null)
                    }
                }
            }
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            timeoutJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = it.info == null,
                    message = null
                )
            }

            val ok = transport.nodeInfoCmd(nodeId)
            if (!ok) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = if (it.info == null) {
                            context.getString(R.string.failed_retrieve_data)
                        } else it.message
                    )
                }
                return@launch
            }

            timeoutJob = viewModelScope.launch {
                delay(3_000)
                _uiState.update { state ->
                    when {
                        !state.isLoading -> state
                        state.info != null -> state.copy(isLoading = false)
                        else -> state.copy(
                            isLoading = false,
                            message = context.getString(R.string.node_info_timeout)
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        timeoutJob?.cancel()
        super.onCleared()
    }
}