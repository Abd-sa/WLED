package com.samroid.wled.presentation.nodes

import com.samroid.wled.domain.model.NodeInfoData

data class NodeInfoUiState(
    val nodeId: Int = 0,
    val info: NodeInfoData? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)