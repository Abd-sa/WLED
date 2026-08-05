package com.samroid.wled.data.ambient

/**
 * Pixel byte order for WLED UDP/DDP payloads (must match controller wiring).
 */
enum class ColorOrder {
    RGB, RBG, GRB, GBR, BRG, BGR;

    fun pack(r: Int, g: Int, b: Int): ByteArray {
        val R = r.coerceIn(0, 255).toByte()
        val G = g.coerceIn(0, 255).toByte()
        val B = b.coerceIn(0, 255).toByte()
        return when (this) {
            RGB -> byteArrayOf(R, G, B)
            RBG -> byteArrayOf(R, B, G)
            GRB -> byteArrayOf(G, R, B)
            GBR -> byteArrayOf(G, B, R)
            BRG -> byteArrayOf(B, R, G)
            BGR -> byteArrayOf(B, G, R)
        }
    }
}