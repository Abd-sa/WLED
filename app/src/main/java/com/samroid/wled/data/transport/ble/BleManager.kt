package com.samroid.wled.data.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.samroid.wled.R
import com.samroid.wled.data.protocol.Protocol
import com.samroid.wled.data.protocol.ResponseParser
import com.samroid.wled.domain.model.ControllerInfo
import com.samroid.wled.domain.model.DeviceResponse
import com.samroid.wled.domain.model.NodeInfoData
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.domain.model.TransportConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BleManager(private val context: Context) {

    companion object {
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner get() = adapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null

    private var serviceUuid: UUID? = null
    private var writeCharUuid: UUID? = null
    private var notifyCharUuid: UUID? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val packetBuffer = mutableListOf<Byte>()

    private val _Transport_connectionState = MutableStateFlow(TransportConnectionState.DISCONNECTED)
    val transportConnectionState: StateFlow<TransportConnectionState> = _Transport_connectionState.asStateFlow()

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse: StateFlow<String?> = _lastResponse.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _lastDeviceResponse = MutableStateFlow<DeviceResponse?>(null)
    val lastDeviceResponse: StateFlow<DeviceResponse?> = _lastDeviceResponse.asStateFlow()

    private val _nodeList = MutableStateFlow<List<NodeListItem>>(emptyList())
    val nodeList: StateFlow<List<NodeListItem>> = _nodeList.asStateFlow()

    private val _nodeInfo = MutableStateFlow<NodeInfoData?>(null)
    val nodeInfo: StateFlow<NodeInfoData?> = _nodeInfo.asStateFlow()

    private val _controllerInfo = MutableStateFlow<ControllerInfo?>(null)
    val controllerInfo: StateFlow<ControllerInfo?> = _controllerInfo.asStateFlow()

    private var bondReceiver: BroadcastReceiver? = null
    private var pendingBondAddress: String? = null

    // ------------------- Scan -------------------

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = device.name ?: result.scanRecord?.deviceName
            addDevice(device, name)
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            addLog(context.getString(R.string.failed_ble_scan_code, errorCode))
        }
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, name: String?) {
        val current = _devices.value.toMutableList()
        if (current.none { it.address == device.address }) {
            current.add(device)
            _devices.value = current
            addLog("Found: ${name ?: "No Name"} [${device.address}]")
        }
    }

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (adapter == null || !adapter.isEnabled) {
            addLog("Bluetooth is Off!")
            return
        }
        if (_isScanning.value) return

        _devices.value = emptyList()
        bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).forEach {
            addDevice(it, it.name)
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(null, settings, scanCallback)
            _isScanning.value = true
            addLog("BLE Scan Started...")
            scope.launch {
                delay(15_000)
                if (_isScanning.value) stopScan()
            }
        } catch (e: SecurityException) {
            addLog("Scan permission not found: ${e.message}")
        } catch (e: Exception) {
            addLog("Scan failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) { }
        _isScanning.value = false
        addLog("Scan Stopped.")
    }

    // ------------------- Connect -------------------

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stopScan()
        unregisterBondReceiver()

        _Transport_connectionState.value = TransportConnectionState.CONNECTING
        addLog("Connecting to ${device.name ?: device.address} ...")

        try {
            gatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (e: SecurityException) {
            addLog("Connect permission denied: ${e.message}")
            _Transport_connectionState.value = TransportConnectionState.ERROR
        } catch (e: Exception) {
            addLog("Failed to Connect ${e.message}")
            _Transport_connectionState.value = TransportConnectionState.ERROR
        }
    }
    @SuppressLint("MissingPermission")
    fun disconnect() {
        unregisterBondReceiver()
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {
        }
        gatt = null
        writeChar = null
        notifyChar = null
        packetBuffer.clear()
        _Transport_connectionState.value = TransportConnectionState.DISCONNECTED
    }

    /**
     * Called when GATT is usable for I/O setup (services + optional notify).
     * Only transitions to CONNECTED after bond is done (or already bonded / not required).
     */

    @SuppressLint("MissingPermission")
    private fun onReadyToMarkConnected(gatt: BluetoothGatt) {
        val device = gatt.device
        addLog("Link ready check — bondState=${device.bondState}")

        // Already paired
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            finishAsConnected("already bonded")
            return
        }

        // Stay CONNECTING until bond or timeout
        _Transport_connectionState.value = TransportConnectionState.CONNECTING

        // 1) Register receiver FIRST (avoid missing BOND_BONDED)
        waitForBond(device)

        // 2) Request bond only if not already bonding
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            try {
                val started = device.createBond()
                addLog("createBond() started=$started — accept pairing notification if shown")
            } catch (e: Exception) {
                addLog("createBond error: ${e.message}")
            }
        } else {
            addLog("Bond already in progress — accept pairing notification")
        }

        // 3) Poll + timeout (BroadcastReceiver alone is not reliable)
        scope.launch {
            val address = device.address
            val deadline = System.currentTimeMillis() + 45_000

            while (System.currentTimeMillis() < deadline) {
                delay(500)

                // State already moved on (user disconnected / error)
                val st = _Transport_connectionState.value
                if (st == TransportConnectionState.CONNECTED ||
                    st == TransportConnectionState.DISCONNECTED ||
                    st == TransportConnectionState.ERROR
                ) {
                    return@launch
                }

                val bonded = try {
                    // Re-fetch device; bondState can update on the same object too
                    val d = adapter?.getRemoteDevice(address) ?: device
                    d.bondState == BluetoothDevice.BOND_BONDED
                } catch (_: Exception) {
                    device.bondState == BluetoothDevice.BOND_BONDED
                }

                if (bonded) {
                    addLog("Bond detected by poll")
                    finishAsConnected("bond polled")
                    return@launch
                }
            }

            // Timeout: some firmwares pair at OS level but stay BOND_NONE in app;
            // if GATT is still up, allow CONNECTED so ping can be tried.
            val stillGatt = try {
                gatt.device // local ref
                true
            } catch (_: Exception) {
                false
            }
            val gattConnected = try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                    .any { it.address.equals(address, true) }
            } catch (_: Exception) {
                false
            }

            if (stillGatt || gattConnected) {
                addLog("Bond wait timeout — GATT still up, marking CONNECTED (try ping)")
                finishAsConnected("timeout fallback, GATT up")
            } else {
                addLog("Bond wait timeout — giving up")
                unregisterBondReceiver()
                _Transport_connectionState.value = TransportConnectionState.ERROR
            }
        }
    }

    private fun finishAsConnected(reason: String) {
        // Idempotent
        if (_Transport_connectionState.value == TransportConnectionState.CONNECTED) {
            unregisterBondReceiver()
            return
        }
        unregisterBondReceiver()
        pendingBondAddress = null
        _Transport_connectionState.value = TransportConnectionState.CONNECTED
        addLog("CONNECTED ($reason)")
    }

    @SuppressLint("MissingPermission")
    private fun waitForBond(device: BluetoothDevice) {
        unregisterBondReceiver()
        pendingBondAddress = device.address

        bondReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return

                val dev = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
                } ?: return

                val expected = pendingBondAddress ?: return
                if (!dev.address.equals(expected, ignoreCase = true)) return

                val state = intent.getIntExtra(
                    BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.BOND_NONE
                )
                val prev = intent.getIntExtra(
                    BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                    BluetoothDevice.BOND_NONE
                )
                addLog("Bond broadcast: $prev → $state")

                when (state) {
                    BluetoothDevice.BOND_BONDED -> {
                        finishAsConnected("bond broadcast")
                    }
                    BluetoothDevice.BOND_NONE -> {
                        if (prev == BluetoothDevice.BOND_BONDING) {
                            addLog("Pairing rejected")
                            unregisterBondReceiver()
                            pendingBondAddress = null
                            _Transport_connectionState.value = TransportConnectionState.ERROR
                            // optional: disconnect()
                        }
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        addLog("Waiting for pairing accept…")
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    bondReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(bondReceiver, filter)
            }
            addLog("Bond receiver registered")
        } catch (e: Exception) {
            addLog("registerReceiver failed: ${e.message}")
        }

        // Immediate re-check (event may have already fired)
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            finishAsConnected("already bonded after register")
        }
    }

    private fun unregisterBondReceiver() {
        val receiver = bondReceiver ?: return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        bondReceiver = null
    }



    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    addLog("Connected. Caching Services")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    addLog("Disconnected (status=$status)")
                    _Transport_connectionState.value = TransportConnectionState.DISCONNECTED
                    writeChar = null
                    notifyChar = null
                }
            }
            if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_DISCONNECTED) {
                addLog("Failed to Connect: status=$status")
                _Transport_connectionState.value = TransportConnectionState.ERROR
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                addLog("Failed to Discover: $status")
                _Transport_connectionState.value = TransportConnectionState.ERROR
                return
            }

            // لاگ همه سرویس‌ها برای دیباگ UUID واقعی
            gatt.services.forEach { service ->
                addLog("Service: ${service.uuid}")
                service.characteristics.forEach { ch ->
                    val p = ch.properties
                    val flags = buildString {
                        if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) append("R ")
                        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) append("W ")
                        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) append("WNR ")
                        if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) append("N ")
                        if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) append("I ")
                    }
                    addLog("  Char: ${ch.uuid} [$flags]")
                }
            }

            val found = pickCharacteristics(gatt)
            if (!found) {
                addLog("Characteristic Not Found")
                _Transport_connectionState.value = TransportConnectionState.ERROR
                return
            }
            addLog("Write  = $writeCharUuid")
            addLog("Notify = $notifyCharUuid")
            if (notifyChar != null) {
                enableNotifications(gatt, notifyChar!!)
            } else {
                addLog("Notify Not Found. Only Sending")
                onReadyToMarkConnected(gatt)
            }

