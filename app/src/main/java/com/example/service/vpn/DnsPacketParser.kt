package com.example.service.vpn

import java.nio.ByteBuffer

object DnsPacketParser {
    /**
     * Inspects a standard UDP DNS request packet and extracts the QNAME (host).
     * Returns null if packet is not a DNS query or has no questions.
     */
    fun extractDomainFromDnsPacket(packet: ByteBuffer): String? {
        try {
            if (packet.remaining() < 28) return null

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
                if (labelLength == 0) break

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

        // UDP Checksum to 0 (disabled)
        response[ipHeaderLength + 6] = 0
        response[ipHeaderLength + 7] = 0

        // Set DNS Flags: Response, Name Error (NXDOMAIN -> RCODE 3: 0x8183)
        if (response.size > dnsOffset + 3) {
            response[dnsOffset + 2] = 0x81.toByte()
            response[dnsOffset + 3] = 0x83.toByte()
        }

        // Recalculate IP Checksum
        response[10] = 0
        response[11] = 0
        val checksum = computeChecksum(response, ipHeaderLength)
        response[10] = ((checksum shr 8) and 0xFF).toByte()
        response[11] = (checksum and 0xFF).toByte()

        return response
    }

    /**
     * Builds a complete, valid IPv4 + UDP packet wrapping an upstream DNS answer
     * to deliver back through the TUN interface to the local requesting app.
     */
    fun buildForwardedDnsResponsePacket(
        requestPacket: ByteArray,
        ipHeaderLen: Int,
        dnsAnswerPayload: ByteArray,
        dnsAnswerLen: Int
    ): ByteArray {
        val totalLen = ipHeaderLen + 8 + dnsAnswerLen
        val response = ByteArray(totalLen)

        // 1. Copy original IP Header
        System.arraycopy(requestPacket, 0, response, 0, ipHeaderLen)

        // 2. Set Total Length in IPv4 Header
        response[2] = ((totalLen shr 8) and 0xFF).toByte()
        response[3] = (totalLen and 0xFF).toByte()

        // 3. Flags: Don't fragment
        response[6] = 0x40.toByte()
        response[7] = 0x00.toByte()

        // 4. TTL
        response[8] = 64.toByte()

        // 5. Protocol: UDP (17)
        response[9] = 17.toByte()

        // 6. Swap IPs: Source is DNS server (was Dst in query), Destination is Client (was Src in query)
        for (i in 0 until 4) {
            response[12 + i] = requestPacket[16 + i]
            response[16 + i] = requestPacket[12 + i]
        }

        // 7. Calculate IPv4 Header Checksum
        response[10] = 0
        response[11] = 0
        val checksum = computeChecksum(response, ipHeaderLen)
        response[10] = ((checksum shr 8) and 0xFF).toByte()
        response[11] = (checksum and 0xFF).toByte()

        // 8. Build UDP Header (8 bytes at ipHeaderLen)
        // Source Port = 53 (DNS server)
        response[ipHeaderLen] = 0
        response[ipHeaderLen + 1] = 53

        // Destination Port = original client source port
        response[ipHeaderLen + 2] = requestPacket[ipHeaderLen]
        response[ipHeaderLen + 3] = requestPacket[ipHeaderLen + 1]

        // UDP Length = 8 + dnsAnswerLen
        val udpLen = 8 + dnsAnswerLen
        response[ipHeaderLen + 4] = ((udpLen shr 8) and 0xFF).toByte()
        response[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()

        // UDP Checksum = 0 (optional in IPv4)
        response[ipHeaderLen + 6] = 0
        response[ipHeaderLen + 7] = 0

        // 9. Copy DNS payload
        System.arraycopy(dnsAnswerPayload, 0, response, ipHeaderLen + 8, dnsAnswerLen)

        return response
    }

    private fun computeChecksum(data: ByteArray, length: Int): Int {
        var sum = 0
        var i = 0
        while (i < length) {
            val high = (data[i].toInt() and 0xFF) shl 8
            val low = if (i + 1 < length) data[i + 1].toInt() and 0xFF else 0
            sum += (high or low)
            i += 2
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv()) and 0xFFFF
    }
}
