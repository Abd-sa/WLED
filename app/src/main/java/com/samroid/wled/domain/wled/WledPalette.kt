package com.samroid.wled.domain.wled

import androidx.compose.ui.graphics.Color

/**
 * @param stops gradient stops: position 0f..1f + Color (WLED-style interpolation)
 */
data class WledPalette(
    val id: Int,
    val name: String,
    val stops: List<Pair<Float, Color>>
) {
    /** Sample N colors along the gradient for UI strip (like official WLED). */
    fun sampleColors(count: Int = 16): List<Color> {
        if (stops.isEmpty()) return List(count) { Color.Gray }
        if (stops.size == 1) return List(count) { stops[0].second }
        return (0 until count).map { i ->
            val t = i / (count - 1).coerceAtLeast(1).toFloat()
            sampleAt(t)
        }
    }

    fun sampleAt(t: Float): Color {
        val x = t.coerceIn(0f, 1f)
        if (stops.isEmpty()) return Color.Black
        if (x <= stops.first().first) return stops.first().second
        if (x >= stops.last().first) return stops.last().second
        for (i in 0 until stops.lastIndex) {
            val (p0, c0) = stops[i]
            val (p1, c1) = stops[i + 1]
            if (x in p0..p1) {
                val u = if (p1 == p0) 0f else (x - p0) / (p1 - p0)
                return lerpColor(c0, c1, u)
            }
        }
        return stops.last().second
    }

    private fun lerpColor(a: Color, b: Color, t: Float): Color {
        val u = t.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * u,
            green = a.green + (b.green - a.green) * u,
            blue = a.blue + (b.blue - a.blue) * u,
            alpha = 1f
        )
    }
}

