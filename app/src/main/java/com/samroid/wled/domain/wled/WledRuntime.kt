package com.samroid.wled.domain.wled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Minimal WLED-like 1D strip runtime for UI effect preview.
 * Models SEGMENT / SEGENV concepts used by FX.cpp — not a full firmware emulator.
 */
class WledRuntime(
    ledCount: Int = 48
) {
    var leds: Array<Color> = Array(ledCount.coerceIn(8, 128)) { Color.Black }
        private set

    val n: Int get() = leds.size

    // Segment colors
    var primary: Color = Color.White
    var secondary: Color = Color.Black
    var tertiary: Color = Color.Gray

    // Segment params (0..255 style, same as WLED UI)
    var speed: Int = 128
    var intensity: Int = 128
    var custom1: Int = 128
    var custom2: Int = 128
    var custom3: Int = 128
    var paletteId: Int = 0
    var effectId: Int = 0

    // SEGENV-like state
    var step: Long = 0L
    var call: Long = 0L
    var aux0: Int = 0
    var aux1: Int = 0
    var timeMs: Long = 0L

    private var randState: Int = 1

    fun resetEnv() {
        step = 0L
        call = 0L
        aux0 = 0
        aux1 = 0
        randState = 1 xor (effectId * 7919)
        fill(Color.Black)
    }

    fun resize(count: Int) {
        val c = count.coerceIn(8, 128)
        if (c != leds.size) {
            leds = Array(c) { Color.Black }
            resetEnv()
        }
    }

    fun fill(c: Color) {
        for (i in leds.indices) {
            leds[i] = c
        }
    }

    fun setPixel(i: Int, c: Color) {
        if (i in leds.indices) {
            leds[i] = c
        }
    }

    fun getPixel(i: Int): Color =
        if (i in leds.indices) leds[i] else Color.Black

    fun snapshot(): List<Color> = leds.toList()

    // -------------------------------------------------------------------------
    // Timing (FX-style helpers)
    // -------------------------------------------------------------------------

    /** speed 0..255 → continuous phase advance */
    fun beatPhase(scale: Float = 1f): Float {
        val sx = 0.25f + (speed / 255f) * 3.5f
        return (timeMs / 1000f) * sx * scale
    }

    fun beatsin8(beatsPerMinute: Int, lowest: Int = 0, highest: Int = 255): Int {
        val bpm = beatsPerMinute.coerceAtLeast(1)
        val angle = timeMs / 1000f * (bpm / 60f) * 2f * PI.toFloat()
        val s = (sin(angle) + 1f) * 0.5f
        return (lowest + s * (highest - lowest)).toInt().coerceIn(0, 255)
    }

    fun beat8(beatsPerMinute: Int): Int {
        val bpm = beatsPerMinute.coerceAtLeast(1)
        val v = timeMs / 1000f * (bpm / 60f) * 255f
        return (v % 255f).toInt().coerceIn(0, 255)
    }

    // -------------------------------------------------------------------------
    // Random (deterministic stream; good for stable preview)
    // -------------------------------------------------------------------------

    fun random8(): Int {
        randState = randState * 1103515245 + 12345
        return (randState ushr 16) and 0xFF
    }

    fun random8(lim: Int): Int {
        if (lim <= 1) return 0
        return random8() % lim
    }

    fun random16(): Int = (random8() shl 8) or random8()

    // -------------------------------------------------------------------------
    // Noise
    // -------------------------------------------------------------------------

    fun inoise8(x: Int): Int {
        val xf = x / 256f
        val i = floor(xf).toInt()
        val f = xf - i
        val a = hash8(i)
        val b = hash8(i + 1)
        val u = f * f * (3f - 2f * f)
        return (a + (b - a) * u).toInt().coerceIn(0, 255)
    }

    fun inoise8(x: Int, y: Int): Int = inoise8(x + y * 57)

    private fun hash8(i: Int): Float {
        var x = i * 374761393 + effectId * 668265263
        x = (x xor (x ushr 13)) * 1274126177
        return ((x xor (x ushr 16)) and 0xFF).toFloat()
    }

    // -------------------------------------------------------------------------
    // Pixel ops
    // -------------------------------------------------------------------------

    fun fadeToBlackBy(amount: Int) {
        val keep = 1f - (amount.coerceIn(0, 255) / 255f)
        for (i in leds.indices) {
            val c = leds[i]
            leds[i] = Color(c.red * keep, c.green * keep, c.blue * keep, 1f)
        }
    }

    fun fadePixelToBlackBy(i: Int, amount: Int) {
        if (i !in leds.indices) return
        val keep = 1f - amount.coerceIn(0, 255) / 255f
        val c = leds[i]
        leds[i] = Color(c.red * keep, c.green * keep, c.blue * keep, 1f)
    }

    fun blur1d(amount: Int) {
        if (n < 3) return
        val k = (amount.coerceIn(0, 255) / 255f) * 0.45f
        val src = snapshot()
        for (i in 1 until n - 1) {
            val l = src[i - 1]
            val c = src[i]
            val r = src[i + 1]
            leds[i] = Color(
                c.red * (1f - 2f * k) + (l.red + r.red) * k,
                c.green * (1f - 2f * k) + (l.green + r.green) * k,
                c.blue * (1f - 2f * k) + (l.blue + r.blue) * k,
                1f
            )
        }
    }

    // -------------------------------------------------------------------------
    // Color helpers
    // -------------------------------------------------------------------------

    fun hsv(h: Int, s: Int = 255, v: Int = 255): Color {
        return hsvFloat(((h % 256 + 256) % 256) * 360f / 255f, s / 255f, v / 255f)
    }

    fun hsvFloat(hDeg: Float, s: Float, v: Float): Color {
        val h = ((hDeg % 360f) + 360f) % 360f
        val sat = s.coerceIn(0f, 1f)
        val `val` = v.coerceIn(0f, 1f)
        val c = `val` * sat
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = `val` - c
        val (rp, gp, bp) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(rp + m, gp + m, bp + m, 1f)
    }

    fun lerpColor(a: Color, b: Color, t: Float): Color =
        lerp(a, b, t.coerceIn(0f, 1f))

    /**
     * Sample current palette.
     * Prefer project [WledPaletteCatalog] when available; fallback to built-in stops.
     */
    fun colorFromPalette(index: Int, brightness: Int = 255): Color {
        val t = (index and 0xFF) / 255f
        val base = samplePalette(paletteId, t, primary)
        val b = brightness.coerceIn(0, 255) / 255f
        return Color(base.red * b, base.green * b, base.blue * b, 1f)
    }

    fun samplePalette(pid: Int, t: Float, fallback: Color): Color {
        val u = ((t % 1f) + 1f) % 1f
        // Try project catalog first
        try {
            val pal = WledPaletteCatalog.get(pid)
            return pal.sampleAt(u)
        } catch (_: Throwable) {
            // fall through
        }
        return paletteColorFallback(pid, u, fallback)
    }

    private fun paletteColorFallback(pid: Int, u: Float, fallback: Color): Color {
        return when (pid) {
            0, 1 -> fallback
            2 -> hsvFloat(u * 360f, 1f, 1f)                          // Rainbow
            3 -> lerpColor(Color.Black, fallback, u)
            4 -> firePalette(u)                                      // Heat
            5 -> lerpColor(Color(0xFF001133), Color(0xFF66CCFF), u) // Ocean
            6 -> lerpColor(Color(0xFF1B5E20), Color(0xFFC8E6C9), u) // Forest
            7 -> lerpColor(Color(0xFF4A148C), Color(0xFFE1BEE7), u)
            8 -> lerpColor(Color(0xFFE65100), Color(0xFFFFF9C4), u)
            11 -> if (((u * 2f).toInt() % 2) == 0) Color(0xFFE53935) else Color(0xFF43A047)
            else -> hsvFloat((u * 360f + pid * 17f) % 360f, 0.85f, 1f)
        }
    }

    private fun firePalette(u: Float): Color = when {
        u < 0.33f -> Color(u / 0.33f, 0f, 0f)
        u < 0.66f -> Color(1f, (u - 0.33f) / 0.33f, 0f)
        else -> Color(1f, 1f, (u - 0.66f) / 0.34f)
    }
}