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
import dev.andikune.homeio.presentation.settings.UpdateDialog
import dev.andikune.homeio.service.UpdateDownloadService
import dev.andikune.homeio.util.ApkDownloader
import dev.andikune.homeio.util.CrashHandler
import dev.andikune.homeio.util.SoundFeedback
import dev.andikune.homeio.util.UpdateManager
import dev.andikune.homeio.util.UpdatePrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var lastUserInteractionTime = System.currentTimeMillis()
    private var screensaverTimeoutMs = 5 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            var isInstalling by remember { mutableStateOf(false) }

            // Observe Settings
            val config by appContainer.preferencesManager.settingsFlow.collectAsState(
                initial = SettingsConfig()
            )

            screensaverTimeoutMs = (config.screensaverTimeoutMinutes * 60 * 1000L).coerceAtLeast(60000L)

            // Observe Installed Apps
            val installedApps by appContainer.appRepository.getInstalledApps().collectAsState(
                initial = emptyList()
            )

            LaunchedEffect(Unit) {
                appContainer.appRepository.refreshApps()
            }

            // ============================================================
            // REALTIME WEATHER (Open-Meteo)
            // ============================================================
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
            // CEK UPDATE — Hanya setelah Splash selesai (cooldown 6 jam)
            // ============================================================
            LaunchedEffect(isSplashFinished) {
                if (!isSplashFinished) return@LaunchedEffect
                if (!UpdatePrefs.shouldCheckUpdate(context)) return@LaunchedEffect
                UpdatePrefs.markChecked(context)
                try {
                    val info = UpdateManager.checkForUpdate()
                    if (info.available && UpdatePrefs.shouldShowDialog(context, info.latestVersion, info.isForceUpdate)) {
                        updateInfo = info
                        showUpdateDialog = true
                    }
                } catch (_: Exception) {
                    // silent fail — tidak ganggu user
                }
            }

            // ============================================================
            // BROADCAST RECEIVER — Progress download real-time
            // ============================================================
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        if (intent?.action == UpdateDownloadService.ACTION_PROGRESS) {
                            val state = UpdateDownloadService.readState(context)
                            downloadProgress = state.progress
                            when (state.status) {
                                UpdateDownloadService.STATUS_COMPLETE -> {
                                    isDownloading = false
                                    downloadReady = true
                                    Toast.makeText(context, "Download selesai! Klik INSTALL.", Toast.LENGTH_SHORT).show()
                                }
                                UpdateDownloadService.STATUS_ERROR -> {
                                    isDownloading = false
                                    downloadReady = false
                                    Toast.makeText(context, "Download gagal: ${state.error}", Toast.LENGTH_LONG).show()
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

            // ============================================================
            // INACTIVITY MONITOR (Screensaver OLED)
            // ============================================================
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
                SettingsScreen(
                    currentConfig = config,
                    onSaveConfig = { updated ->
                        scope.launch {
                            appContainer.preferencesManager.updateSettings(updated)
                        }
                    },
                    onClose = { isSettingsOpen = false }
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
            // UPDATE DIALOG — Overlay di atas semua screen
            // ============================================================
            if (showUpdateDialog && updateInfo != null) {
                UpdateDialog(
                    currentVersion = updateInfo!!.currentVersion,
                    latestVersion = updateInfo!!.latestVersion,
                    releaseNotes = updateInfo!!.releaseNotes,
                    forceUpdate = updateInfo!!.isForceUpdate,
                    downloadProgress = if (isDownloading || downloadReady) downloadProgress else null,
                    isDownloading = isDownloading,
                    downloadReady = downloadReady,
                    isInstalling = isInstalling,
                    onUpdateClick = {
                        if (isInstalling) return@UpdateDialog

                        if (downloadReady) {
                            // === INSTALL MODE ===
                            if (!ApkDownloader.canInstallApk(context)) {
                                Toast.makeText(context, "Beri izin 'Install unknown apps' dulu", Toast.LENGTH_LONG).show()
                                ApkDownloader.openInstallPermissionSettings(context)
                            } else {
                                val state = UpdateDownloadService.readState(context)
                                val path = state.filePath
                                if (path != null) {
                                    isInstalling = true
                                    val ok = ApkDownloader.installApk(context, path)
                                    if (!ok) {
                                        isInstalling = false
                                        Toast.makeText(context, "Gagal membuka installer APK", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "File APK tidak ditemukan", Toast.LENGTH_LONG).show()
                                    downloadReady = false
                                }
                            }
                        } else {
                            // === DOWNLOAD MODE ===
                            isDownloading = true
                            downloadProgress = 0f
                            downloadReady = false
                            val safeFileName = "HomeIO_Update_${System.currentTimeMillis()}.apk"
                            UpdateDownloadService.start(
                                context = context,
                                downloadUrl = updateInfo!!.downloadUrl ?: "",
                                fileName = safeFileName
                            )
                        }
                    },
                    onLaterClick = {
                        if (updateInfo!!.isForceUpdate) return@UpdateDialog
                        showUpdateDialog = false
                    },
                    onSkipClick = {
                        if (updateInfo!!.isForceUpdate) return@UpdateDialog
                        UpdatePrefs.setSkippedVersion(context, updateInfo!!.latestVersion)
                        showUpdateDialog = false
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
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    SoundFeedback.playClick(this)
                }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {
                    SoundFeedback.playClick(this)
                }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassColors.DeepBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .liquidGlass(cornerRadius = 32.dp, isFocused = true),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home.io Logo",
                    tint = GlassColors.AccentGlow,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Home.io",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Liquid Glass Launcher",
                color = GlassColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CrashLogScreen(crashLog: String, onClearAndReload: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassColors.DeepBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlass(cornerRadius = 24.dp)
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = GlassColors.AccentRed,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.crash_detected_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.crash_detected_desc),
                        color = GlassColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF000000))
                    .border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = crashLog,
                    color = Color(0xFFFF8080),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        SoundFeedback.playClick(context)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Home.io Crash Log", crashLog)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.GlassTint),
                    modifier = Modifier
                        .testTag("copy_crash_log_button")
                        .border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(20.dp))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_quick_copy), color = Color.White)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        SoundFeedback.playClick(context)
                        onClearAndReload()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow),
                    modifier = Modifier.testTag("reload_launcher_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_return_home), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
