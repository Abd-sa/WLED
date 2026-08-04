package com.samroid.wled.data.protocol

import com.samroid.wled.data.protocol.Crc16

object Protocol {
    const val HEADER1: Byte = 0xAA.toByte()
    const val HEADER2: Byte = 0x55.toByte()
    const val VERSION: Byte = 0x01

    // Commands
    const val CMD_PING: Byte = 0x01
    const val CMD_GET_INFO: Byte = 0x02
    const val CMD_WIFI_CONNECT: Byte = 0x11
    const val CMD_NETWORK_CONFIG: Byte = 0x12
    const val CMD_SCAN_WLED: Byte = 0x20
    const val CMD_PROVISION: Byte = 0x21
    const val CMD_NODE_LIST: Byte = 0x30
    const val CMD_NODE_INFO: Byte = 0x31
    const val CMD_SET_EFFECT: Byte = 0x40
    const val CMD_SET_BRIGHTNESS: Byte = 0x41
    const val CMD_SET_COLOR: Byte = 0x42
    const val CMD_SET_CCT: Byte = 0x43
    const val CMD_ON_OFF: Byte = 0x44
    const val CMD_SET_EFFECT_SX: Byte = 0x45
    const val CMD_SET_EFFECT_IX: Byte = 0x46
    const val CMD_SET_EFFECT_PAL: Byte = 0x47
    const val CMD_PRESET_SAVE: Byte = 0x60
    const val CMD_PRESET_LOAD: Byte = 0x61
    const val CMD_UDP_START: Byte = 0x70
    const val CMD_UDP_STOP: Byte = 0x71
    const val CMD_UDP_MAP_SET: Byte = 0x72
    const val CMD_UDP_STREAM_ENABLE: Byte = 0x73

    // Provision steps
    const val CMD_GPIO_VALUE: Byte = 0x90.toByte()
    const val CMD_GPIO_CONFIRM: Byte = 0x91.toByte()
    const val CMD_COLOR_VALUE: Byte = 0x92.toByte()
    const val CMD_COLOR_CONFIRM: Byte = 0x93.toByte()
    const val CMD_LENGTH_VALUE: Byte = 0x94.toByte()
    const val CMD_LENGTH_CONFIRM: Byte = 0x95.toByte()
    const val CMD_OUTPUT_VALUE: Byte = 0x96.toByte()
    const val CMD_OUTPUT_CONFIRM: Byte = 0x97.toByte()
    const val CMD_STORE_VALUE: Byte = 0x98.toByte()
    const val CMD_CANCEL: Byte = 0x99.toByte()

    // Responses
    const val RESP_ACK: Byte = 0x80.toByte()
    const val RESP_NACK: Byte = 0x81.toByte()
    const val RESP_NODE_LIST: Byte = 0x82.toByte()
    const val RESP_NODE_INFO: Byte = 0x83.toByte()

    private var sequence: Int = 0

    fun nextSequence(): Byte {
        sequence = (sequence + 1) and 0xFF
        return sequence.toByte()
    }

    fun buildPacket(command: Byte, payload: ByteArray = byteArrayOf()): ByteArray {
        val seq = nextSequence()
        val length = payload.size.toByte()

        // داده برای CRC: HEADER1..PAYLOAD
        val forCrc = ByteArray(6 + payload.size)
        forCrc[0] = HEADER1
        forCrc[1] = HEADER2
        forCrc[2] = VERSION
        forCrc[3] = command
        forCrc[4] = seq
        forCrc[5] = length
        if (payload.isNotEmpty()) {
            System.arraycopy(payload, 0, forCrc, 6, payload.size)
        }

        val crc = Crc16.calculate(forCrc)
        val crcBytes = Crc16.toBytes(crc)

        return forCrc + crcBytes
    }

    fun parsePacket(data: ByteArray): ParsedPacket? {
        if (data.size < 8) return null
        if (data[0] != HEADER1 || data[1] != HEADER2) return null
        if (data[2] != VERSION) return null

        val command = data[3]
        val seq = data[4]
        val length = data[5].toInt() and 0xFF

        if (data.size < 6 + length + 2) return null

        val payload = if (length > 0) data.copyOfRange(6, 6 + length) else byteArrayOf()
        val receivedCrc = ((data[6 + length].toInt() and 0xFF) shl 8) or
                (data[7 + length].toInt() and 0xFF)

        val forCrc = data.copyOfRange(0, 6 + length)
        val calculatedCrc = Crc16.calculate(forCrc)

        if (receivedCrc != calculatedCrc) return null

        return ParsedPacket(command, seq, payload)
    }

    data class ParsedPacket(
        val command: Byte,
        val sequence: Byte,
        val payload: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParsedPacket

            if (command != other.command) return false
            if (sequence != other.sequence) return false
            if (!payload.contentEquals(other.payload)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = command
            result = (31 * result + sequence).toByte()
            result = (31 * result + payload.contentHashCode()).toByte()
            return result.toInt()
        }
    }
}