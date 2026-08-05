package com.samroid.wled.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samroid.wled.data.local.entity.BtDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BtDeviceDao {

    @Query("SELECT * FROM bt_device WHERE id = 1 LIMIT 1")
    fun observe(): Flow<BtDeviceEntity?>

    @Query("SELECT * FROM bt_device WHERE id = 1 LIMIT 1")
    suspend fun get(): BtDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(device: BtDeviceEntity)

    @Query("DELETE FROM bt_device")
    suspend fun clear()
}