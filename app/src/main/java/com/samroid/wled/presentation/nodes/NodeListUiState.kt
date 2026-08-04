package com.samroid.wled.presentation.nodes

import com.samroid.wled.domain.model.NodeListItem

data class NodeListUiState(
    val bluetoothConnected: Boolean = false,
    val nodes: List<NodeListItem> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)