package com.samroid.wled.presentation.ambilight


import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AmbilightViewModel @Inject constructor(
    application: Application,
    private val transport: DeviceTransport
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AmbilightUiState())
    val uiState: StateFlow<AmbilightUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                _uiState.update {
                    it.copy(bluetoothConnected = state == TransportConnectionState.CONNECTED)
                }
            }
        }
    }

    fun setFps(fps: Int) {
        _uiState.update { it.copy(fps = fps) }
    }

    fun setQuality(q: String) {
        _uiState.update { it.copy(quality = q) }
    }

    fun setSmoothing(v: Float) {
        _uiState.update { it.copy(smoothing = v.coerceIn(0f, 100f)) }
    }

    fun setCaptureSource(source: String) {
        _uiState.update { it.copy(captureSource = source) }
    }

    fun setTarget(host: String, port: Int) {
        _uiState.update { it.copy(targetHost = host, targetPort = port) }
    }

    fun start() {
        val projection = MediaProjectionHolder.projection
        if (projection == null) {
            _uiState.update { it.copy(message = "مجوز ضبط صفحه لازم است") }
            return
        }
        val s = _uiState.value
        val ctx = getApplication<Application>()

        // سوییچ استریم روی Master (اختیاری ولی هم‌راستا با مستند)
        viewModelScope.launch {
            transport.udpStreamEnable(true)
        }

        val intent = Intent(ctx, AmbilightService::class.java).apply {
            action = AmbilightService.ACTION_START
            putExtra(AmbilightService.EXTRA_FPS, s.fps)
            putExtra(AmbilightService.EXTRA_QUALITY, s.quality)
            putExtra(AmbilightService.EXTRA_SMOOTHING, s.smoothing)
            putExtra(AmbilightService.EXTRA_HOST, s.targetHost)
            putExtra(AmbilightService.EXTRA_PORT, s.targetPort)
        }
        ctx.startForegroundService(intent)

        _uiState.update {
            it.copy(isRunning = true, message = "Ambient شروع شد (اسکلت — روز ۱۲ کامل می‌شود)")
        }
    }

    fun stop() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, AmbilightService::class.java).apply {
            action = AmbilightService.ACTION_STOP
        }
        ctx.startService(intent)

        viewModelScope.launch {
            transport.udpStreamEnable(false)
        }

        _uiState.update {
            it.copy(isRunning = false, message = "Ambient متوقف شد")
        }
    }

    fun toggle() {
        if (_uiState.value.isRunning) stop() else start()
    }
}