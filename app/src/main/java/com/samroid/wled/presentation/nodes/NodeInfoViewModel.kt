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

    private val _uiState = MutableStateFlow(NodeInfoUiState(nodeId = nodeId))
    val uiState: StateFlow<NodeInfoUiState> = _uiState.asStateFlow()

    init {

        viewModelScope.launch {
            localNodes.observeNodeInfo(nodeId).collect { cached ->
                if (cached != null) {
                    _uiState.update {
                        it.copy(info = cached, isLoading = false)
                    }
                }
            }
        }
        viewModelScope.launch {
            transport.nodeInfo.collect { info ->
                if (info != null && info.nodeId == nodeId) {
                    _uiState.update {
                        it.copy(info = info, isLoading = false)
                    }
                }
            }
        }
        refresh()
    }

    fun refresh() {
  
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = _uiState.value.info == null) }
            val ok = transport.nodeInfoCmd(nodeId)
            if (!ok) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = if (it.info == null) context.getString(R.string.failed_retrive_data) else it.message
                    )
                }
            }
            // اگر پاسخ نآمد ولی کش داریم، لودینگ را قطع کن
            kotlinx.coroutines.delay(2500)
            _uiState.update { state ->
                if (state.isLoading && state.info != null) state.copy(isLoading = false)
                else if (state.isLoading) state.copy(isLoading = false, message = context.getString(
                    R.string.node_not_found
                ))
                else state
            }
    }
    }
}