package com.samroid.wled.presentation.nodecontrol


data class NodeControlUiState(
    val nodeId: Int = 0,
    val online: Boolean = true,
    val cctEnabled: Boolean = true,

    // RGB
    val rgbOn: Boolean = true,
    val brightnessRgb: Float = 200f,   // 0..255
    val colorR: Float = 255f,
    val colorG: Float = 120f,
    val colorB: Float = 40f,

    // CCT
    val cctOn: Boolean = true,
    val brightnessCct: Float = 200f,
    val cctValue: Float = 128f,        // 0..255

    val selectedTab: ControlTab = ControlTab.CONTROL,
    val isBusy: Boolean = false,
    val message: String? = null,

    // Effect (segment 0 = RGB ، در صورت نیاز بعداً CCT جدا)
    val effectId: Int = 0,          // 0..159
    val paletteId: Int = 0,         // 0..71
    val effectSpeed: Float = 128f,  // 0..255  SX
    val effectIntensity: Float = 128f, // 0..255 IX

    val effectExpanded: Boolean = false,
    val paletteExpanded: Boolean = false,


    /** presetهایی که حداقل یک‌بار Save شده‌اند (برای UI) */
    val savedPresets: Set<Int> = emptySet(),

    val lastPresetAction: String? = null,
    val isPresetBusy: Boolean = false
)

enum class ControlTab { CONTROL, PRESETS }

object EffectCatalog {
    fun effectLabel(id: Int) = "Effect $id"
    fun paletteLabel(id: Int) = "Palette $id"
}