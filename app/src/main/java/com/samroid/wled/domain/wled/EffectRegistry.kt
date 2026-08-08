package com.samroid.wled.domain.wled

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Full registry for client effect list — every id has a preview renderer.
 * Algorithms approximate WLED FX.cpp 1D behaviour for UI (not byte-identical firmware).
 */
object EffectRegistry {

    val MAX_EFFECT_ID: Int get() = WledEffectCatalog.MAX_ID

    fun interface EffectFn {
        fun render(rt: WledRuntime)
    }

    private val handlers: Array<EffectFn> =
        Array(WledEffectCatalog.NAMES.size) { id -> resolve(id, WledEffectCatalog.NAMES[id]) }

    init {
        check(handlers.size == WledEffectCatalog.NAMES.size)
        handlers.forEachIndexed { i, h ->
            requireNotNull(h) { "gap at $i" }
        }
    }

    fun render(rt: WledRuntime) {
        val id = rt.effectId.coerceIn(0, MAX_EFFECT_ID)
        handlers[id].render(rt)
        rt.call++
        rt.step++
    }

    // ------------------------------------------------------------------
    // Name → implementation
    // ------------------------------------------------------------------

    private fun resolve(id: Int, name: String): EffectFn = when (name) {
        "Solid" -> fx { it.fill(it.primary) }
        "Blink" -> fx(::blink)
        "Breathe" -> fx(::breathe)
        "Wipe" -> fx { wipe(it, random = false, pingpong = false) }
        "Wipe Random" -> fx { wipe(it, random = true, pingpong = false) }
        "Random Colors" -> fx(::randomColors)
        "Sweep" -> fx { wipe(it, random = false, pingpong = true) }
        "Dynamic" -> fx { dynamic(it, smooth = false) }
        "Colorloop" -> fx(::colorloop)
        "Rainbow" -> fx(::rainbow)
        "Scan" -> fx { scan(it, dual = false) }
        "Scan Dual" -> fx { scan(it, dual = true) }
        "Fade" -> fx(::fade)
        "Theater" -> fx { chase(it, rainbow = false, gap = 3) }
        "Theater Rainbow" -> fx { chase(it, rainbow = true, gap = 3) }
        "Running" -> fx { running(it, dual = false) }
        "Saw" -> fx(::saw)
        "Twinkle" -> fx { twinkle(it, densityScale = 1f) }
        "Dissolve" -> fx { dissolve(it, random = false) }
        "Dissolve Rnd" -> fx { dissolve(it, random = true) }
        "Sparkle" -> fx { sparkle(it, dense = false, dark = false) }
        "Sparkle Dark" -> fx { sparkle(it, dense = false, dark = true) }
        "Sparkle+" -> fx { sparkle(it, dense = true, dark = false) }
        "Strobe" -> fx { strobe(it, rainbow = false, mega = false) }
        "Strobe Rainbow" -> fx { strobe(it, rainbow = true, mega = false) }
        "Strobe Mega" -> fx { strobe(it, rainbow = false, mega = true) }
        "Blink Rainbow" -> fx(::blinkRainbow)
        "Android" -> fx(::android)
        "Chase" -> fx { chase(it, rainbow = false, gap = 1) }
        "Chase Random" -> fx { chase(it, rainbow = false, gap = 1, randomHue = true) }
        "Chase Rainbow" -> fx { chase(it, rainbow = true, gap = 1) }
        "Chase Flash" -> fx { chaseFlash(it, rnd = false) }
        "Chase Flash Rnd" -> fx { chaseFlash(it, rnd = true) }
        "Rainbow Runner" -> fx(::rainbowRunner)
        "Colorful" -> fx(::colorful)
        "Traffic Light" -> fx(::trafficLight)
        "Sweep Random" -> fx { wipe(it, random = true, pingpong = true) }
        "Chase 2" -> fx { chase(it, rainbow = false, gap = 2) }
        "Aurora" -> fx(::aurora)
        "Stream" -> fx { stream(it, variant = 1) }
        "Scanner" -> fx { scanner(it, dual = false) }
        "Lighthouse" -> fx(::lighthouse)
        "Fireworks" -> fx { fireworks(it, starburst = false) }
        "Rain" -> fx(::rain)
        "Tetrix" -> fx(::tetrix)
        "Fire Flicker" -> fx(::fireFlicker)
        "Gradient" -> fx(::gradient)
        "Loading" -> fx(::loading)
        "Rolling Balls" -> fx(::rollingBalls)
        "Fairy" -> fx { fairy(it, multi = false) }
        "Two Dots" -> fx(::twoDots)
        "Fairytwinkle" -> fx { fairy(it, multi = true) }
        "Running Dual" -> fx { running(it, dual = true) }
        "Chase 3" -> fx { chase(it, rainbow = false, gap = 4) }
        "Tri Wipe" -> fx(::triWipe)
        "Tri Fade" -> fx(::triFade)
        "Lightning" -> fx(::lightning)
        "ICU" -> fx(::icu)
        "Multi Comet" -> fx(::multiComet)
        "Scanner Dual" -> fx { scanner(it, dual = true) }
        "Stream 2" -> fx { stream(it, variant = 2) }
        "Oscillate" -> fx(::oscillate)
        "Pride 2015" -> fx(::pride2015)
        "Juggle" -> fx { juggle(it, count = 8) }
        "Palette" -> fx(::paletteMove)
        "Fire 2012" -> fx(::fire2012)
        "Colorwaves" -> fx(::colorwaves)
        "Bpm" -> fx(::bpm)
        "Fill Noise" -> fx { noiseBand(it, mode = 0) }
        "Noise 1" -> fx { noiseBand(it, mode = 1) }
        "Noise 2" -> fx { noiseBand(it, mode = 2) }
        "Noise 3" -> fx { noiseBand(it, mode = 3) }
        "Noise 4" -> fx { noiseBand(it, mode = 4) }
        "Colortwinkles" -> fx(::colorTwinkles)
        "Lake" -> fx(::lake)
        "Meteor" -> fx { meteor(it, smooth = false) }
        "Copy Segment" -> fx { meteor(it, smooth = true) } // preview: smooth trail
        "Railway" -> fx(::railway)
        "Ripple" -> fx { ripple(it, rainbow = false) }
        "Twinklefox" -> fx { twinklePalette(it, cool = false) }
        "Twinklecat" -> fx { twinklePalette(it, cool = true) }
        "Halloween Eyes" -> fx(::halloweenEyes)
        "Solid Pattern" -> fx { solidPattern(it, tri = false) }
        "Solid Pattern Tri" -> fx { solidPattern(it, tri = true) }
        "Spots" -> fx { spots(it, fade = false) }
        "Spots Fade" -> fx { spots(it, fade = true) }
        "Glitter" -> fx(::glitter)
        "Candle" -> fx { candle(it, multi = false) }
        "Fireworks Starburst" -> fx { fireworks(it, starburst = true) }
        "Fireworks 1D" -> fx { fireworks(it, starburst = false) }
        "Bouncing Balls" -> fx(::bouncingBalls)
        "Sinelon" -> fx { sinelon(it, dual = false, rainbow = false) }
        "Sinelon Dual" -> fx { sinelon(it, dual = true, rainbow = false) }
        "Sinelon Rainbow" -> fx { sinelon(it, dual = false, rainbow = true) }
        "Popcorn" -> fx(::popcorn)
        "Drip" -> fx(::drip)
        "Plasma" -> fx(::plasma)
        "Percent" -> fx(::percent)
        "Ripple Rainbow" -> fx { ripple(it, rainbow = true) }
        "Heartbeat" -> fx(::heartbeat)
        "Pacifica" -> fx(::pacifica)
        "Candle Multi" -> fx { candle(it, multi = true) }
        "Solid Glitter" -> fx(::solidGlitter)
        "Sunrise" -> fx(::sunrise)
        "Phased" -> fx { phased(it, noise = false) }
        "Twinkleup" -> fx(::twinkleUp)
        "Noise Pal" -> fx { noiseBand(it, mode = 5) }
        "Sine" -> fx(::sineBars)
        "Phased Noise" -> fx { phased(it, noise = true) }
        "Flow" -> fx(::flow)
        "Chunchun" -> fx(::chunchun)
        "Dancing Shadows" -> fx(::dancingShadows)
        "Washing Machine" -> fx(::washingMachine)
        "Rotozoomer" -> fx(::rotozoomer)
        "Blends" -> fx(::blends)
        "TV Simulator" -> fx(::tvSimulator)
        "Dynamic Smooth" -> fx { dynamic(it, smooth = true) }
        "Spaceships" -> fx(::spaceships)
        "Crazy Bees" -> fx(::crazyBees)
        "Ghost Rider" -> fx(::ghostRider)
        "Blobs" -> fx(::blobs)
        "Scrolling Text" -> fx(::scrollingTextProxy) // 1D proxy
        "Drift Rose" -> fx(::driftRose)
        "Distortion Waves" -> fx(::distortionWaves)
        "Soap" -> fx(::soap)
        "Octopus" -> fx(::octopus)
        "Waving Cell" -> fx(::wavingCell)
        "Pixels", "Pixelwave", "Juggles", "Matripix", "Gravimeter", "Plasmoid",
        "Puddles", "Midnoise", "Noisemeter", "Freqwave", "Freqmatrix", "GEQ",
        "Waterfall", "Freqpixels", "Noisefire", "Puddlepeak", "Noisemove", "Noise2D",
        "Perlin Move", "Ripple Peak", "Firenoise", "Squared Swirl", "Freqmap",
        "Gravcenter", "Gravcentric", "Gravfreq", "DJ Light", "Funky Plank"
            -> fx { audioOr2dProxy(it, id) }
        "PacMan" -> fx(::pacMan)
        "DNA" -> fx { dna(it, spiral = false) }
        "Matrix" -> fx(::matrixRain)
        "Metaballs" -> fx(::metaballs)
        "Shimmer" -> fx(::shimmer)
        "Pulser" -> fx(::pulser)
        "Blurz" -> fx(::blurz)
        "Drift" -> fx(::drift)
        "Waverly" -> fx(::waverly)
        "Sun Radiation" -> fx(::sunRadiation)
        "Colored Bursts" -> fx(::coloredBursts)
        "Julia" -> fx(::juliaProxy)
        "Game Of Life" -> fx(::gameOfLife1d)
        "Tartan" -> fx(::tartan)
        "Polar Lights" -> fx(::polarLights)
        "Swirl" -> fx(::swirl)
        "Lissajous" -> fx(::lissajous)
        "Frizzles" -> fx(::frizzles)
        "Plasma Ball" -> fx(::plasmaBall)
        "Flow Stripe" -> fx(::flowStripe)
        "Hiphotic" -> fx(::hiphotic)
        "Sindots" -> fx(::sindots)
        "DNA Spiral" -> fx { dna(it, spiral = true) }
        "Black Hole" -> fx(::blackHole)
        "Wavesins" -> fx(::wavesins)
        "Rocktaves" -> fx(::rocktaves)
        "Akemi" -> fx(::akemi)
        "Color Clouds" -> fx(::colorClouds)
        "Slow Transition" -> fx(::slowTransition)
        // Particle-system family → dedicated 1D proxies
        "PS DripDrop" -> fx(::drip)
        "PS Pinball" -> fx(::rollingBalls)
        "PS Dancing Shadows" -> fx(::dancingShadows)
        "PS Fireworks 1D" -> fx { fireworks(it, starburst = false) }
        "PS Sparkler" -> fx { sparkle(it, dense = true, dark = true) }
        "PS Hourglass" -> fx(::hourglass)
        "PS Spray 1D" -> fx(::spray1d)
        "PS 1D Balance" -> fx(::balance1d)
        "PS Chase" -> fx { chase(it, rainbow = false, gap = 1) }
        "PS Starburst" -> fx { fireworks(it, starburst = true) }
        "PS GEQ 1D" -> fx { audioOr2dProxy(it, id) }
        "PS Fire 1D" -> fx(::fire2012)
        "PS Sonic Stream" -> fx { stream(it, variant = 2) }
        "PS Sonic Boom" -> fx(::sonicBoom)
        "PS Springy" -> fx(::springy)
        "RSVD" -> fx { rsvd(it, id) }
        else -> fx { audioOr2dProxy(it, id) }
    }

