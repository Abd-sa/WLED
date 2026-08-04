package com.samroid.wled.data.transport

import com.samroid.wled.domain.model.ControllerInfo
import com.samroid.wled.domain.model.DeviceResponse
import com.samroid.wled.domain.model.NodeInfoData
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.domain.model.TransportDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FakeDeviceTransport : DeviceTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _Transport_connectionState = MutableStateFlow(TransportConnectionState.DISCONNECTED)
    override val transportConnectionState: StateFlow<TransportConnectionState> = _Transport_connectionState.asStateFlow()

    private val _devices = MutableStateFlow<List<TransportDevice>>(emptyList())
    override val devices: StateFlow<List<TransportDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastResponse = MutableStateFlow<String?>(null)
    override val lastResponse: StateFlow<String?> = _lastResponse.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    override val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _lastDeviceResponse = MutableStateFlow<DeviceResponse?>(null)
    override val lastDeviceResponse: StateFlow<DeviceResponse?> = _lastDeviceResponse.asStateFlow()

    private val _nodeList = MutableStateFlow<List<NodeListItem>>(emptyList())
    override val nodeList: StateFlow<List<NodeListItem>> = _nodeList.asStateFlow()

    private val _nodeInfo = MutableStateFlow<NodeInfoData?>(null)
    override val nodeInfo: StateFlow<NodeInfoData?> = _nodeInfo.asStateFlow()

    private val _controllerInfo = MutableStateFlow<ControllerInfo?>(null)
    override val controllerInfo: StateFlow<ControllerInfo?> = _controllerInfo.asStateFlow()

    // ---------- state شبیه‌ساز ----------
    private var seq = 0
    private var provisionActive = false
    private var wifiConfigured = false
    private var wifiConnected = false
    private var udpStreamEnabled = false

    private data class NodeState(
        var online: Boolean = true,
        var deviceId: String,
        var ipSuffix: Int,
        var rssi: Int = -60,
        var ledCount: Int = 100,
        var cctEnabled: Boolean = true,
        var udpEnabled: Boolean = false,
        var processorId: Int = 0,
        var startPixel: Int = 0,
        var endPixel: Int = 99,
        var rgbOn: Boolean = true,
        var cctOn: Boolean = true,
        var brightnessRgb: Int = 128,
        var brightnessCct: Int = 128,
        var colorR: Int = 255,
        var colorG: Int = 255,
        var colorB: Int = 255,
        var cct: Int = 128,
        var effectId: Int = 0,
        var effectSx: Int = 128,
        var effectIx: Int = 128,
        var effectPal: Int = 0,
        val presets: MutableMap<Int, String> = mutableMapOf()
    )

    private val nodes = linkedMapOf(
        1 to NodeState(deviceId = "dev-node-0001", ipSuffix = 101, ledCount = 184, endPixel = 185),
        2 to NodeState(deviceId = "dev-node-0002", ipSuffix = 102, ledCount = 60, endPixel = 59, cctEnabled = false),
        3 to NodeState(online = false, deviceId = "dev-node-0003", ipSuffix = 103, ledCount = 120, endPixel = 119)
    )

    private fun nextSeq(): Byte {
        seq = (seq + 1) and 0xFF
        return seq.toByte()
    }

    private fun addLog(msg: String) {
        val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _log.value = (_log.value + "[$t] $msg").takeLast(100)
    }

    private fun emitAck(human: String, payload: ByteArray = byteArrayOf()): Boolean {
        val ack = DeviceResponse.Ack(nextSeq(), payload)
        _lastDeviceResponse.value = ack
        _lastResponse.value = human
        addLog(human)
        return true
    }

    private fun emitNack(code: Int): Boolean {
        val nack = DeviceResponse.Nack(nextSeq(), code, byteArrayOf(code.toByte()))
        _lastDeviceResponse.value = nack
        val msg = "NACK: ${nack.errorMessage()}"
        _lastResponse.value = msg
        addLog(msg)
        return true
    }

    private suspend fun latency(ms: Long = 100) = delay(ms)

    private fun ensureConnected(): Boolean {
        if (_Transport_connectionState.value != TransportConnectionState.CONNECTED) {
            addLog("Not Connected")
            _lastResponse.value = "Not Connected"
            return false
        }
        return true
    }

    private fun requireNode(nodeId: Int): NodeState? {
        val n = nodes[nodeId]
        if (n == null) {
            emitNack(8)
            return null
        }
        return n
    }

    // ---------- Scan / Connect ----------

    override fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        addLog("Fake Scan Started...")
        scope.launch {
            delay(500)
            _devices.value = listOf(
                TransportDevice("fake-1", "WLED-Master-Fake", "AA:BB:CC:DD:EE:01"),
                TransportDevice("fake-2", "ESP32-BT-Sim", "AA:BB:CC:DD:EE:02")
            )
            delay(400)
            _isScanning.value = false
            addLog("Scan Finished")
        }
    }

    override fun stopScan() {
        _isScanning.value = false
        addLog("Scan Stopped")
    }

    override fun connect(deviceId: String) {
        stopScan()
        _Transport_connectionState.value = TransportConnectionState.CONNECTING
        addLog("Connectint to  $deviceId ...")
        scope.launch {
            delay(400)
            _Transport_connectionState.value = TransportConnectionState.CONNECTED
            addLog(" Connected ✓ (Fake)")
            _lastResponse.value = "Connected (Fake)"
        }
    }

    override fun disconnect() {
        provisionActive = false
        _Transport_connectionState.value = TransportConnectionState.DISCONNECTED
        addLog("Disconnected")
    }

    override fun clearLog() {
        _log.value = emptyList()
        _lastResponse.value = null
    }

    override fun release() {
        scope.cancel()
        disconnect()
    }

    // ---------- System ----------

    override suspend fun ping(): Boolean {
        if (!ensureConnected()) return false
        latency()
        return emitAck("ACK ✓ PING")
    }

    override suspend fun getInfo(): Boolean {
        if (!ensureConnected()) return false
        latency()
        val info = ControllerInfo(1, 0, 0, nodes.size)
        _controllerInfo.value = info
        val payload = byteArrayOf(
            info.major.toByte(), info.minor.toByte(),
            info.patch.toByte(), info.nodeCount.toByte()
        )
        return emitAck("ACK ✓ Info v${info.version} nodes=${info.nodeCount}", payload)
    }

    // ---------- Network ----------

    override suspend fun networkConfig(ssid: String, password: String, baseIp: ByteArray): Boolean {
        if (!ensureConnected()) return false
        if (baseIp.size != 3) return emitNack(2)
        latency(150)
        wifiConfigured = true
        addLog("NETWORK_CONFIG ssid=$ssid pass=*** ip=${baseIp.joinToString(".") { (it.toInt() and 0xFF).toString() }}")
        return emitAck("ACK ✓ NETWORK_CONFIG")
    }

    override suspend fun wifiConnect(): Boolean {
        if (!ensureConnected()) return false
        latency(250)
        if (!wifiConfigured) return emitNack(7)
        wifiConnected = true
        return emitAck("ACK ✓ WIFI_CONNECT")
    }

    // ---------- Provision ----------

    override suspend fun scanWled(): Boolean {
        if (!ensureConnected()) return false
        latency(200)
        val count = 2
        return emitAck("ACK ✓ SCAN_WLED count=$count", byteArrayOf(count.toByte()))
    }

    override suspend fun provision(): Boolean {
        if (!ensureConnected()) return false
        latency()
        provisionActive = true
        addLog("PROVISION Started")
        return emitAck("ACK ✓ PROVISION")
    }

    override suspend fun gpioValue(pin: Int): Boolean {
        if (!ensureConnected()) return false
        latency()
        addLog("GPIO_VALUE pin=$pin")
        return emitAck("ACK ✓ GPIO_VALUE")
    }

    override suspend fun gpioConfirm(): Boolean {
        if (!ensureConnected()) return false
        latency()
        return emitAck("ACK ✓ GPIO_CONFIRM")
    }

    override suspend fun colorValue(order: Int): Boolean {
        if (!ensureConnected()) return false
        if (order !in 0..5) return emitNack(2)
        latency()
        val names = listOf("GRB", "RGB", "BRG", "RBG", "BGR", "GBR")
        addLog("COLOR_VALUE order=${names.getOrElse(order) { "?" }}")
        return emitAck("ACK ✓ COLOR_VALUE")
    }

    override suspend fun colorConfirm(): Boolean {
        if (!ensureConnected()) return false
        latency()
        return emitAck("ACK ✓ COLOR_CONFIRM")
    }

    override suspend fun lengthValue(length: Int): Boolean {
        if (!ensureConnected()) return false
        if (length !in 1..300) return emitNack(2)
        latency()
        addLog("LENGTH_VALUE length=$length")
        return emitAck("ACK ✓ LENGTH_VALUE")
    }

    override suspend fun lengthConfirm(): Boolean {
        if (!ensureConnected()) return false
        latency()
        return emitAck("ACK ✓ LENGTH_CONFIRM")
    }

    override suspend fun outputValue(pin1: Int, pin2: Int): Boolean {
        if (!ensureConnected()) return false
        latency()
        addLog("OUTPUT_VALUE pins=$pin1,$pin2")
        return emitAck("ACK ✓ OUTPUT_VALUE")
    }

    override suspend fun outputConfirm(): Boolean {
        if (!ensureConnected()) return false
        latency()
        return emitAck("ACK ✓ OUTPUT_CONFIRM")
    }

    override suspend fun storeValue(nodeId: Int): Boolean {
        if (!ensureConnected()) return false
        if (nodeId !in 1..250) return emitNack(2)
        latency(150)
        nodes[nodeId] = NodeState(
            deviceId = "dev-node-${nodeId.toString().padStart(4, '0')}",
            ipSuffix = 100 + nodeId,
            ledCount = 100,
            endPixel = 99
        )
        provisionActive = false
        _nodeList.value = nodes.map { (id, n) -> NodeListItem(id, n.online) }
        addLog("STORE_VALUE nodeId=$nodeId Saved.")
        return emitAck("ACK ✓ STORE_VALUE #$nodeId")
    }

    override suspend fun cancelProvision(): Boolean {
        if (!ensureConnected()) return false
        latency()
        provisionActive = false
        return emitAck("ACK ✓ CANCEL")
    }

    // ---------- Node ----------

    override suspend fun nodeListCmd(): Boolean {
        if (!ensureConnected()) return false
        latency()
        val list = nodes.map { (id, n) -> NodeListItem(id, n.online) }
        _nodeList.value = list
        val resp = DeviceResponse.NodeList(nextSeq(), list)
        _lastDeviceResponse.value = resp
        val human = "NODE_LIST: ${list.size} Node"
        _lastResponse.value = human
        addLog(human + " → " + list.joinToString { "#${it.nodeId}/${if (it.online) "on" else "off"}" })
        return true
    }

    override suspend fun nodeInfoCmd(nodeId: Int): Boolean {
        if (!ensureConnected()) return false
        latency()
        val n = requireNode(nodeId) ?: return true
        val info = NodeInfoData(
            nodeId = nodeId,
            deviceId = n.deviceId,
            ip = "192.168.1.${n.ipSuffix}",
            rssi = n.rssi,
            ledCount = n.ledCount,
            cctEnabled = n.cctEnabled,
            udpEnabled = n.udpEnabled,
            processorId = n.processorId,
            startPixel = n.startPixel,
            endPixel = n.endPixel
        )
        _nodeInfo.value = info
        val resp = DeviceResponse.NodeInfo(nextSeq(), info)
        _lastDeviceResponse.value = resp
        val human = "NODE_INFO #$nodeId ${info.ip}"
        _lastResponse.value = human
        addLog(human)
        return true
    }

    // ---------- Control ----------

    override suspend fun setBrightness(nodeId: Int, output: Int, brightness: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        latency()
        if (output == 0) n.brightnessRgb = brightness.coerceIn(0, 255)
        else n.brightnessCct = brightness.coerceIn(0, 255)
        addLog("SET_BRIGHTNESS #$nodeId out=$output bri=$brightness")
        return emitAck("ACK ✓ brightness")
    }

    override suspend fun setColor(nodeId: Int, r: Int, g: Int, b: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        latency()
        n.colorR = r.coerceIn(0, 255)
        n.colorG = g.coerceIn(0, 255)
        n.colorB = b.coerceIn(0, 255)
        addLog("SET_COLOR #$nodeId rgb=($r,$g,$b)")
        return emitAck("ACK ✓ color")
    }

    override suspend fun setCct(nodeId: Int, brightness: Int, cct: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        if (!n.cctEnabled) return emitNack(2)
        latency()
        n.brightnessCct = brightness.coerceIn(0, 255)
        n.cct = cct.coerceIn(0, 255)
        addLog("SET_CCT #$nodeId bri=$brightness cct=$cct")
        return emitAck("ACK ✓ cct")
    }

    override suspend fun onOff(nodeId: Int, output: Int, on: Boolean): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        latency()
        if (output == 0) n.rgbOn = on else n.cctOn = on
        addLog("ON_OFF #$nodeId out=$output on=$on")
        return emitAck("ACK ✓ on/off")
    }

    override suspend fun setEffect(nodeId: Int, segment: Int, effectId: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        if (effectId !in 0..159) return emitNack(2)
        latency()
        n.effectId = effectId
        addLog("SET_EFFECT #$nodeId seg=$segment fx=$effectId")
        return emitAck("ACK ✓ effect")
    }

    override suspend fun setEffectSx(nodeId: Int, segment: Int, speed: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        latency()
        n.effectSx = speed.coerceIn(0, 255)
        addLog("SET_EFFECT_SX #$nodeId sx=$speed")
        return emitAck("ACK ✓ sx")
    }

    override suspend fun setEffectIx(nodeId: Int, segment: Int, intensity: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        latency()
        n.effectIx = intensity.coerceIn(0, 255)
        addLog("SET_EFFECT_IX #$nodeId ix=$intensity")
        return emitAck("ACK ✓ ix")
    }

    override suspend fun setEffectPal(nodeId: Int, segment: Int, paletteId: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        if (paletteId !in 0..71) return emitNack(2)
        latency()
        n.effectPal = paletteId
        addLog("SET_EFFECT_PAL #$nodeId pal=$paletteId")
        return emitAck("ACK ✓ palette")
    }

    // ---------- پریست ----------

    override suspend fun presetSave(nodeId: Int, presetId: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        if (presetId !in 1..250) return emitNack(2)
        latency()
        n.presets[presetId] = "rgb(${n.colorR},${n.colorG},${n.colorB}) fx=${n.effectId}"
        addLog("PRESET_SAVE #$nodeId ps=$presetId ${if (presetId == 1) "(BOOT)" else ""}")
        return emitAck("ACK ✓ preset save")
    }

    override suspend fun presetLoad(nodeId: Int, presetId: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        if (presetId !in 1..250) return emitNack(2)
        latency()
        val saved = n.presets[presetId]
        addLog("PRESET_LOAD #$nodeId ps=$presetId → ${saved ?: "Empty"}")
        return emitAck("ACK ✓ preset load")
    }

    // ---------- UDP ----------

    override suspend fun udpStart(nodeId: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        latency()
        n.udpEnabled = true
        addLog("UDP_START #$nodeId")
        return emitAck("ACK ✓ UDP_START")
    }

    override suspend fun udpStop(nodeId: Int): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        latency()
        n.udpEnabled = false
        addLog("UDP_STOP #$nodeId")
        return emitAck("ACK ✓ UDP_STOP")
    }

    override suspend fun udpMapSet(
        nodeId: Int,
        processorId: Int,
        startPixel: Int,
        endPixel: Int
    ): Boolean {
        if (!ensureConnected()) return false
        val n = requireNode(nodeId) ?: return true
        if (processorId !in 0..1) return emitNack(2)
        latency()
        n.processorId = processorId
        n.startPixel = startPixel.coerceAtLeast(0)
        n.endPixel = endPixel.coerceAtLeast(0)
        addLog("UDP_MAP_SET #$nodeId proc=$processorId range=$startPixel..$endPixel")
        return emitAck("ACK ✓ UDP_MAP_SET")
    }

    override suspend fun udpStreamEnable(enable: Boolean): Boolean {
        if (!ensureConnected()) return false
        latency()
        udpStreamEnabled = enable
        addLog("UDP_STREAM_ENABLE enable=$enable")
        return emitAck("ACK ✓ UDP_STREAM_ENABLE")
    }
}