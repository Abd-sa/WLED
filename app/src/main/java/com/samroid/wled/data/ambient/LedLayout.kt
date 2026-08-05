package com.samroid.wled.data.ambient

/**
 * LED strip layout. Employer rule: strip starts at middle of bottom edge,
 * then goes e.g. bottom-right → right → top → left → bottom-left.
 */
data class LedLayout(
    val top: Int = 60,
    val right: Int = 34,
    val bottom: Int = 60,
    val left: Int = 34,
    val clockwise: Boolean = true,
    /** true = start at center of bottom edge (employer default) */
    val startBottomCenter: Boolean = true,
    val enableTop: Boolean = true,
    val enableRight: Boolean = true,
    val enableBottom: Boolean = true,
    val enableLeft: Boolean = true
) {
    val totalLeds: Int
        get() = (if (enableTop) top else 0) +
                (if (enableRight) right else 0) +
                (if (enableBottom) bottom else 0) +
                (if (enableLeft) left else 0)
}