    private fun fx(block: (WledRuntime) -> Unit) = EffectFn { block(it) }

    // ===================== implementations =====================

    private fun blink(rt: WledRuntime) {
        val duty = 0.2f + rt.intensity / 255f * 0.6f
        val period = 0.3f + (1f - rt.speed / 255f) * 1.2f
        val on = ((rt.timeMs / 1000f) / period) % 1f < duty
        rt.fill(if (on) rt.primary else rt.secondary)
    }

    private fun breathe(rt: WledRuntime) {
        val v = rt.beatsin8(6 + rt.speed / 10, 15, 255)
        rt.fill(rt.lerpColor(rt.secondary, rt.primary, v / 255f))
    }

    private fun wipe(rt: WledRuntime, random: Boolean, pingpong: Boolean) {
        val n = rt.n
        val sp = 0.45f + rt.speed / 255f * 3.2f
        val cycle = if (pingpong) n * 2 else n
        val pos = ((rt.timeMs / 1000f * sp * n) % cycle).toInt()
        val going = !pingpong || pos < n
        val edge = if (going) pos % n else (2 * n - pos - 1)
        for (i in 0 until n) {
            val filled = if (going) i <= edge else i >= edge
            rt.setPixel(
                i,
                when {
                    !filled -> rt.secondary
                    random -> rt.hsv((i * 255 / n + (rt.timeMs / 20).toInt()) and 0xFF)
                    else -> rt.primary
                }
            )
        }
    }

    private fun randomColors(rt: WledRuntime) {
        val step = (rt.timeMs / (45 + (255 - rt.speed) / 2)).toInt()
        for (i in 0 until rt.n) {
            rt.setPixel(i, rt.hsv(((step * 31 + i * 17) * 47) and 0xFF, 220, 255))
        }
    }

    private fun dynamic(rt: WledRuntime, smooth: Boolean) {
        val step = (rt.timeMs / (if (smooth) 70 else 40 + (255 - rt.speed) / 3)).toInt()
        val all = rt.intensity > 127
        for (i in 0 until rt.n) {
            val seed = if (all) step else step + i * 13
            var c = rt.hsv((seed * 97) and 0xFF)
            if (smooth) c = rt.lerpColor(rt.getPixel(i), c, 0.25f)
            rt.setPixel(i, c)
        }
    }

    private fun colorloop(rt: WledRuntime) {
        val sat = if (rt.intensity < 128) 150 else 255
        rt.fill(rt.hsv(((rt.timeMs / 8) * (1 + rt.speed / 28)).toInt() and 0xFF, sat, 255))
    }

    private fun rainbow(rt: WledRuntime) {
        val bands = 1 + rt.intensity / 64
        val off = ((rt.timeMs / 8) * (1 + rt.speed / 22)).toInt()
        for (i in 0 until rt.n) {
            rt.setPixel(i, rt.hsv((i * 255 * bands / rt.n + off) and 0xFF))
        }
    }

    private fun scan(rt: WledRuntime, dual: Boolean) {
        rt.fill(rt.secondary)
        val pos = rt.beatsin8(5 + rt.speed / 18, 0, rt.n - 1)
        val w = 1 + rt.intensity / 45
        fun mark(p: Int) {
            for (d in 0..w) {
                val c = rt.lerpColor(rt.primary, Color.Companion.White, 1f - d / (w + 1f))
                if (p + d < rt.n) rt.setPixel(p + d, c)
                if (p - d >= 0) rt.setPixel(p - d, c)
            }
        }
        mark(pos)
        if (dual) mark(rt.n - 1 - pos)
    }

