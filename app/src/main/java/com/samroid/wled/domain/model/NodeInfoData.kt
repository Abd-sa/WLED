package com.samroid.wled.domain.model

data class NodeInfoData(
    val nodeId: Int,
    val deviceId: String,
    val ip: String,
    val rssi: Int,
    val ledCount: Int,
    val cctEnabled: Boolean,
    val udpEnabled: Boolean,
    val processorId: Int,
    val startPixel: Int,
    val endPixel: Int
)