package com.samroid.wled.data.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val nodeId: Int,
    val deviceId: String = "",
    val ip: String = "",
    val rssi: Int = 0,
    val ledCount: Int = 0,
    val cctEnabled: Boolean = false,
    val udpEnabled: Boolean = false,
    val processorId: Int = 0,
    val startPixel: Int = 0,
    val endPixel: Int = 0,

    val online: Boolean = false,
    val customName: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)