    private fun fade(rt: WledRuntime) {
        val v = rt.beatsin8(5 + rt.speed / 14)
        rt.fill(rt.lerpColor(rt.secondary, rt.primary, v / 255f))
    }

    private fun chase(
        rt: WledRuntime,
        rainbow: Boolean,
        gap: Int,
        randomHue: Boolean = false
    ) {
        val n = rt.n
        val size = (1 + rt.intensity / 50).coerceAtLeast(1)
        val pos = ((rt.timeMs / (28 + (255 - rt.speed) / 3)) % n).toInt()
        for (i in 0 until n) {
            val d = (i - pos + n) % n
            val on = d < size || (gap > 1 && d % gap == 0 && d < size * gap)
            rt.setPixel(
                i,
                when {
                    !on -> rt.secondary
                    rainbow -> rt.hsv((i * 255 / n + (rt.timeMs / 18).toInt()) and 0xFF)
                    randomHue -> rt.hsv(((pos + i) * 53) and 0xFF)
                    else -> rt.primary
                }
            )
        }
    }

    private fun running(rt: WledRuntime, dual: Boolean) {
        val wave = rt.beatPhase(1.15f)
        for (i in 0 until rt.n) {
            val a = sin((i / rt.n.toFloat() * 4f + wave) * PI.toFloat())
            val b = if (dual) sin((i / rt.n.toFloat() * 4f - wave) * PI.toFloat()) else -2f
            val on = a > 0.15f || b > 0.15f
            rt.setPixel(i, if (on) rt.primary else rt.secondary)
        }
    }

