package com.samroid.wled.data.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_config")
data class NetworkConfigEntity(
    @PrimaryKey val id: Int = 1,
    val ssid: String = "",
    val password: String = "",
    val baseIp1: Int = 192,
    val baseIp2: Int = 168,
    val baseIp3: Int = 1
)