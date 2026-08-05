package com.samroid.wled.data.ambient

/**
 * Temporal smoothing to reduce LED flicker (UAL-style).
 * alpha: 0 = freeze previous, 1 = no smoothing.
 */
class ColorSmoother(private var alpha: Float = 0.35f) {
    private var prev: IntArray? = null

    fun setAlpha(value: Float) {
        alpha = value.coerceIn(0.05f, 1f)
    }

    fun apply(current: IntArray): IntArray {
        val p = prev
        if (p == null || p.size != current.size || alpha >= 0.999f) {
            prev = current.copyOf()
            return current
        }
        val out = IntArray(current.size)
        val a = alpha
        val ia = 1f - a
        for (i in current.indices) {
            val c = current[i]
            val o = p[i]
            val r = (((c shr 16) and 0xFF) * a + ((o shr 16) and 0xFF) * ia).toInt()
            val g = (((c shr 8) and 0xFF) * a + ((o shr 8) and 0xFF) * ia).toInt()
            val b = ((c and 0xFF) * a + (o and 0xFF) * ia).toInt()
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        prev = out
        return out
    }

    fun reset() {
        prev = null
    }
}