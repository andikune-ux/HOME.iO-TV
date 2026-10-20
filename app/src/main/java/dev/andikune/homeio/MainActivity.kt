package dev.andikune.homeio

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.domain.model.WeatherInfo
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.presentation.home.HomeScreen
import dev.andikune.homeio.presentation.screensaver.ScreensaverActivity
import dev.andikune.homeio.presentation.settings.SettingsScreen
import dev.andikune.homeio.service.UpdateDownloadService
import dev.andikune.homeio.util.ApkDownloader
import dev.andikune.homeio.util.CrashHandler
import dev.andikune.homeio.util.SoundFeedback
import dev.andikune.homeio.util.UpdateDialog
import dev.andikune.homeio.util.UpdateManager
import dev.andikune.homeio.util.UpdatePrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var lastUserInteractionTime = System.currentTimeMillis()
    private var screensaverTimeoutMs = 5 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Strict landscape orientation enforcement for TV & Tablet 16:9 view
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()

        val appContainer = (application as HomeIoApplication).container

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var crashLog by remember { mutableStateOf(CrashHandler.getLastCrashLog(context)) }
            var isSplashFinished by remember { mutableStateOf(false) }
            var isSettingsOpen by remember { mutableStateOf(false) }

            // ============================================================
            // OTA UPDATE STATE
            // ============================================================
            var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableFloatStateOf(0f) }
            var isDownloading by remember { mutableStateOf(false) }
            var downloadReady by remember { mutableStateOf(false) }

            // Observe Settings
            val config by appContainer.preferencesManager.settingsFlow.collectAsState(
                initial = SettingsConfig()
            )

            // Update screensaver timeout
            screensaverTimeoutMs = (config.screensaverTimeoutMinutes * 60 * 1000L).coerceAtLeast(60000L)

            // Observe Installed Apps
            val installedApps by appContainer.appRepository.getInstalledApps().collectAsState(
                initial = emptyList()
            )

            // Refresh apps on startup
            LaunchedEffect(Unit) {
                appContainer.appRepository.refreshApps()
            }

            // Realtime Weather from Open-Meteo
            var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
            LaunchedEffect(Unit) {
                try {
                    val response = appContainer.weatherApiService.getCurrentWeather()
                    val cw = response.currentWeather
                    if (cw != null) {
                        val desc = when (cw.weathercode) {
                            0 -> "Clear Sky"
                            1, 2, 3 -> "Partly Cloudy"
                            45, 48 -> "Foggy"
                            51, 53, 55 -> "Drizzle"
                            61, 63, 65 -> "Rain"
                            71, 73, 75 -> "Snow"
                            95, 96, 99 -> "Thunderstorm"
                            else -> "Clear"
                        }
                        weatherInfo = WeatherInfo(
                            temperature = cw.temperature,
                            weatherCode = cw.weathercode,
                            description = desc,
                            locationName = "Jakarta, ID"
                        )
                    }
                } catch (_: Exception) {
                    weatherInfo = WeatherInfo(
                        temperature = 29.0,
                        weatherCode = 1,
                        description = "Partly Cloudy",
                        locationName = "Jakarta, ID"
                    )
                }
            }

            // ============================================================
            // CEK UPDATE OTA — Hanya setelah Splash selesai
            // ============================================================
            LaunchedEffect(isSplashFinished) {
                if (!isSplashFinished) return@LaunchedEffect
                if (!UpdatePrefs.shouldCheck(context)) return@LaunchedEffect
                UpdatePrefs.markChecked(context)
                try {
                    val info = UpdateManager.checkForUpdate()
                    if (info.available && UpdatePrefs.shouldShow(context, info.latestVersion, info.isForceUpdate)) {
                        updateInfo = info
                        showUpdateDialog = true
                    }
                } catch (_: Exception) { }
            }

            // ============================================================
            // BROADCAST RECEIVER — Progress download OTA real-time
            // ============================================================
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        if (intent?.action == UpdateDownloadService.ACTION_PROGRESS) {
                            val prefs = context.getSharedPreferences(UpdateDownloadService.PREFS, Context.MODE_PRIVATE)
                            val status = prefs.getString(UpdateDownloadService.KEY_STATUS, "IDLE") ?: "IDLE"
                            val prog = prefs.getFloat(UpdateDownloadService.KEY_PROGRESS, 0f)
                            val err = prefs.getString(UpdateDownloadService.KEY_ERROR, null)

                            downloadProgress = prog
                            when (status) {
                                "DOWNLOADING" -> {
                                    isDownloading = true
                                    downloadReady = false
                                }
                                "COMPLETE" -> {
                                    isDownloading = false
                                    downloadReady = true
                                    downloadProgress = 1f
                                    Toast.makeText(context, "Download selesai — klik INSTALL", Toast.LENGTH_SHORT).show()
                                }
                                "ERROR" -> {
                                    isDownloading = false
                                    downloadReady = false
                                    Toast.makeText(context, "Download gagal: ${err ?: "unknown"}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
                val filter = IntentFilter(UpdateDownloadService.ACTION_PROGRESS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    context.registerReceiver(receiver, filter)
                }
                onDispose { context.unregisterReceiver(receiver) }
            }

            // Inactivity monitor for OLED Screensaver
            LaunchedEffect(screensaverTimeoutMs) {
                while (true) {
                    delay(5000)
                    val elapsed = System.currentTimeMillis() - lastUserInteractionTime
                    if (elapsed >= screensaverTimeoutMs
                        && !isSettingsOpen
                        && crashLog == null
                        && isSplashFinished
                        && !showUpdateDialog) {
                        startActivity(Intent(this@MainActivity, ScreensaverActivity::class.java))
                        lastUserInteractionTime = System.currentTimeMillis()
                    }
                }
            }

            // Back handler
            BackHandler(enabled = isSettingsOpen) {
                isSettingsOpen = false
            }

            // ============================================================
            // RENDER SCREEN STATE
            // ============================================================
            if (crashLog != null) {
                CrashLogScreen(
                    crashLog = crashLog!!,
                    onClearAndReload = {
                        CrashHandler.clearCrashLog(context)
                        crashLog = null
                    }
                )
            } else if (!isSplashFinished) {
                SplashScreen(onFinish = { isSplashFinished = true })
            } else if (isSettingsOpen) {
                // PERBAIKAN: Hanya passing onBack karena SettingsScreen versi 2-Pane kita hanya menerima onBack
                SettingsScreen(
                    onBack = { isSettingsOpen = false }
                )
            } else {
                HomeScreen(
                    config = config,
                    allApps = installedApps,
                    weatherInfo = weatherInfo,
                    onOpenSettings = { isSettingsOpen = true },
                    onSaveConfig = { updated ->
                        scope.launch {
                            appContainer.preferencesManager.updateSettings(updated)
                        }
                    }
                )
            }

            // ============================================================
            // UPDATE DIALOG OVERLAY — Skenario A
            // ============================================================
            if (showUpdateDialog && updateInfo != null) {
                UpdateDialog(
                    info = updateInfo!!,
                    progress = downloadProgress,
                    isDownloading = isDownloading,
                    isReady = downloadReady,
                    onUpdate = {
                        if (downloadReady) {
                            val prefs = context.getSharedPreferences(UpdateDownloadService.PREFS, Context.MODE_PRIVATE)
                            val path = prefs.getString(UpdateDownloadService.KEY_PATH, null)
                            if (path != null) {
                                val ok = ApkDownloader.install(context, path)
                                if (!ok) {
                                    Toast.makeText(context, "Gagal membuka installer. Pastikan izin diberikan.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "File tidak ditemukan", Toast.LENGTH_LONG).show()
                                downloadReady = false
                            }
                        } else {
                            val url = updateInfo!!.downloadUrl
                            if (url.isNullOrBlank()) {
                                Toast.makeText(context, "URL download tidak tersedia", Toast.LENGTH_LONG).show()
                                return@UpdateDialog
                            }
                            isDownloading = true
                            downloadProgress = 0f
                            downloadReady = false
                            val safeName = "HomeIO_Update_${System.currentTimeMillis()}.apk"
                            UpdateDownloadService.start(context, url, safeName)
                        }
                    },
                    onLater = {
                        if (updateInfo!!.isForceUpdate) return@UpdateDialog
                        showUpdateDialog = false
                        isDownloading = false
                        downloadReady = false
                        downloadProgress = 0f
                    },
                    onSkip = {
                        if (updateInfo!!.isForceUpdate) return@UpdateDialog
                        UpdatePrefs.setSkipped(context, updateInfo!!.latestVersion)
                        showUpdateDialog = false
                        isDownloading = false
                        downloadReady = false
                        downloadProgress = 0f
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lastUserInteractionTime = System.currentTimeMillis()
        val appContainer = (application as HomeIoApplication).container
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            appContainer.appRepository.refreshApps()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        lastUserInteractionTime = System.currentTimeMillis()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lastUserInteractionTime = System.currentTimeMillis()
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> SoundFeedback.playClick(this)
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> SoundFeedback.playClick(this)
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        lastUserInteractionTime = System.currentTimeMillis()
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(400))
        delay(600)
        alphaAnim.animateTo(0f, animationSpec = tween(400))
        onFinish()
    }
    Box(modifier = Modifier.fillMaxSize().background(GlassColors.DeepBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alphaAnim.value)) {
            Box(modifier = Modifier.size(110.dp).liquidGlass(cornerRadius = 32.dp, isFocused = true), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Home, "Home.io Logo", tint = GlassColors.AccentGlow, modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Home.io", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Liquid Glass Launcher", color = GlassColors.TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
fun CrashLogScreen(crashLog: String, onClearAndReload: () -> Unit) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(GlassColors.DeepBackground).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize().liquidGlass(cornerRadius = 24.dp).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Warning, null, tint = GlassColors.AccentRed, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.crash_detected_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.crash_detected_desc), color = GlassColors.TextSecondary, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF000000)).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(16.dp)).padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(crashLog, color = Color(0xFFFF8080), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        SoundFeedback.playClick(context)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", crashLog))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.GlassTint),
                    modifier = Modifier.border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(20.dp))
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_quick_copy), color = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { SoundFeedback.playClick(context); onClearAndReload() },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_return_home), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
