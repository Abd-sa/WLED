package com.samroid.wled.data.protocol

import com.samroid.wled.domain.model.DeviceResponse
import com.samroid.wled.domain.model.NodeInfoData
import com.samroid.wled.domain.model.NodeListItem

object ResponseParser {

    fun parse(packet: Protocol.ParsedPacket): DeviceResponse {
        return when (packet.command) {
            Protocol.RESP_ACK -> {
                DeviceResponse.Ack(packet.sequence, packet.payload)
            }

            Protocol.RESP_NACK -> {
                val code = packet.payload.firstOrNull()?.toInt()?.and(0xFF)
                DeviceResponse.Nack(packet.sequence, code, packet.payload)
            }

            Protocol.RESP_NODE_LIST -> {
                DeviceResponse.NodeList(
                    sequence = packet.sequence,
                    nodes = parseNodeList(packet.payload)
                )
            }

            Protocol.RESP_NODE_INFO -> {
                val info = parseNodeInfo(packet.payload)
                if (info != null) {
                    DeviceResponse.NodeInfo(packet.sequence, info)
                } else {
                    DeviceResponse.Unknown(packet.command, packet.sequence, packet.payload)
                }
            }

            else -> DeviceResponse.Unknown(packet.command, packet.sequence, packet.payload)
        }
    }

    /**
     * Payload NODE_LIST:
     * [count][id0][online0][id1][online1]...
     */
    private fun parseNodeList(payload: ByteArray): List<NodeListItem> {
        if (payload.isEmpty()) return emptyList()
        val count = payload[0].toInt() and 0xFF
        val list = mutableListOf<NodeListItem>()
        var i = 1
        repeat(count) {
            if (i + 1 >= payload.size) return@repeat
            val id = payload[i].toInt() and 0xFF
            val online = payload[i + 1].toInt() and 0xFF == 1
            list.add(NodeListItem(id, online))
            i += 2
        }
        return list
    }

    /**
     * Payload NODE_INFO (طبق مستند):
     * 0: nodeId (1)
     * 1..40: deviceId (40)
     * 41..44: IP (4)
     * 45: RSSI (1)
     * 46..47: LED count big-endian (2)
     * 48: CCT (1)
     * 49: UDP (1)
     * 50: processor (1)
     * 51..52: start pixel (2)
     * 53..54: end pixel (2)
     *
     * حداقل طول مورد انتظار ≈ 55 بایت
     */
    private fun parseNodeInfo(payload: ByteArray): NodeInfoData? {
        if (payload.size < 55) return null

        val nodeId = payload[0].toInt() and 0xFF

        val deviceIdBytes = payload.copyOfRange(1, 41)
        val deviceId = deviceIdBytes
            .toString(Charsets.US_ASCII)
            .trim { it <= ' ' || it == '\u0000' }
            .ifEmpty {
                deviceIdBytes.joinToString("") { "%02x".format(it) }
            }

        val ip = "%d.%d.%d.%d".format(
            payload[41].toInt() and 0xFF,
            payload[42].toInt() and 0xFF,
            payload[43].toInt() and 0xFF,
            payload[44].toInt() and 0xFF
        )

        // RSSI در مستند به‌صورت بایت خام آمده؛ مقدار نمونه‌ها منفی‌اند
        val rssiRaw = payload[45].toInt() and 0xFF
        val rssi = if (rssiRaw > 127) rssiRaw - 256 else rssiRaw

        val ledCount = ((payload[46].toInt() and 0xFF) shl 8) or (payload[47].toInt() and 0xFF)
        val cctEnabled = payload[48].toInt() and 0xFF == 1
        val udpEnabled = payload[49].toInt() and 0xFF == 1
        val processorId = payload[50].toInt() and 0xFF
        val startPixel = ((payload[51].toInt() and 0xFF) shl 8) or (payload[52].toInt() and 0xFF)
        val endPixel = ((payload[53].toInt() and 0xFF) shl 8) or (payload[54].toInt() and 0xFF)

        return NodeInfoData(
            nodeId = nodeId,
            deviceId = deviceId,
            ip = ip,
            rssi = rssi,
            ledCount = ledCount,
            cctEnabled = cctEnabled,
            udpEnabled = udpEnabled,
            processorId = processorId,
            startPixel = startPixel,
            endPixel = endPixel
        )
    }
}