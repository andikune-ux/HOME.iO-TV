package dev.andikune.homeio

import android.content.BroadcastReceiver
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.domain.model.WeatherInfo
import dev.andikune.homeio.kiosk.KioskManager
import dev.andikune.homeio.kiosk.WatchdogService
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.ResponsiveProvider
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.presentation.home.HomeScreen
import dev.andikune.homeio.presentation.onboarding.OnboardingPermissionDialog
import dev.andikune.homeio.presentation.onboarding.QrisDonationPopup // IMPORT BARU
import dev.andikune.homeio.presentation.screensaver.ScreensaverActivity
import dev.andikune.homeio.presentation.settings.SettingsScreen
import dev.andikune.homeio.receiver.UsbReceiver
import dev.andikune.homeio.service.UpdateDownloadService
import dev.andikune.homeio.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var lastUserInteractionTime = System.currentTimeMillis()
    private var screensaverTimeoutMs = 5 * 60 * 1000L
    private var pendingFileManagerTrigger by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()

        intent?.let { handleUsbIntent(it) }
        val appContainer = (application as HomeIoApplication).container

        setContent {
            ResponsiveProvider {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                var crashLog by remember { mutableStateOf<String?>(null) }
                var isSplashFinished by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var showOnboarding by remember { mutableStateOf(false) }
                var showQrisPopup by remember { mutableStateOf(false) }
                var showLauncherPrompt by remember { 
                    mutableStateOf(!LauncherHelper.isDefaultLauncher(context) && !OnboardingPrefs.hasAskedLauncher(context)) 
                }

                LaunchedEffect(Unit) {
                    crashLog = withContext(Dispatchers.IO) { CrashHandler.getLastCrashLog(context) }
                }

                LaunchedEffect(Unit) {
                    if (!OnboardingPrefs.isCompleted(context)) showOnboarding = true
                }
                
                // Logika: Munculkan QRIS setelah Onboarding selesai
                LaunchedEffect(showOnboarding, isSplashFinished) {
                    if (!showOnboarding && isSplashFinished && !OnboardingPrefs.hasAskedLauncher(context)) {
                        // Cek apakah ini pertama kali buka (bisa menggunakan pref lain jika mau, sementara ini muncul setiap splash selesai)
                        showQrisPopup = true
                    }
                }

                var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                var downloadProgress by remember { mutableFloatStateOf(0f) }
                var isDownloading by remember { mutableStateOf(false) }
                var downloadReady by remember { mutableStateOf(false) }
                var isError by remember { mutableStateOf(false) }

                val config by appContainer.preferencesManager.settingsFlow.collectAsState(initial = SettingsConfig())

                LaunchedEffect(config.isKioskModeEnabled) {
                    if (config.isKioskModeEnabled) {
                        KioskManager.enableKiosk(this@MainActivity)
                        try { startService(Intent(this@MainActivity, WatchdogService::class.java)) } catch (_: Exception) {}
                    } else {
                        KioskManager.disableKiosk(this@MainActivity)
                        try { stopService(Intent(this@MainActivity, WatchdogService::class.java)) } catch (_: Exception) {}
                    }
                }

                LaunchedEffect(config.brightnessMode, config.manualBrightness) {
                    val brightness = if (config.brightnessMode == "MANUAL") (config.manualBrightness / 100f).coerceIn(0.01f, 1f) else 1f
                    runOnUiThread {
                        val attrs = window.attributes
                        attrs.screenBrightness = brightness
                        window.attributes = attrs
                    }
                }

                screensaverTimeoutMs = if (!config.screensaverEnabled) 0L else (config.screensaverTimeoutMinutes * 60 * 1000L)
                LaunchedEffect(screensaverTimeoutMs) {
                    if (screensaverTimeoutMs <= 0L) return@LaunchedEffect
                    while (true) {
                        delay(5000)
                        val elapsed = System.currentTimeMillis() - lastUserInteractionTime
                        if (elapsed >= screensaverTimeoutMs && !isSettingsOpen && crashLog == null && isSplashFinished && !showUpdateDialog && !showOnboarding && !showQrisPopup) {
                            try { startActivity(Intent(this@MainActivity, ScreensaverActivity::class.java)) } catch (_: Exception) {}
                            lastUserInteractionTime = System.currentTimeMillis()
                        }
                    }
                }

                val installedApps by appContainer.appRepository.getInstalledApps().collectAsState(initial = emptyList())
                var lastRefreshTime by remember { mutableLongStateOf(0L) }
                LaunchedEffect(Unit) {
                    if (System.currentTimeMillis() - lastRefreshTime > 10000L) {
                        lastRefreshTime = System.currentTimeMillis()
                        appContainer.appRepository.refreshApps()
                    }
                }

                var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
                LaunchedEffect(isSplashFinished) {
                    if (!isSplashFinished) return@LaunchedEffect
                    while (true) {
                        try {
                            val response = appContainer.weatherApiService.getCurrentWeather()
                            val cw = response.currentWeather
                            if (cw != null) {
                                val desc = when (cw.weathercode) {
                                    0 -> "Clear Sky"; 1, 2, 3 -> "Partly Cloudy"; 45, 48 -> "Foggy"; 51, 53, 55 -> "Drizzle"
                                    61, 63, 65 -> "Rain"; 71, 73, 75 -> "Snow"; 95, 96, 99 -> "Thunderstorm"; else -> "Clear"
                                }
                                weatherInfo = WeatherInfo(cw.temperature, cw.weathercode, desc, config.weatherLocation)
                            }
                        } catch (_: Exception) {
                            if (weatherInfo == null) weatherInfo = WeatherInfo(29.0, 1, "Partly Cloudy", config.weatherLocation)
                        }
                        delay(30 * 60 * 1000L)
                    }
                }

                // CEK UPDATE: Tambahkan syarat !showQrisPopup
                LaunchedEffect(isSplashFinished, showQrisPopup, showOnboarding) {
                    if (!isSplashFinished || showQrisPopup || showOnboarding) return@LaunchedEffect
                    try {
                        val info = UpdateManager.checkForUpdate()
                        if (info.available) {
                            val savedPath = UpdateDownloadService.getCompletedFilePath(context, info.latestVersion)
                            if (savedPath != null) {
                                updateInfo = info
                                downloadProgress = 1f
                                downloadReady = true
                                isDownloading = false
                                showUpdateDialog = true
                            } else if (UpdatePrefs.shouldShow(context, info.latestVersion, info.isForceUpdate)) {
                                UpdatePrefs.markChecked(context)
                                updateInfo = info
                                downloadProgress = 0f
                                downloadReady = false
                                isDownloading = false
                                showUpdateDialog = true
                            }
                        }
                    } catch (_: Exception) {}
                }

                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context?, intent: Intent?) {
                            if (intent?.action == UpdateDownloadService.ACTION_PROGRESS) {
                                val state = UpdateDownloadService.readState(context)
                                downloadProgress = state.progress
                                when (state.status) {
                                    "DOWNLOADING" -> { isDownloading = true; downloadReady = false; isError = false }
                                    "COMPLETE" -> { isDownloading = false; downloadReady = true; isError = false; downloadProgress = 1f }
                                    "ERROR" -> { isDownloading = false; downloadReady = false; isError = true; downloadProgress = 0f }
                                }
                            }
                        }
                    }
                    val filter = IntentFilter(UpdateDownloadService.ACTION_PROGRESS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        @Suppress("UnspecifiedRegisterReceiverFlag") context.registerReceiver(receiver, filter)
                    }
                    onDispose { context.unregisterReceiver(receiver) }
                }

                BackHandler(enabled = isSettingsOpen || showUpdateDialog || showOnboarding || showQrisPopup) { 
                    if (showUpdateDialog && updateInfo?.isForceUpdate == true) return@BackHandler
                    if (showQrisPopup) return@BackHandler // Tidak bisa back jika QRIS muncul
                    if (showOnboarding) return@BackHandler
                    if (isSettingsOpen) isSettingsOpen = false
                    else if (showUpdateDialog) showUpdateDialog = false
                }

                when {
                    crashLog != null -> CrashLogScreen(crashLog!!) { CrashHandler.clearCrashLog(context); crashLog = null }
                    showOnboarding -> OnboardingPermissionDialog { 
                        OnboardingPrefs.markCompleted(context); showOnboarding = false 
                    }
                    !isSplashFinished -> SplashScreen { isSplashFinished = true }
                    isSettingsOpen -> SettingsScreen(config, { updated -> scope.launch { appContainer.preferencesManager.updateSettings(updated) } }, onBack = { isSettingsOpen = false })
                    else -> HomeScreen(
                        config = config,
                        allApps = installedApps,
                        weatherInfo = weatherInfo,
                        onOpenSettings = { isSettingsOpen = true },
                        onSaveConfig = { updated -> scope.launch { appContainer.preferencesManager.updateSettings(updated) } },
                        openFileManagerTrigger = pendingFileManagerTrigger,
                        onFileManagerOpened = { pendingFileManagerTrigger = false }
                    )
                }

                // QRIS POPUP OVERLAY
                if (showQrisPopup) {
                    QrisDonationPopup(onDismiss = { showQrisPopup = false })
                }

                if (showLauncherPrompt && isSplashFinished && !showOnboarding && !showQrisPopup) {
                    DefaultLauncherDialog(
                        onDismiss = { 
                            OnboardingPrefs.markLauncherAsked(context)
                            showLauncherPrompt = false 
                        }
                    )
                }

                if (showUpdateDialog && updateInfo != null && !showQrisPopup) {
                    UpdateDialog(
                        info = updateInfo!!, progress = downloadProgress, isDownloading = isDownloading, isReady = downloadReady, isError = isError,
                        onUpdate = {
                            if (downloadReady || isError) {
                                if (isError) {
                                    isError = false; downloadProgress = 0f; isDownloading = true
                                    updateInfo!!.downloadUrl?.let {
                                        val cleanVer = updateInfo!!.latestVersion.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                                        UpdateDownloadService.start(context, it, "HomeIO_${cleanVer}.apk", updateInfo!!.latestVersion)
                                    }
                                } else {
                                    val path = UpdateDownloadService.getCompletedFilePath(context, updateInfo!!.latestVersion)
                                        ?: UpdateDownloadService.readState(context).filePath
                                    if (path != null) {
                                        if (ApkDownloader.install(context, path)) {
                                            UpdateDownloadService.resetState(context)
                                            showUpdateDialog = false
                                        } else {
                                            Toast.makeText(context, "Beri izin install APK", Toast.LENGTH_LONG).show()
                                        }
                                    } else { downloadReady = false; isError = true }
                                }
                            } else {
                                updateInfo!!.downloadUrl?.let {
                                    isDownloading = true; downloadProgress = 0f; downloadReady = false; isError = false
                                    val cleanVer = updateInfo!!.latestVersion.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                                    UpdateDownloadService.start(context, it, "HomeIO_${cleanVer}.apk", updateInfo!!.latestVersion)
                                }
                            }
                        },
                        onLater = { if (!updateInfo!!.isForceUpdate && !isDownloading) { showUpdateDialog = false; if (!downloadReady) { downloadProgress = 0f; isError = false } } },
                        onSkip = { if (!updateInfo!!.isForceUpdate && !isDownloading) { UpdatePrefs.setSkipped(context, updateInfo!!.latestVersion); showUpdateDialog = false; downloadProgress = 0f; downloadReady = false; isError = false } }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent) {
        if (intent.getBooleanExtra(UsbReceiver.EXTRA_OPEN_FILE_MANAGER, false)) {
            pendingFileManagerTrigger = true
            intent.removeExtra(UsbReceiver.EXTRA_OPEN_FILE_MANAGER)
        }
    }

    override fun onResume() { super.onResume(); lastUserInteractionTime = System.currentTimeMillis(); KioskManager.isMainActivityForeground = true }
    override fun onPause() { super.onPause(); KioskManager.isMainActivityForeground = false }
    override fun onUserInteraction() { super.onUserInteraction(); lastUserInteractionTime = System.currentTimeMillis() }
    
    override fun onDestroy() {
        super.onDestroy()
        SoundFeedback.release()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lastUserInteractionTime = System.currentTimeMillis()
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> SoundFeedback.playClick(this)
            }
        }
        return super.dispatchKeyEvent(event)
    }
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean { lastUserInteractionTime = System.currentTimeMillis(); return super.dispatchTouchEvent(ev) }
}