    private fun saw(rt: WledRuntime) {
        val pos = rt.beatPhase(0.85f) % 1f
        for (i in 0 until rt.n) {
            val x = (i / (rt.n - 1f).coerceAtLeast(1f) + pos) % 1f
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, x))
        }
    }

    private fun twinkle(rt: WledRuntime, densityScale: Float) {
        rt.fill(rt.secondary)
        val dens = ((1 + rt.intensity / 14) * densityScale).toInt().coerceAtLeast(1)
        val seed = (rt.timeMs / (38 + (255 - rt.speed) / 2)).toInt()
        repeat(dens) { k ->
            val i = abs(seed * 73 + k * 19) % rt.n
            val tw = rt.beatsin8(10 + k, 60, 255)
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, tw / 255f))
        }
    }

    private fun dissolve(rt: WledRuntime, random: Boolean) {
        val step = (rt.timeMs / (48 + (255 - rt.speed) / 2)).toInt()
        for (i in 0 until rt.n) {
            val h = abs((i * 7 + step * 3) * 2654435761L).toInt() and 0xFF
            val use = if (random) (h and 1) == 0 else h < rt.intensity
            rt.setPixel(i, if (use) rt.primary else rt.secondary)
        }
    }

    private fun sparkle(rt: WledRuntime, dense: Boolean, dark: Boolean) {
        rt.fill(if (dark) Color.Companion.Black else rt.lerpColor(rt.secondary, rt.primary, 0.22f))
        val count = if (dense) 2 + rt.intensity / 18 else 1 + rt.intensity / 40
        val seed = (rt.timeMs / (22 + (255 - rt.speed) / 4)).toInt()
        repeat(count) { k -> rt.setPixel(abs(seed * 29 + k * 47) % rt.n, Color.Companion.White) }
    }

    private fun strobe(rt: WledRuntime, rainbow: Boolean, mega: Boolean) {
        val period = if (mega) 70 else 110 + (255 - rt.speed)
        val on = (rt.timeMs % period) < period / 3
        rt.fill(
            when {
                !on -> rt.secondary
                rainbow -> rt.hsv(((rt.timeMs / 9) % 256).toInt())
                else -> rt.primary
            }
        )
    }

    private fun blinkRainbow(rt: WledRuntime) {
        val period = 180 + (255 - rt.speed)
        val on = (rt.timeMs % period) < period / 2
        rt.fill(if (on) rt.hsv(((rt.timeMs / 18) % 256).toInt()) else rt.secondary)
    }

    private fun android(rt: WledRuntime) {
        val len = 2 + rt.intensity / 22
        val pos = ((rt.timeMs / (32 + (255 - rt.speed) / 3)) % rt.n).toInt()
        rt.fill(rt.secondary)
        for (d in 0 until len) rt.setPixel((pos + d) % rt.n, rt.primary)
    }

    private fun chaseFlash(rt: WledRuntime, rnd: Boolean) {
        chase(rt, rainbow = false, gap = 1, randomHue = rnd)
        if ((rt.timeMs / 40) % 7L == 0L) {
            val i = (rt.timeMs / 40).toInt() % rt.n
            rt.setPixel(i, Color.Companion.White)
        }
    }

    private fun rainbowRunner(rt: WledRuntime) {
        val size = 3 + rt.intensity / 28
        val pos = ((rt.timeMs / (28 + (255 - rt.speed) / 3)) % rt.n).toInt()
        rt.fill(Color.Companion.Black)
        for (d in 0 until size)
            rt.setPixel((pos + d) % rt.n, rt.hsv((d * 28 + (rt.timeMs / 14).toInt()) and 0xFF))
    }

    private fun colorful(rt: WledRuntime) {
        val off = (rt.timeMs / (18 + (255 - rt.speed) / 4)).toInt()
        for (i in 0 until rt.n) rt.setPixel(i, rt.hsv((i * 47 + off) and 0xFF))
    }

    private fun trafficLight(rt: WledRuntime) {
        val phase = ((rt.timeMs / (400 + (255 - rt.speed) * 2)) % 3).toInt()
        val c = when (phase) {
            0 -> Color(0xFFE53935)
            1 -> Color(0xFFFDD835)
            else -> Color(0xFF43A047)
        }
        rt.fill(rt.secondary)
        val third = rt.n / 3
        val start = phase * third
        for (i in start until min(start + third, rt.n)) rt.setPixel(i, c)
    }

    private fun aurora(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val n1 = rt.inoise8(i * 12 + (rt.timeMs / 20).toInt())
            val n2 = rt.inoise8(i * 6 + (rt.timeMs / 35).toInt() + 50)
            val h = (140 + n1 / 4 + rt.intensity / 8) and 0xFF
            rt.setPixel(i, rt.hsv(h, 200, (80 + n2 / 2).coerceAtMost(255)))
        }
    }

    private fun stream(rt: WledRuntime, variant: Int) {
        val sp = 20 + (255 - rt.speed) / 2
        val pos = (rt.timeMs / sp).toInt()
        for (i in 0 until rt.n) {
            val d = if (variant == 1) (i + pos) else (i * 2 + pos)
            rt.setPixel(i, rt.colorFromPalette(d and 0xFF))
        }
    }

    private fun scanner(rt: WledRuntime, dual: Boolean) {
        rt.fadeToBlackBy(50)
        val pos = rt.beatsin8(4 + rt.speed / 20, 0, rt.n - 1)
        rt.setPixel(pos, rt.primary)
        if (dual) rt.setPixel(rt.n - 1 - pos, rt.secondary)
    }

    private fun lighthouse(rt: WledRuntime) {
        rt.fadeToBlackBy(40)
        val pos = ((rt.timeMs / (24 + (255 - rt.speed) / 4)) % rt.n).toInt()
        for (d in 0..3) {
            val f = 1f - d / 4f
            val c = Color(rt.primary.red * f, rt.primary.green * f, rt.primary.blue * f)
            rt.setPixel((pos + d) % rt.n, c)
        }
    }

    private fun fireworks(rt: WledRuntime, starburst: Boolean) {
        rt.fadeToBlackBy(36 + (255 - rt.intensity) / 10)
        val count = 1 + rt.intensity / 55
        val sp = 0.9f + rt.speed / 255f * 2.2f
        repeat(count) { k ->
            val age = ((rt.timeMs / 1000f * sp) + k * 0.41f) % 1f
            val center = abs(k * 97 + (rt.timeMs / 380).toInt() * 13) % rt.n
            val radius = (age * rt.n * if (starburst) 0.3f else 0.2f).toInt()
            val fade = 1f - age
            for (d in 0..radius) {
                val f = fade * (1f - d / (radius + 1f))
                val c = rt.lerpColor(
                    Color.Companion.Black,
                    if (k % 2 == 0) rt.primary else rt.hsv((k * 45) and 0xFF),
                    f
                )
                rt.setPixel((center + d).coerceIn(0, rt.n - 1), c)
                rt.setPixel((center - d).coerceIn(0, rt.n - 1), c)
            }
        }
    }

    private fun rain(rt: WledRuntime) {
        rt.fadeToBlackBy(40)
        val drops = 2 + rt.intensity / 30
        val seed = (rt.timeMs / (30 + (255 - rt.speed) / 3)).toInt()
        repeat(drops) { k ->
            val i = abs(seed * 17 + k * 53) % rt.n
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, 0.7f))
        }
    }

    private fun tetrix(rt: WledRuntime) {
        rt.fadeToBlackBy(25)
        val cell = max(2, rt.n / 10)
        val step = (rt.timeMs / (120 + (255 - rt.speed))).toInt()
        val col = (step % (rt.n / cell + 1)) * cell
        val h = (step * 37) and 0xFF
        for (i in 0 until cell) {
            val x = (col + i).coerceIn(0, rt.n - 1)
            rt.setPixel(x, rt.hsv(h))
        }
    }

    private fun fireFlicker(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val f = 0.45f + 0.55f * (rt.inoise8(i * 18 + (rt.timeMs / (7 + (255 - rt.speed) / 10)).toInt()) / 255f)
            val heat = (0.35f + rt.intensity / 255f * 0.65f) * f
            rt.setPixel(
                i,
                Color(
                    (rt.primary.red * heat).coerceIn(0f, 1f),
                    (rt.primary.green * heat * 0.5f).coerceIn(0f, 1f),
                    (rt.primary.blue * heat * 0.12f).coerceIn(0f, 1f)
                )
            )
        }
    }

    private fun gradient(rt: WledRuntime) {
        for (i in 0 until rt.n)
            rt.setPixel(i, rt.lerpColor(rt.primary, rt.secondary, i / (rt.n - 1f).coerceAtLeast(1f)))
    }

    private fun loading(rt: WledRuntime) {
        rt.fill(rt.secondary)
        val len = (rt.n * (0.15f + rt.intensity / 255f * 0.35f)).toInt().coerceAtLeast(2)
        val pos = ((rt.timeMs / (22 + (255 - rt.speed) / 4)) % rt.n).toInt()
        for (d in 0 until len) {
            val f = 1f - d / len.toFloat()
            val i = (pos + d) % rt.n
            rt.setPixel(i, Color(rt.primary.red * f, rt.primary.green * f, rt.primary.blue * f))
        }
    }

    private fun rollingBalls(rt: WledRuntime) {
        rt.fadeToBlackBy(45)
        val balls = 2 + rt.intensity / 60
        repeat(balls) { k ->
            val pos = rt.beatsin8(6 + k + rt.speed / 30, 0, rt.n - 1)
            rt.setPixel(pos, rt.hsv((k * 60) and 0xFF))
        }
    }

    private fun fairy(rt: WledRuntime, multi: Boolean) {
        rt.fadeToBlackBy(30)
        val n = if (multi) 4 + rt.intensity / 40 else 2 + rt.intensity / 50
        val seed = (rt.timeMs / (36 + (255 - rt.speed) / 3)).toInt()
        repeat(n) { k ->
            val i = abs(seed * 19 + k * 41) % rt.n
            rt.setPixel(i, rt.hsv((k * 40 + seed) and 0xFF, 180, 255))
        }
    }

    private fun twoDots(rt: WledRuntime) {
        rt.fill(rt.secondary)
        val a = rt.beatsin8(5 + rt.speed / 20, 0, rt.n - 1)
        val b = rt.beatsin8(7 + rt.speed / 22, 0, rt.n - 1)
        rt.setPixel(a, rt.primary)
        rt.setPixel(b, rt.secondary.let { if (it == Color.Companion.Black) Color.Companion.White else it })
    }

    private fun triWipe(rt: WledRuntime) {
        val third = max(1, rt.n / 3)
        val pos = ((rt.timeMs / (40 + (255 - rt.speed) / 2)) % (third + 1)).toInt()
        for (i in 0 until rt.n) {
            val band = i / third
            val local = i % third
            val c = when (band % 3) {
                0 -> rt.primary
                1 -> rt.secondary
                else -> rt.tertiary
            }
            rt.setPixel(i, if (local <= pos) c else Color.Companion.Black)
        }
    }

    private fun triFade(rt: WledRuntime) {
        val v = rt.beatsin8(5 + rt.speed / 16)
        val phase = (rt.timeMs / 1500) % 3
        val a = when (phase.toInt()) {
            0 -> rt.primary
            1 -> rt.secondary
            else -> rt.tertiary
        }
        val b = when (phase.toInt()) {
            0 -> rt.secondary
            1 -> rt.tertiary
            else -> rt.primary
        }
        rt.fill(rt.lerpColor(a, b, v / 255f))
    }

    private fun lightning(rt: WledRuntime) {
        rt.fadeToBlackBy(60)
        if (rt.random8() < 8 + rt.intensity / 20) {
            val start = rt.random8(rt.n)
            val len = 2 + rt.random8(rt.n / 4)
            for (i in 0 until len) {
                val x = (start + i).coerceIn(0, rt.n - 1)
                rt.setPixel(x, Color.Companion.White)
            }
        }
    }

    private fun icu(rt: WledRuntime) {
        rt.fill(rt.secondary)
        val eye1 = rt.n / 3
        val eye2 = 2 * rt.n / 3
        val blink = (rt.timeMs / (500 + (255 - rt.speed))) % 5L == 0L
        if (!blink) {
            rt.setPixel(eye1, rt.primary)
            rt.setPixel(eye2, rt.primary)
        }
    }

    private fun multiComet(rt: WledRuntime) {
        rt.fadeToBlackBy(48)
        val comets = 2 + rt.intensity / 50
        repeat(comets) { k ->
            val pos = ((rt.timeMs / (26 + (255 - rt.speed) / 4)) + k * rt.n / comets).toInt() % rt.n
            for (d in 0..4) {
                val f = 1f - d / 5f
                val i = (pos - d + rt.n) % rt.n
                val base = rt.hsv((k * 50) and 0xFF)
                rt.setPixel(i, Color(base.red * f, base.green * f, base.blue * f))
            }
        }
    }

    private fun oscillate(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val v = (sin((i / rt.n.toFloat() + rt.beatPhase(1f)) * 2f * PI.toFloat()) + 1f) * 0.5f
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, v))
        }
    }

    private fun pride2015(rt: WledRuntime) {
        val t = rt.timeMs / 10
        for (i in 0 until rt.n) {
            val h = ((i * 255 / rt.n) + t * (1 + rt.speed / 40)).toInt() and 0xFF
            val s = 180 + rt.inoise8(i * 8 + t.toInt()) / 4
            rt.setPixel(i, rt.hsv(h, s.coerceAtMost(255), 255))
        }
    }

    private fun juggle(rt: WledRuntime, count: Int) {
        rt.fadeToBlackBy(40)
        val dots = count.coerceIn(3, 12)
        for (k in 0 until dots) {
            val pos = rt.beatsin8(6 + k + rt.speed / 25, 0, rt.n - 1)
            rt.setPixel(pos, rt.hsv((k * 255 / dots) and 0xFF))
        }
    }

    private fun paletteMove(rt: WledRuntime) {
        val off = (rt.timeMs / (18 + (255 - rt.speed) / 4)).toInt()
        for (i in 0 until rt.n) rt.setPixel(i, rt.colorFromPalette((i * 255 / rt.n + off) and 0xFF))
    }

    private fun fire2012(rt: WledRuntime) {
        val frame = (rt.timeMs / (11 + (255 - rt.speed) / 12)).toInt()
        for (i in 0 until rt.n) {
            val noise = rt.inoise8(i * 15 + frame * 22)
            val heat = ((noise * (160 + rt.intensity / 2)) / 255 - i * 40 / rt.n).coerceAtLeast(0)
            rt.setPixel(i, heatColor(heat.coerceAtMost(255)))
        }
        if (rt.random8() < 35 + rt.speed / 5) {
            rt.setPixel(rt.random8(max(1, rt.n / 5)), heatColor(210 + rt.random8(45)))
        }
        rt.blur1d(28)
    }

    private fun heatColor(h: Int): Color {
        val x = h.coerceIn(0, 255)
        return when {
            x < 85 -> Color(x * 3 / 255f, 0f, 0f)
            x < 170 -> Color(1f, (x - 85) * 3 / 255f, 0f)
            else -> Color(1f, 1f, (x - 170) * 3 / 255f)
        }
    }

    private fun colorwaves(rt: WledRuntime) {
        val t = (rt.timeMs / (12 + (255 - rt.speed) / 8)).toInt()
        for (i in 0 until rt.n) {
            val h = (rt.inoise8(i * 10 + t) + t / 2) and 0xFF
            rt.setPixel(i, rt.colorFromPalette(h))
        }
    }

    private fun bpm(rt: WledRuntime) {
        val beat = rt.beatsin8(20 + rt.speed / 8)
        for (i in 0 until rt.n) {
            val h = ((i * 255 / rt.n) + beat) and 0xFF
            rt.setPixel(i, rt.hsv(h, 255, beat))
        }
    }

    private fun noiseBand(rt: WledRuntime, mode: Int) {
        val t = (rt.timeMs / (14 + (255 - rt.speed) / 6)).toInt()
        val scale = 8 + mode * 4 + rt.intensity / 20
        for (i in 0 until rt.n) {
            val n = rt.inoise8(i * scale + t * (1 + mode / 2))
            rt.setPixel(i, rt.colorFromPalette(n, n))
        }
    }

    private fun colorTwinkles(rt: WledRuntime) {
        rt.fadeToBlackBy(18 + (255 - rt.intensity) / 12)
        val dens = 1 + rt.intensity / 18
        val seed = (rt.timeMs / (48 + (255 - rt.speed) / 2)).toInt()
        repeat(dens) { k ->
            rt.setPixel(abs(seed * 31 + k * 17) % rt.n, rt.hsv((k * 40 + seed) and 0xFF, 255, 220))
        }
    }

    private fun lake(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val wave = sin((i / rt.n.toFloat() * 3f + rt.beatPhase(0.65f)) * PI.toFloat())
            val v = (wave + 1f) * 0.5f
            rt.setPixel(i, rt.hsvFloat(190f + v * 45f, 0.75f, 0.28f + v * 0.72f))
        }
    }

    private fun meteor(rt: WledRuntime, smooth: Boolean) {
        rt.fadeToBlackBy(if (smooth) 42 else 60)
        val pos = ((rt.timeMs / (26 + (255 - rt.speed) / 3)) % rt.n).toInt()
        val tail = 3 + rt.intensity / 26
        for (d in 0..tail) {
            var f = 1f - d / (tail + 1f)
            if (smooth) f *= f
            val c = rt.primary
            rt.setPixel((pos - d + rt.n) % rt.n, Color(c.red * f, c.green * f, c.blue * f))
        }
    }

    private fun railway(rt: WledRuntime) {
        val off = (rt.timeMs / (20 + (255 - rt.speed) / 4)).toInt()
        for (i in 0 until rt.n) {
            val band = ((i + off) / 3) % 2
            rt.setPixel(i, if (band == 0) rt.primary else rt.secondary)
        }
    }

    private fun ripple(rt: WledRuntime, rainbow: Boolean) {
        rt.fill(rt.secondary)
        val center = rt.n / 2
        val radius = (rt.beatPhase(1.05f) * rt.n) % (rt.n * 1.15f)
        val thick = 1.2f + rt.intensity / 45f
        for (i in 0 until rt.n) {
            val wave = abs(abs(i - center).toFloat() - radius)
            if (wave < thick) {
                val f = 1f - wave / thick
                val c = if (rainbow) rt.hsv(((i * 255 / rt.n) + (rt.timeMs / 20).toInt()) and 0xFF)
                else rt.primary
                rt.setPixel(i, rt.lerpColor(rt.secondary, c, f))
            }
        }
    }

    private fun twinklePalette(rt: WledRuntime, cool: Boolean) {
        rt.fadeToBlackBy(25)
        val dens = 2 + rt.intensity / 25
        val seed = (rt.timeMs / (42 + (255 - rt.speed) / 2)).toInt()
        repeat(dens) { k ->
            val i = abs(seed * 23 + k * 29) % rt.n
            val h = if (cool) (160 + k * 10) and 0xFF else (seed * 13 + k * 30) and 0xFF
            rt.setPixel(i, rt.hsv(h, 200, 255))
        }
    }

    private fun halloweenEyes(rt: WledRuntime) {
        rt.fill(Color.Companion.Black)
        if ((rt.timeMs / 800) % 4L == 0L) return
        val e1 = rt.n / 3
        val e2 = 2 * rt.n / 3
        rt.setPixel(e1, Color(0xFFFF6600))
        rt.setPixel(e2, Color(0xFFFF6600))
        if (e1 + 1 < rt.n) rt.setPixel(e1 + 1, Color(0xFFFFAA00))
        if (e2 + 1 < rt.n) rt.setPixel(e2 + 1, Color(0xFFFFAA00))
    }

    private fun solidPattern(rt: WledRuntime, tri: Boolean) {
        val mod = if (tri) 3 else 2
        for (i in 0 until rt.n) {
            val c = when (i % mod) {
                0 -> rt.primary
                1 -> rt.secondary
                else -> rt.tertiary
            }
            rt.setPixel(i, c)
        }
    }

    private fun spots(rt: WledRuntime, fade: Boolean) {
        if (fade) rt.fadeToBlackBy(30) else rt.fill(rt.secondary)
        val count = 2 + rt.intensity / 35
        val seed = (rt.timeMs / (55 + (255 - rt.speed) / 2)).toInt()
        repeat(count) { k ->
            val i = abs(seed * 17 + k * 31) % rt.n
            rt.setPixel(i, rt.primary)
            if (i + 1 < rt.n) rt.setPixel(i + 1, rt.lerpColor(rt.primary, rt.secondary, 0.5f))
        }
    }

    private fun glitter(rt: WledRuntime) {
        for (i in 0 until rt.n) rt.setPixel(i, rt.primary)
        if (rt.random8() < 20 + rt.intensity / 5)
            rt.setPixel(rt.random8(rt.n), Color.Companion.White)
    }

    private fun candle(rt: WledRuntime, multi: Boolean) {
        val count = if (multi) max(3, rt.n / 8) else 1
        rt.fill(Color.Companion.Black)
        repeat(count) { k ->
            val center = if (multi) (k + 1) * rt.n / (count + 1) else rt.n / 2
            val flick = 0.6f + 0.4f * (rt.inoise8(k * 30 + (rt.timeMs / 8).toInt()) / 255f)
            for (d in 0..3) {
                val f = flick * (1f - d / 4f)
                val i = (center + d - 1).coerceIn(0, rt.n - 1)
                rt.setPixel(i, Color(1f * f, 0.45f * f, 0.05f * f))
            }
        }
    }

    private fun bouncingBalls(rt: WledRuntime) {
        rt.fadeToBlackBy(50)
        val balls = 2 + rt.intensity / 55
        repeat(balls) { k ->
            // parabolic bounce approximation
            val t = (rt.timeMs / 1000f * (0.7f + rt.speed / 255f) + k * 0.3f)
            val cycle = t - floor(t)
            val y = 1f - 4f * (cycle - 0.5f) * (cycle - 0.5f) // 0..1..0
            val pos = (y * (rt.n - 1)).toInt().coerceIn(0, rt.n - 1)
            rt.setPixel(pos, rt.hsv((k * 55) and 0xFF))
        }
    }

    private fun sinelon(rt: WledRuntime, dual: Boolean, rainbow: Boolean) {
        rt.fadeToBlackBy(35)
        val pos = rt.beatsin8(6 + rt.speed / 18, 0, rt.n - 1)
        val c = if (rainbow) rt.hsv(((rt.timeMs / 20) % 256).toInt()) else rt.primary
        rt.setPixel(pos, c)
        if (dual) rt.setPixel(rt.n - 1 - pos, if (rainbow) rt.hsv(((rt.timeMs / 20 + 80) % 256).toInt()) else rt.secondary)
    }

    private fun popcorn(rt: WledRuntime) {
        rt.fadeToBlackBy(40)
        if (rt.random8() < 10 + rt.intensity / 15) {
            rt.setPixel(rt.random8(rt.n), rt.hsv(rt.random8()))
        }
    }

    private fun drip(rt: WledRuntime) {
        rt.fadeToBlackBy(35)
        val pos = ((rt.timeMs / (35 + (255 - rt.speed) / 3)) % rt.n).toInt()
        rt.setPixel(pos, rt.primary)
        if (pos > 0) rt.setPixel(pos - 1, rt.lerpColor(rt.primary, Color.Companion.Black, 0.5f))
    }

    private fun plasma(rt: WledRuntime) {
        val t = rt.timeMs / 20f
        for (i in 0 until rt.n) {
            val v = sin(i * 0.2f + t) + sin(i * 0.3f + t * 1.3f)
            val h = (((v + 2f) * 0.25f) * 255).toInt() and 0xFF
            rt.setPixel(i, rt.colorFromPalette(h))
        }
    }

    private fun percent(rt: WledRuntime) {
        val filled = ((rt.intensity / 255f) * rt.n).toInt()
        for (i in 0 until rt.n)
            rt.setPixel(i, if (i < filled) rt.primary else rt.secondary)
    }

    private fun heartbeat(rt: WledRuntime) {
        // double pulse
        val p = (rt.timeMs % (900 + (255 - rt.speed).toLong())).toFloat()
        val v = when {
            p < 80 -> p / 80f
            p < 160 -> 1f - (p - 80) / 80f
            p < 240 -> (p - 160) / 80f * 0.7f
            p < 320 -> 0.7f - (p - 240) / 80f * 0.7f
            else -> 0f
        }
        rt.fill(rt.lerpColor(rt.secondary, rt.primary, v.coerceIn(0f, 1f)))
    }

    private fun pacifica(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val n1 = rt.inoise8(i * 8 + (rt.timeMs / 30).toInt())
            val n2 = rt.inoise8(i * 14 + (rt.timeMs / 45).toInt())
            val h = (150 + n1 / 5) and 0xFF
            rt.setPixel(i, rt.hsv(h, 200, (60 + n2 / 2).coerceAtMost(255)))
        }
    }

    private fun solidGlitter(rt: WledRuntime) {
        rt.fill(rt.primary)
        if (rt.random8() < 25 + rt.intensity / 6) rt.setPixel(rt.random8(rt.n), Color.Companion.White)
    }

    private fun sunrise(rt: WledRuntime) {
        val phase = ((rt.timeMs / (40f + (255 - rt.speed))) % (rt.n * 2f))
        for (i in 0 until rt.n) {
            val d = (phase - i)
            val f = when {
                d < 0 -> 0f
                d < rt.n -> (d / rt.n).coerceIn(0f, 1f)
                else -> 1f
            }
            // black -> red -> orange -> yellow -> white
            val c = when {
                f < 0.25f -> Color(f * 4f, 0f, 0f)
                f < 0.5f -> Color(1f, (f - 0.25f) * 4f * 0.6f, 0f)
                f < 0.75f -> Color(1f, 0.6f + (f - 0.5f) * 1.6f, (f - 0.5f) * 2f)
                else -> Color(1f, 1f, (f - 0.75f) * 4f)
            }
            rt.setPixel(i, c)
        }
    }

    private fun phased(rt: WledRuntime, noise: Boolean) {
        for (i in 0 until rt.n) {
            var v = sin((i / rt.n.toFloat() * 2f * PI.toFloat()) + rt.beatPhase(1.2f))
            if (noise) v += (rt.inoise8(i * 10 + (rt.timeMs / 20).toInt()) / 255f - 0.5f)
            val t = ((v + 1f) * 0.5f).coerceIn(0f, 1f)
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, t))
        }
    }

    private fun twinkleUp(rt: WledRuntime) {
        rt.fadeToBlackBy(20)
        val i = ((rt.timeMs / (30 + (255 - rt.speed) / 3)) % rt.n).toInt()
        rt.setPixel(i, rt.primary)
    }

    private fun sineBars(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val v = (sin(i * 0.4f + rt.beatPhase(1.4f) * 2f * PI.toFloat()) + 1f) * 0.5f
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, v))
        }
    }

    private fun flow(rt: WledRuntime) {
        val off = (rt.timeMs / (16 + (255 - rt.speed) / 5)).toInt()
        for (i in 0 until rt.n) {
            val h = (rt.inoise8(i * 6 + off) + off / 2) and 0xFF
            rt.setPixel(i, rt.colorFromPalette(h))
        }
    }

    private fun chunchun(rt: WledRuntime) {
        rt.fadeToBlackBy(30)
        val pos = rt.beatsin8(8 + rt.speed / 16, 0, rt.n - 1)
        val pos2 = rt.beatsin8(11 + rt.speed / 18, 0, rt.n - 1)
        rt.setPixel(pos, rt.primary)
        rt.setPixel(pos2, rt.secondary)
        rt.blur1d(50)
    }

    private fun dancingShadows(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val n = rt.inoise8(i * 9 + (rt.timeMs / 18).toInt())
            val v = (n / 255f) * (rt.intensity / 255f)
            rt.setPixel(i, rt.lerpColor(Color.Companion.Black, rt.primary, v))
        }
    }

    private fun washingMachine(rt: WledRuntime) {
        val pos = ((rt.timeMs / (22 + (255 - rt.speed) / 4)) % rt.n).toInt()
        for (i in 0 until rt.n) {
            val d = min(abs(i - pos), rt.n - abs(i - pos))
            val f = (1f - d / (rt.n / 3f).coerceAtLeast(1f)).coerceIn(0f, 1f)
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, f))
        }
    }

    private fun rotozoomer(rt: WledRuntime) {
        val t = rt.timeMs / 200f
        for (i in 0 until rt.n) {
            val x = i / rt.n.toFloat() - 0.5f
            val ang = t * (0.5f + rt.speed / 255f)
            val u = x * cos(ang)
            val h = ((u + 0.5f) * 255).toInt() and 0xFF
            rt.setPixel(i, rt.colorFromPalette(h))
        }
    }

    private fun blends(rt: WledRuntime) {
        val v = rt.beatsin8(4 + rt.speed / 20)
        for (i in 0 until rt.n) {
            val t = (i / (rt.n - 1f) + v / 255f) % 1f
            rt.setPixel(i, rt.colorFromPalette((t * 255).toInt()))
        }
    }

    private fun tvSimulator(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val n = rt.random8()
            rt.setPixel(i, Color(n / 255f, rt.random8() / 255f, rt.random8() / 255f))
        }
        rt.blur1d(20)
    }

    private fun spaceships(rt: WledRuntime) {
        rt.fadeToBlackBy(45)
        val ships = 2 + rt.intensity / 60
        repeat(ships) { k ->
            val pos = ((rt.timeMs / (30 + k * 3 + (255 - rt.speed) / 4)) + k * 20).toInt() % rt.n
            rt.setPixel(pos, Color.Companion.White)
            if (pos > 0) rt.setPixel(pos - 1, rt.hsv((k * 40) and 0xFF, 255, 120))
        }
    }

    private fun crazyBees(rt: WledRuntime) {
        rt.fadeToBlackBy(40)
        val bees = 3 + rt.intensity / 45
        repeat(bees) { k ->
            val pos = abs(rt.inoise8(k * 40 + (rt.timeMs / 12).toInt()) * rt.n / 255)
            rt.setPixel(pos.coerceIn(0, rt.n - 1), rt.hsv((40 + k * 20) and 0xFF))
        }
    }

    private fun ghostRider(rt: WledRuntime) {
        rt.fadeToBlackBy(55)
        val pos = ((rt.timeMs / (24 + (255 - rt.speed) / 3)) % rt.n).toInt()
        for (d in 0..6) {
            val f = 1f - d / 7f
            rt.setPixel((pos - d + rt.n) % rt.n, Color(1f * f, 0.15f * f, 0f))
        }
    }

    private fun blobs(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            var v = 0f
            for (k in 0 until 3) {
                val c = rt.beatsin8(5 + k * 2 + rt.speed / 25, 0, rt.n - 1)
                val d = abs(i - c).toFloat()
                v += 1f / (1f + d * 0.35f)
            }
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, (v / 2f).coerceIn(0f, 1f)))
        }
    }

    private fun scrollingTextProxy(rt: WledRuntime) {
        // 1D proxy: moving block "cursor"
        loading(rt)
    }

    private fun driftRose(rt: WledRuntime) {
        val t = rt.timeMs / 300f
        for (i in 0 until rt.n) {
            val a = i / rt.n.toFloat() * 2f * PI.toFloat()
            val r = 0.5f + 0.5f * sin(a * 3f + t)
            rt.setPixel(i, rt.hsv(((r * 80 + t * 30).toInt()) and 0xFF, 220, (r * 255).toInt()))
        }
    }

    private fun distortionWaves(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val v = sin(i * 0.25f + rt.beatPhase(1.3f) * 6f) *
                    cos(i * 0.1f + rt.beatPhase(0.7f) * 4f)
            val h = (((v + 1f) * 0.5f) * 255).toInt() and 0xFF
            rt.setPixel(i, rt.colorFromPalette(h))
        }
    }

    private fun soap(rt: WledRuntime) {
        val t = (rt.timeMs / 25).toInt()
        for (i in 0 until rt.n) {
            val n = rt.inoise8(i * 7 + t)
            rt.setPixel(i, rt.hsv((n / 2 + 140) and 0xFF, 120, n))
        }
    }

    private fun octopus(rt: WledRuntime) {
        rt.fadeToBlackBy(30)
        val arms = 4 + rt.intensity / 50
        repeat(arms) { k ->
            val pos = rt.beatsin8(5 + k + rt.speed / 20, 0, rt.n - 1)
            rt.setPixel(pos, rt.hsv((k * 40) and 0xFF))
        }
    }

    private fun wavingCell(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val cell = i / max(1, rt.n / 8)
            val v = (sin(cell + rt.beatPhase(1.1f) * 2f * PI.toFloat()) + 1f) * 0.5f
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, v))
        }
    }

    private fun audioOr2dProxy(rt: WledRuntime, id: Int) {
        // Silent / matrix projection: mirrored noise spectrum style
        val t = (rt.timeMs / (16 + (255 - rt.speed) / 5)).toInt()
        val mid = rt.n / 2
        for (i in 0 until rt.n) {
            val dist = abs(i - mid)
            val n = rt.inoise8(dist * 10 + t + id * 3)
            val bar = (n * (rt.intensity + 40) / 300).coerceAtMost(255)
            val show = dist < bar * mid / 255
            rt.setPixel(
                i,
                if (show) rt.colorFromPalette((n + id * 5) and 0xFF, bar)
                else Color.Companion.Black
            )
        }
    }

    private fun pacMan(rt: WledRuntime) {
        rt.fill(Color.Companion.Black)
        val pos = ((rt.timeMs / (28 + (255 - rt.speed) / 3)) % rt.n).toInt()
        for (d in 0..4) rt.setPixel((pos + d) % rt.n, Color(0xFFFFE000))
        // dots
        for (i in 0 until rt.n step 4) if (i > pos + 4) rt.setPixel(i, Color.Companion.White)
    }

    private fun dna(rt: WledRuntime, spiral: Boolean) {
        for (i in 0 until rt.n) {
            val a = i / rt.n.toFloat() * 4f * PI.toFloat() + rt.beatPhase(1.2f) * 2f * PI.toFloat()
            val s = sin(a)
            val c1 = s > 0
            val h = if (spiral) ((i * 5 + (rt.timeMs / 20).toInt()) and 0xFF) else if (c1) 0 else 160
            rt.setPixel(i, rt.hsv(h, 255, ((abs(s)) * 255).toInt()))
        }
    }

    private fun matrixRain(rt: WledRuntime) {
        rt.fadeToBlackBy(35)
        if (rt.random8() < 30 + rt.intensity / 8) {
            rt.setPixel(rt.random8(rt.n), Color(0xFF00FF66))
        }
    }

    private fun metaballs(rt: WledRuntime) {
        blobs(rt)
    }

    private fun shimmer(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val n = rt.inoise8(i * 12 + (rt.timeMs / 10).toInt())
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, n / 255f))
        }
    }

    private fun pulser(rt: WledRuntime) {
        val v = rt.beatsin8(8 + rt.speed / 12)
        val len = (v / 255f * rt.n).toInt()
        for (i in 0 until rt.n)
            rt.setPixel(i, if (i < len) rt.primary else rt.secondary)
    }

    private fun blurz(rt: WledRuntime) {
        colorful(rt)
        rt.blur1d(80 + rt.intensity / 4)
    }

    private fun drift(rt: WledRuntime) {
        val off = (rt.timeMs / (30 + (255 - rt.speed) / 3)).toInt()
        for (i in 0 until rt.n)
            rt.setPixel(i, rt.colorFromPalette((i * 3 + off) and 0xFF))
    }

    private fun waverly(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val v = sin(i * 0.35f + rt.beatPhase(1.5f) * 2f * PI.toFloat())
            rt.setPixel(i, rt.colorFromPalette((((v + 1f) * 0.5f) * 255).toInt()))
        }
    }

    private fun sunRadiation(rt: WledRuntime) {
        val c = rt.n / 2
        for (i in 0 until rt.n) {
            val d = abs(i - c).toFloat() / c
            val pulse = 0.5f + 0.5f * sin(rt.beatPhase(1.2f) * 2f * PI.toFloat() - d * 4f)
            val f = (1f - d) * pulse
            rt.setPixel(i, Color(1f * f, 0.7f * f, 0.1f * f))
        }
    }

    private fun coloredBursts(rt: WledRuntime) {
        rt.fadeToBlackBy(40)
        if (rt.random8() < 12 + rt.intensity / 20) {
            val c = rt.random8(rt.n)
            val h = rt.random8()
            for (d in 0..3) {
                val i = (c + d).coerceIn(0, rt.n - 1)
                rt.setPixel(i, rt.hsv(h))
            }
        }
    }

    private fun juliaProxy(rt: WledRuntime) {
        // 1D slice of julia-ish iteration
        val t = rt.timeMs / 1000f
        for (i in 0 until rt.n) {
            var x = i / rt.n.toFloat() * 2f - 1f
            var y = sin(t)
            var it = 0
            while (x * x + y * y < 4f && it < 12) {
                val nx = x * x - y * y + cos(t) * 0.3f
                y = 2f * x * y + sin(t * 0.7f) * 0.3f
                x = nx
                it++
            }
            rt.setPixel(i, rt.hsv((it * 20) and 0xFF, 255, it * 20))
        }
    }

    private fun gameOfLife1d(rt: WledRuntime) {
        // 1D cellular automata preview
        if (rt.call % 4L == 0L) {
            val src = rt.snapshot()
            for (i in 0 until rt.n) {
                val l = if (src[(i - 1 + rt.n) % rt.n] != Color.Companion.Black) 1 else 0
                val r = if (src[(i + 1) % rt.n] != Color.Companion.Black) 1 else 0
                val self = if (src[i] != Color.Companion.Black) 1 else 0
                val sum = l + self + r
                rt.setPixel(i, if (sum == 1 || sum == 2) rt.primary else Color.Companion.Black)
            }
            if (rt.random8() < 15) rt.setPixel(rt.random8(rt.n), rt.primary)
        }
    }

    private fun tartan(rt: WledRuntime) {
        val off = (rt.timeMs / 40).toInt()
        for (i in 0 until rt.n) {
            val a = ((i + off) / 4) % 2
            val b = ((i * 2 + off) / 5) % 2
            rt.setPixel(i, if (a xor b == 0) rt.primary else rt.secondary)
        }
    }

    private fun polarLights(rt: WledRuntime) {
        aurora(rt)
    }

    private fun swirl(rt: WledRuntime) {
        val off = (rt.timeMs / (18 + (255 - rt.speed) / 4)).toInt()
        for (i in 0 until rt.n)
            rt.setPixel(i, rt.hsv((i * 4 + off) and 0xFF))
    }

    private fun lissajous(rt: WledRuntime) {
        rt.fadeToBlackBy(40)
        val x = rt.beatsin8(5 + rt.speed / 20, 0, rt.n - 1)
        val y = rt.beatsin8(7 + rt.speed / 22, 0, rt.n - 1)
        rt.setPixel(x, rt.primary)
        rt.setPixel(y, rt.secondary)
        rt.blur1d(60)
    }

    private fun frizzles(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val n = rt.inoise8(i * 20 + (rt.timeMs / 8).toInt())
            rt.setPixel(i, rt.hsv(n, 255, n))
        }
    }

    private fun plasmaBall(rt: WledRuntime) {
        val c = rt.n / 2
        for (i in 0 until rt.n) {
            val d = abs(i - c).toFloat()
            val v = sin(d * 0.4f - rt.beatPhase(1.4f) * 6f)
            rt.setPixel(i, rt.hsv((((v + 1f) * 0.5f) * 255).toInt() and 0xFF, 255, ((1.2f - d / c).coerceIn(0f, 1f) * 255).toInt()))
        }
    }

    private fun flowStripe(rt: WledRuntime) {
        val off = (rt.timeMs / (15 + (255 - rt.speed) / 5)).toInt()
        for (i in 0 until rt.n) {
            val band = ((i * 3 + off) and 0xFF)
            rt.setPixel(i, rt.colorFromPalette(band))
        }
    }

    private fun hiphotic(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val v = sin(i * 0.5f + rt.beatPhase(2f)) * cos(i * 0.2f + rt.beatPhase(1.1f))
            rt.setPixel(i, rt.hsv((((v + 1f) * 0.5f) * 255).toInt() and 0xFF))
        }
    }

    private fun sindots(rt: WledRuntime) {
        rt.fadeToBlackBy(50)
        for (k in 0 until 6) {
            val p = rt.beatsin8(4 + k * 2 + rt.speed / 30, 0, rt.n - 1)
            rt.setPixel(p, rt.hsv((k * 40) and 0xFF))
        }
    }

    private fun blackHole(rt: WledRuntime) {
        val c = rt.n / 2
        for (i in 0 until rt.n) {
            val d = abs(i - c).toFloat() / c
            val spin = sin(d * 8f - rt.beatPhase(1.5f) * 6f)
            val f = (1f - d) * (0.5f + 0.5f * spin)
            rt.setPixel(i, rt.hsv((200 + (spin * 20)).toInt() and 0xFF, 255, (f * 255).toInt().coerceIn(0, 255)))
        }
        rt.setPixel(c, Color.Companion.White)
    }

    private fun wavesins(rt: WledRuntime) {
        for (i in 0 until rt.n) {
            val v = sin(i * 0.3f + rt.beatPhase(1.2f) * 2f * PI.toFloat())
            rt.setPixel(i, rt.lerpColor(rt.secondary, rt.primary, (v + 1f) * 0.5f))
        }
    }

    private fun rocktaves(rt: WledRuntime) {
        bpm(rt)
        rt.blur1d(40)
    }

    private fun akemi(rt: WledRuntime) {
        // playful proxy: rainbow body + bounce head
        rainbow(rt)
        val head = rt.beatsin8(8, 0, rt.n - 1)
        rt.setPixel(head, Color.Companion.White)
    }

    private fun colorClouds(rt: WledRuntime) {
        val t = (rt.timeMs / 30).toInt()
        for (i in 0 until rt.n) {
            val n = rt.inoise8(i * 5 + t)
            rt.setPixel(i, rt.colorFromPalette(n, (n * 0.8f).toInt()))
        }
    }

    private fun slowTransition(rt: WledRuntime) {
        val v = rt.beatsin8(2 + rt.speed / 40)
        rt.fill(rt.lerpColor(rt.secondary, rt.primary, v / 255f))
    }

    private fun hourglass(rt: WledRuntime) {
        val fill = ((rt.timeMs / (30f + (255 - rt.speed))) % (rt.n * 2f))
        for (i in 0 until rt.n) {
            val fromBottom = i < fill && fill <= rt.n
            val fromTop = fill > rt.n && i > rt.n * 2 - fill
            rt.setPixel(i, if (fromBottom || fromTop) rt.primary else rt.secondary)
        }
    }

    private fun spray1d(rt: WledRuntime) {
        rt.fadeToBlackBy(40)
        val origin = rt.intensity * (rt.n - 1) / 255
        if (rt.random8() < 40 + rt.speed / 4) {
            val i = (origin + rt.random8(7) - 3).coerceIn(0, rt.n - 1)
            rt.setPixel(i, rt.primary)
        }
    }

    private fun balance1d(rt: WledRuntime) {
        val tilt = rt.beatsin8(5 + rt.speed / 20, 0, rt.n - 1)
        for (i in 0 until rt.n) {
            val d = abs(i - tilt).toFloat()
            rt.setPixel(i, rt.lerpColor(rt.primary, rt.secondary, (d / rt.n).coerceIn(0f, 1f)))
        }
    }

    private fun sonicBoom(rt: WledRuntime) {
        rt.fadeToBlackBy(30)
        val radius = ((rt.timeMs / (20f + (255 - rt.speed) / 5)) % rt.n)
        val c = rt.n / 2
        for (i in 0 until rt.n) {
            if (abs(abs(i - c) - radius) < 1.5f) rt.setPixel(i, Color.Companion.White)
        }
    }

    private fun springy(rt: WledRuntime) {
        val pos = rt.beatsin8(10 + rt.speed / 12, 0, rt.n - 1)
        rt.fill(rt.secondary)
        for (d in 0..2) {
            val i = (pos + d).coerceIn(0, rt.n - 1)
            rt.setPixel(i, rt.primary)
        }
    }

    private fun rsvd(rt: WledRuntime, id: Int) {
        // Reserved slots: quiet dim marker, still “has preview”
        rt.fill(Color(0xFF1A1A1A))
        val i = id % rt.n
        rt.setPixel(i, Color(0xFF333333))
    }
}