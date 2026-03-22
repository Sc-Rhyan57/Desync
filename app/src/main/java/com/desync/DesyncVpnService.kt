package com.desync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.random.Random

class DesyncVpnService : VpnService() {

    companion object {
        const val ACTION_START  = "com.desync.START"
        const val ACTION_STOP   = "com.desync.STOP"
        const val CHANNEL_ID    = "desync_vpn"
        const val NOTIF_ID      = 1

        val isRunning      = AtomicBoolean(false)
        val totalPackets   = AtomicLong(0L)
        val delayedPackets = AtomicLong(0L)
        val droppedPackets = AtomicLong(0L)
        val lastDelayMs    = AtomicLong(0L)

        var minLagMs      = 60L
        var maxLagMs      = 180L
        var dropPercent   = 0f
        var spikePercent  = 5f
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunnelJob: Job? = null

    data class DelayedPacket(val data: ByteArray, val releaseAt: Long, val proto: Byte)

    private val outQueue = ConcurrentLinkedQueue<DelayedPacket>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        if (!isRunning.get()) {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        createNotificationChannel()
        val notif = buildNotification()
        startForeground(NOTIF_ID, notif)

        val builder = Builder()
            .setSession("Desync")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .addDnsServer("1.1.1.1")
            .setMtu(1500)
            .setBlocking(true)

        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) {}

        tunInterface = builder.establish() ?: return
        isRunning.set(true)
        totalPackets.set(0L)
        delayedPackets.set(0L)
        droppedPackets.set(0L)
        DesyncLog.add("START", "TUN established — intercepting all device traffic")

        tunnelJob = scope.launch { runTunnel() }
    }

    private fun stopVpn() {
        tunnelJob?.cancel()
        tunInterface?.close()
        tunInterface = null
        isRunning.set(false)
        outQueue.clear()
        DesyncLog.add("STOP", "TUN closed — traffic restored to normal")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun runTunnel() {
        val tun   = tunInterface ?: return
        val inFd  = FileInputStream(tun.fileDescriptor)
        val outFd = FileOutputStream(tun.fileDescriptor)
        val buf   = ByteBuffer.allocate(32767)

        scope.launch { drainQueue(outFd) }

        while (isActive) {
            buf.clear()
            val len = inFd.read(buf.array())
            if (len <= 0) { delay(1); continue }

            val pkt = buf.array().copyOf(len)
            totalPackets.incrementAndGet()

            if (dropPercent > 0f && Random.nextFloat() < dropPercent / 100f) {
                droppedPackets.incrementAndGet()
                DesyncLog.add("DROP", "Packet dropped — ${len}B (loss=${dropPercent.toInt()}%)")
                continue
            }

            val proto: Byte = if (len >= 10) pkt[9] else 0
            val lagMs = computeLag()
            lastDelayMs.set(lagMs)
            delayedPackets.incrementAndGet()

            val releaseAt = System.currentTimeMillis() + lagMs
            outQueue.add(DelayedPacket(pkt, releaseAt, proto))

            val protoName = when (proto.toInt() and 0xFF) { 6 -> "TCP"; 17 -> "UDP"; 1 -> "ICMP"; else -> "IP" }
            DesyncLog.add("LAG", "Anchor +${lagMs}ms [$protoName ${len}B]")
        }
    }

    private suspend fun drainQueue(outFd: FileOutputStream) {
        while (isActive) {
            val now  = System.currentTimeMillis()
            val iter = outQueue.iterator()
            while (iter.hasNext()) {
                val pkt = iter.next()
                if (now >= pkt.releaseAt) {
                    try {
                        outFd.write(pkt.data)
                        DesyncLog.add("OUT", "Released ${pkt.data.size}B → real NIC")
                    } catch (_: Exception) {}
                    iter.remove()
                    lastDelayMs.set(0L)
                }
            }
            delay(2)
        }
    }

    private fun computeLag(): Long {
        val isSpike = spikePercent > 0f && Random.nextFloat() < spikePercent / 100f
        return if (isSpike) {
            val base = if (maxLagMs > minLagMs)
                minLagMs + abs(Random.nextLong()) % (maxLagMs - minLagMs)
            else maxLagMs
            (base * (1.5f + Random.nextFloat() * 2f)).toLong()
                .also { DesyncLog.add("SPIKE", "Lag spike! +${it}ms") }
        } else {
            if (maxLagMs > minLagMs)
                minLagMs + abs(Random.nextLong()) % (maxLagMs - minLagMs)
            else minLagMs
        }
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Desync VPN", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Desync fake lag service"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, DesyncVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID)
        else
            @Suppress("DEPRECATION") Notification.Builder(this)

        return builder
            .setContentTitle("Desync Active")
            .setContentText("Fake lag engine running — ${minLagMs}–${maxLagMs}ms")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pi)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPi)
            .setOngoing(true)
            .build()
    }
}

object DesyncLog {
    private val _logs   = mutableListOf<LogEntry>()
    val logs: List<LogEntry> get() = _logs.toList()
    val enabled = AtomicBoolean(true)

    data class LogEntry(val ts: String, val level: String, val message: String)

    fun add(level: String, message: String) {
        if (!enabled.get()) return
        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        synchronized(_logs) {
            _logs.add(0, LogEntry(ts, level, message))
            if (_logs.size > 400) _logs.removeAt(_logs.size - 1)
        }
    }

    fun clear() = synchronized(_logs) { _logs.clear() }

    fun snapshot(): List<LogEntry> = synchronized(_logs) { _logs.toList() }
}
