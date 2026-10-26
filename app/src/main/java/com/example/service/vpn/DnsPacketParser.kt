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
     * Builds a standard DNS Sinkhole (0.0.0.0 / ::) response.
     * This is the industry-standard method used by Pi-hole and AdGuard because
     * modern browsers like Chrome immediately reject 0.0.0.0 without retrying DoH or external resolvers.
     */
    fun buildBlockedDnsResponse(request: ByteArray, length: Int): ByteArray {
        try {
            val ipHeaderLength = (request[0].toInt() and 0x0F) * 4
            val dnsOffset = ipHeaderLength + 8
            if (length < dnsOffset + 12) return request

            // Parse question length
            var pos = dnsOffset + 12
            while (pos < length) {
                val len = request[pos].toInt() and 0xFF
                pos++
                if (len == 0) break
                pos += len
            }
            // pos is now after QNAME null terminator. Next 2 bytes = QTYPE, 2 bytes = QCLASS
            val qType = if (pos + 2 <= length) {
                ((request[pos].toInt() and 0xFF) shl 8) or (request[pos + 1].toInt() and 0xFF)
            } else 1
            pos += 4 // questionEnd

            val questionLen = pos - (dnsOffset + 12)
            val isA = (qType == 1)
            val isAaaa = (qType == 28)
            val hasAnswer = isA || isAaaa
            val answerDataLen = if (isAaaa) 16 else if (isA) 4 else 0

            // If query is A or AAAA, return Sinkhole (0.0.0.0 or ::).
            // For other queries (such as Type 65 HTTPS / ECH), return authoritative NXDOMAIN (RCODE = 3)
            val dnsPayloadLen = if (hasAnswer) {
                12 + questionLen + 10 + answerDataLen
            } else {
                12 + questionLen
            }
            val totalLen = ipHeaderLength + 8 + dnsPayloadLen
            val response = ByteArray(totalLen)

            // Copy IP header
            System.arraycopy(request, 0, response, 0, ipHeaderLength)
            response[2] = ((totalLen shr 8) and 0xFF).toByte()
            response[3] = (totalLen and 0xFF).toByte()
            response[6] = 0x40.toByte() // Don't fragment
            response[7] = 0x00.toByte()
            response[8] = 64.toByte()   // TTL
            response[9] = 17.toByte()   // UDP

            // Swap IPs
            for (i in 0 until 4) {
                response[12 + i] = request[16 + i]
                response[16 + i] = request[12 + i]
            }

            // Calculate IP Checksum
            response[10] = 0
            response[11] = 0
            val ipChk = computeChecksum(response, ipHeaderLength)
            response[10] = ((ipChk shr 8) and 0xFF).toByte()
            response[11] = (ipChk and 0xFF).toByte()

            // UDP Header
            // Source Port = 53
            response[ipHeaderLength] = 0
            response[ipHeaderLength + 1] = 53
            // Dest Port = client port
            response[ipHeaderLength + 2] = request[ipHeaderLength]
            response[ipHeaderLength + 3] = request[ipHeaderLength + 1]
            val udpLen = 8 + dnsPayloadLen
            response[ipHeaderLength + 4] = ((udpLen shr 8) and 0xFF).toByte()
            response[ipHeaderLength + 5] = (udpLen and 0xFF).toByte()
            response[ipHeaderLength + 6] = 0
            response[ipHeaderLength + 7] = 0

            // DNS Header
            val respDns = ipHeaderLength + 8
            // Transaction ID
            response[respDns] = request[dnsOffset]
            response[respDns + 1] = request[dnsOffset + 1]
            // Flags: If hasAnswer -> 0x8180 (No error), else -> 0x8183 (NXDOMAIN / Name Error)
            response[respDns + 2] = 0x81.toByte()
            response[respDns + 3] = if (hasAnswer) 0x80.toByte() else 0x83.toByte()
            // QDCOUNT = 1
            response[respDns + 4] = 0
            response[respDns + 5] = 1
            // ANCOUNT = 1 or 0
            response[respDns + 6] = 0
            response[respDns + 7] = if (hasAnswer) 1.toByte() else 0.toByte()
            // NSCOUNT = 0
            response[respDns + 8] = 0
            response[respDns + 9] = 0
            // ARCOUNT = 0
            response[respDns + 10] = 0
            response[respDns + 11] = 0

            // Copy Question section
            System.arraycopy(request, dnsOffset + 12, response, respDns + 12, questionLen)

            // Answer Section (only if A or AAAA)
            if (hasAnswer) {
                var ansPos = respDns + 12 + questionLen
                // Name: compression pointer 0xC00C pointing to QNAME at offset 12 in DNS
                response[ansPos++] = 0xC0.toByte()
                response[ansPos++] = 0x0C.toByte()

                // Type (A = 1, AAAA = 28)
                if (isAaaa) {
                    response[ansPos++] = 0x00
                    response[ansPos++] = 0x1C
                } else {
                    response[ansPos++] = 0x00
                    response[ansPos++] = 0x01
                }

                // Class: IN (1)
                response[ansPos++] = 0x00
                response[ansPos++] = 0x01

                // TTL: 60 seconds (0x0000003C)
                response[ansPos++] = 0x00
                response[ansPos++] = 0x00
                response[ansPos++] = 0x00
                response[ansPos++] = 0x3C

                // RDLENGTH
                response[ansPos++] = 0x00
                response[ansPos++] = answerDataLen.toByte()

                // RDATA: 0.0.0.0 (IPv4) or :: (IPv6) - array is already 0 initialized!
            }

            return response
        } catch (_: Exception) {
            return request
        }
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
