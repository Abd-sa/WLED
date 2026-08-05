package com.samroid.wled.presentation.nodecontrol

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.R
import com.samroid.wled.data.transport.DeviceTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NodeControlViewModel @Inject constructor(
    private val transport: DeviceTransport,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val nodeId: Int = savedStateHandle.get<Int>("nodeId") ?: 1

    private val _uiState = MutableStateFlow(NodeControlUiState(nodeId = nodeId))
    val uiState: StateFlow<NodeControlUiState> = _uiState.asStateFlow()

    companion object {
        const val SEG_RGB = 0
        const val SEG_CCT = 1
        const val OUT_RGB = 0
        const val OUT_CCT = 1
    }

    init {
        viewModelScope.launch {
            transport.nodeInfo.collect { info ->
                if (info != null && info.nodeId == nodeId) {
                    _uiState.update {
                        it.copy(
                            cctEnabled = info.cctEnabled,
                            online = true
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            transport.nodeInfoCmd(nodeId)
        }
    }

    fun selectTab(tab: ControlTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // ================= RGB =================

    fun setRgbOn(on: Boolean) {
        _uiState.update {
            it.copy(
                rgbOn = on,
                rgbEffectExpanded = if (on) it.rgbEffectExpanded else false,
                rgbPaletteExpanded = if (on) it.rgbPaletteExpanded else false
            )
        }
        viewModelScope.launch {
            transport.onOff(nodeId, output = OUT_RGB, on = on)
        }
    }

    fun setBrightnessRgb(value: Float) {
        _uiState.update { it.copy(brightnessRgb = value.coerceIn(0f, 255f)) }
    }

    fun commitBrightnessRgb() {
        val v = _uiState.value.brightnessRgb.toInt().coerceIn(0, 255)
        viewModelScope.launch {
            transport.setBrightness(nodeId, output = OUT_RGB, brightness = v)
        }
    }

    fun setColor(r: Float, g: Float, b: Float) {
        _uiState.update {
            it.copy(
                colorR = r.coerceIn(0f, 255f),
                colorG = g.coerceIn(0f, 255f),
                colorB = b.coerceIn(0f, 255f)
            )
        }
    }

    fun commitColor() {
        val s = _uiState.value
        viewModelScope.launch {
            transport.setColor(
                nodeId,
                s.colorR.toInt().coerceIn(0, 255),
                s.colorG.toInt().coerceIn(0, 255),
                s.colorB.toInt().coerceIn(0, 255)
            )
        }
    }

    fun setRgbEffectId(id: Int) {
        val v = id.coerceIn(0, 159)
        _uiState.update { it.copy(rgbEffectId = v, rgbEffectExpanded = false) }
        viewModelScope.launch {
            transport.setEffect(nodeId, segment = SEG_RGB, effectId = v)
        }
    }

    fun setRgbPaletteId(id: Int) {
        val v = id.coerceIn(0, 71)
        _uiState.update { it.copy(rgbPaletteId = v, rgbPaletteExpanded = false) }
        viewModelScope.launch {
            transport.setEffectPal(nodeId, segment = SEG_RGB, paletteId = v)
        }
    }

    fun setRgbEffectSpeed(value: Float) {
        _uiState.update { it.copy(rgbEffectSpeed = value.coerceIn(0f, 255f)) }
    }

    fun commitRgbEffectSpeed() {
        val v = _uiState.value.rgbEffectSpeed.toInt().coerceIn(0, 255)
        viewModelScope.launch {
            transport.setEffectSx(nodeId, segment = SEG_RGB, speed = v)
        }
    }


    fun setRgbEffectIntensity(value: Float) {
        _uiState.update { it.copy(rgbEffectIntensity = value.coerceIn(0f, 255f)) }
    }

    fun commitRgbEffectIntensity() {
        val v = _uiState.value.rgbEffectIntensity.toInt().coerceIn(0, 255)
        viewModelScope.launch {
            transport.setEffectIx(nodeId, segment = SEG_RGB, intensity = v)
        }
    }

    fun toggleRgbEffectMenu(expanded: Boolean) {
        _uiState.update { it.copy(rgbEffectExpanded = expanded) }
    }

    fun toggleRgbPaletteMenu(expanded: Boolean) {
        _uiState.update { it.copy(rgbPaletteExpanded = expanded) }
    }

    // ================= CCT =================

    fun setCctOn(on: Boolean) {
        _uiState.update {
            it.copy(
                cctOn = on,
                cctEffectExpanded = if (on) it.cctEffectExpanded else false,
                cctPaletteExpanded = if (on) it.cctPaletteExpanded else false
            )
        }
        viewModelScope.launch {
            transport.onOff(nodeId, output = OUT_CCT, on = on)
        }
    }

    fun setBrightnessCct(value: Float) {
        _uiState.update { it.copy(brightnessCct = value.coerceIn(0f, 255f)) }
    }

    fun setCctValue(value: Float) {
        _uiState.update { it.copy(cctValue = value.coerceIn(0f, 255f)) }
    }

    fun commitCct() {
        val s = _uiState.value
        viewModelScope.launch {
            transport.setCct(
                nodeId,
                brightness = s.brightnessCct.toInt().coerceIn(0, 255),
                cct = s.cctValue.toInt().coerceIn(0, 255)
            )
        }
    }



    fun setCctEffectId(id: Int) {
        val v = id.coerceIn(0, 159)
        _uiState.update { it.copy(cctEffectId = v, cctEffectExpanded = false) }
        viewModelScope.launch {
            transport.setEffect(nodeId, segment = SEG_CCT, effectId = v)
        }
    }

    fun setCctPaletteId(id: Int) {
        val v = id.coerceIn(0, 71)
        _uiState.update { it.copy(cctPaletteId = v, cctPaletteExpanded = false) }
        viewModelScope.launch {
            transport.setEffectPal(nodeId, segment = SEG_CCT, paletteId = v)
        }
    }

    fun setCctEffectSpeed(value: Float) {
        _uiState.update { it.copy(cctEffectSpeed = value.coerceIn(0f, 255f)) }
    }

    fun commitCctEffectSpeed() {
        val v = _uiState.value.cctEffectSpeed.toInt().coerceIn(0, 255)
        viewModelScope.launch {
            transport.setEffectSx(nodeId, segment = SEG_CCT, speed = v)
        }
    }

    fun setCctEffectIntensity(value: Float) {
        _uiState.update { it.copy(cctEffectIntensity = value.coerceIn(0f, 255f)) }
    }

    fun commitCctEffectIntensity() {
        val v = _uiState.value.cctEffectIntensity.toInt().coerceIn(0, 255)
        viewModelScope.launch {
            transport.setEffectIx(nodeId, segment = SEG_CCT, intensity = v)
        }
    }

    fun toggleCctEffectMenu(expanded: Boolean) {
        _uiState.update { it.copy(cctEffectExpanded = expanded) }
    }

    fun toggleCctPaletteMenu(expanded: Boolean) {
        _uiState.update { it.copy(cctPaletteExpanded = expanded) }
    }

    // ================= Presets =================

    fun savePreset(presetId: Int) {
        val id = presetId.coerceIn(1, 6)
        viewModelScope.launch {
            _uiState.update { it.copy(isPresetBusy = true, lastPresetAction = null) }
            val ok = transport.presetSave(nodeId, id)
            _uiState.update {
                it.copy(
                    isPresetBusy = false,
                    savedPresets = if (ok) it.savedPresets + id else it.savedPresets,
                    lastPresetAction = when {
                        !ok -> context.getString(R.string.preset_save_failed, id)
                        id == 1 -> context.getString(R.string.preset_1_saved_boot)
                        else -> context.getString(R.string.preset_saved, id)
                    }
                )
            }
        }
    }

    fun loadPreset(presetId: Int) {
        val id = presetId.coerceIn(1, 6)
        viewModelScope.launch {
            _uiState.update { it.copy(isPresetBusy = true, lastPresetAction = null) }
            val ok = transport.presetLoad(nodeId, id)
            _uiState.update {
                it.copy(
                    isPresetBusy = false,
                    lastPresetAction = if (ok) {
                        context.getString(R.string.preset_loaded, id)
                    } else {
                        context.getString(R.string.preset_load_failed, id)
                    }
                )
            }
        }
    }
}