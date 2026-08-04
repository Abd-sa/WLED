package com.samroid.wled.data.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.samroid.wled.R
import com.samroid.wled.data.transport.ble.BleManager
import com.samroid.wled.domain.model.ControllerInfo
import com.samroid.wled.domain.model.DeviceResponse
import com.samroid.wled.domain.model.NodeInfoData
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.domain.model.TransportDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * آداپتر: DeviceTransport ← BleManager
 * UI / ViewModel فقط با DeviceTransport کار می‌کنند.
 */
class BleDeviceTransport(
    context: Context
) : DeviceTransport {

    private val ble = BleManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** address → BluetoothDevice برای connect */
    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    // ---------- map StateFlowهای BleManager به DeviceTransport ----------

    private val _devices = MutableStateFlow<List<TransportDevice>>(emptyList())
    override val devices: StateFlow<List<TransportDevice>> = _devices.asStateFlow()

    private val _Transport_connectionState = MutableStateFlow(TransportConnectionState.DISCONNECTED)
    override val transportConnectionState: StateFlow<TransportConnectionState> = _Transport_connectionState.asStateFlow()

    override val isScanning: StateFlow<Boolean> = ble.isScanning
    override val lastResponse: StateFlow<String?> = ble.lastResponse
    override val log: StateFlow<List<String>> = ble.log
    override val lastDeviceResponse: StateFlow<DeviceResponse?> = ble.lastDeviceResponse
    override val nodeList: StateFlow<List<NodeListItem>> = ble.nodeList
    override val nodeInfo: StateFlow<NodeInfoData?> = ble.nodeInfo
    override val controllerInfo: StateFlow<ControllerInfo?> = ble.controllerInfo

    init {
        // همگام‌سازی لیست دستگاه‌ها
        scope.launch {
            ble.devices.collect { list ->
                deviceMap.clear()
                val mapped = list.map { dev ->
                    val id = dev.address
                    deviceMap[id] = dev
                    TransportDevice(
                        id = id,
                        name = safeName(dev),
                        address = dev.address
                    )
                }
                _devices.value = mapped
            }
        }

        // همگام‌سازی وضعیت اتصال
        scope.launch {
            ble.transportConnectionState.collect { state ->
                _Transport_connectionState.value = when (state) {
                    TransportConnectionState.DISCONNECTED -> TransportConnectionState.DISCONNECTED
                    TransportConnectionState.CONNECTING -> TransportConnectionState.CONNECTING
                    TransportConnectionState.CONNECTED -> TransportConnectionState.CONNECTED
                    TransportConnectionState.ERROR -> TransportConnectionState.ERROR
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeName(device: BluetoothDevice): String {
        return try {
            device.name?.takeIf { it.isNotBlank() } ?: "BLE Device"
        } catch (_: SecurityException) {
            "BLE Device"
        }
    }

    // ---------- Scan / Connect ----------

    override fun startScan() = ble.startScan()

    override fun stopScan() = ble.stopScan()

    override fun connect(deviceId: String) {
        val device = deviceMap[deviceId]
        if (device == null) {
            // اگر از قبل در map نبود، ممکن است address مستقیم داده شده باشد
            // در این حالت فقط لاگ — اسکن دوباره لازم است
            return
        }
        ble.connect(device)
    }

    override fun disconnect() = ble.disconnect()

    override fun clearLog() = ble.clearLog()

    override fun release() {
        ble.release()
    }

    // ---------- سیستم ----------

    override suspend fun ping(): Boolean = ble.sendPing()

    override suspend fun getInfo(): Boolean = ble.getInfo()

    // ---------- شبکه ----------

    override suspend fun networkConfig(
        ssid: String,
        password: String,
        baseIp: ByteArray
    ): Boolean = ble.networkConfig(ssid, password, baseIp)

    override suspend fun wifiConnect(): Boolean = ble.wifiConnect()

    // ---------- Provision ----------

    override suspend fun scanWled(): Boolean = ble.scanWled()

    override suspend fun provision(): Boolean = ble.provision()

    override suspend fun gpioValue(pin: Int): Boolean = ble.gpioValue(pin)

    override suspend fun gpioConfirm(): Boolean = ble.gpioConfirm()

    override suspend fun colorValue(order: Int): Boolean = ble.colorValue(order)

    override suspend fun colorConfirm(): Boolean = ble.colorConfirm()

    override suspend fun lengthValue(length: Int): Boolean = ble.lengthValue(length)

    override suspend fun lengthConfirm(): Boolean = ble.lengthConfirm()

    override suspend fun outputValue(pin1: Int, pin2: Int): Boolean =
        ble.outputValue(pin1, pin2)

    override suspend fun outputConfirm(): Boolean = ble.outputConfirm()

    override suspend fun storeValue(nodeId: Int): Boolean = ble.storeValue(nodeId)

    override suspend fun cancelProvision(): Boolean = ble.cancelProvision()

    // ---------- نود ----------

    override suspend fun nodeListCmd(): Boolean = ble.nodeList()

    override suspend fun nodeInfoCmd(nodeId: Int): Boolean = ble.nodeInfo(nodeId)

    // ---------- کنترل ----------

    override suspend fun setBrightness(nodeId: Int, output: Int, brightness: Int): Boolean =
        ble.setBrightness(nodeId, output, brightness)

    override suspend fun setColor(nodeId: Int, r: Int, g: Int, b: Int): Boolean =
        ble.setColor(nodeId, r, g, b)

    override suspend fun setCct(nodeId: Int, brightness: Int, cct: Int): Boolean =
        ble.setCct(nodeId, brightness, cct)

    override suspend fun onOff(nodeId: Int, output: Int, on: Boolean): Boolean =
        ble.onOff(nodeId, output, on)

    override suspend fun setEffect(nodeId: Int, segment: Int, effectId: Int): Boolean =
        ble.setEffect(nodeId, segment, effectId)

    override suspend fun setEffectSx(nodeId: Int, segment: Int, speed: Int): Boolean =
        ble.setEffectSx(nodeId, segment, speed)

    override suspend fun setEffectIx(nodeId: Int, segment: Int, intensity: Int): Boolean =
        ble.setEffectIx(nodeId, segment, intensity)

    override suspend fun setEffectPal(nodeId: Int, segment: Int, paletteId: Int): Boolean =
        ble.setEffectPal(nodeId, segment, paletteId)

    // ---------- پریست ----------

    override suspend fun presetSave(nodeId: Int, presetId: Int): Boolean =
        ble.presetSave(nodeId, presetId)

    override suspend fun presetLoad(nodeId: Int, presetId: Int): Boolean =
        ble.presetLoad(nodeId, presetId)

    // ---------- UDP ----------

    override suspend fun udpStart(nodeId: Int): Boolean = ble.udpStart(nodeId)

    override suspend fun udpStop(nodeId: Int): Boolean = ble.udpStop(nodeId)

    override suspend fun udpMapSet(
        nodeId: Int,
        processorId: Int,
        startPixel: Int,
        endPixel: Int
    ): Boolean = ble.udpMapSet(nodeId, processorId, startPixel, endPixel)

    override suspend fun udpStreamEnable(enable: Boolean): Boolean =
        ble.udpStreamEnable(enable)
}