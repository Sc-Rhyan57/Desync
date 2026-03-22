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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.random.Random

class DesyncVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.desync.START"
        const val ACTION_STOP  = "com.desync.STOP"
        const val CHANNEL_ID   = "desync_vpn"
        const val NOTIF_ID     = 1

        val isRunning      = AtomicBoolean(false)
        val totalPackets   = AtomicLong(0L)
        val delayedPackets = AtomicLong(0L)
        val droppedPackets = AtomicLong(0L)
        val lastDelayMs    = AtomicLong(0L)

        @Volatile var minLagMs      = 60L
        @Volatile var maxLagMs      = 180L
        @Volatile var dropPercent   = 0f
        @Volatile var spikePercent  = 5f
    }

    private var tunFd: ParcelFileDescriptor? = null
    private val scope      = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunnelJob: Job? = null

    private data class DelayedPacket(val data: ByteArray, val releaseAt: Long)

    private val queue = ConcurrentLinkedQueue<DelayedPacket>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTun()
            return START_NOT_STICKY
        }
        if (!isRunning.get()) startTun()
        return START_STICKY
    }

    private fun startTun() {
        createChannel()
        startForeground(NOTIF_ID, buildNotif())

        val tun = Builder()
            .setSession("Desync")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .addDnsServer("1.1.1.1")
            .setMtu(1500)
            .setBlocking(true)
            .also { try { it.addDisallowedApplication(packageName) } catch (_: Exception) {} }
            .establish() ?: return

        tunFd = tun
        isRunning.set(true)
        totalPackets.set(0L)
        delayedPackets.set(0L)
        droppedPackets.set(0L)
        DesyncLog.add("START", "TUN ativo — interceptando todo o tráfego do dispositivo")

        tunnelJob = scope.launch { runTunnel() }
    }

    private fun stopTun() {
        tunnelJob?.cancel()
        tunFd?.close()
        tunFd = null
        isRunning.set(false)
        queue.clear()
        lastDelayMs.set(0L)
        DesyncLog.add("STOP", "TUN fechado — tráfego restaurado ao normal")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun runTunnel() {
        val fd    = tunFd ?: return
        val inFd  = FileInputStream(fd.fileDescriptor)
        val outFd = FileOutputStream(fd.fileDescriptor)
        val buf   = ByteArray(32767)

        scope.launch { drain(outFd) }

        while (currentCoroutineContext().isActive) {
            val len = inFd.read(buf)
            if (len <= 0) { delay(1); continue }

            totalPackets.incrementAndGet()

            if (dropPercent > 0f && Random.nextFloat() < dropPercent / 100f) {
                droppedPackets.incrementAndGet()
                val proto = protoName(if (len >= 10) buf[9].toInt() and 0xFF else 0)
                DesyncLog.add("DROP", "Descartado $proto ${len}B (${dropPercent.toInt()}% loss)")
                continue
            }

            val lagMs     = calcLag()
            val releaseAt = System.currentTimeMillis() + lagMs
            lastDelayMs.set(lagMs)
            delayedPackets.incrementAndGet()

            val proto = protoName(if (len >= 10) buf[9].toInt() and 0xFF else 0)
            DesyncLog.add("LAG", "+${lagMs}ms [$proto ${len}B] (âncora)")
            queue.add(DelayedPacket(buf.copyOf(len), releaseAt))
        }
    }

    private suspend fun drain(outFd: FileOutputStream) {
        while (currentCoroutineContext().isActive) {
            val now  = System.currentTimeMillis()
            val iter = queue.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                if (now >= p.releaseAt) {
                    try { outFd.write(p.data) } catch (_: Exception) {}
                    iter.remove()
                    lastDelayMs.set(0L)
                }
            }
            delay(2)
        }
    }

    private fun calcLag(): Long {
        val base = if (maxLagMs > minLagMs)
            minLagMs + abs(Random.nextLong()) % (maxLagMs - minLagMs)
        else minLagMs

        return if (spikePercent > 0f && Random.nextFloat() < spikePercent / 100f) {
            val spike = (base * (1.5f + Random.nextFloat() * 2f)).toLong()
            DesyncLog.add("SPIKE", "Spike! +${spike}ms")
            spike
        } else base
    }

    private fun protoName(proto: Int) = when (proto) {
        6 -> "TCP"; 17 -> "UDP"; 1 -> "ICMP"; else -> "IP"
    }

    override fun onDestroy() { stopTun(); super.onDestroy() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Desync VPN", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Fake lag engine"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotif(): Notification {
        val contentPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, DesyncVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Desync Ativo")
                .setContentText("Lag: ${minLagMs}–${maxLagMs}ms em todos os pacotes")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentIntent(contentPi)
                .addAction(
                    Notification.Action.Builder(
                        null, "Parar",
                        stopPi
                    ).build()
                )
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Desync Ativo")
                .setContentText("Lag: ${minLagMs}–${maxLagMs}ms em todos os pacotes")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentIntent(contentPi)
                .setOngoing(true)
                .build()
        }
    }
}

object DesyncLog {
    data class LogEntry(val ts: String, val level: String, val message: String)

    private val _logs = mutableListOf<LogEntry>()
    val enabled       = AtomicBoolean(true)

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
