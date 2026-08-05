package com.samroid.wled.domain.wled

import androidx.compose.ui.graphics.Color

/**
 * Default fixed palettes 0..71 (project protocol).
 * Names from WLED JSON_palette_names / kno.wled.ge.
 * Gradient colors approximate official FastLED / WLED palettes for UI preview.
 * Dynamic palettes (* Color 1, …) use placeholder segment colors.
 */
object WledPaletteCatalog {

    private val C1 = Color(0xFFFFAA00) // primary placeholder
    private val C2 = Color(0xFF000000) // secondary
    private val C3 = Color(0xFFFFFFFF) // tertiary

    val all: List<WledPalette> by lazy { buildAll() }

    fun nameOf(id: Int): String =
        all.getOrNull(id.coerceIn(0, 71))?.name ?: "Palette $id"

    fun get(id: Int): WledPalette =
        all.getOrElse(id.coerceIn(0, 71)) {
            WledPalette(id, "Palette $id", listOf(0f to Color.Gray, 1f to Color.DarkGray))
        }

    fun asDropdownItems(): List<Pair<Int, String>> =
        all.map { it.id to it.name }

    private fun p(id: Int, name: String, vararg rgbStops: Triple<Float, Int, Int>): WledPalette {
        // rgbStops: pos, color as 0xRRGGBB packed in second? Use Triple(pos, rrg gbb as Int color)
        error("use stops helper")
    }

    private fun pal(id: Int, name: String, vararg stops: Pair<Float, Long>): WledPalette =
        WledPalette(
            id = id,
            name = name,
            stops = stops.map { (pos, hex) -> pos to Color(hex or 0xFF000000) }
        )

