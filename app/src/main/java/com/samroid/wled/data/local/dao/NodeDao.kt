package com.samroid.wled.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samroid.wled.data.local.entity.NodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {

    @Query("SELECT * FROM nodes ORDER BY nodeId ASC")
    fun observeAll(): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes ORDER BY nodeId ASC")
    suspend fun getAll(): List<NodeEntity>

    @Query("SELECT * FROM nodes WHERE nodeId = :id LIMIT 1")
    suspend fun getById(id: Int): NodeEntity?

    @Query("SELECT * FROM nodes WHERE nodeId = :id LIMIT 1")
    fun observeById(id: Int): Flow<NodeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(node: NodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(nodes: List<NodeEntity>)

    @Update
    suspend fun update(node: NodeEntity)

    @Query("UPDATE nodes SET online = :online WHERE nodeId = :id")
    suspend fun setOnline(id: Int, online: Boolean)

    @Query("UPDATE nodes SET online = 0")
    suspend fun markAllOffline()

    @Query("UPDATE nodes SET customName = :name WHERE nodeId = :id")
    suspend fun setCustomName(id: Int, name: String)

    @Query("DELETE FROM nodes WHERE nodeId = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM nodes")
    suspend fun clear()
}