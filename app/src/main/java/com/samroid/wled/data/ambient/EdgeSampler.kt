package com.samroid.wled.data.ambient

import android.graphics.Bitmap
import kotlin.math.max

/**
 * Samples edge colors from a downscaled capture bitmap into a linear LED strip.
 */
class EdgeSampler(
    private var layout: LedLayout = LedLayout()
) {
    fun updateLayout(layout: LedLayout) {
        this.layout = layout
    }

    /**
     * Returns ARGB colors in strip order. Empty if bitmap invalid.
     */
    fun sample(bitmap: Bitmap): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 2 || h < 2 || layout.totalLeds <= 0) return IntArray(0)

        val out = IntArray(layout.totalLeds)
        var idx = 0

        fun avgOnLine(
            count: Int,
            x0: Float, y0: Float, x1: Float, y1: Float
        ) {
            if (count <= 0) return
            for (i in 0 until count) {
                val t = if (count == 1) 0.5f else i / (count - 1f)
                val x = (x0 + (x1 - x0) * t).toInt().coerceIn(0, w - 1)
                val y = (y0 + (y1 - y0) * t).toInt().coerceIn(0, h - 1)
                // small neighborhood average for stability
                var r = 0
                var g = 0
                var b = 0
                var n = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val xx = (x + dx).coerceIn(0, w - 1)
                        val yy = (y + dy).coerceIn(0, h - 1)
                        val c = bitmap.getPixel(xx, yy)
                        r += (c shr 16) and 0xFF
                        g += (c shr 8) and 0xFF
                        b += c and 0xFF
                        n++
                    }
                }
                n = max(n, 1)
                out[idx++] = (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }

        // Default path: Top L→R, Right T→B, Bottom R→L, Left B→T (clockwise from top-left)
        if (layout.clockwise && layout.startTopLeft) {
            if (layout.enableTop) avgOnLine(layout.top, 0f, 0f, (w - 1).toFloat(), 0f)
            if (layout.enableRight) avgOnLine(layout.right, (w - 1).toFloat(), 0f, (w - 1).toFloat(), (h - 1).toFloat())
            if (layout.enableBottom) avgOnLine(layout.bottom, (w - 1).toFloat(), (h - 1).toFloat(), 0f, (h - 1).toFloat())
            if (layout.enableLeft) avgOnLine(layout.left, 0f, (h - 1).toFloat(), 0f, 0f)
        } else {
            // Simplified counter-clockwise from top-left
            if (layout.enableLeft) avgOnLine(layout.left, 0f, 0f, 0f, (h - 1).toFloat())
            if (layout.enableBottom) avgOnLine(layout.bottom, 0f, (h - 1).toFloat(), (w - 1).toFloat(), (h - 1).toFloat())
            if (layout.enableRight) avgOnLine(layout.right, (w - 1).toFloat(), (h - 1).toFloat(), (w - 1).toFloat(), 0f)
            if (layout.enableTop) avgOnLine(layout.top, (w - 1).toFloat(), 0f, 0f, 0f)
        }
        return if (idx == out.size) out else out.copyOf(idx)
    }

    fun averageColor(colors: IntArray): IntArray {
        if (colors.isEmpty()) return colors
        var r = 0
        var g = 0
        var b = 0
        for (c in colors) {
            r += (c shr 16) and 0xFF
            g += (c shr 8) and 0xFF
            b += c and 0xFF
        }
        val n = colors.size
        val avg = (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
        return IntArray(colors.size) { avg }
    }
}