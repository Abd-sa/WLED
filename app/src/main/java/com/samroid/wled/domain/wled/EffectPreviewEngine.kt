package com.samroid.wled.domain.wled

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

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
//    fun render(params: Params, timeSec: Float): List<Color> {
//        runtime.resize(params.ledCount)
//        runtime.effectId = params.effectId.coerceIn(0, WledEffectCatalog.MAX_ID)
//        runtime.paletteId = params.paletteId
//        runtime.speed = params.speed.toInt().coerceIn(0, 255)
//        runtime.intensity = params.intensity.toInt().coerceIn(0, 255)
//        runtime.primary = params.primary
//        runtime.secondary = params.secondary
//        runtime.timeMs = (timeSec * 1000f).toLong().coerceAtLeast(0L)
//
//        EffectRegistry.render(runtime)
//        return runtime.snapshot()
//    }
    fun render(params: Params, timeSec: Float): List<Color> {
        val n = params.ledCount.coerceIn(8, 128)
        val t = timeSec
        val sp = (params.speed / 255f).coerceIn(0.05f, 1f)
        val ix = (params.intensity / 255f).coerceIn(0f, 1f)
        val id = params.effectId.coerceIn(0, WledEffectCatalog.MAX_ID.coerceAtLeast(159))
        val palette = WledPaletteCatalog.get(params.paletteId)

        fun pal(pos: Float) = palette.sampleAt(((pos % 1f) + 1f) % 1f)
        fun mix(a: Color, b: Color, u: Float): Color {
            val x = u.coerceIn(0f, 1f)
            return Color(
                a.red + (b.red - a.red) * x,
                a.green + (b.green - a.green) * x,
                a.blue + (b.blue - a.blue) * x,
                1f
            )
        }
        fun scale(c: Color, s: Float) =
            Color(c.red * s, c.green * s, c.blue * s, 1f)
        fun hash01(n: Int): Float {
            var x = n * 374761393 + 668265263
            x = (x xor (x ushr 13)) * 1274126177
            x = x xor (x ushr 16)
            return ((x ushr 1) and 0x7FFFFFFF) / 2147483647f

        }
        fun pixel(
            id: Int,
            i: Int,
            n: Int,
            x: Float,
            t: Float,
            sp: Float,
            ix: Float,
            c1: Color,
            c2: Color,
            pal: (Float) -> Color,
            mix: (Color, Color, Float) -> Color,
            scale: (Color, Float) -> Color
        ): Color {
            val speed = 0.3f + sp * 2.2f
            val phase = t * speed

            return when (id) {
                // 0 Solid
                0 -> c1

                // 1 Blink
                1 -> if (((phase * (0.5f + ix)) % 1f) < 0.5f) c1 else c2

                // 2 Breathe
                2 -> {
                    val b = (sin(phase * PI.toFloat()) * 0.5f + 0.5f)
                    mix(c2, c1, b)
                }

                // 3 Wipe / 4 Wipe Random
                3, 4 -> {
                    val head = ((phase * 0.35f) % 1.2f)
                    if (x < head) (if (id == 4) pal(x + phase * 0.1f) else c1) else c2
                }

                // 5 Random Colors
                5 -> {
                    val cell = floor(phase * (1f + ix * 3f) + i * 0.17f).toInt()
                    pal((cell * 0.13f) % 1f)
                }

                // 6 Sweep
                6 -> {
                    val p = ((x + phase * 0.4f) % 1f)
                    mix(c2, c1, if (p < 0.5f) p * 2f else (1f - p) * 2f)
                }

                // 7 Dynamic
                7 -> pal((x * 0.5f + phase * 0.15f + sin(i * 0.4f + phase) * 0.05f))

                // 8 Colorloop / 9 Rainbow
                8, 9 -> pal((x + phase * 0.25f) % 1f)

                // 10 Scan / 11 Dual Scan
                10, 11 -> {
                    val pos = ((sin(phase) * 0.5f + 0.5f))
                    val d = abs(x - pos)
                    val d2 = if (id == 11) abs(x - (1f - pos)) else 1f
                    val w = 0.04f + ix * 0.08f
                    val a = (1f - (d / w).coerceIn(0f, 1f)).coerceIn(0f, 1f)
                    val b = (1f - (d2 / w).coerceIn(0f, 1f)).coerceIn(0f, 1f)
                    mix(c2, c1, maxOf(a, b))
                }

                // 12 Fade
                12 -> mix(c1, c2, (sin(phase) * 0.5f + 0.5f))

                // 13 Theater / 14 Theater Rainbow
                13, 14 -> {
                    val step = 3 + (ix * 4).toInt()
                    val on = ((i + (phase * 8).toInt()) % step) == 0
                    if (on) (if (id == 14) pal(x + phase * 0.2f) else c1) else c2
                }

                // 15 Running / 37 Running 2
                15, 37 -> {
                    val wave = sin((x * 8f - phase * 3f) * PI.toFloat())
                    val a = (wave * 0.5f + 0.5f)
                    mix(c2, if (id == 37) pal(x) else c1, a)
                }

                // 16 Saw
                16 -> {
                    val s = ((x * 4f - phase) % 1f + 1f) % 1f
                    mix(c2, c1, s)
                }

                // 17 Twinkle / 80 Twinklefox / 81 Twinklecat / 106 Twinkleup
                17, 80, 81, 106 -> {
                    val spark = hash01(i * 17 + floor(phase * (2f + ix * 6f)).toInt())
                    if (spark > 0.92f - ix * 0.15f) scale(pal(spark), spark) else scale(c2, 0.15f)
                }

                // 18 Dissolve / 19 Dissolve Rnd
                18, 19 -> {
                    val thr = (sin(phase * 0.7f) * 0.5f + 0.5f)
                    val h = hash01(i * 31)
                    if (h < thr) (if (id == 19) pal(h) else c1) else c2
                }

                // 20–22 Sparkle family
                20, 21, 22 -> {
                    val s = hash01(i * 13 + floor(phase * 10f).toInt())
                    when {
                        s > 0.97f -> Color.White
                        id == 21 -> scale(c1, 0.25f)
                        else -> mix(c2, c1, 0.35f)
                    }
                }

                // 23–25 Strobe
                23, 24, 25 -> {
                    val duty = if (id == 25) 0.12f else 0.2f
                    val flash = ((phase * (3f + sp * 5f)) % 1f) < duty
                    when {
                        flash && id == 24 -> pal(phase)
                        flash -> c1
                        else -> c2
                    }
                }

                // 26 Blink Rainbow
                26 -> if (((phase * 2f) % 1f) < 0.5f) pal(phase * 0.3f) else c2

                // 27 Android
                27 -> {
                    val len = 0.1f + ix * 0.25f
                    val head = (phase * 0.3f) % 1f
                    if (x in head..(head + len) || x in (head - 1f)..(head - 1f + len)) c1 else c2
                }

                // 28–34 Chase family
                in 28..34 -> {
                    val head = ((phase * 0.5f) % 1f)
                    val d = minOf(abs(x - head), abs(x - head + 1f), abs(x - head - 1f))
                    val w = 0.06f + ix * 0.1f
                    val a = (1f - d / w).coerceIn(0f, 1f)
                    val col = when (id) {
                        30, 33 -> pal(x + phase * 0.2f)
                        29, 32 -> pal(hash01(floor(phase * 2).toInt()))
                        else -> c1
                    }
                    mix(c2, col, a)
                }

                // 35 Colorful / 36 Traffic Light
                35 -> pal((x * 3f + phase * 0.2f) % 1f)
                36 -> {
                    val seg = floor(x * 3f).toInt()
                    when (seg) {
                        0 -> Color.Red
                        1 -> Color.Yellow
                        else -> Color.Green
                    }.let { if (((phase.toInt() + seg) % 3) == 0) it else scale(it, 0.2f) }
                }

                // 38 Red & Blue / 39 Stream / 40 Scanner / 41 Lighthouse
                38 -> if (((i + (phase * 6).toInt()) % 2) == 0) Color.Red else Color.Blue
                39, 40, 41 -> {
                    val pos = (phase * 0.4f) % 1f
                    val d = abs(x - pos)
                    mix(c2, pal(x), (1f - d * (4f - ix * 2f)).coerceIn(0f, 1f))
                }

                // 42 Fireworks / 43 Rain / 88 Fireworks Starburst / 90 Fireworks 1D
                42, 43, 88, 90 -> {
                    val burst = hash01(i * 7 + floor(phase * 4f).toInt())
                    if (burst > 0.93f) Color.White
                    else if (burst > 0.85f) pal(burst)
                    else scale(c2, 0.1f)
                }

                // 44 Merry Christmas
                44 -> if ((i + (phase * 3).toInt()) % 2 == 0) Color.Red else Color(0xFF00AA00)

                // 45 Fire Flicker / 66 Fire 2012 / 35 Fire palette used
                45, 66 -> {
                    val heat = (
                            0.55f + 0.45f * sin(i * 0.35f + phase * 3f) +
                                    0.25f * hash01(i + floor(phase * 20).toInt())
                            ).coerceIn(0f, 1f)
                    // black -> red -> yellow -> white
                    when {
                        heat < 0.33f -> mix(Color.Black, Color.Red, heat / 0.33f)
                        heat < 0.66f -> mix(Color.Red, Color(0xFFFFAA00), (heat - 0.33f) / 0.33f)
                        else -> mix(Color(0xFFFFAA00), Color.White, (heat - 0.66f) / 0.34f)
                    }
                }

                // 46 Gradient / 47 Loading
                46 -> mix(c1, c2, x)
                47 -> {
                    val head = (phase * 0.5f) % 1f
                    if (x < head) c1 else c2
                }

                // 48–49 Police
                48, 49 -> {
                    val left = x < 0.5f
                    val on = ((phase * 4f).toInt() % 2 == 0)
                    when {
                        left && on -> Color.Red
                        !left && !on -> Color.Blue
                        else -> c2
                    }
                }

                // 50 Two Dots / 51 Two Areas / 52 Circus / 53 Halloween
                50, 51 -> {
                    val a = abs(x - ((phase * 0.3f) % 1f)) < 0.05f
                    val b = abs(x - ((phase * 0.3f + 0.5f) % 1f)) < 0.05f
                    when {
                        a -> c1
                        b -> Color.Blue
                        else -> c2
                    }
                }
                52 -> pal((floor(x * 6f) + phase) * 0.15f)
                53 -> if ((i + phase.toInt()) % 2 == 0) Color(0xFFFF6600) else Color(0xFF220033)

                // 54–56 Tri *
                in 54..56 -> pal((floor(x * 3f) / 3f + phase * 0.1f) % 1f)

                // 57 Lightning
                57 -> {
                    val bolt = hash01(floor(phase * 5f).toInt())
                    if (bolt > 0.92f && abs(x - bolt) < 0.15f) Color.White else scale(c2, 0.05f)
                }

                // 58 ICU
                58 -> {
                    val eye1 = abs(x - 0.35f) < 0.04f
                    val eye2 = abs(x - 0.65f) < 0.04f
                    if ((eye1 || eye2) && sin(phase * 2f) > 0f) c1 else c2
                }

                // 59 Multi Comet / 60 Scanner Dual / 61 Stream 2
                in 59..61 -> {
                    val heads = listOf(0f, 0.33f, 0.66f).map { (it + phase * 0.35f) % 1f }
                    val a = heads.minOf { abs(x - it).coerceAtMost(abs(x - it + 1f)) }
                    mix(c2, pal(x), (1f - a * 8f).coerceIn(0f, 1f))
                }

                // 62 Oscillate
                62 -> mix(c2, c1, (sin(x * PI.toFloat() * 2f + phase) * 0.5f + 0.5f))

                // 63 Pride 2015 / 64 Juggle / 65 Palette / 67 Colorwaves / 68 BPM
                63, 65, 67 -> pal((x + phase * 0.2f + sin(phase + i * 0.1f) * 0.05f) % 1f)
                64 -> {
                    val dots = 4
                    var a = 0f
                    for (d in 0 until dots) {
                        val pos = (sin(phase * (1f + d * 0.3f) + d) * 0.5f + 0.5f)
                        a = maxOf(a, (1f - abs(x - pos) * 12f).coerceIn(0f, 1f))
                    }
                    mix(c2, pal(phase * 0.1f), a)
                }
                68 -> {
                    val beat = (sin(phase * (1f + sp * 2f) * PI.toFloat()) * 0.5f + 0.5f)
                    scale(pal(x * 0.5f + phase * 0.05f), 0.3f + beat * 0.7f)
                }

                // 69–73 Noise family
                in 69..73, 107 -> {
                    val n1 = sin(i * 0.4f + phase * 1.3f)
                    val n2 = cos(i * 0.27f - phase * 0.9f)
                    pal(((n1 + n2) * 0.25f + 0.5f + phase * 0.05f) % 1f)
                }

                // 74 Colortwinkles / 75 Lake
                74 -> {
                    val s = hash01(i * 9 + floor(phase * 3f).toInt())
                    if (s > 0.88f) pal(s) else scale(c2, 0.12f)
                }
                75 -> {
                    val wave = sin(x * PI.toFloat() * 3f - phase * 1.5f) * 0.5f + 0.5f
                    mix(Color(0xFF001122), pal(0.55f), wave)
                }

                // 76–77 Meteor
                76, 77 -> {
                    val head = (phase * 0.45f) % 1f
                    val trail = 0.12f + ix * 0.2f
                    val d = ((head - x + 1f) % 1f)
                    if (d < trail) scale(if (id == 77) pal(x) else c1, 1f - d / trail) else c2
                }

                // 78 Railway / 79 Ripple / 99 Ripple Rainbow
                78 -> if (((i / 2) + (phase * 3).toInt()) % 2 == 0) c1 else c2
                79, 99 -> {
                    val center = (phase * 0.3f) % 1f
                    val d = abs(x - center)
                    val ring = abs(d - ((phase * 0.5f) % 0.5f))
                    mix(c2, if (id == 99) pal(x) else c1, (1f - ring * 15f).coerceIn(0f, 1f))
                }

                // 82 Halloween Eyes
                82 -> {
                    val blink = sin(phase * 1.5f) > 0.3f
                    val e1 = abs(x - 0.3f) < 0.03f
                    val e2 = abs(x - 0.7f) < 0.03f
                    if (blink && (e1 || e2)) Color(0xFFFF2200) else c2
                }

                // 83–84 Solid Pattern
                83, 84 -> {
                    val on = 2 + (ix * 6).toInt()
                    val off = 1 + ((1f - ix) * 4).toInt()
                    val m = on + off
                    if ((i % m) < on) (if (id == 84) listOf(c1, c2, Color.Blue)[i % 3] else c1) else Color.Black
                }

                // 85–86 Spots
                85, 86 -> {
                    val spacing = 4 + ((1f - ix) * 8).toInt()
                    val on = (i % spacing) == ((phase * 2).toInt() % spacing)
                    if (on) c1 else if (id == 86) scale(c1, 0.15f) else c2
                }

                // 87 Glitter / 103 Solid Glitter
                87, 103 -> {
                    val base = if (id == 103) c1 else c2
                    val g = hash01(i * 3 + floor(phase * 15f).toInt())
                    if (g > 0.96f) Color.White else base
                }

                // 88–89 Candle / Candle Multi handled above fireworks; 89 Candle
                89, 102 -> {
                    val flicker = 0.7f + 0.3f * hash01(floor(phase * 25f).toInt() + i / 3)
                    scale(Color(0xFFFFAA44), flicker)
                }

                // 91 Bouncing Balls
                91 -> {
                    val balls = 1 + (ix * 4).toInt()
                    var a = 0f
                    for (b in 0 until balls) {
                        val pos = abs(sin(phase * (1.2f + b * 0.35f) + b))
                        a = maxOf(a, (1f - abs(x - pos) * 14f).coerceIn(0f, 1f))
                    }
                    mix(c2, pal(phase * 0.1f), a)
                }

                // 92–94 Sinelon
                in 92..94 -> {
                    val pos = sin(phase * 1.4f) * 0.5f + 0.5f
                    val a = (1f - abs(x - pos) * (10f - ix * 4f)).coerceIn(0f, 1f)
                    mix(scale(c2, 0.2f), if (id == 94) pal(x) else c1, a)
                }

                // 95 Popcorn / 96 Drip
                95, 96 -> {
                    val pos = abs(sin(phase * 1.6f + i * 0.02f))
                    val a = (1f - abs(x - pos) * 12f).coerceIn(0f, 1f)
                    mix(c2, pal(pos), a)
                }

                // 97 Plasma / 98 Percent
                97 -> {
                    val v = sin(x * 6f + phase) + sin(x * 3f - phase * 1.3f)
                    pal((v * 0.25f + 0.5f) % 1f)
                }
                98 -> if (x < ix) c1 else c2

                // 100 Heartbeat
                100 -> {
                    val beat = abs(sin(phase * PI.toFloat() * (1.2f + sp)))
                    val pulse = beat.pow(3f)
                    scale(c1, 0.2f + pulse * 0.8f)
                }

                // 101 Pacifica
                101 -> {
                    val w1 = sin(x * 4f + phase * 0.8f)
                    val w2 = sin(x * 7f - phase * 1.1f)
                    val v = (w1 + w2) * 0.25f + 0.5f
                    mix(Color(0xFF001A33), Color(0xFF66CCFF), v)
                }

                // 104 Sunrise
                104 -> {
                    val sun = (phase * 0.08f) % 1f
                    when {
                        x < sun - 0.15f -> Color(0xFF001133)
                        x < sun -> mix(Color(0xFFFF6600), Color(0xFFFFEE88), (x - (sun - 0.15f)) / 0.15f)
                        else -> Color(0xFF87CEEB).let { scale(it, 0.3f + 0.7f * (1f - x)) }
                    }
                }

                // 105 Phased / 108 Sine / 109 Phased Noise / 110 Flow
                105, 108, 109, 110 -> {
                    val v = sin((x * (2f + ix * 4f) - phase * 2f) * PI.toFloat()) * 0.5f + 0.5f
                    mix(c2, pal(x + phase * 0.05f), v)
                }

                // 111 Chunchun / 112 Dancing Shadows
                111, 112 -> {
                    val a = abs(sin(x * 5f + phase * 1.5f))
                    mix(c2, pal(phase * 0.1f + x * 0.2f), a)
                }

                // default: moving palette (good fallback for 113–159)
                else -> {
                    // Slight variation by id so different effects don't look identical
                    val k = 1f + (id % 7) * 0.15f
                    val wobble = sin(i * 0.2f * k + phase * k) * 0.08f
                    pal((x * (0.5f + (id % 5) * 0.1f) + phase * 0.2f + wobble) % 1f)
                }
            }
        }

        // Palette 0/1 ≈ default / solid → keep primary; others tint from palette
        val usePalette = params.paletteId > 1

        return List(n) { i ->
            val x = i / (n - 1).coerceAtLeast(1).toFloat()
            // Foreground from palette so changing palette is always visible
            val c1 = if (usePalette) pal(x) else params.primary
            val c2 = params.secondary
            //if (usePalette) pal(x) else params.primary
            pixel(id, i, n, x, t, sp, ix, c1, c2, ::pal, ::mix, ::scale)
        }
    }
}