package com.desync

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.roundToInt

private object C {
    val Bg      = Color(0xFF0E0F13)
    val Surface = Color(0xFF17181C)
    val Card    = Color(0xFF1E1F26)
    val Border  = Color(0xFF2C2D35)
    val Primary = Color(0xFF6C63FF)
    val Accent  = Color(0xFF00D4FF)
    val Success = Color(0xFF23A55A)
    val Warning = Color(0xFFFAA61A)
    val Error   = Color(0xFFED4245)
    val ErrCont = Color(0xFF3B1A1B)
    val OnErr   = Color(0xFFFFDFDE)
    val White   = Color(0xFFF2F3F5)
    val Sub     = Color(0xFFB5BAC1)
    val Muted   = Color(0xFF72767D)
    val Pink    = Color(0xFFFF6B9D)
}

private object R {
    val S  = 8.dp;  val M  = 12.dp; val L  = 16.dp
    val XL = 20.dp; val C  = 18.dp; val Bt = 14.dp; val Bg = 20.dp
}

private val Scheme = darkColorScheme(
    primary        = C.Primary,
    onPrimary      = Color.White,
    background     = C.Bg,
    surface        = C.Surface,
    surfaceVariant = C.Card,
    onBackground   = C.White,
    onSurface      = C.White,
    error          = C.Error,
    errorContainer = C.ErrCont,
    onError        = C.OnErr
)

private const val PREF_CRASH = "crash_prefs"
private const val KEY_CRASH  = "crash_trace"
private const val PREF_SETS  = "desync_settings"
private const val KEY_MIN    = "min_lag"
private const val KEY_MAX    = "max_lag"
private const val KEY_DROP   = "drop_pct"
private const val KEY_SPIKE  = "spike_pct"
private const val KEY_RISK   = "risk_acked"

class MainActivity : ComponentActivity() {

    private var floatWm: WindowManager? = null
    private var floatView: ComposeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCrash()
        val crashPrefs = getSharedPreferences(PREF_CRASH, Context.MODE_PRIVATE)
        val trace = crashPrefs.getString(KEY_CRASH, null)
        if (trace != null) crashPrefs.edit().remove(KEY_CRASH).apply()

