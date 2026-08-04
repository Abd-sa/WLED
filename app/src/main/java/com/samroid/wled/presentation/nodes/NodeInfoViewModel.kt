package com.samroid.wled.presentation.nodes


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.transport.DeviceTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class NodeInfoViewModel @Inject constructor(
    private val transport: DeviceTransport,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val nodeId: Int = savedStateHandle.get<Int>("nodeId") ?: 0

    private val _uiState = MutableStateFlow(NodeInfoUiState(nodeId = nodeId))
    val uiState: StateFlow<NodeInfoUiState> = _uiState.asStateFlow()

    init {
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
            _uiState.update { it.copy(isLoading = true, message = null) }
            val ok = transport.nodeInfoCmd(nodeId)
            if (!ok) {
                _uiState.update {
                    it.copy(isLoading = false, message = "خطا در دریافت اطلاعات نود")
                }
            }
        }
    }
}