package com.example

import com.example.domain.model.ProtectionProfile
import com.example.service.vpn.DnsPacketParser
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class ExampleUnitTest {

    @Test
    fun testDnsDomainExtraction() {
        // Construct a synthetic DNS query buffer:
        // 20 bytes IP Header (IHL=5 -> 20 bytes) + 8 bytes UDP Header + DNS Payload
        val packet = ByteBuffer.allocate(64)
        packet.put(0, 0x45.toByte()) // IPv4 with IHL = 5 (20 bytes)

        val dnsOffset = 20 + 8
        packet.putShort(dnsOffset + 2, 0x0100.toShort()) // Flags: standard query
        packet.putShort(dnsOffset + 4, 1.toShort())      // Question count = 1

        // QNAME: 7'e','x','a','m','p','l','e', 3'c','o','m', 0
        var pos = dnsOffset + 12
        packet.put(pos++, 7.toByte())
        "example".toByteArray(Charsets.US_ASCII).forEach { packet.put(pos++, it) }
        packet.put(pos++, 3.toByte())
        "com".toByteArray(Charsets.US_ASCII).forEach { packet.put(pos++, it) }
        packet.put(pos++, 0.toByte()) // null terminator

        packet.limit(pos)

        val parsedDomain = DnsPacketParser.extractDomainFromDnsPacket(packet)
        assertNotNull(parsedDomain)
        assertEquals("example.com", parsedDomain)
    }

    @Test
    fun testBlockedDnsResponseGeneration() {
        val request = ByteArray(64)
        request[0] = 0x45.toByte()
        // Source IP: 192.168.1.50
        request[12] = 192.toByte()
        request[13] = 168.toByte()
        request[14] = 1.toByte()
        request[15] = 50.toByte()
        // Dst IP: 1.1.1.3
        request[16] = 1.toByte()
        request[17] = 1.toByte()
        request[18] = 1.toByte()
        request[19] = 3.toByte()

        val response = DnsPacketParser.buildBlockedDnsResponse(request, request.size)

        // Verify IP addresses swapped
        assertEquals(1.toByte(), response[12])
        assertEquals(1.toByte(), response[13])
        assertEquals(1.toByte(), response[14])
        assertEquals(3.toByte(), response[15])
        assertEquals(192.toByte(), response[16])
        assertEquals(168.toByte(), response[17])
        assertEquals(1.toByte(), response[18])
        assertEquals(50.toByte(), response[19])

        // Verify DNS flags (NXDOMAIN -> RCODE 3: 0x8183)
        val dnsOffset = 20 + 8
        assertEquals(0x81.toByte(), response[dnsOffset + 2])
        assertEquals(0x83.toByte(), response[dnsOffset + 3])
    }

    @Test
    fun testProtectionProfilesEnum() {
        assertEquals(3, ProtectionProfile.values().size)
        assertNotNull(ProtectionProfile.GENTLE.titleAr)
        assertNotNull(ProtectionProfile.BALANCED.titleAr)
        assertNotNull(ProtectionProfile.STRICT.titleAr)
    }
}
