package com.samroid.wled.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.samroid.wled.data.local.dao.BtDeviceDao
import com.samroid.wled.data.local.dao.NetworkConfigDao
import com.samroid.wled.data.local.dao.NodeDao
import com.samroid.wled.data.local.entity.BtDeviceEntity
import com.samroid.wled.data.local.entity.NetworkConfigEntity
import com.samroid.wled.data.local.entity.NodeEntity

@Database(
    entities = [
        NodeEntity::class,
        NetworkConfigEntity::class,
        BtDeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WledDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao
    abstract fun networkConfigDao(): NetworkConfigDao
    abstract fun btDeviceDao(): BtDeviceDao
}