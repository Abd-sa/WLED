package com.samroid.wled.presentation.nodecontrol

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.transport.DeviceTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NodeControlViewModel @Inject constructor(
    private val transport: DeviceTransport,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val nodeId: Int = savedStateHandle.get<Int>("nodeId") ?: 1

    private val _uiState = MutableStateFlow(NodeControlUiState(nodeId = nodeId))
    val uiState: StateFlow<NodeControlUiState> = _uiState.asStateFlow()

    init {
        // اگر NODE_INFO قبلاً آمده، CCT را از آن بردار
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
        // یک‌بار info بگیر
        viewModelScope.launch {
            transport.nodeInfoCmd(nodeId)
        }
    }

    fun selectTab(tab: ControlTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // ---- RGB ----

    fun setRgbOn(on: Boolean) {
        _uiState.update { it.copy(rgbOn = on) }
        viewModelScope.launch {
            transport.onOff(nodeId, output = 0, on = on)
        }
    }

    fun setBrightnessRgb(value: Float) {
        _uiState.update { it.copy(brightnessRgb = value) }
    }

    fun commitBrightnessRgb() {
        val v = _uiState.value.brightnessRgb.toInt().coerceIn(0, 255)
        viewModelScope.launch {
            transport.setBrightness(nodeId, output = 0, brightness = v)
        }
    }

    fun setColor(r: Float, g: Float, b: Float) {
        _uiState.update {
            it.copy(colorR = r, colorG = g, colorB = b)
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

    // ---- CCT ----

    fun setCctOn(on: Boolean) {
        _uiState.update { it.copy(cctOn = on) }
        viewModelScope.launch {
            transport.onOff(nodeId, output = 1, on = on)
        }
    }

    fun setBrightnessCct(value: Float) {
        _uiState.update { it.copy(brightnessCct = value) }
    }

    fun setCctValue(value: Float) {
        _uiState.update { it.copy(cctValue = value) }
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
    fun setEffectId(id: Int) {
        val v = id.coerceIn(0, 159)
        _uiState.update { it.copy(effectId = v, effectExpanded = false) }
        viewModelScope.launch {
            transport.setEffect(nodeId, segment = 0, effectId = v)
        }
    }

    fun setPaletteId(id: Int) {
        val v = id.coerceIn(0, 71)
        _uiState.update { it.copy(paletteId = v, paletteExpanded = false) }
        viewModelScope.launch {
            transport.setEffectPal(nodeId, segment = 0, paletteId = v)
        }
    }

    fun setEffectSpeed(value: Float) {
        _uiState.update { it.copy(effectSpeed = value) }
    }

    fun commitEffectSpeed() {
        val v = _uiState.value.effectSpeed.toInt().coerceIn(0, 255)
        viewModelScope.launch {
            transport.setEffectSx(nodeId, segment = 0, speed = v)
        }
    }

    fun setEffectIntensity(value: Float) {
        _uiState.update { it.copy(effectIntensity = value) }
    }

    fun commitEffectIntensity() {
        val v = _uiState.value.effectIntensity.toInt().coerceIn(0, 255)
        viewModelScope.launch {
            transport.setEffectIx(nodeId, segment = 0, intensity = v)
        }
    }

    fun toggleEffectMenu(expanded: Boolean) {
        _uiState.update { it.copy(effectExpanded = expanded) }
    }

    fun togglePaletteMenu(expanded: Boolean) {
        _uiState.update { it.copy(paletteExpanded = expanded) }
    }

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
                        !ok -> "خطا در ذخیره پریست $id"
                        id == 1 -> "پریست ۱ ذخیره شد (Apply on Boot)"
                        else -> "پریست $id ذخیره شد"
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
                    lastPresetAction = if (ok) "پریست $id بارگذاری شد" else "خطا در بارگذاری پریست $id"
                )
            }
        }
    }
}