//            addLog("Write  = $writeCharUuid")
//            addLog("Notify = $notifyCharUuid")
//            enableNotifications(gatt, notifyChar!!)
        }
        private fun pickCharacteristics(gatt: BluetoothGatt): Boolean {
            val nordicService = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
            val nordicWrite   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
            val nordicNotify  = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

            // اولویت با NUS
            gatt.getService(nordicService)?.let { svc ->
                val w = svc.getCharacteristic(nordicWrite)
                val n = svc.getCharacteristic(nordicNotify)
                if (w != null && n != null) {
                    serviceUuid = nordicService
                    writeChar = w
                    notifyChar = n
                    writeCharUuid = nordicWrite
                    notifyCharUuid = nordicNotify
                    addLog("Nordic UART is used")
                    return true
                }
            }

            var write: BluetoothGattCharacteristic? = null
            var notify: BluetoothGattCharacteristic? = null
            var svcId: UUID? = null

            for (service in gatt.services) {
                val sid = service.uuid.toString().uppercase()
                if (sid.startsWith("00001800") || sid.startsWith("00001801")) continue

                for (ch in service.characteristics) {
                    val p = ch.properties
                    val canWrite =
                        (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) ||
                                (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
                    val canNotify =
                        (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) ||
                                (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)

                    if (write == null && canWrite) {
                        write = ch
                        svcId = service.uuid
                    }
                    if (notify == null && canNotify) {
                        notify = ch
                        if (svcId == null) svcId = service.uuid
                    }
                }

                if (write != null && notify != null && write.service.uuid == notify.service.uuid) {
                    break
                }
            }

            if (write == null) return false

            writeChar = write
            writeCharUuid = write.uuid
            serviceUuid = svcId ?: write.service.uuid

            if (notify != null) {
                notifyChar = notify
                notifyCharUuid = notify.uuid
            } else {
                addLog("Notify Not Found. Only Sending")
            }
            return true
        }


        @SuppressLint("MissingPermission")
        private fun enableNotifications(gatt: BluetoothGatt, char: BluetoothGattCharacteristic) {
            gatt.setCharacteristicNotification(char, true)
            val cccd = char.getDescriptor(CCCD_UUID)
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccd)

            } else {
//                _Transport_connectionState.value = TransportConnectionState.CONNECTED
//                addLog("Connected without CCCD")

                addLog("No CCCD — notify may be limited")
                onReadyToMarkConnected(gatt)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                addLog("Notify enabled")
            } else {
                addLog("Notify Activation Failed: $status (continue)")
            }
            // Do NOT set CONNECTED here — wait for bond if needed
            onReadyToMarkConnected(gatt)
        }
        @SuppressLint("MissingPermission")
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            //val data = gatt.readCharacteristic(characteristic)

            val data = characteristic.value ?: return
            onDataReceived(data)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            onDataReceived(value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                addLog("Send Successfully")
            } else {
                addLog("Failed to Send: $status")
            }
        }
    }

    private fun onDataReceived(data: ByteArray) {
        addLog("${data.size} Byte Received:  ${data.joinToString(" ") { "%02X".format(it) }}")
        synchronized(packetBuffer) {
            data.forEach { packetBuffer.add(it) }
            processBuffer()
        }
    }

    private fun processBuffer() {
        val buffer = packetBuffer
        while (buffer.size >= 8) {
            val start = buffer.indexOfFirst { it == Protocol.HEADER1 }
            if (start < 0) {
                buffer.clear()
                return
            }
            if (start > 0) repeat(start) { buffer.removeAt(0) }
            if (buffer.size < 2 || buffer[1] != Protocol.HEADER2) {
                buffer.removeAt(0)
                continue
            }
            if (buffer.size < 6) return
            val length = buffer[5].toInt() and 0xFF
            val total = 6 + length + 2
            if (buffer.size < total) return

            val bytes = buffer.take(total).toByteArray()
            repeat(total) { buffer.removeAt(0) }

            val parsed = Protocol.parsePacket(bytes) ?: run {
                addLog("Invalid CRC / Packet")
                return@processBuffer
            }

            handleParsedPacket(parsed)
        }

    }
    private fun handleParsedPacket(parsed: Protocol.ParsedPacket) {
        val response = ResponseParser.parse(parsed)
        _lastDeviceResponse.value = response

        when (response) {
            is DeviceResponse.Ack -> {
                _lastResponse.value = "ACK ✓"
                addLog("ACK seq=${response.sequence.toInt() and 0xFF} len=${response.payload.size}")

                response.asInfo()?.let {
                    _controllerInfo.value = it
                    addLog("Info: v${it.version}, nodes=${it.nodeCount}")
                }
                response.asScanCount()?.let {
                    addLog("SCAN_WLED: $it AP Found")
                }
            }

            is DeviceResponse.Nack -> {
                _lastResponse.value = "NACK: ${response.errorMessage()}"
                addLog("NACK: ${response.errorMessage()}")
            }

            is DeviceResponse.NodeList -> {
                _nodeList.value = response.nodes
                _lastResponse.value = "Nodes List: ${response.nodes.size}"
                addLog(
                    "NODE_LIST: " + response.nodes.joinToString { n ->
                        "#${n.nodeId}${if (n.online) " online" else " offline"}"
                    }
                )
            }

            is DeviceResponse.NodeInfo -> {
                _nodeInfo.value = response.info
                val n = response.info
                _lastResponse.value = "Node #${n.nodeId}"
                addLog(
                    "NODE_INFO #${n.nodeId} IP=${n.ip} RSSI=${n.rssi} " +
                            "LED=${n.ledCount} CCT=${n.cctEnabled} UDP=${n.udpEnabled}"
                )
            }

            is DeviceResponse.Unknown -> {
                _lastResponse.value = "Unknown Response 0x%02X".format(response.command)
                addLog(
                    "Unknown cmd=0x%02X payload=%s".format(
                        response.command,
                        response.payload.joinToString(" ") { "%02X".format(it) }
                    )
                )
            }
        }
    }
    // ------------------- Send -------------------

    @SuppressLint("MissingPermission")
    suspend fun sendPacket(packet: ByteArray): Boolean = withContext(Dispatchers.Main) {
        _lastDeviceResponse.value = null
        val g = gatt
        val ch = writeChar
        if (_Transport_connectionState.value != TransportConnectionState.CONNECTED) {
            addLog("Skip send: not fully connected (state=${_Transport_connectionState.value})")
            return@withContext false
        }
        if (g == null || ch == null) {
            addLog("Sending is not Ready")
            return@withContext false
        }
        return@withContext try {
            addLog("Send: ${packet.joinToString(" ") { "%02X".format(it) }}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val writeType =
                    if (ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    else
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                val result = g.writeCharacteristic(ch, packet, writeType)
                result == BluetoothStatusCodes.SUCCESS
            } else {
                ch.value = packet
                ch.writeType =
                    if (ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    else
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                g.writeCharacteristic(ch)
            }
        } catch (e: Exception) {
            addLog("Sending Failed: ${e.message}")
            false
        }
    }

    // ------------------- System Methods -------------------
    @SuppressLint("MissingPermission")
    suspend fun sendPing(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_PING))
    }

    suspend fun getInfo(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_GET_INFO))
    }

    // ------------------- Wi-Fi Methods -------------------

    /**
     * @param baseIp سه بایت اول IP پایه، مثلاً 192,168,1 برای 192.168.1.x
     */
    suspend fun networkConfig(ssid: String, password: String, baseIp: ByteArray): Boolean =
        withContext(Dispatchers.Main) {
            require(baseIp.size == 3) { "baseIp must be 3 bytes" }

            val ssidBytes = ssid.toByteArray(Charsets.UTF_8)
            val passBytes = password.toByteArray(Charsets.UTF_8)

            val payload = ByteArray(1 + ssidBytes.size + 1 + passBytes.size + 3)
            var i = 0
            payload[i++] = ssidBytes.size.toByte()
            System.arraycopy(ssidBytes, 0, payload, i, ssidBytes.size)
            i += ssidBytes.size
            payload[i++] = passBytes.size.toByte()
            System.arraycopy(passBytes, 0, payload, i, passBytes.size)
            i += passBytes.size
            System.arraycopy(baseIp, 0, payload, i, 3)

            return@withContext sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_NETWORK_CONFIG,
                    payload
                )
            )
        }

    suspend fun wifiConnect(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_WIFI_CONNECT))
    }

    // ------------------- Provision Methods -------------------

    suspend fun scanWled(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_SCAN_WLED))
    }

    suspend fun provision(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_PROVISION))
    }

    suspend fun gpioValue(pin: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_GPIO_VALUE, byteArrayOf(pin.toByte())))
    }

    suspend fun gpioConfirm(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_GPIO_CONFIRM))
    }

    /** order: 0=GRB, 1=RGB, 2=BRG, 3=RBG, 4=BGR, 5=GBR */
    suspend fun colorValue(order: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_COLOR_VALUE, byteArrayOf(order.toByte())))
    }

    suspend fun colorConfirm(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_COLOR_CONFIRM))
    }

    /** length: 1..300 — little-endian */
    suspend fun lengthValue(length: Int): Boolean = withContext(Dispatchers.Main) {
        val payload = byteArrayOf(
            (length and 0xFF).toByte(),
            ((length shr 8) and 0xFF).toByte()
        )
        return@withContext sendPacket(Protocol.buildPacket(Protocol.CMD_LENGTH_VALUE, payload))
    }


    suspend fun lengthConfirm(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_LENGTH_CONFIRM))
    }

    suspend fun outputValue(pin1: Int, pin2: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(
            Protocol.buildPacket(
                Protocol.CMD_OUTPUT_VALUE,
                byteArrayOf(pin1.toByte(), pin2.toByte())
            )
        )
    }

    suspend fun outputConfirm(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_OUTPUT_CONFIRM))
    }

    suspend fun storeValue(nodeId: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_STORE_VALUE, byteArrayOf(nodeId.toByte())))
    }

    suspend fun cancelProvision(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_CANCEL))
    }

    // ------------------- Nodes Methods -------------------
    suspend fun nodeList(): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_NODE_LIST))
    }
    suspend fun nodeInfo(nodeId: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_NODE_INFO, byteArrayOf(nodeId.toByte())))
    }
    /** output: 0=RGB, 1=CCT — brightness: 0..255 */
    suspend fun setBrightness(nodeId: Int, output: Int, brightness: Int): Boolean =
        withContext(Dispatchers.Main) {
            sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_SET_BRIGHTNESS,
                    byteArrayOf(nodeId.toByte(), output.toByte(), brightness.toByte())
                )
            )
        }
    suspend fun setColor(nodeId: Int, r: Int, g: Int, b: Int): Boolean =
        withContext(Dispatchers.Main) {
            sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_SET_COLOR,
                    byteArrayOf(nodeId.toByte(), r.toByte(), g.toByte(), b.toByte())
                )
            )
        }
    /** cct: 0=گرم .. 255=سرد */
    suspend fun setCct(nodeId: Int, brightness: Int, cct: Int): Boolean =
        withContext(Dispatchers.Main) {
            sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_SET_CCT,
                    byteArrayOf(nodeId.toByte(), brightness.toByte(), cct.toByte())
                )
            )
        }
    /** output: 0=RGB, 1=CCT */
    suspend fun onOff(nodeId: Int, output: Int, on: Boolean): Boolean =
        withContext(Dispatchers.Main) {
            sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_ON_OFF,
                    byteArrayOf(nodeId.toByte(), output.toByte(), if (on) 1 else 0)
                )
            )
        }
    /** effectId: 0..159 */
    suspend fun setEffect(nodeId: Int, segment: Int, effectId: Int): Boolean =
        withContext(Dispatchers.Main) {
            sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_SET_EFFECT,
                    byteArrayOf(nodeId.toByte(), segment.toByte(), effectId.toByte())
                )
            )
        }
    /** speed: 0..255 */
    suspend fun setEffectSx(nodeId: Int, segment: Int, speed: Int): Boolean =
        withContext(Dispatchers.Main) {
            sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_SET_EFFECT_SX,
                    byteArrayOf(nodeId.toByte(), segment.toByte(), speed.toByte())
                )
            )
        }
    /** intensity: 0..255 */
    suspend fun setEffectIx(nodeId: Int, segment: Int, intensity: Int): Boolean =
        withContext(Dispatchers.Main) {
            sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_SET_EFFECT_IX,
                    byteArrayOf(nodeId.toByte(), segment.toByte(), intensity.toByte())
                )
            )
        }
    /** paletteId: 0..71 */
    suspend fun setEffectPal(nodeId: Int, segment: Int, paletteId: Int): Boolean =
        withContext(Dispatchers.Main) {
            sendPacket(
                Protocol.buildPacket(
                    Protocol.CMD_SET_EFFECT_PAL,
                    byteArrayOf(nodeId.toByte(), segment.toByte(), paletteId.toByte())
                )
            )
        }

    // ------------------- Preset Methods -------------------
    /** presetId: 1..250 — 1 = apply on boot */
    suspend fun presetSave(nodeId: Int, presetId: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(
            Protocol.buildPacket(
                Protocol.CMD_PRESET_SAVE,
                byteArrayOf(nodeId.toByte(), presetId.toByte())
            )
        )
    }

    suspend fun presetLoad(nodeId: Int, presetId: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(
            Protocol.buildPacket(
                Protocol.CMD_PRESET_LOAD,
                byteArrayOf(nodeId.toByte(), presetId.toByte())
            )
        )
    }

    // ------------------- UDP Methods -------------------

    suspend fun udpStart(nodeId: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_UDP_START, byteArrayOf(nodeId.toByte())))
    }
    suspend fun udpStop(nodeId: Int): Boolean = withContext(Dispatchers.Main) {
        sendPacket(Protocol.buildPacket(Protocol.CMD_UDP_STOP, byteArrayOf(nodeId.toByte())))
    }
    /**
     * processorId: 0=COPY, 1=AVERAGE
     * start/end: little-endian
     */
    suspend fun udpMapSet(
        nodeId: Int,
        processorId: Int,
        startPixel: Int,
        endPixel: Int
    ): Boolean = withContext(Dispatchers.Main) {
        val payload = byteArrayOf(
            nodeId.toByte(),
            processorId.toByte(),
            (startPixel and 0xFF).toByte(),
            ((startPixel shr 8) and 0xFF).toByte(),
            (endPixel and 0xFF).toByte(),
            ((endPixel shr 8) and 0xFF).toByte()
        )
        return@withContext sendPacket(Protocol.buildPacket(Protocol.CMD_UDP_MAP_SET, payload))
    }

    /** enable: true=فعال */
    suspend fun udpStreamEnable(enable: Boolean): Boolean = withContext(Dispatchers.Main) {
        sendPacket(
            Protocol.buildPacket(
                Protocol.CMD_UDP_STREAM_ENABLE,
                byteArrayOf(if (enable) 1 else 0)
            )
        )
    }

    private fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date())
        _log.value = (_log.value + "[$time] $msg").takeLast(80)
    }

    fun clearLog() {
        _log.value = emptyList()
        _lastResponse.value = null
    }

    fun release() {
        stopScan()
        disconnect()
        scope.cancel()
    }
}