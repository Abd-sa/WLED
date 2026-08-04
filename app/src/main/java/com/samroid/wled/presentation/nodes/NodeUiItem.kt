package com.samroid.wled.presentation.nodes

data class NodeUiItem(
    val id: Int,
    val name: String,
    val ip: String,
    val ledCount: Int,
    val online: Boolean
)