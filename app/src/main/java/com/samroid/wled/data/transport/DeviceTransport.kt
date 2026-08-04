package com.samroid.wled.data.transport

import com.samroid.wled.domain.model.ControllerInfo
import com.samroid.wled.domain.model.DeviceResponse
import com.samroid.wled.domain.model.NodeInfoData
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.domain.model.TransportDevice
import kotlinx.coroutines.flow.StateFlow

interface DeviceTransport {

    val transportConnectionState: StateFlow<TransportConnectionState>
    val devices: StateFlow<List<TransportDevice>>
    val isScanning: StateFlow<Boolean>
    val lastResponse: StateFlow<String?>
    val log: StateFlow<List<String>>

    val lastDeviceResponse: StateFlow<DeviceResponse?>
    val nodeList: StateFlow<List<NodeListItem>>
    val nodeInfo: StateFlow<NodeInfoData?>
    val controllerInfo: StateFlow<ControllerInfo?>

    fun startScan()
    fun stopScan()
    fun connect(deviceId: String)
    fun disconnect()
    fun clearLog()
    fun release()

    suspend fun ping(): Boolean
    suspend fun getInfo(): Boolean

    suspend fun networkConfig(ssid: String, password: String, baseIp: ByteArray): Boolean
    suspend fun wifiConnect(): Boolean

    suspend fun scanWled(): Boolean
    suspend fun provision(): Boolean
    suspend fun gpioValue(pin: Int): Boolean
    suspend fun gpioConfirm(): Boolean
    suspend fun colorValue(order: Int): Boolean
    suspend fun colorConfirm(): Boolean
    suspend fun lengthValue(length: Int): Boolean
    suspend fun lengthConfirm(): Boolean
    suspend fun outputValue(pin1: Int, pin2: Int): Boolean
    suspend fun outputConfirm(): Boolean
    suspend fun storeValue(nodeId: Int): Boolean
    suspend fun cancelProvision(): Boolean

    suspend fun nodeListCmd(): Boolean
    suspend fun nodeInfoCmd(nodeId: Int): Boolean

    suspend fun setBrightness(nodeId: Int, output: Int, brightness: Int): Boolean
    suspend fun setColor(nodeId: Int, r: Int, g: Int, b: Int): Boolean
    suspend fun setCct(nodeId: Int, brightness: Int, cct: Int): Boolean
    suspend fun onOff(nodeId: Int, output: Int, on: Boolean): Boolean
    suspend fun setEffect(nodeId: Int, segment: Int, effectId: Int): Boolean
    suspend fun setEffectSx(nodeId: Int, segment: Int, speed: Int): Boolean
    suspend fun setEffectIx(nodeId: Int, segment: Int, intensity: Int): Boolean
    suspend fun setEffectPal(nodeId: Int, segment: Int, paletteId: Int): Boolean

    suspend fun presetSave(nodeId: Int, presetId: Int): Boolean
    suspend fun presetLoad(nodeId: Int, presetId: Int): Boolean

    suspend fun udpStart(nodeId: Int): Boolean
    suspend fun udpStop(nodeId: Int): Boolean
    suspend fun udpMapSet(nodeId: Int, processorId: Int, startPixel: Int, endPixel: Int): Boolean
    suspend fun udpStreamEnable(enable: Boolean): Boolean
}