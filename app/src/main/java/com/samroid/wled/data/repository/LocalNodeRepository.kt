package com.samroid.wled.data.repository

import com.samroid.wled.data.local.dao.NodeDao
import com.samroid.wled.data.local.entity.NodeEntity
import com.samroid.wled.data.local.mapper.toEntity
import com.samroid.wled.data.local.mapper.toEntityStub
import com.samroid.wled.data.local.mapper.toInfoData
import com.samroid.wled.data.local.mapper.toListItem
import com.samroid.wled.domain.model.NodeInfoData
import com.samroid.wled.domain.model.NodeListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNodeRepository @Inject constructor(
    private val nodeDao: NodeDao
) {
    fun observeNodes(): Flow<List<NodeListItem>> =
        nodeDao.observeAll().map { list -> list.map { it.toListItem() } }

    fun observeNodeInfo(nodeId: Int): Flow<NodeInfoData?> =
        nodeDao.observeById(nodeId).map { it?.toInfoData() }

    suspend fun cacheList(items: List<NodeListItem>) {
        val existing = nodeDao.getAll().associateBy { it.nodeId }
        val merged = items.map { item ->
            val old = existing[item.nodeId]
            old?.copy(online = item.online, updatedAt = System.currentTimeMillis())
                ?: item.toEntityStub()
        }
        // نودهایی که در لیست جدید نیستند آفلاین شوند
        nodeDao.markAllOffline()
        nodeDao.upsertAll(merged)
    }

    suspend fun cacheInfo(info: NodeInfoData, online: Boolean = true) {
        val old = nodeDao.getById(info.nodeId)
        nodeDao.upsert(
            info.toEntity(
                online = online,
                customName = old?.customName.orEmpty()
            )
        )
    }

    suspend fun setCustomName(nodeId: Int, name: String) {
        nodeDao.setCustomName(nodeId, name)
    }

    suspend fun delete(nodeId: Int) = nodeDao.delete(nodeId)
}