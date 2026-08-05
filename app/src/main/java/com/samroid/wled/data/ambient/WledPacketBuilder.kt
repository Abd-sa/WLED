package com.samroid.wled.data.ambient

/**
 * Builds WLED realtime packets (DDP + UDP Raw).
 * DDP header layout follows common WLED/DDP practice (10-byte header + RGB data).
 */
object WledPacketBuilder {

    private const val DDP_MAX_DATA = 1440 // 480 RGB pixels per packet

    /**
     * @param colors ARGB ints, one per LED in strip order
     */
    fun buildUdpRaw(colors: IntArray, order: ColorOrder): ByteArray {
        val out = ByteArray(colors.size * 3)
        var i = 0
        for (c in colors) {
            val rgb = order.pack(
                (c shr 16) and 0xFF,
                (c shr 8) and 0xFF,
                c and 0xFF
            )
            out[i++] = rgb[0]
            out[i++] = rgb[1]
            out[i++] = rgb[2]
        }
        return out
    }

    /**
     * Split into one or more DDP datagrams.
     */
    fun buildDdpPackets(
        colors: IntArray,
        order: ColorOrder,
        sequence: Int
    ): List<ByteArray> {
        val packets = mutableListOf<ByteArray>()
        var offsetLeds = 0
        var seq = sequence and 0xFF
        while (offsetLeds < colors.size) {
            val remaining = colors.size - offsetLeds
            val chunkLeds = minOf(remaining, DDP_MAX_DATA / 3)
            val dataLen = chunkLeds * 3
            val packet = ByteArray(10 + dataLen)

            // flags: version=1, push on last chunk
            val isLast = offsetLeds + chunkLeds >= colors.size
            packet[0] = ((0x40) or (if (isLast) 0x01 else 0x00)).toByte()
            packet[1] = seq.toByte()
            packet[2] = 0x01 // data type RGB
            packet[3] = 0x01 // destination id

            val byteOffset = offsetLeds * 3
            packet[4] = ((byteOffset ushr 24) and 0xFF).toByte()
            packet[5] = ((byteOffset ushr 16) and 0xFF).toByte()
            packet[6] = ((byteOffset ushr 8) and 0xFF).toByte()
            packet[7] = (byteOffset and 0xFF).toByte()
            packet[8] = ((dataLen ushr 8) and 0xFF).toByte()
            packet[9] = (dataLen and 0xFF).toByte()

            var p = 10
            for (i in 0 until chunkLeds) {
                val c = colors[offsetLeds + i]
                val rgb = order.pack(
                    (c shr 16) and 0xFF,
                    (c shr 8) and 0xFF,
                    c and 0xFF
                )
                packet[p++] = rgb[0]
                packet[p++] = rgb[1]
                packet[p++] = rgb[2]
            }
            packets += packet
            offsetLeds += chunkLeds
            seq = (seq + 1) and 0xFF
        }
        return packets
    }
}