    private fun buildAll(): List<WledPalette> {
        val list = mutableListOf<WledPalette>()

        fun add(p: WledPalette) { list += p }

        // 0–5 dynamic / segment-based
        add(pal(0, "Default", 0f to 0xFFFFAA00, 1f to 0xFFFFAA00))
        add(pal(1, "* Random Cycle", 0f to 0xFFFF0000, 0.33f to 0xFF00FF00, 0.66f to 0xFF0000FF, 1f to 0xFFFF0000))
        add(pal(2, "* Color 1", 0f to 0xFFFFAA00, 1f to 0xFFFFAA00))
        add(pal(3, "* Colors 1&2", 0f to 0xFFFFAA00, 0.5f to 0xFF000000, 1f to 0xFFFFAA00))
        add(pal(4, "* Color Gradient", 0f to 0xFFFFAA00, 0.5f to 0xFF000000, 1f to 0xFFFFFFFF))
        add(pal(5, "* Colors Only", 0f to 0xFFFFAA00, 0.5f to 0xFF000000, 1f to 0xFFFFFFFF))

        // 6– FastLED classics
        add(pal(6, "Party", 0f to 0xFF5500AB, 0.25f to 0xFF84007C, 0.5f to 0xFFB5004B, 0.75f to 0xFFE5001B, 1f to 0xFFFF3D00))
        add(pal(7, "Cloud", 0f to 0xFF00008B, 0.4f to 0xFF0000FF, 0.7f to 0xFFADD8E6, 1f to 0xFFFFFFFF))
        add(pal(8, "Lava", 0f to 0xFF000000, 0.3f to 0xFF7F0000, 0.5f to 0xFFFF0000, 0.7f to 0xFFFF7F00, 1f to 0xFFFFFFFF))
        add(pal(9, "Ocean", 0f to 0xFF000507, 0.3f to 0xFF000730, 0.55f to 0xFF00219E, 0.8f to 0xFF1CA2FE, 1f to 0xFFE0F8FF))
        add(pal(10, "Forest", 0f to 0xFF006400, 0.35f to 0xFF228B22, 0.7f to 0xFF7CFC00, 1f to 0xFFADFF2F))
        add(pal(11, "Rainbow", 0f to 0xFFFF0000, 0.17f to 0xFFFFFF00, 0.33f to 0xFF00FF00, 0.5f to 0xFF00FFFF, 0.67f to 0xFF0000FF, 0.83f to 0xFFFF00FF, 1f to 0xFFFF0000))
        add(pal(12, "Rainbow Bands", 0f to 0xFFFF0000, 0.14f to 0xFF000000, 0.28f to 0xFFFFFF00, 0.42f to 0xFF000000, 0.57f to 0xFF00FF00, 0.71f to 0xFF000000, 0.85f to 0xFF0000FF, 1f to 0xFF000000))

        add(pal(13, "Sunset", 0f to 0xFF12022A, 0.25f to 0xFF6B0F3C, 0.5f to 0xFFC43C11, 0.75f to 0xFFE88B2E, 1f to 0xFFFFE5B4))
        add(pal(14, "Rivendell", 0f to 0xFF1B4332, 0.4f to 0xFF2D6A4F, 0.7f to 0xFF95D5B2, 1f to 0xFFD8F3DC))
        add(pal(15, "Breeze", 0f to 0xFF001F3F, 0.4f to 0xFF0074D9, 0.7f to 0xFF7FDBFF, 1f to 0xFFFFFFFF))
        add(pal(16, "Red & Blue", 0f to 0xFFFF0000, 0.5f to 0xFF000000, 1f to 0xFF0000FF))
        add(pal(17, "Yellowout", 0f to 0xFFFFFF00, 1f to 0xFF000000))
        add(pal(18, "Analogous", 0f to 0xFF0000FF, 0.5f to 0xFFFF0000, 1f to 0xFF0000FF))
        add(pal(19, "Splash", 0f to 0xFF1565C0, 0.4f to 0xFF26C6DA, 0.7f to 0xFFFFEB3B, 1f to 0xFFFFFFFF))
        add(pal(20, "Pastel", 0f to 0xFFFFCDD2, 0.25f to 0xFFFFE0B2, 0.5f to 0xFFFFF9C4, 0.75f to 0xFFC8E6C9, 1f to 0xFFBBDEFB))
        add(pal(21, "Sunset 2", 0f to 0xFF2B1055, 0.35f to 0xFF7597DE, 0.65f to 0xFFFF6B6B, 1f to 0xFFFFD93D))
        add(pal(22, "Beech", 0f to 0xFF1A237E, 0.4f to 0xFF00838F, 0.7f to 0xFF80DEEA, 1f to 0xFFE0F7FA))
        add(pal(23, "Vintage", 0f to 0xFF3E2723, 0.4f to 0xFF8D6E63, 0.7f to 0xFFD7CCC8, 1f to 0xFFFFF8E1))
        add(pal(24, "Departure", 0f to 0xFF0D47A1, 0.5f to 0xFF42A5F5, 1f to 0xFFE3F2FD))
        add(pal(25, "Landscape", 0f to 0xFF1B5E20, 0.35f to 0xFF66BB6A, 0.65f to 0xFFFFF176, 1f to 0xFF81D4FA))
        add(pal(26, "Beach", 0f to 0xFF01579B, 0.4f to 0xFF4FC3F7, 0.7f to 0xFFFFF59D, 1f to 0xFFFFCC80))
        add(pal(27, "Sherbet", 0f to 0xFFFF8A80, 0.33f to 0xFFFFD180, 0.66f to 0xFFFFFF8D, 1f to 0xFFCCFF90))
        add(pal(28, "Hult", 0f to 0xFFFF00FF, 0.5f to 0xFF00FFFF, 1f to 0xFFFFFF00))
        add(pal(29, "Hult 64", 0f to 0xFF9C27B0, 0.5f to 0xFF00BCD4, 1f to 0xFFFFEB3B))
        add(pal(30, "Drywet", 0f to 0xFF5D4037, 0.5f to 0xFF0288D1, 1f to 0xFFB3E5FC))
        add(pal(31, "Jul", 0f to 0xFFB71C1C, 0.4f to 0xFFFF5252, 0.7f to 0xFFFFCDD2, 1f to 0xFFFFFFFF))
        add(pal(32, "Grintage", 0f to 0xFF33691E, 0.5f to 0xFFAFB42B, 1f to 0xFFFFF59D))
        add(pal(33, "Rewhi", 0f to 0xFF4A148C, 0.5f to 0xFFE040FB, 1f to 0xFFF3E5F5))
        add(pal(34, "Tertiary", 0f to 0xFFFFAA00, 0.5f to 0xFF000000, 1f to 0xFFFFFFFF))
        add(pal(35, "Fire", 0f to 0xFF000000, 0.25f to 0xFF7F0000, 0.5f to 0xFFFF3D00, 0.75f to 0xFFFFAB00, 1f to 0xFFFFFFFF))
        add(pal(36, "Icefire", 0f to 0xFF000033, 0.3f to 0xFF001A66, 0.55f to 0xFF3399FF, 0.8f to 0xFF99CCFF, 1f to 0xFFFFFFFF))
        add(pal(37, "Cyane", 0f to 0xFF006064, 0.5f to 0xFF00E5FF, 1f to 0xFFE0FFFF))
        add(pal(38, "Light Pink", 0f to 0xFFFF80AB, 0.5f to 0xFFFFC1E3, 1f to 0xFFFFFFFF))
        add(pal(39, "Autumn", 0f to 0xFFBF360C, 0.35f to 0xFFFF6F00, 0.65f to 0xFFFFD600, 1f to 0xFF8BC34A))
        add(pal(40, "Magenta", 0f to 0xFF880E4F, 0.5f to 0xFFFF00FF, 1f to 0xFFFCE4EC))
        add(pal(41, "Magred", 0f to 0xFFC51162, 0.5f to 0xFFFF1744, 1f to 0xFFFF8A80))
        add(pal(42, "Yelmag", 0f to 0xFFFFFF00, 0.5f to 0xFFFF00FF, 1f to 0xFFFF0000))
        add(pal(43, "Yelblu", 0f to 0xFFFFFF00, 0.5f to 0xFF00BCD4, 1f to 0xFF0D47A1))
        add(pal(44, "Orange & Teal", 0f to 0xFFFF6D00, 0.5f to 0xFF000000, 1f to 0xFF00897B))
        add(pal(45, "Tiamat", 0f to 0xFF1A237E, 0.25f to 0xFF7B1FA2, 0.5f to 0xFFC2185B, 0.75f to 0xFFFF5722, 1f to 0xFFFFEB3B))
        add(pal(46, "April Night", 0f to 0xFF0D1B2A, 0.4f to 0xFF1B263B, 0.7f to 0xFF415A77, 1f to 0xFFE0E1DD))
        add(pal(47, "Orangery", 0f to 0xFFE65100, 0.4f to 0xFFFF9800, 0.7f to 0xFFFFCC80, 1f to 0xFFFFF3E0))
        add(pal(48, "C9", 0f to 0xFFB71C1C, 0.33f to 0xFF1B5E20, 0.66f to 0xFFF9A825, 1f to 0xFFFFFFFF))
        add(pal(49, "Sakura", 0f to 0xFFF8BBD0, 0.4f to 0xFFF48FB1, 0.7f to 0xFFCE93D8, 1f to 0xFFFFFFFF))
        add(pal(50, "Aurora", 0f to 0xFF00C853, 0.35f to 0xFF00B0FF, 0.65f to 0xFF651FFF, 1f to 0xFF00E676))
        add(pal(51, "Atlantica", 0f to 0xFF004D40, 0.4f to 0xFF00897B, 0.7f to 0xFF4DD0E1, 1f to 0xFFE0F7FA))
        add(pal(52, "C9 2", 0f to 0xFFD32F2F, 0.33f to 0xFF388E3C, 0.66f to 0xFFFBC02D, 1f to 0xFF212121))
        add(pal(53, "C9 New", 0f to 0xFFC62828, 0.25f to 0xFF2E7D32, 0.5f to 0xFFF9A825, 0.75f to 0xFF1565C0, 1f to 0xFFFFFFFF))
        add(pal(54, "Temperature", 0f to 0xFF0000FF, 0.25f to 0xFF00FFFF, 0.5f to 0xFF00FF00, 0.75f to 0xFFFFFF00, 1f to 0xFFFF0000))
        add(pal(55, "Aurora 2", 0f to 0xFF1A237E, 0.3f to 0xFF00E676, 0.6f to 0xFF00B0FF, 1f to 0xFFE1BEE7))
        add(pal(56, "Retro Clown", 0f to 0xFFFF1744, 0.33f to 0xFFFFEA00, 0.66f to 0xFF00E676, 1f to 0xFF2979FF))
        add(pal(57, "Candy", 0f to 0xFFFF4081, 0.33f to 0xFF7C4DFF, 0.66f to 0xFF18FFFF, 1f to 0xFFFFFF00))
        add(pal(58, "Toxy Reaf", 0f to 0xFF00BCD4, 0.5f to 0xFF76FF03, 1f to 0xFFFFEA00))
        add(pal(59, "Fairy Reaf", 0f to 0xFFE040FB, 0.5f to 0xFF18FFFF, 1f to 0xFFFFFF8D))
        add(pal(60, "Semi Blue", 0f to 0xFF0D47A1, 0.5f to 0xFF42A5F5, 1f to 0xFFBBDEFB))
        add(pal(61, "Pink Candy", 0f to 0xFFFF80AB, 0.5f to 0xFFFF4081, 1f to 0xFFFFE0F0))
        add(pal(62, "Red Reaf", 0f to 0xFFB71C1C, 0.5f to 0xFFFF5252, 1f to 0xFFFFCDD2))
        add(pal(63, "Aqua Flash", 0f to 0xFF006064, 0.4f to 0xFF00E5FF, 0.7f to 0xFFFFFFFF, 1f to 0xFF00E5FF))
        add(pal(64, "Yelblu Hot", 0f to 0xFFFFFF00, 0.4f to 0xFFFF6D00, 0.7f to 0xFF00B0FF, 1f to 0xFF0D47A1))
        add(pal(65, "Lite Light", 0f to 0xFFFFFDE7, 0.5f to 0xFFFFF9C4, 1f to 0xFFFFECB3))
        add(pal(66, "Red Flash", 0f to 0xFFB71C1C, 0.5f to 0xFFFFFFFF, 1f to 0xFFB71C1C))
        add(pal(67, "Blink Red", 0f to 0xFFFF1744, 0.5f to 0xFF000000, 1f to 0xFFFF1744))
        add(pal(68, "Red Shift", 0f to 0xFF3E0A0A, 0.35f to 0xFFB71C1C, 0.65f to 0xFFFF5252, 1f to 0xFFFFCDD2))
        add(pal(69, "Red Tide", 0f to 0xFF4A0000, 0.4f to 0xFFC62828, 0.7f to 0xFFFF8A65, 1f to 0xFFFFE0B2))
        add(pal(70, "Candy2", 0f to 0xFFFF5252, 0.25f to 0xFFFFEA00, 0.5f to 0xFF69F0AE, 0.75f to 0xFF40C4FF, 1f to 0xFFE040FB))
        add(pal(71, "Traffic Light", 0f to 0xFFFF0000, 0.33f to 0xFFFFFF00, 0.66f to 0xFF00FF00, 1f to 0xFF000000))

        // Ensure exactly 72 entries (0..71)
        while (list.size < 72) {
            val id = list.size
            list += WledPalette(id, "Palette $id", listOf(0f to Color.DarkGray, 1f to Color.Gray))
        }
        return list.take(72)
    }
}