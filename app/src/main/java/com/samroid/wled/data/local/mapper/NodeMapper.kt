package com.samroid.wled.data.local.mapper

import com.samroid.wled.data.local.entity.NodeEntity
import com.samroid.wled.domain.model.NodeInfoData
import com.samroid.wled.domain.model.NodeListItem

fun NodeEntity.toListItem() = NodeListItem(
    nodeName = customName,
    nodeId = nodeId,
    online = online
)

fun NodeEntity.toInfoData() = NodeInfoData(
    nodeId = nodeId,
    deviceId = deviceId,
    ip = ip,
    rssi = rssi,
    ledCount = ledCount,
    cctEnabled = cctEnabled,
    udpEnabled = udpEnabled,
    processorId = processorId,
    startPixel = startPixel,
    endPixel = endPixel,
    nodeName = customName
)

fun NodeInfoData.toEntity(online: Boolean = true, customName: String = "") = NodeEntity(
    nodeId = nodeId,
    deviceId = deviceId,
    ip = ip,
    rssi = rssi,
    ledCount = ledCount,
    cctEnabled = cctEnabled,
    udpEnabled = udpEnabled,
    processorId = processorId,
    startPixel = startPixel,
    endPixel = endPixel,
    online = online,
    customName = customName.ifBlank { "Node$nodeId" }
)

fun NodeListItem.toEntityStub() = NodeEntity(
    nodeId = nodeId,
    online = online,
    customName = nodeName.ifBlank { "Node$nodeId" }
)