package dev.andikune.homeio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.andikune.homeio.domain.model.AppItem
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.domain.model.WeatherInfo
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.presentation.home.HomeScreen
import dev.andikune.homeio.presentation.screensaver.ScreensaverActivity
import dev.andikune.homeio.presentation.settings.SettingsScreen
import dev.andikune.homeio.util.CrashHandler
import dev.andikune.homeio.util.SoundFeedback
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

            // Inactivity monitor for OLED Screensaver
            LaunchedEffect(screensaverTimeoutMs) {
                while (true) {
                    delay(5000)
                    val elapsed = System.currentTimeMillis() - lastUserInteractionTime
                    if (elapsed >= screensaverTimeoutMs && !isSettingsOpen && crashLog == null && isSplashFinished) {
                        startActivity(Intent(this@MainActivity, ScreensaverActivity::class.java))
                        lastUserInteractionTime = System.currentTimeMillis()
                    }
                }
            }

            // Back handler
            BackHandler(enabled = isSettingsOpen) {
                isSettingsOpen = false
            }

            // RENDER SCREEN STATE
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
