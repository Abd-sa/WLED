package com.samroid.wled.presentation.nodecontrol

data class NodeControlUiState(
    val nodeId: Int = 0,
    val online: Boolean = true,
    val cctEnabled: Boolean = true,

    // RGB power + brightness + color
    val rgbOn: Boolean = true,
    val brightnessRgb: Float = 200f,
    val colorR: Float = 255f,
    val colorG: Float = 120f,
    val colorB: Float = 40f,

    // RGB effect (segment 0)
    val rgbEffectId: Int = 0,
    val rgbPaletteId: Int = 0,
    val rgbEffectSpeed: Float = 128f,
    val rgbEffectIntensity: Float = 128f,
    val rgbEffectExpanded: Boolean = false,
    val rgbPaletteExpanded: Boolean = false,

    // CCT power + brightness + value
    val cctOn: Boolean = true,
    val brightnessCct: Float = 200f,
    val cctValue: Float = 128f,

    // CCT effect (segment 1)
    val cctEffectId: Int = 0,
    val cctPaletteId: Int = 0,
    val cctEffectSpeed: Float = 128f,
    val cctEffectIntensity: Float = 128f,
    val cctEffectExpanded: Boolean = false,
    val cctPaletteExpanded: Boolean = false,

    val selectedTab: ControlTab = ControlTab.CONTROL,
    val isBusy: Boolean = false,
    val message: String? = null,

    val savedPresets: Set<Int> = emptySet(),
    val lastPresetAction: String? = null,
    val isPresetBusy: Boolean = false
)

enum class ControlTab { CONTROL, PRESETS }

object EffectCatalog {
    fun effectLabel(id: Int) = "Effect $id"
    fun paletteLabel(id: Int) = "Palette $id"
}