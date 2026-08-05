package com.samroid.wled.data.ambient

/**
 * Per-side LED counts and strip traversal (UAL-style layout).
 */
data class LedLayout(
    val top: Int = 60,
    val right: Int = 34,
    val bottom: Int = 60,
    val left: Int = 34,
    val clockwise: Boolean = true,
    val startTopLeft: Boolean = true,
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