package com.samroid.wled.data.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bt_device")
data class BtDeviceEntity(
    @PrimaryKey val id: Int = 1,
    val address: String,
    val name: String
)