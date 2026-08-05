package com.samroid.wled.domain.wled

/**
 * Default effect names aligned with classic WLED FX IDs.
 * Protocol range for this project: 0..159
 * Source lineage: wled/WLED FX mode names (Solid, Blink, …).
 */
object WledEffectCatalog {

    /** Full list id → name (0..159). Unknown slots keep a fallback name. */
    val all: List<WledEffect> by lazy {
        val names = BASE_NAMES
        (0 until 160).map { id ->
            WledEffect(id, names.getOrElse(id) { "Effect $id" })
        }
    }

    fun nameOf(id: Int): String =
        all.getOrNull(id.coerceIn(0, 159))?.name ?: "Effect $id"

    fun asDropdownItems(): List<Pair<Int, String>> =
        all.map { it.id to it.name }

    fun search(query: String): List<WledEffect> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter {
            it.name.contains(q, ignoreCase = true) || it.id.toString() == q
        }
    }

    /**
     * Official-style names for IDs 0..117 (classic WLED FX.h JSON_mode_names),
     * then common later effects through ~159 used in newer builds.
     * If firmware has fewer/more modes, labels may differ but IDs still match protocol.
     */
    private val BASE_NAMES: List<String> = listOf(
        // 0–9
        "Solid", "Blink", "Breathe", "Wipe", "Wipe Random", "Random Colors", "Sweep", "Dynamic", "Colorloop", "Rainbow",
        // 10–19
        "Scan", "Scan Dual", "Fade", "Theater", "Theater Rainbow", "Running", "Saw", "Twinkle", "Dissolve", "Dissolve Rnd",
        // 20–29
        "Sparkle", "Sparkle Dark", "Sparkle+", "Strobe", "Strobe Rainbow", "Strobe Mega", "Blink Rainbow", "Android", "Chase", "Chase Random",
        // 30–39
        "Chase Rainbow", "Chase Flash", "Chase Flash Rnd", "Rainbow Runner", "Colorful", "Traffic Light", "Sweep Random", "Running 2", "Red & Blue", "Stream",
        // 40–49
        "Scanner", "Lighthouse", "Fireworks", "Rain", "Merry Christmas", "Fire Flicker", "Gradient", "Loading", "Police", "Police All",
        // 50–59
        "Two Dots", "Two Areas", "Circus", "Halloween", "Tri Chase", "Tri Wipe", "Tri Fade", "Lightning", "ICU", "Multi Comet",
        // 60–69
        "Scanner Dual", "Stream 2", "Oscillate", "Pride 2015", "Juggle", "Palette", "Fire 2012", "Colorwaves", "BPM", "Fill Noise",
        // 70–79
        "Noise 1", "Noise 2", "Noise 3", "Noise 4", "Colortwinkles", "Lake", "Meteor", "Meteor Smooth", "Railway", "Ripple",
        // 80–89
        "Twinklefox", "Twinklecat", "Halloween Eyes", "Solid Pattern", "Solid Pattern Tri", "Spots", "Spots Fade", "Glitter", "Candle", "Fireworks Starburst",
        // 90–99
        "Fireworks 1D", "Bouncing Balls", "Sinelon", "Sinelon Dual", "Sinelon Rainbow", "Popcorn", "Drip", "Plasma", "Percent", "Ripple Rainbow",
        // 100–109
        "Heartbeat", "Pacifica", "Candle Multi", "Solid Glitter", "Sunrise", "Phased", "Twinkleup", "Noise Pal", "Sine", "Phased Noise",
        // 110–117 classic end in older FX.h
        "Flow", "Chunchun", "Dancing Shadows", "Washing Machine", "Candy Cane", "Blends", "TV Simulator", "Dynamic Smooth",
        // 118–129 (common post-0.13 names; may vary by firmware)
        "Spaceships", "Crash", "Puddlepeak", "Midnoise", "Noisemove", "Pixelwave", "Juggles", "Matripix", "Gravimeter", "Plasmoid",
        "Puddles", "Pixels",
        // 130–149
        "Blurz", "DJ Light", "Freqwave", "Noisefire", "Noisemeter", "Waterfall", "Ripple Peak", "Binmap", "Noisefire 2", "Freqmatrix",
        "Freqpixels", "Noisemeter 2", "Audio bars", "Noise 5", "GEQ", "Bandmap", "Noise 6", "Noise 7", "Rocktaves", "Noise 8",
        // 150–159
        "Akemi", "2D Square", "2D Hypnotic", "2D Distortion Waves", "2D Soap", "2D Noise", "2D Matrix", "2D Game Of Life", "2D Tartan", "2D Polar Lights"
    )
}