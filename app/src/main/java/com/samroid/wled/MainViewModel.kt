package com.samroid.wled

import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.transport.BleDeviceTransport
import com.samroid.wled.data.transport.FakeDeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.data.transport.DeviceTransport
import kotlinx.coroutines.launch

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.samroid.wled.domain.model.DeviceResponse
import com.samroid.wled.domain.model.ControllerInfo
import com.samroid.wled.domain.model.NodeInfoData
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.domain.model.TransportDevice
import dagger.hilt.android.lifecycle.HiltViewModel
/**
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val btManager = Hc05Manager(application)

    val connectionState = btManager.connectionState
    val incomingPacket = btManager.incomingPacket

    fun connectToHc05(device: BluetoothDevice) {
        viewModelScope.launch {
            btManager.connect(device)
        }
    }

    fun sendNetworkConfig(ssid: String, password: String) {
        viewModelScope.launch {
            // مثال: 192.168.1.x
            val baseIp = byteArrayOf(192.toByte(), 168.toByte(), 1.toByte())
            btManager.networkConfig(ssid, password, baseIp)
        }
    }

    fun connectWifi() {
        viewModelScope.launch {
            btManager.wifiConnect()
        }
    }

    fun requestNodeList() {
        viewModelScope.launch {
            btManager.nodeList()
        }
    }

    override fun onCleared() {
        btManager.release()
        super.onCleared()
    }
}
 **/

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val useFake = true // روی امولاتور true بگذار

    val manager: DeviceTransport =
        if (useFake) FakeBleManager()
        else BleDeviceTransport(application)
    //private val manager = BleManager(application)

    val connectionState: StateFlow<ConnectionState> = manager.connectionState
    val devices: StateFlow<List<BluetoothDevice>> = manager.devices
    val isScanning: StateFlow<Boolean> = manager.isScanning
    val lastResponse: StateFlow<String?> = manager.lastResponse
    val log: StateFlow<List<String>> = manager.log

    //val bondingState = manager.bondingState

    fun isBluetoothEnabled() = manager.isBluetoothEnabled()

    fun startScan() = manager.startScan()
    fun stopScan() = manager.stopScan()
    fun pair(device: BluetoothDevice) {
        manager.connect(device)
    }

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            manager.connect(device)
        }
    }

    fun disconnect() = manager.disconnect()

    fun sendPing() {
        viewModelScope.launch {
            manager.sendPing()
        }
    }

    fun clearLog() = manager.clearLog()

    override fun onCleared() {
        manager.release()
        super.onCleared()
    }
}**/



@HiltViewModel
class MainViewModel @Inject constructor(
    private val transport: DeviceTransport
) : ViewModel() {

    private val useFake: Boolean = isEmulator() // یا دستی: true روی امولاتور

//    val transport: DeviceTransport =
//        if (useFake) FakeDeviceTransport()
//        else BleDeviceTransport(application.applicationContext)

    val transportConnectionState: StateFlow<TransportConnectionState> = transport.transportConnectionState
    val devices: StateFlow<List<TransportDevice>> = transport.devices
    val isScanning: StateFlow<Boolean> = transport.isScanning
    val lastResponse: StateFlow<String?> = transport.lastResponse
    val log: StateFlow<List<String>> = transport.log

    val lastDeviceResponse: StateFlow<DeviceResponse?> = transport.lastDeviceResponse
    val nodeList: StateFlow<List<NodeListItem>> = transport.nodeList
    val nodeInfo: StateFlow<NodeInfoData?> = transport.nodeInfo
    val controllerInfo: StateFlow<ControllerInfo?> = transport.controllerInfo

    // ---------- اتصال ----------

    fun startScan() = transport.startScan()

    fun stopScan() = transport.stopScan()

    /** deviceId = TransportDevice.id (معمولاً MAC address) */
    fun connect(deviceId: String) {
        transport.connect(deviceId)
    }

    fun connect(device: TransportDevice) {
        transport.connect(device.id)
    }

    fun disconnect() = transport.disconnect()

    fun clearLog() = transport.clearLog()

    // ---------- سیستم ----------

    fun sendPing() {
        viewModelScope.launch { transport.ping() }
    }

    fun getInfo() {
        viewModelScope.launch { transport.getInfo() }
    }

    // ---------- شبکه ----------

    fun networkConfig(ssid: String, password: String, baseIp: ByteArray) {
        viewModelScope.launch { transport.networkConfig(ssid, password, baseIp) }
    }

    fun wifiConnect() {
        viewModelScope.launch { transport.wifiConnect() }
    }

    // ---------- نود ----------

    fun requestNodeList() {
        viewModelScope.launch { transport.nodeListCmd() }
    }

    fun requestNodeInfo(nodeId: Int) {
        viewModelScope.launch { transport.nodeInfoCmd(nodeId) }
    }

    // ---------- کنترل (نمونه) ----------

    fun setColor(nodeId: Int, r: Int, g: Int, b: Int) {
        viewModelScope.launch { transport.setColor(nodeId, r, g, b) }
    }

    fun setBrightness(nodeId: Int, output: Int, brightness: Int) {
        viewModelScope.launch { transport.setBrightness(nodeId, output, brightness) }
    }

    fun onOff(nodeId: Int, output: Int, on: Boolean) {
        viewModelScope.launch { transport.onOff(nodeId, output, on) }
    }

    // ---------- Provision / UDP / Preset در صورت نیاز ----------

    fun provision() {
        viewModelScope.launch { transport.provision() }
    }

    fun storeValue(nodeId: Int) {
        viewModelScope.launch { transport.storeValue(nodeId) }
    }

    fun udpStreamEnable(enable: Boolean) {
        viewModelScope.launch { transport.udpStreamEnable(enable) }
    }

    fun presetSave(nodeId: Int, presetId: Int) {
        viewModelScope.launch { transport.presetSave(nodeId, presetId) }
    }

    fun presetLoad(nodeId: Int, presetId: Int) {
        viewModelScope.launch { transport.presetLoad(nodeId, presetId) }
    }

    override fun onCleared() {
        transport.release()
        super.onCleared()
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic", ignoreCase = true)
                || Build.MODEL.contains("Emulator", ignoreCase = true)
                || Build.MODEL.contains("google_sdk", ignoreCase = true)
                || Build.PRODUCT.contains("sdk", ignoreCase = true)
                || Build.MANUFACTURER.contains("Genymotion", ignoreCase = true)
    }
}