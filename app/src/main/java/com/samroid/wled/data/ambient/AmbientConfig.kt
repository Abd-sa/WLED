package com.samroid.wled.data.ambient

data class AmbientConfig(
    val protocol: WledProtocol = WledProtocol.DDP,
    val colorOrder: ColorOrder = ColorOrder.GRB,
    val fps: Int = 30,
    /** Capture long-edge size: 64 / 128 / 256 / 512 */
    val qualityPx: Int = 128,
    val smoothing: Boolean = true,
    val smoothAlpha: Float = 0.35f,
    val averageColor: Boolean = false,
    val layout: LedLayout = LedLayout(),
    val targets: List<AmbientTarget> = emptyList()
)