package com.samroid.wled.data.repository

import com.samroid.wled.data.local.dao.BtDeviceDao
import com.samroid.wled.data.local.dao.NetworkConfigDao
import com.samroid.wled.data.local.entity.BtDeviceEntity
import com.samroid.wled.data.local.entity.NetworkConfigEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSettingsRepository @Inject constructor(
    private val networkConfigDao: NetworkConfigDao,
    private val btDeviceDao: BtDeviceDao
) {
    fun observeNetworkConfig(): Flow<NetworkConfigEntity?> = networkConfigDao.observe()
    suspend fun getNetworkConfig() = networkConfigDao.get()

    suspend fun saveNetworkConfig(
        ssid: String,
        password: String,
        b1: Int,
        b2: Int,
        b3: Int
    ) {
        networkConfigDao.save(
            NetworkConfigEntity(
                ssid = ssid,
                password = password,
                baseIp1 = b1,
                baseIp2 = b2,
                baseIp3 = b3
            )
        )
    }

    fun observeLastBtDevice(): Flow<BtDeviceEntity?> = btDeviceDao.observe()
    suspend fun getLastBtDevice() = btDeviceDao.get()

    suspend fun saveLastBtDevice(address: String, name: String) {
        btDeviceDao.save(BtDeviceEntity(address = address, name = name))
    }

    suspend fun clearLastBtDevice() = btDeviceDao.clear()
}