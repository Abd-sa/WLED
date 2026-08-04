package com.samroid.wled.presentation.ambilight


import android.graphics.Bitmap

object EdgeColorSampler {

    /**
     * از چهار لبه تصویر، [left + top + right + bottom] رنگ میانگین می‌گیرد.
     * @param ledsPerSide تعداد نمونه در هر ضلع (مثلاً 10)
     */
    fun sample(bitmap: Bitmap, ledsPerSide: Int = 10): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 4 || h < 4) return IntArray(0)

        val result = IntArray(ledsPerSide * 4)
        var idx = 0

        // Left (پایین → بالا برای Ambilight رایج)
        for (i in 0 until ledsPerSide) {
            val y = (h - 1) - (i * (h - 1) / (ledsPerSide - 1).coerceAtLeast(1))
            result[idx++] = averageStrip(bitmap, 0, y, (w * 0.05f).toInt().coerceAtLeast(1), 1)
        }
        // Top (چپ → راست)
        for (i in 0 until ledsPerSide) {
            val x = i * (w - 1) / (ledsPerSide - 1).coerceAtLeast(1)
            result[idx++] = averageStrip(bitmap, x, 0, 1, (h * 0.05f).toInt().coerceAtLeast(1))
        }
        // Right (بالا → پایین)
        for (i in 0 until ledsPerSide) {
            val y = i * (h - 1) / (ledsPerSide - 1).coerceAtLeast(1)
            result[idx++] = averageStrip(
                bitmap,
                (w * 0.95f).toInt().coerceAtMost(w - 1),
                y,
                (w * 0.05f).toInt().coerceAtLeast(1),
                1
            )
        }
        // Bottom (راست → چپ)
        for (i in 0 until ledsPerSide) {
            val x = (w - 1) - (i * (w - 1) / (ledsPerSide - 1).coerceAtLeast(1))
            result[idx++] = averageStrip(
                bitmap,
                x,
                (h * 0.95f).toInt().coerceAtMost(h - 1),
                1,
                (h * 0.05f).toInt().coerceAtLeast(1)
            )
        }
        return result
    }

    private fun averageStrip(bmp: Bitmap, x: Int, y: Int, bw: Int, bh: Int): Int {
        val x0 = x.coerceIn(0, bmp.width - 1)
        val y0 = y.coerceIn(0, bmp.height - 1)
        val x1 = (x0 + bw).coerceAtMost(bmp.width)
        val y1 = (y0 + bh).coerceAtMost(bmp.height)
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0
        for (yy in y0 until y1) {
            for (xx in x0 until x1) {
                val c = bmp.getPixel(xx, yy)
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
                n++
            }
        }
        if (n == 0) return 0
        return (0xFF shl 24) or
                ((r / n).toInt() shl 16) or
                ((g / n).toInt() shl 8) or
                (b / n).toInt()
    }

    /** Smoothing ساده بین فریم قبلی و فعلی */
    fun smooth(previous: IntArray?, current: IntArray, factor: Float): IntArray {
        if (previous == null || previous.size != current.size) return current
        val t = (factor / 100f).coerceIn(0f, 1f) // 0=بدون نرم، 1=خیلی نرم
        val out = IntArray(current.size)
        for (i in current.indices) {
            val pr = (previous[i] shr 16) and 0xFF
            val pg = (previous[i] shr 8) and 0xFF
            val pb = previous[i] and 0xFF
            val cr = (current[i] shr 16) and 0xFF
            val cg = (current[i] shr 8) and 0xFF
            val cb = current[i] and 0xFF
            val r = (pr * t + cr * (1 - t)).toInt().coerceIn(0, 255)
            val g = (pg * t + cg * (1 - t)).toInt().coerceIn(0, 255)
            val b = (pb * t + cb * (1 - t)).toInt().coerceIn(0, 255)
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }
}