package com.samroid.wled.domain.model

sealed class DeviceResponse {
    data class Ack(
        val sequence: Byte,
        val payload: ByteArray = byteArrayOf()
    ) : DeviceResponse() {
        /** GET_INFO */
        fun asInfo(): ControllerInfo? {
            if (payload.size < 4) return null
            return ControllerInfo(
                major = payload[0].toInt() and 0xFF,
                minor = payload[1].toInt() and 0xFF,
                patch = payload[2].toInt() and 0xFF,
                nodeCount = payload[3].toInt() and 0xFF
            )
        }

        /** SCAN_WLED */
        fun asScanCount(): Int? =
            if (payload.isNotEmpty()) payload[0].toInt() and 0xFF else null
    }

    data class Nack(
        val sequence: Byte,
        val errorCode: Int?,
        val rawPayload: ByteArray
    ) : DeviceResponse() {
        fun errorMessage(): String = when (errorCode) {
            1 -> "Unknown Request"
            2 -> "Invalid Length"
            3 -> "CRC Error"
            4 -> "Invalid Version"
            5 -> "Timeout"
            6 -> "Max Buffer Reached"
            7 -> "WiFi Error"
            8 -> "Node Not Found"
            9 -> "Failed To Save"
            null -> "NACK Not Fond"
            else -> "Unknown Error : ($errorCode)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Nack

            if (sequence != other.sequence) return false
            if (errorCode != other.errorCode) return false
            if (!rawPayload.contentEquals(other.rawPayload)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = sequence
            result = (31 * result + (errorCode ?: 0)).toByte()
            result = (31 * result + rawPayload.contentHashCode()).toByte()
            return result.toInt()
        }
    }

    data class NodeList(
        val sequence: Byte,
        val nodes: List<NodeListItem>
    ) : DeviceResponse()

    data class NodeInfo(
        val sequence: Byte,
        val info: NodeInfoData
    ) : DeviceResponse()

    data class Unknown(
        val command: Byte,
        val sequence: Byte,
        val payload: ByteArray
    ) : DeviceResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Unknown

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