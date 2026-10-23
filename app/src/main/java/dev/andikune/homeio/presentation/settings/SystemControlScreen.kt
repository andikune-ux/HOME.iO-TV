package dev.andikune.homeio.presentation.settings

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged // <--- INI IMPORT YANG TADI HILANG
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.kiosk.KioskManager
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.liquidGlass

@Composable
fun SystemControlScreen(
    config: SettingsConfig,
    onUpdateConfig: (SettingsConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // STATE UNTUK KENDALI
    var brightness by remember { mutableFloatStateOf(50f) }
    var volume by remember { mutableFloatStateOf(50f) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }

    // INITIAL LOAD
    LaunchedEffect(Unit) {
        try {
            val currentBrightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            brightness = (currentBrightness / 255f) * 100f
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            volume = (currentVol.toFloat() / maxVol.toFloat()) * 100f
        } catch (_: Exception) {}
    }

    Box(Modifier.fillMaxSize().background(GlassColors.DeepBackgroundGradient)) {
        Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
            
            // HEADER
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(16.dp))
                Text("KENDALI SISTEM", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
            }

            // WIFI PANEL
            SystemPanel(
                title = "WiFi",
                icon = Icons.Default.Wifi,
                description = if (wifiManager.isWifiEnabled) "WiFi Aktif. Tekan OK untuk memindai." else "WiFi Nonaktif. Tekan OK untuk menyalakan.",
                onClick = {
                    try {
                        if (!wifiManager.isWifiEnabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                                context.startActivity(panelIntent)
                            } else {
                                @Suppress("DEPRECATION") wifiManager.isWifiEnabled = true
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                                context.startActivity(panelIntent)
                            } else {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            }
                        }
                    } catch (_: Exception) {}
                }
            )

            Spacer(Modifier.height(16.dp))

            // BLUETOOTH PANEL
            SystemPanel(
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth,
                description = if (bluetoothAdapter?.isEnabled == true) "Bluetooth Aktif. Tekan OK untuk memindai." else "Bluetooth Nonaktif. Tekan OK untuk menyalakan.",
                onClick = {
                    try {
                        if (bluetoothAdapter?.isEnabled == false) {
                            @Suppress("DEPRECATION") bluetoothAdapter.enable()
                        } else {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }
                    } catch (_: Exception) {}
                }
            )

            Spacer(Modifier.height(16.dp))

            // SUARA PANEL
            Column(Modifier.fillMaxWidth().liquidGlass(cornerRadius = 16.dp).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, null, tint = GlassColors.AccentGlow)
                    Spacer(Modifier.width(12.dp))
                    Text("Volume Suara", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Slider(
                    value = volume,
                    onValueChange = { newVal ->
                        volume = newVal
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val targetVol = ((newVal / 100f) * maxVol).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    },
                    colors = SliderDefaults.colors(thumbColor = GlassColors.AccentGlow, activeTrackColor = GlassColors.AccentGlow)
                )
            }

            Spacer(Modifier.height(16.dp))

            // KECERAHAN PANEL
            Column(Modifier.fillMaxWidth().liquidGlass(cornerRadius = 16.dp).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BrightnessMedium, null, tint = GlassColors.AccentGlow)
                    Spacer(Modifier.width(12.dp))
                    Text("Kecerahan Layar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Slider(
                    value = brightness,
                    onValueChange = { newVal ->
                        brightness = newVal
                        if (Settings.System.canWrite(context)) {
                            val targetBrightness = ((newVal / 100f) * 255).toInt()
                            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, targetBrightness)
                        }
                    },
                    colors = SliderDefaults.colors(thumbColor = GlassColors.AccentGlow, activeTrackColor = GlassColors.AccentGlow)
                )
                if (!Settings.System.canWrite(context)) {
                    Text("Butuh izin 'Ubah Pengaturan Sistem' untuk menggunakan slider ini.", fontSize = 11.sp, color = GlassColors.AccentRed)
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // PINTASAN PENGATURAN ASLI
            SystemPanel(
                title = "Buka Pengaturan Android TV",
                icon = Icons.Default.Settings,
                description = "Akses semua pengaturan sistem asli (Suara, Bahasa, Info TV, dll)",
                onClick = {
                    try { context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                }
            )

            Spacer(Modifier.height(16.dp))

            // KIOSK PANEL
            SystemPanel(
                title = "Kiosk Mode",
                icon = Icons.Default.Lock,
                tint = GlassColors.AccentRed,
                description = if (config.isKioskModeEnabled) "Kiosk Aktif. Tekan OK untuk mematikan." else "Kiosk Nonaktif. Tekan OK untuk mengaktifkan.",
                onClick = {
                    val newKioskState = !config.isKioskModeEnabled
                    onUpdateConfig(config.copy(isKioskModeEnabled = newKioskState))
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 12.dp, modifier = Modifier.size(48.dp)) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                .onFocusChanged { isFocused = it.isFocused }.focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, "Kembali", tint = if (isFocused) GlassColors.AccentGlow else Color.White)
        }
    }
}

@Composable
private fun SystemPanel(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, tint: Color = GlassColors.AccentGlow, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                .border(if (isFocused) 2.dp else 1.dp, if (isFocused) GlassColors.AccentGlow else GlassColors.GlassBorder, RoundedCornerShape(16.dp))
                .onFocusChanged { isFocused = it.isFocused }.focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() }.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(description, fontSize = 13.sp, color = GlassColors.TextSecondary)
            }
        }
    }
}
