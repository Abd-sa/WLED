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

    /** Pending MediaProjection result waiting to start service after UDP setup. */
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
                // Keep selection where possible; refresh details via NODE_INFO
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
                _uiState.update { it.copy(message = "Bluetooth must be connected") }
                return@launch
            }
            _uiState.update { it.copy(isLoadingTargets = true, message = null) }
            transport.nodeListCmd()
            delay(300)
            val ids = transport.nodeList.value.map { it.nodeId to it.online }
            loadTargetDetails(ids)
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
                endPixel = info?.endPixel
                    ?: prev?.endPixel
                    ?: ((info?.ledCount ?: 100) - 1).coerceAtLeast(0),
                processorId = info?.processorId ?: prev?.processorId ?: 0,
                ledCount = info?.ledCount ?: prev?.ledCount ?: 0
            )
            delay(80)
        }

        _uiState.update { it.copy(targets = built) }
    }

    fun setProtocol(value: String) {
        _uiState.update { it.copy(protocol = value) }
    }

    fun setColorOrder(value: String) {
        _uiState.update { it.copy(colorOrder = value) }
    }

    fun setFps(fps: Int) {
        _uiState.update { it.copy(fps = fps) }
    }

    fun setQuality(q: String) {
        _uiState.update { it.copy(quality = q) }
    }

    fun setSmoothingEnabled(enabled: Boolean) {
        _uiState.update { it.copy(smoothingEnabled = enabled) }
    }

    fun setSmoothingPercent(v: Float) {
        _uiState.update { it.copy(smoothingPercent = v.coerceIn(0f, 100f)) }
    }

    fun setAverageColor(enabled: Boolean) {
        _uiState.update { it.copy(averageColor = enabled) }
    }

    fun toggleTarget(nodeId: Int) {
        _uiState.update { state ->
            state.copy(
                targets = state.targets.map {
                    if (it.nodeId == nodeId) it.copy(selected = !it.selected) else it
                }
            )
        }
    }

    fun setTargetRange(nodeId: Int, start: Int, end: Int) {
        _uiState.update { state ->
            state.copy(
                targets = state.targets.map {
                    if (it.nodeId == nodeId) {
                        it.copy(
                            startPixel = start.coerceIn(0, 300),
                            endPixel = end.coerceIn(0, 300)
                        )
                    } else it
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * User tapped Start: if projection not ready, ask UI to request it.
     * If projection already granted (onProjectionGranted), prepare UDP then start service.
     */
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
        val selected = s.targets.filter { it.selected && it.ip.isNotBlank() }
        if (selected.isEmpty()) {
            _uiState.update {
                it.copy(message = "No targets with IP. Refresh nodes or check NODE_INFO.")
            }
            return
        }
        // Ask UI for MediaProjection permission
        _uiState.update { it.copy(needsProjection = true, message = null) }
    }

    /**
     * Called from UI after MediaProjection permission is granted.
     */
    fun onProjectionGranted(resultCode: Int, data: Intent) {
        pendingResultCode = resultCode
        pendingData = data
        _uiState.update { it.copy(needsProjection = false) }
        viewModelScope.launch {
            prepareUdpAndStartService()
        }
    }

    fun onProjectionDenied() {
        pendingResultCode = null
        pendingData = null
        _uiState.update {
            it.copy(needsProjection = false, message = "Screen capture permission denied")
        }
    }

    private suspend fun prepareUdpAndStartService() {
        val s = _uiState.value
        val selected = s.targets.filter { it.selected && it.ip.isNotBlank() }
        if (selected.isEmpty()) {
            _uiState.update { it.copy(message = "No valid targets") }
            return
        }

        _uiState.update { it.copy(isPreparing = true, message = "Enabling UDP stream…") }

        // 1) Master stream enable
        val streamOk = transport.udpStreamEnable(true)
        if (!streamOk) {
            _uiState.update {
                it.copy(isPreparing = false, message = "UDP_STREAM_ENABLE failed")
            }
            return
        }

        // 2) Map + start per selected node
        for (t in selected) {
            val mapOk = transport.udpMapSet(
                nodeId = t.nodeId,
                processorId = t.processorId.coerceIn(0, 1),
                startPixel = t.startPixel.coerceIn(0, 300),
                endPixel = t.endPixel.coerceIn(0, 300)
            )
            if (!mapOk) {
                _uiState.update {
                    it.copy(
                        isPreparing = false,
                        message = "UDP_MAP_SET failed for node ${t.nodeId}"
                    )
                }
                return
            }
            val startOk = transport.udpStart(t.nodeId)
            if (!startOk) {
                _uiState.update {
                    it.copy(
                        isPreparing = false,
                        message = "UDP_START failed for node ${t.nodeId}"
                    )
                }
                return
            }
            delay(50)
        }

        // 3) Start foreground capture service
        val resultCode = pendingResultCode
        val data = pendingData
        if (resultCode == null || data == null) {
            _uiState.update {
                it.copy(isPreparing = false, message = "Missing screen capture result")
            }
            return
        }

        val port = protocolDefaultPort(s.protocol)
        val primary = selected.first()
        val ctx = getApplication<Application>()

        AmbilightService.start(
            context = ctx,
            resultCode = resultCode,
            data = data,
            host = primary.ip,
            port = port,
            protocol = s.protocol,
            colorOrder = s.colorOrder,
            fps = s.fps,
            quality = qualityToPx(s.quality),
            smoothing = s.smoothingEnabled,
            average = s.averageColor
        )

        // Optional: pass all hosts if your Service supports arrays
        // (extend Service later for multi-target send)

        _uiState.update {
            it.copy(
                isPreparing = false,
                isRunning = true,
                message = "Streaming to ${selected.size} node(s)"
            )
        }
    }

    fun stop() {
        val ctx = getApplication<Application>()
        AmbilightService.stop(ctx)

        viewModelScope.launch {
            val selected = _uiState.value.targets.filter { it.selected }
            for (t in selected) {
                runCatching { transport.udpStop(t.nodeId) }
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