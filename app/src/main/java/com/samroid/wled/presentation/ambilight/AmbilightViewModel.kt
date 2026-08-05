package com.samroid.wled.presentation.ambilight

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class AmbilightViewModel @Inject constructor(
    application: Application,
    private val transport: DeviceTransport
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AmbilightUiState())
    val uiState: StateFlow<AmbilightUiState> = _uiState.asStateFlow()

    private var pendingResultCode: Int? = null
    private var pendingData: Intent? = null

    init {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                val connected = state == TransportConnectionState.CONNECTED
                _uiState.update { it.copy(bluetoothConnected = connected) }
                if (connected) refreshTargets()
            }
        }
        viewModelScope.launch {
            transport.nodeList.collect { list ->
                if (list.isNotEmpty() && _uiState.value.bluetoothConnected) {
                    loadTargetDetails(list.map { it.nodeId to it.online })
                } else if (list.isEmpty()) {
                    _uiState.update { it.copy(targets = emptyList()) }
                }
            }
        }
    }

    fun refreshTargets() {
        viewModelScope.launch {
            if (!_uiState.value.bluetoothConnected) {
                _uiState.update { it.copy(message = "Connect Bluetooth first") }
                return@launch
            }
            _uiState.update { it.copy(isLoadingTargets = true, message = null) }
            transport.nodeListCmd()
            delay(250)
            loadTargetDetails(transport.nodeList.value.map { it.nodeId to it.online })
            _uiState.update { it.copy(isLoadingTargets = false) }
        }
    }

    private suspend fun loadTargetDetails(nodes: List<Pair<Int, Boolean>>) {
        val previous = _uiState.value.targets.associateBy { it.nodeId }
        val built = mutableListOf<AmbientTargetUi>()
        for ((nodeId, online) in nodes) {
            val prev = previous[nodeId]
            transport.nodeInfoCmd(nodeId)
            val info = withTimeoutOrNull(2_500) {
                transport.nodeInfo.first { it != null && it.nodeId == nodeId }
            }
            built += AmbientTargetUi(
                nodeId = nodeId,
                name = info?.nodeName?.ifBlank { "Node$nodeId" } ?: prev?.name ?: "Node$nodeId",
                ip = info?.ip?.takeIf { it.isNotBlank() } ?: prev?.ip.orEmpty(),
                online = online,
                selected = prev?.selected ?: (online && !(info?.ip.isNullOrBlank())),
                startPixel = info?.startPixel ?: prev?.startPixel ?: 0,
                endPixel = info?.endPixel ?: prev?.endPixel
                ?: ((info?.ledCount ?: 100) - 1).coerceAtLeast(0),
                processorId = info?.processorId ?: prev?.processorId ?: 0,
                ledCount = info?.ledCount ?: prev?.ledCount ?: 0
            )
            delay(60)
        }
        _uiState.update { it.copy(targets = built) }
    }

    fun setProtocol(v: String) = _uiState.update { it.copy(protocol = v) }
    fun setColorOrder(v: String) = _uiState.update { it.copy(colorOrder = v) }
    fun setFps(v: Int) = _uiState.update { it.copy(fps = v) }
    fun setQuality(v: String) = _uiState.update { it.copy(quality = v) }
    fun setSmoothingEnabled(v: Boolean) = _uiState.update { it.copy(smoothingEnabled = v) }
    fun setSmoothingPercent(v: Float) =
        _uiState.update { it.copy(smoothingPercent = v.coerceIn(0f, 100f)) }
    fun setAverageColor(v: Boolean) = _uiState.update { it.copy(averageColor = v) }
    fun setLedTop(v: Int) = _uiState.update { it.copy(ledTop = v.coerceIn(0, 300)) }
    fun setLedRight(v: Int) = _uiState.update { it.copy(ledRight = v.coerceIn(0, 300)) }
    fun setLedBottom(v: Int) = _uiState.update { it.copy(ledBottom = v.coerceIn(0, 300)) }
    fun setLedLeft(v: Int) = _uiState.update { it.copy(ledLeft = v.coerceIn(0, 300)) }

    fun toggleTarget(nodeId: Int) {
        _uiState.update { s ->
            s.copy(targets = s.targets.map {
                if (it.nodeId == nodeId) it.copy(selected = !it.selected) else it
            })
        }
    }

    fun onStartClicked() {
        val s = _uiState.value
        if (s.isRunning) {
            stop()
            return
        }
        if (!s.bluetoothConnected) {
            _uiState.update { it.copy(message = "Connect Bluetooth first") }
            return
        }
        if (s.targets.none { it.selected && it.ip.isNotBlank() }) {
            _uiState.update { it.copy(message = "Select at least one node with IP (Refresh)") }
            return
        }
        // Ask UI for MediaProjection (real device). On Fake you can call startFakeUdpOnly().
        _uiState.update { it.copy(needsProjection = true, message = null) }
    }

    /** Fake / no hardware: UDP_STREAM + MAP + START without screen capture. */
    fun startFakeUdpOnly() {
        viewModelScope.launch {
            val ok = prepareUdpCommands()
            if (ok) {
                _uiState.update {
                    it.copy(
                        isRunning = true,
                        isPreparing = false,
                        message = "Fake OK: UDP pipeline ready (no screen capture)"
                    )
                }
            }
        }
    }

    fun onProjectionGranted(resultCode: Int, data: Intent) {
        pendingResultCode = resultCode
        pendingData = data
        _uiState.update { it.copy(needsProjection = false) }
        viewModelScope.launch { prepareUdpAndStartService() }
    }

    fun onProjectionDenied() {
        pendingResultCode = null
        pendingData = null
        _uiState.update {
            it.copy(
                needsProjection = false,
                message = "Screen capture denied — use Fake UDP only if testing without device"
            )
        }
    }

    private suspend fun prepareUdpCommands(): Boolean {
        val s = _uiState.value
        val selected = s.targets.filter { it.selected && it.ip.isNotBlank() }
        if (selected.isEmpty()) {
            _uiState.update { it.copy(isPreparing = false, message = "No valid targets") }
            return false
        }
        _uiState.update { it.copy(isPreparing = true, message = "UDP_STREAM_ENABLE…") }

        if (!transport.udpStreamEnable(true)) {
            _uiState.update { it.copy(isPreparing = false, message = "UDP_STREAM_ENABLE failed") }
            return false
        }
        for (t in selected) {
            if (!transport.udpMapSet(
                    t.nodeId,
                    t.processorId.coerceIn(0, 1),
                    t.startPixel.coerceIn(0, 300),
                    t.endPixel.coerceIn(0, 300)
                )
            ) {
                _uiState.update {
                    it.copy(isPreparing = false, message = "UDP_MAP_SET failed #${t.nodeId}")
                }
                return false
            }
            if (!transport.udpStart(t.nodeId)) {
                _uiState.update {
                    it.copy(isPreparing = false, message = "UDP_START failed #${t.nodeId}")
                }
                return false
            }
            delay(40)
        }
        return true
    }

    private suspend fun prepareUdpAndStartService() {
        if (!prepareUdpCommands()) return

        val code = pendingResultCode
        val data = pendingData
        if (code == null || data == null) {
            _uiState.update { it.copy(isPreparing = false, message = "Missing projection result") }
            return
        }

        val s = _uiState.value
        val selected = s.targets.filter { it.selected && it.ip.isNotBlank() }
        val port = protocolDefaultPort(s.protocol)
        val hosts = ArrayList(selected.map { it.ip })
        val starts = selected.map { it.startPixel }.toIntArray()
        val ends = selected.map { it.endPixel }.toIntArray()
        val alpha = (1f - s.smoothingPercent / 100f).coerceIn(0.05f, 1f)

        AmbilightService.start(
            context = getApplication(),
            resultCode = code,
            data = data,
            hosts = hosts,
            port = port,
            protocol = s.protocol,
            colorOrder = s.colorOrder,
            fps = s.fps,
            quality = qualityToPx(s.quality),
            smoothing = s.smoothingEnabled,
            smoothAlpha = alpha,
            average = s.averageColor,
            ledTop = s.ledTop,
            ledRight = s.ledRight,
            ledBottom = s.ledBottom,
            ledLeft = s.ledLeft,
            startLeds = starts,
            endLeds = ends
        )

        _uiState.update {
            it.copy(
                isPreparing = false,
                isRunning = true,
                message = "Streaming to ${selected.size} node(s)"
            )
        }
    }

    fun stop() {
        AmbilightService.stop(getApplication())
        viewModelScope.launch {
            _uiState.value.targets.filter { it.selected }.forEach {
                runCatching { transport.udpStop(it.nodeId) }
            }
            runCatching { transport.udpStreamEnable(false) }
        }
        pendingResultCode = null
        pendingData = null
        _uiState.update {
            it.copy(isRunning = false, isPreparing = false, message = "Ambient stopped")
        }
    }
}