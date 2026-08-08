package com.samroid.wled.presentation.nodecontrol

import com.samroid.wled.domain.wled.WledEffectCatalog
import com.samroid.wled.domain.wled.WledPaletteCatalog

data class NodeControlUiState(
    val nodeId: Int = 0,
    val online: Boolean = true,
    val cctEnabled: Boolean = true,

    val rgbOn: Boolean = true,
    val brightnessRgb: Float = 200f,
    val colorR: Float = 255f,
    val colorG: Float = 120f,
    val colorB: Float = 40f,

    val rgbEffectId: Int = 0,
    val rgbPaletteId: Int = 0,
    val rgbEffectSpeed: Float = 128f,
    val rgbEffectIntensity: Float = 128f,
    val rgbEffectExpanded: Boolean = false,
    val rgbPaletteExpanded: Boolean = false,

    val cctOn: Boolean = true,
    val brightnessCct: Float = 200f,
    val cctValue: Float = 128f,

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