@Composable
fun DefaultLauncherDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val focusReq = remember { FocusRequester() }
    var isFocusedBtn by remember { mutableStateOf(false) }
    var isFocusedSkip by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { 
        delay(400); 
        runCatching { focusReq.requestFocus() } 
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth(0.85f).liquidGlass(cornerRadius = 20.dp).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Home, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("JADIKAN LAUNCHER UTAMA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
                Spacer(Modifier.height(8.dp))
                Text("Agar tombol HOME di remote selalu membuka HOME.iO.", fontSize = 13.sp, color = GlassColors.TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                
                NeonFocusBorder(focused = isFocusedBtn, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (isFocusedBtn) GlassColors.AccentGlow else GlassColors.GlassTint)
                            .focusRequester(focusReq).onFocusChanged { isFocusedBtn = it.isFocused }.focusable().clickable {
                                LauncherHelper.openLauncherChooser(context); onDismiss()
                            }.padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("JADIKAN LAUNCHER SEKARANG", color = if (isFocusedBtn) GlassColors.DeepBackground else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                NeonFocusBorder(focused = isFocusedSkip, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isFocusedSkip) Color(0x33FFFFFF) else Color.Transparent).onFocusChanged { isFocusedSkip = it.isFocused }.focusable().clickable { onDismiss() }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("NANTI SAJA", color = if (isFocusedSkip) Color.White else GlassColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(600, easing = LinearEasing))
        scaleAnim.animateTo(1f, tween(600, easing = LinearEasing))
        delay(800)
        alphaAnim.animateTo(0f, tween(400, easing = LinearEasing))
        onFinish()
    }

    Box(
        Modifier.fillMaxSize().background(GlassColors.DeepBackgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value).scale(scaleAnim.value)
        ) {
            Box(
                Modifier.size(120.dp).liquidGlass(cornerRadius = 36.dp, isFocused = true),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, "Logo", tint = GlassColors.AccentGlow, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "HOME.iO",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                style = TextStyle(shadow = Shadow(color = GlassColors.AccentGlow, blurRadius = 20f))
            )
            Spacer(Modifier.height(8.dp))
            Text("Liquid Glass Launcher", color = GlassColors.TextSecondary, fontSize = 14.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun CrashLogScreen(crashLog: String, onClearAndReload: () -> Unit) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(GlassColors.DeepBackgroundGradient).padding(24.dp)) {
        Column(Modifier.fillMaxSize().liquidGlass(cornerRadius = 24.dp).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Warning, null, tint = GlassColors.AccentRed, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.crash_detected_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.crash_detected_desc), color = GlassColors.TextSecondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(16.dp)).padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(crashLog, color = Color(0xFFFF8080), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    SoundFeedback.playClick(context)
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Crash Log", crashLog))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = GlassColors.GlassTint), modifier = Modifier.border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(20.dp))) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_quick_copy), color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { SoundFeedback.playClick(context); onClearAndReload() }, colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow)) {
                    Icon(Icons.Default.Refresh, null, tint = Color.Black); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_return_home), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
