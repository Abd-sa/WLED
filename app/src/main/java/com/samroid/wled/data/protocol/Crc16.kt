package com.samroid.wled.data.protocol

object Crc16 {
    private const val POLY = 0x1021
    private const val INIT = 0xFFFF

    fun calculate(data: ByteArray): Int {
        var crc = INIT
        for (b in data) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor POLY
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }

    fun toBytes(crc: Int): ByteArray {
        return byteArrayOf(
            ((crc shr 8) and 0xFF).toByte(), // CRC_H
            (crc and 0xFF).toByte()          // CRC_L
        )
    }
}