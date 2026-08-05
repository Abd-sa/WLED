package com.samroid.wled.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samroid.wled.data.local.entity.NetworkConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkConfigDao {

    @Query("SELECT * FROM network_config WHERE id = 1 LIMIT 1")
    fun observe(): Flow<NetworkConfigEntity?>

    @Query("SELECT * FROM network_config WHERE id = 1 LIMIT 1")
    suspend fun get(): NetworkConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: NetworkConfigEntity)
}