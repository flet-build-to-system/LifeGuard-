package com.example.service.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.local.LifeGuardDatabase
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class LifeGuardDnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isRunning = false

    companion object {
        const val ACTION_START = "com.example.service.vpn.START"
        const val ACTION_STOP = "com.example.service.vpn.STOP"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "lifeguard_vpn_channel"
        private const val UPSTREAM_FAMILY_DNS = "1.1.1.3" // Cloudflare Family (blocks malware + adult content)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return
        isRunning = true

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            try {
                val builder = Builder()
                    .addAddress("10.10.0.2", 32)
                    .addRoute("1.1.1.3", 32)
                    .addDnsServer("10.10.0.2")
                    .setSession("LifeGuard Web Shield")
                    .setBlocking(true)

                vpnInterface = builder.establish()
                runPacketLoop()
            } catch (_: Exception) {
                stopVpn()
            }
        }
    }

    private suspend fun runPacketLoop() = withContext(Dispatchers.IO) {
        val vpnFileDescriptor = vpnInterface?.fileDescriptor ?: return@withContext
        val inputStream = FileInputStream(vpnFileDescriptor)
        val outputStream = FileOutputStream(vpnFileDescriptor)
        val packet = ByteBuffer.allocate(32767)

        val upstreamSocket = DatagramSocket()
        protect(upstreamSocket)
        val upstreamIp = InetAddress.getByName(UPSTREAM_FAMILY_DNS)
        val buffer = ByteArray(32767)

        val blockedDomainDao = LifeGuardDatabase.getInstance(applicationContext).blockedDomainDao()

        while (isActive && isRunning) {
            try {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    packet.limit(length)
                    val domain = DnsPacketParser.extractDomainFromDnsPacket(packet)

                    if (domain != null && isBlocked(domain, blockedDomainDao)) {
                        blockedDomainDao.incrementBlockedHits(domain)
                        val response = DnsPacketParser.buildBlockedDnsResponse(packet.array(), length)
                        outputStream.write(response)
                    } else {
                        val ipHeaderLen = (packet.array()[0].toInt() and 0x0F) * 4
                        val dnsOffset = ipHeaderLen + 8
                        val dnsPayloadLength = length - dnsOffset
                        if (dnsPayloadLength > 0 && dnsOffset < length) {
                            val dnsQueryPacket = DatagramPacket(
                                packet.array(), dnsOffset, dnsPayloadLength, upstreamIp, 53
                            )
                            upstreamSocket.send(dnsQueryPacket)

                            val responseDatagram = DatagramPacket(buffer, buffer.size)
                            upstreamSocket.receive(responseDatagram)
                            outputStream.write(responseDatagram.data, 0, responseDatagram.length)
                        }
                    }
                    packet.clear()
                }
            } catch (_: Exception) {
                // Ignore transient network breaks
            }
        }
    }

    private suspend fun isBlocked(domain: String, dao: com.example.data.local.dao.BlockedDomainDao): Boolean {
        val parts = domain.split(".")
        for (i in parts.indices) {
            val subDomain = parts.subList(i, parts.size).joinToString(".")
            if (dao.isDomainBlocked(subDomain)) {
                return true
            }
        }
        return false
    }

    private fun stopVpn() {
        isRunning = false
        serviceJob.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LifeGuard Web Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "فلترة آمنة ومحلية لنطاقات الإنترنت والمواقع الضارة"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_running))
            .setContentText("الحماية الرقمية نشطة وخالية من التعقب")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
