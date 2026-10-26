package com.example.service.vpn

import java.nio.ByteBuffer

object DnsPacketParser {
    /**
     * Inspects a standard UDP DNS request packet and extracts the QNAME (host).
     * Returns null if packet is not a DNS query or has no questions.
     */
    fun extractDomainFromDnsPacket(packet: ByteBuffer): String? {
        try {
            if (packet.remaining() < 28) return null // 20 byte IP + 8 byte UDP min

            val ipHeaderLength = (packet.get(0).toInt() and 0x0F) * 4
            val dnsOffset = ipHeaderLength + 8
            if (packet.limit() < dnsOffset + 12) return null

            packet.position(dnsOffset)
            val flags = packet.getShort(dnsOffset + 2).toInt()
            val isQuery = (flags and 0x8000) == 0
            if (!isQuery) return null

            val questionCount = packet.getShort(dnsOffset + 4).toInt()
            if (questionCount < 1) return null

            packet.position(dnsOffset + 12)
            val domainBuilder = StringBuilder()

            while (packet.hasRemaining()) {
                val labelLength = packet.get().toInt() and 0xFF
                if (labelLength == 0) break // End of QNAME

                if (domainBuilder.isNotEmpty()) {
                    domainBuilder.append('.')
                }

                if (packet.remaining() < labelLength) return null
                val labelBytes = ByteArray(labelLength)
                packet.get(labelBytes)
                domainBuilder.append(String(labelBytes, Charsets.US_ASCII))
            }

            return domainBuilder.toString().lowercase()
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Builds a DNS NXDOMAIN response in-place to reject blocked domains locally.
     */
    fun buildBlockedDnsResponse(request: ByteArray, length: Int): ByteArray {
        val response = ByteArray(length)
        System.arraycopy(request, 0, response, 0, length)

        val ipHeaderLength = (response[0].toInt() and 0x0F) * 4
        val dnsOffset = ipHeaderLength + 8

        // Swap IP addresses (Src <-> Dst)
        for (i in 0 until 4) {
            val temp = response[12 + i]
            response[12 + i] = response[16 + i]
            response[16 + i] = temp
        }

        // Swap UDP Ports
        val srcPort0 = response[ipHeaderLength]
        val srcPort1 = response[ipHeaderLength + 1]
        response[ipHeaderLength] = response[ipHeaderLength + 2]
        response[ipHeaderLength + 1] = response[ipHeaderLength + 3]
        response[ipHeaderLength + 2] = srcPort0
        response[ipHeaderLength + 3] = srcPort1

        // Set DNS Flags: Response, Name Error (NXDOMAIN -> RCODE 3: 0x8183)
        if (response.size > dnsOffset + 3) {
            response[dnsOffset + 2] = 0x81.toByte()
            response[dnsOffset + 3] = 0x83.toByte()
        }

        return response
    }
}