        setContent {
            MaterialTheme(colorScheme = Scheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color     = C.Bg
                ) {
                    if (trace != null) CrashScreen(trace)
                    else App()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatButton()
    }

    private fun setupCrash() {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
                val log = "Desync Crash\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\n\n$sw"
                getSharedPreferences(PREF_CRASH, Context.MODE_PRIVATE).edit().putString(KEY_CRASH, log).commit()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            } catch (_: Exception) { def?.uncaughtException(t, e) }
            android.os.Process.killProcess(android.os.Process.myPid()); System.exit(2)
        }
    }

    private fun removeFloatButton() {
        try { floatWm?.removeView(floatView) } catch (_: Exception) {}
        floatView = null
    }

    @Composable
    fun CrashScreen(trace: String) {
        Column(Modifier.fillMaxSize().background(C.Bg)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Warning, null, tint = C.Error, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Desync Crashed", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = C.White)
                }
                TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) {
                    Text("Close", color = C.Error, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                Text("Erro inesperado. Copie o log e reporte.", fontSize = 13.sp, color = C.Sub, modifier = Modifier.padding(bottom = 12.dp))
                Button(
                    onClick = { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Crash", trace)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = C.Primary),
                    shape    = RoundedCornerShape(R.Bt)
                ) {
                    Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copiar Log", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = C.ErrCont), shape = RoundedCornerShape(R.C)) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item { Text(trace, Modifier.padding(14.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = C.OnErr, lineHeight = 18.sp) }
                    }
                }
            }
        }
    }

    @Composable
    fun App() {
        val ctx   = this
        val prefs = remember { getSharedPreferences(PREF_SETS, Context.MODE_PRIVATE) }

        var active        by remember { mutableStateOf(DesyncVpnService.isRunning.get()) }
        var minLag        by remember { mutableFloatStateOf(prefs.getFloat(KEY_MIN, 60f)) }
        var maxLag        by remember { mutableFloatStateOf(prefs.getFloat(KEY_MAX, 180f)) }
        var dropPct       by remember { mutableFloatStateOf(prefs.getFloat(KEY_DROP, 0f)) }
        var spikePct      by remember { mutableFloatStateOf(prefs.getFloat(KEY_SPIKE, 5f)) }
        var riskAcked     by remember { mutableStateOf(prefs.getBoolean(KEY_RISK, false)) }
        var showSettings  by remember { mutableStateOf(false) }
        var showLogs      by remember { mutableStateOf(false) }
        var showRisk      by remember { mutableStateOf(false) }
        var logEnabled    by remember { mutableStateOf(true) }
        var footerClicks  by remember { mutableIntStateOf(0) }
        var totalPkts     by remember { mutableLongStateOf(0L) }
        var delayedPkts   by remember { mutableLongStateOf(0L) }
        var droppedPkts   by remember { mutableLongStateOf(0L) }
        var curDelay      by remember { mutableLongStateOf(0L) }
        var wifiName      by remember { mutableStateOf<String?>(null) }
        val pingHistory   = remember { mutableStateListOf<Long>() }
        val logSnapshot   = remember { mutableStateListOf<DesyncLog.LogEntry>() }

        DesyncLog.enabled.set(logEnabled)

        val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) launchVpn(minLag, maxLag, dropPct, spikePct)
        }

        LaunchedEffect(minLag, maxLag, dropPct, spikePct) {
            prefs.edit()
                .putFloat(KEY_MIN, minLag).putFloat(KEY_MAX, maxLag)
                .putFloat(KEY_DROP, dropPct).putFloat(KEY_SPIKE, spikePct).apply()
            DesyncVpnService.minLagMs     = minLag.toLong()
            DesyncVpnService.maxLagMs     = maxLag.toLong()
            DesyncVpnService.dropPercent  = dropPct
            DesyncVpnService.spikePercent = spikePct
        }

        LaunchedEffect(Unit) {
            while (true) {
                active      = DesyncVpnService.isRunning.get()
                totalPkts   = DesyncVpnService.totalPackets.get()
                delayedPkts = DesyncVpnService.delayedPackets.get()
                droppedPkts = DesyncVpnService.droppedPackets.get()
                curDelay    = DesyncVpnService.lastDelayMs.get()
                wifiName    = getWifiName(ctx)
                if (active) {
                    DesyncLog.snapshot()
                        .firstOrNull { it.level == "LAG" || it.level == "SPIKE" }
                        ?.message
                        ?.substringAfter("+")
                        ?.substringBefore("ms")
                        ?.toLongOrNull()
                        ?.let { ms ->
                            if (pingHistory.size >= 60) pingHistory.removeAt(0)
                            pingHistory.add(ms)
                        }
                }
                logSnapshot.clear()
                logSnapshot.addAll(DesyncLog.snapshot().take(200))
                delay(500)
            }
        }

        fun toggleVpn(want: Boolean) {
            if (want) {
                if (riskAcked) {
                    val i = VpnService.prepare(ctx)
                    if (i != null) vpnLauncher.launch(i)
                    else launchVpn(minLag, maxLag, dropPct, spikePct)
                } else showRisk = true
            } else stopVpn()
        }

        if (showRisk) {
            AlertDialog(
                onDismissRequest = { showRisk = false },
                icon  = { Icon(Icons.Outlined.Warning, null, tint = C.Warning, modifier = Modifier.size(32.dp)) },
                title = { Text("Aviso", color = C.White, fontWeight = FontWeight.ExtraBold) },
                text  = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Desync usa uma interface TUN local para interceptar TODOS os pacotes IP do dispositivo.", color = C.Warning, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Todos os apps — jogos, Discord, browser — terão lag injetado. Nenhum dado sai do dispositivo. O ícone de VPN aparecerá na barra de status.", color = C.Sub, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            prefs.edit().putBoolean(KEY_RISK, true).apply()
                            riskAcked = true; showRisk = false
                            val i = VpnService.prepare(ctx)
                            if (i != null) vpnLauncher.launch(i)
                            else launchVpn(minLag, maxLag, dropPct, spikePct)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Warning),
                        shape  = RoundedCornerShape(R.Bt)
                    ) { Text("Ativar", fontWeight = FontWeight.Bold, color = Color.Black) }
                },
                dismissButton = { TextButton(onClick = { showRisk = false }) { Text("Cancelar", color = C.Muted) } },
                containerColor = C.Surface, shape = RoundedCornerShape(R.C)
            )
        }

        if (showSettings) SettingsSheet(
            minLag, maxLag, dropPct, spikePct,
            { minLag = it }, { maxLag = it }, { dropPct = it }, { spikePct = it }
        ) { showSettings = false }

        if (showLogs) LogsScreen(logSnapshot, logEnabled, { logEnabled = it; DesyncLog.enabled.set(it) }) { showLogs = false }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            HeroSection(active, wifiName, curDelay, minLag.toLong(), maxLag.toLong())

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))
                ToggleCard(active) { toggleVpn(it) }
                Spacer(Modifier.height(16.dp))
                PingGraph(pingHistory, active)
                Spacer(Modifier.height(16.dp))
                StatsRow(totalPkts, delayedPkts, droppedPkts)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showSettings = true }, modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = C.Card), shape = RoundedCornerShape(R.Bt)) {
                        Icon(Icons.Outlined.Tune, null, tint = C.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Configurar", fontWeight = FontWeight.Bold, color = C.White)
                    }
                    Button(onClick = { showLogs = true }, modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = C.Card), shape = RoundedCornerShape(R.Bt)) {
                        Icon(Icons.Outlined.Terminal, null, tint = C.Accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Logs", fontWeight = FontWeight.Bold, color = C.White)
                    }
                }
                Spacer(Modifier.height(24.dp))
                PresetsRow { mn, mx, dp, sp ->
                    minLag = mn; maxLag = mx; dropPct = dp; spikePct = sp
                    if (active) DesyncLog.add("PRESET", "Preset aplicado — ${mn.toInt()}–${mx.toInt()}ms loss=${dp.toInt()}%")
                }
                Spacer(Modifier.height(32.dp))
                Footer(footerClicks) {
                    footerClicks++
                    if (footerClicks >= 5) { showLogs = true; footerClicks = 0 }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        FloatButton(
            active     = active,
            minLag     = minLag,
            maxLag     = maxLag,
            dropPct    = dropPct,
            spikePct   = spikePct,
            onToggle   = { toggleVpn(!active) },
            onSettings = { showSettings = true },
            onLogs     = { showLogs = true },
            onPreset   = { mn, mx, dp, sp ->
                minLag = mn; maxLag = mx; dropPct = dp; spikePct = sp
            }
        )
    }

    @Composable
    fun FloatButton(
        active: Boolean,
        minLag: Float,
        maxLag: Float,
        dropPct: Float,
        spikePct: Float,
        onToggle: () -> Unit,
        onSettings: () -> Unit,
        onLogs: () -> Unit,
        onPreset: (Float, Float, Float, Float) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        var offsetX  by remember { mutableFloatStateOf(0f) }
        var offsetY  by remember { mutableFloatStateOf(200f) }
        var dragging by remember { mutableStateOf(false) }

        val pulse = rememberInfiniteTransition(label = "fp")
        val pAlpha by pulse.animateFloat(
            0.5f, 1f,
            infiniteRepeatable(tween(900, easing = EaseInOutCubic), RepeatMode.Reverse),
            label = "pa"
        )
        val btnColor by animateFloatAsState(if (active) 1f else 0f, tween(300), label = "bc")
        val fabColor = if (active) C.Primary.copy(alpha = pAlpha) else C.Surface

        Box(
            Modifier
                .fillMaxSize()
                .padding(end = 16.dp, bottom = 80.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter   = fadeIn(tween(200)) + scaleIn(tween(200)) + expandVertically(tween(200)),
                    exit    = fadeOut(tween(150)) + scaleOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    FloatMenu(
                        active   = active,
                        minLag   = minLag,
                        maxLag   = maxLag,
                        dropPct  = dropPct,
                        spikePct = spikePct,
                        onToggle   = { onToggle(); expanded = false },
                        onSettings = { onSettings(); expanded = false },
                        onLogs     = { onLogs(); expanded = false },
                        onPreset   = { mn, mx, dp, sp -> onPreset(mn, mx, dp, sp) },
                        onDismiss  = { expanded = false }
                    )
                }

                Box(
                    Modifier
                        .size(60.dp)
                        .shadow(8.dp, CircleShape)
                        .background(fabColor, CircleShape)
                        .border(
                            1.5.dp,
                            if (active) C.Primary.copy(0.6f) else C.Border,
                            CircleShape
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { dragging = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                    dragging = true
                                },
                                onDragEnd = { dragging = false }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                if (!dragging) expanded = !expanded
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = active,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "fabIcon"
                    ) { on ->
                        Icon(
                            if (on) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            null,
                            tint     = if (on) Color.White else C.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun FloatMenu(
        active: Boolean,
        minLag: Float,
        maxLag: Float,
        dropPct: Float,
        spikePct: Float,
        onToggle: () -> Unit,
        onSettings: () -> Unit,
        onLogs: () -> Unit,
        onPreset: (Float, Float, Float, Float) -> Unit,
        onDismiss: () -> Unit
    ) {
        Card(
            colors   = CardDefaults.cardColors(containerColor = C.Card),
            shape    = RoundedCornerShape(R.C),
            modifier = Modifier
                .width(240.dp)
                .border(1.dp, C.Border, RoundedCornerShape(R.C))
                .shadow(12.dp, RoundedCornerShape(R.C))
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Desync", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = C.White)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(if (active) C.Success else C.Muted, CircleShape))
                        Text(if (active) "Ativo" else "Inativo", fontSize = 11.sp, color = if (active) C.Success else C.Muted, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = C.Border)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                if (active) C.Error.copy(0.15f) else C.Primary.copy(0.15f),
                                RoundedCornerShape(R.M)
                            )
                            .border(1.dp, if (active) C.Error.copy(0.3f) else C.Primary.copy(0.3f), RoundedCornerShape(R.M))
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(if (active) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null,
                                tint = if (active) C.Error else C.Primary, modifier = Modifier.size(15.dp))
                            Text(if (active) "Pausar" else "Iniciar", fontSize = 12.sp,
                                color = if (active) C.Error else C.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("${minLag.toInt()}ms – ${maxLag.toInt()}ms lag", fontSize = 11.sp, color = C.Muted, fontFamily = FontFamily.Monospace)

                HorizontalDivider(color = C.Border)

                Text("Presets rápidos", fontSize = 10.sp, color = C.Muted, fontWeight = FontWeight.Bold)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple("Leve",    Triple(20f,  80f,  0f),  3f),
                        Triple("Médio",   Triple(60f,  180f, 2f),  8f),
                        Triple("Pesado",  Triple(150f, 400f, 5f),  18f),
                        Triple("Extremo", Triple(300f, 900f, 12f), 40f)
                    ).zip(listOf(C.Success, C.Warning, C.Error, C.Pink)).forEach { (preset, color) ->
                        val (name, lagLoss, spike) = preset
                        val (mn, mx, dp) = lagLoss
                        Box(
                            Modifier
                                .weight(1f)
                                .background(color.copy(0.10f), RoundedCornerShape(6.dp))
                                .border(1.dp, color.copy(0.25f), RoundedCornerShape(6.dp))
                                .clickable { onPreset(mn, mx, dp, spike) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, fontSize = 9.sp, color = color, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        }
                    }
                }

                HorizontalDivider(color = C.Border)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.weight(1f).height(34.dp)
                            .background(C.Surface, RoundedCornerShape(R.M))
                            .border(1.dp, C.Border, RoundedCornerShape(R.M))
                            .clickable { onSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Settings, null, tint = C.Primary, modifier = Modifier.size(13.dp))
                            Text("Config", fontSize = 11.sp, color = C.Sub, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        Modifier.weight(1f).height(34.dp)
                            .background(C.Surface, RoundedCornerShape(R.M))
                            .border(1.dp, C.Border, RoundedCornerShape(R.M))
                            .clickable { onLogs() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Terminal, null, tint = C.Accent, modifier = Modifier.size(13.dp))
                            Text("Logs", fontSize = 11.sp, color = C.Sub, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HeroSection(active: Boolean, wifiName: String?, curDelay: Long, minLag: Long, maxLag: Long) {
        val tr    = rememberInfiniteTransition(label = "h")
        val pulse by tr.animateFloat(0.88f, 1f, infiniteRepeatable(tween(1200, easing = EaseInOutCubic), RepeatMode.Reverse), label = "p")
        val glow  by tr.animateFloat(0.3f, 0.9f,  infiniteRepeatable(tween(1400, easing = EaseInOutCubic), RepeatMode.Reverse), label = "g")
        val rot   by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "r")

        Box(
            Modifier.fillMaxWidth().height(280.dp)
                .background(Brush.verticalGradient(listOf(C.Bg, C.Primary.copy(if (active) 0.09f else 0.02f), C.Bg)))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f; val cy = size.height / 2f
                repeat(4) { i ->
                    drawCircle(
                        C.Primary.copy(if (active) (0.12f - i * 0.025f) * glow else 0.02f),
                        (85f + i * 48f) * if (active) pulse else 1f,
                        Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(if (i == 0) 1.5f else 0.8f)
                    )
                }
                if (active) {
                    val rr = 120f; val a = (rot * Math.PI / 180f).toFloat()
                    listOf(C.Accent to 0f, C.Pink to 2.09f, C.Primary to 4.19f).forEach { (col, off) ->
                        drawCircle(col.copy(0.7f), 5f, Offset(cx + rr * kotlin.math.cos(a + off), cy + rr * kotlin.math.sin(a + off)))
                    }
                }
            }
            Column(Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(if (active) (82.dp * pulse) else 82.dp)
                        .background(Brush.radialGradient(listOf(C.Primary.copy(if (active) 0.30f * glow else 0.13f), C.Primary.copy(0.03f))), CircleShape)
                        .border(1.5.dp, C.Primary.copy(if (active) 0.72f * glow else 0.26f), CircleShape))
                    Icon(Icons.Outlined.Wifi, null, tint = if (active) C.Primary else C.Muted, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(14.dp))
                AnimatedContent(active, transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) }, label = "st") { on ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (on) "DESYNC ATIVO" else "DESYNC INATIVO", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (on) C.Primary else C.Muted, letterSpacing = 2.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(if (on) "${minLag}ms – ${maxLag}ms lag em TODOS os pacotes" else "Ative para injetar lag em todos os apps",
                            fontSize = 12.sp, color = C.Muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (wifiName != null) {
                        Row(Modifier.background(C.Card, RoundedCornerShape(R.Bg)).border(1.dp, C.Border, RoundedCornerShape(R.Bg)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Wifi, null, tint = C.Success, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(wifiName, fontSize = 11.sp, color = C.Sub, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (active && curDelay > 0L) {
                        Row(Modifier.background(C.Warning.copy(0.10f), RoundedCornerShape(R.Bg)).border(1.dp, C.Warning.copy(0.25f), RoundedCornerShape(R.Bg)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(9.dp), color = C.Warning, strokeWidth = 1.5.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("anchor ${curDelay}ms", fontSize = 11.sp, color = C.Warning, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ToggleCard(active: Boolean, onToggle: (Boolean) -> Unit) {
        Card(
            colors   = CardDefaults.cardColors(containerColor = if (active) C.Primary.copy(0.08f) else C.Card),
            shape    = RoundedCornerShape(R.C),
            modifier = Modifier.fillMaxWidth().border(1.dp, if (active) C.Primary.copy(0.35f) else C.Border, RoundedCornerShape(R.C))
        ) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Fake Lag Engine", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (active) C.Primary else C.White)
                    Spacer(Modifier.height(3.dp))
                    Text(if (active) "TUN ativo — interceptando todos os pacotes IP" else "Ative para interceptar todo o tráfego do dispositivo",
                        fontSize = 12.sp, color = C.Muted, lineHeight = 16.sp)
                }
                Switch(checked = active, onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = C.Primary, uncheckedThumbColor = C.Muted, uncheckedTrackColor = C.Border))
            }
        }
    }

    @Composable
    fun PingGraph(history: List<Long>, active: Boolean) {
        val tr = rememberInfiniteTransition(label = "sc")
        val sx by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "sx")
        Card(colors = CardDefaults.cardColors(containerColor = C.Card), shape = RoundedCornerShape(R.C),
            modifier = Modifier.fillMaxWidth().border(1.dp, C.Border, RoundedCornerShape(R.C))) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.NetworkCheck, null, tint = C.Accent, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Packet Delay Graph", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.White)
                    }
                    if (history.isNotEmpty()) Text("avg ${history.average().toLong()}ms", fontSize = 11.sp, color = C.Muted, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(80.dp).background(C.Bg.copy(0.6f), RoundedCornerShape(R.S))) {
                    if (history.size > 1) {
                        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                            val w = size.width; val h = size.height
                            val mx = history.max().coerceAtLeast(100L).toFloat()
                            val pts = history.mapIndexed { i, v -> Offset(i.toFloat() / (history.size - 1) * w, h - (v.toFloat() / mx * h).coerceIn(0f, h)) }
                            val fill = Path().apply { moveTo(pts.first().x, h); pts.forEach { lineTo(it.x, it.y) }; lineTo(pts.last().x, h); close() }
                            val line = Path().apply { moveTo(pts.first().x, pts.first().y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                            drawPath(fill, Brush.verticalGradient(listOf(C.Accent.copy(0.22f), Color.Transparent)))
                            drawPath(line, color = C.Accent, style = androidx.compose.ui.graphics.drawscope.Stroke(2f, cap = StrokeCap.Round))
                            if (active) drawLine(C.Accent.copy(0.35f), Offset(sx * w, 0f), Offset(sx * w, h), 1f)
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Ative o Desync para ver os delays em tempo real", fontSize = 11.sp, color = C.Muted, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun StatsRow(total: Long, delayed: Long, dropped: Long) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Total", total.toString(), Icons.Outlined.Wifi, C.Primary, Modifier.weight(1f))
            StatCard("Atrasados", delayed.toString(), Icons.Outlined.Timer, C.Warning, Modifier.weight(1f))
            StatCard("Descartados", dropped.toString(), Icons.AutoMirrored.Outlined.TrendingDown, C.Error, Modifier.weight(1f))
        }
    }

    @Composable
    fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
        Card(colors = CardDefaults.cardColors(containerColor = C.Card), shape = RoundedCornerShape(R.M),
            modifier = modifier.border(1.dp, color.copy(0.15f), RoundedCornerShape(R.M))) {
            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
                Spacer(Modifier.height(5.dp))
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(label, fontSize = 9.sp, color = C.Muted, fontWeight = FontWeight.Medium)
            }
        }
    }

    @Composable
    fun PresetsRow(onPreset: (Float, Float, Float, Float) -> Unit) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(13.dp).background(C.Primary, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text("Presets Rápidos", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = C.White, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    listOf("Leve",    C.Success, 20f,  80f,  0f,  3f),
                    listOf("Médio",   C.Warning, 60f,  180f, 2f,  8f),
                    listOf("Pesado",  C.Error,   150f, 400f, 5f,  18f),
                    listOf("Extremo", C.Pink,    300f, 900f, 12f, 40f)
                ).forEach { p ->
                    val name  = p[0] as String; val color = p[1] as Color
                    val mn = p[2] as Float; val mx = p[3] as Float; val dp = p[4] as Float; val sp = p[5] as Float
                    Box(Modifier.weight(1f).background(color.copy(0.08f), RoundedCornerShape(R.M))
                        .border(1.dp, color.copy(0.2f), RoundedCornerShape(R.M))
                        .clickable { onPreset(mn, mx, dp, sp) }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, fontSize = 11.sp, color = color, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(2.dp))
                            Text("${mn.toInt()}-${mx.toInt()}ms", fontSize = 9.sp, color = color.copy(0.7f), fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SettingsSheet(
        minLag: Float, maxLag: Float, dropPct: Float, spikePct: Float,
        onMin: (Float) -> Unit, onMax: (Float) -> Unit, onDrop: (Float) -> Unit, onSpike: (Float) -> Unit,
        onDismiss: () -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)).clickable { onDismiss() }) {
                Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(C.Card, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable {}.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 32.dp)) {
                    Box(Modifier.width(40.dp).height(4.dp).background(C.Muted.copy(0.4f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Configurar Desync", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = C.White)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Close, null, tint = C.Muted, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.height(20.dp))
                    SliderRow("Min Lag (âncora)", "${minLag.toInt()} ms", minLag, 0f, 500f, C.Success) { onMin(it.coerceAtMost(maxLag)) }
                    SliderRow("Max Lag", "${maxLag.toInt()} ms", maxLag, 0f, 2000f, C.Warning) { onMax(it.coerceAtLeast(minLag)) }
                    SliderRow("Packet Loss", "${dropPct.toInt()} %", dropPct, 0f, 50f, C.Error) { onDrop(it) }
                    SliderRow("Spike Probability", "${spikePct.toInt()} %", spikePct, 0f, 80f, C.Pink) { onSpike(it) }
                    Spacer(Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = C.Primary.copy(0.07f)), shape = RoundedCornerShape(R.M),
                        modifier = Modifier.fillMaxWidth().border(1.dp, C.Primary.copy(0.18f), RoundedCornerShape(R.M))) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Wifi, null, tint = C.Primary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Min Lag = âncora. Pacotes ficam retidos por este tempo (igual ao root.Anchored=true do script Lua).", fontSize = 11.sp, color = C.Muted, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SliderRow(label: String, valueText: String, value: Float, min: Float, max: Float, color: Color, onChange: (Float) -> Unit) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 13.sp, color = C.Sub, fontWeight = FontWeight.Medium)
                Box(Modifier.background(color.copy(0.12f), RoundedCornerShape(R.Bg)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(valueText, fontSize = 12.sp, color = color, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
            }
            Slider(value = value, onValueChange = onChange, valueRange = min..max,
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = C.Border))
        }
    }

    @Composable
    fun LogsScreen(logs: List<DesyncLog.LogEntry>, logEnabled: Boolean, onToggle: (Boolean) -> Unit, onClose: () -> Unit) {
        Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(C.Bg)) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().background(C.Surface).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Terminal, null, tint = C.Accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Desync Console", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = C.White)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.background(C.Accent.copy(0.15f), RoundedCornerShape(R.Bg)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("${logs.size}", fontSize = 10.sp, color = C.Accent, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Log", fontSize = 11.sp, color = C.Muted)
                            Spacer(Modifier.width(4.dp))
                            Switch(checked = logEnabled, onCheckedChange = onToggle, modifier = Modifier.height(24.dp),
                                colors = SwitchDefaults.colors(checkedThumbColor = C.Accent, checkedTrackColor = C.Accent.copy(0.3f)))
                            Spacer(Modifier.width(6.dp))
                            TextButton(onClick = { DesyncLog.clear() }) { Text("Limpar", color = C.Error, fontSize = 11.sp) }
                            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Close, null, tint = C.Muted, modifier = Modifier.size(18.dp)) }
                        }
                    }
                    if (logs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Terminal, null, tint = C.Muted, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Ative o Desync para ver os logs de pacotes", color = C.Muted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                            items(logs) { log ->
                                val lc = when (log.level) {
                                    "START" -> C.Success; "STOP" -> C.Muted; "LAG" -> C.Warning
                                    "SPIKE" -> C.Pink; "DROP" -> C.Error; "OUT" -> C.Accent
                                    "PRESET" -> C.Primary; else -> C.Sub
                                }
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    .background(C.Surface.copy(0.5f), RoundedCornerShape(R.S))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(log.ts, fontSize = 10.sp, color = C.Muted, fontFamily = FontFamily.Monospace)
                                    Spacer(Modifier.width(5.dp))
                                    Box(Modifier.background(lc.copy(0.15f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                        Text(log.level, fontSize = 9.sp, color = lc, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(log.message, fontSize = 11.sp, color = C.Sub, fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Footer(clicks: Int, onClick: () -> Unit) {
        val tr  = rememberInfiniteTransition(label = "rgb")
        val hue by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "h")
        val col = Color.hsv(hue, 0.75f, 1f)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.clickable { onClick() }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Code, null, tint = col, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("By Rhyan57", fontSize = 12.sp, color = col, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.Code, null, tint = col, modifier = Modifier.size(12.dp))
            }
            if (clicks in 1..4) Text("${5 - clicks}x para abrir console", fontSize = 10.sp, color = C.Muted.copy(0.5f))
        }
    }

    private fun launchVpn(min: Float, max: Float, drop: Float, spike: Float) {
        DesyncVpnService.minLagMs     = min.toLong()
        DesyncVpnService.maxLagMs     = max.toLong()
        DesyncVpnService.dropPercent  = drop
        DesyncVpnService.spikePercent = spike
        val i = Intent(this, DesyncVpnService::class.java).setAction(DesyncVpnService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }

    private fun stopVpn() {
        startService(Intent(this, DesyncVpnService::class.java).setAction(DesyncVpnService.ACTION_STOP))
    }
}

private fun getWifiName(ctx: Context): String? = try {
    val wm   = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    @Suppress("DEPRECATION")
    val info = wm?.connectionInfo
    @Suppress("DEPRECATION")
    info?.ssid?.removePrefix("\"")?.removeSuffix("\"")?.takeIf { it.isNotEmpty() && it != "<unknown ssid>" }
} catch (_: Exception) { null }
