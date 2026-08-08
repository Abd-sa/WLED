package com.samroid.wled.domain.wled

import androidx.compose.ui.graphics.Color

/**
 * UI-facing facade. Preview rendering is delegated to EffectRegistry + WledRuntime.
 */
object EffectPreviewEngine {

    data class Params(
        val effectId: Int,
        val paletteId: Int = 6,
        val speed: Float = 128f,
        val intensity: Float = 128f,
        val primary: Color = Color(0xFFFFAA00),
        val secondary: Color = Color(0xFF000000),
        val ledCount: Int = 48
    )

    private val runtime = WledRuntime(48)

    /**
     * @param timeSec continuous time in seconds (from EffectPreviewBar animation)
     */
    fun render(params: Params, timeSec: Float): List<Color> {
        runtime.resize(params.ledCount)
        runtime.effectId = params.effectId.coerceIn(0, WledEffectCatalog.MAX_ID)
        runtime.paletteId = params.paletteId
        runtime.speed = params.speed.toInt().coerceIn(0, 255)
        runtime.intensity = params.intensity.toInt().coerceIn(0, 255)
        runtime.primary = params.primary
        runtime.secondary = params.secondary
        runtime.timeMs = (timeSec * 1000f).toLong().coerceAtLeast(0L)

        EffectRegistry.render(runtime)
        return runtime.snapshot()
    }
}