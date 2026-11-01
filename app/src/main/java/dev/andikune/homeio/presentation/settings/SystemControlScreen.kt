package dev.andikune.homeio.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.util.PermissionHelper
import dev.andikune.homeio.util.PermissionHelper.PermissionType

@Composable
fun SystemControlScreen(
    config: SettingsConfig,
    onUpdateConfig: (SettingsConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var brightness by remember { mutableFloatStateOf(config.manualBrightness.toFloat()) }
    var volume by remember { mutableFloatStateOf(50f) }
    var hasWriteSettings by remember { mutableStateOf(PermissionHelper.checkPermission(context, PermissionType.WRITE_SETTINGS)) }

    LaunchedEffect(Unit) {
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            volume = (currentVol.toFloat() / maxVol.toFloat()) * 100f
        } catch (_: Exception) {}
    }

    Box(Modifier.fillMaxSize().background(GlassColors.DeepBackgroundGradient)) {
        LazyColumn(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    BackButton(onClick = onBack)
                    Spacer(Modifier.width(16.dp))
                    Text("KENDALI SISTEM", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
                }
            }

            // --- WIFI & BLUETOOTH (PANEL NATIVE) ---
            item {
                SystemPanel(
                    title = "Pengaturan Wi-Fi",
                    icon = Icons.Default.Wifi,
                    description = "Hubungkan ke jaringan Wi-Fi dan masukkan sandi",
                    onClick = {
                        try {
                            // Memanggil panel Wi-Fi bawaan Android TV
                            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Fitur Wi-Fi tidak tersedia di TV ini", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                SystemPanel(
                    title = "Pengaturan Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    description = "Lakukan pairing perangkat Bluetooth (Remote, Speaker, dll)",
                    onClick = {
                        try {
                            // Memanggil panel Bluetooth bawaan Android TV
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Fitur Bluetooth tidak tersedia di TV ini", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // --- VOLUME ---
            item {
                Column(Modifier.fillMaxWidth().liquidGlass(cornerRadius = 16.dp).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, null, tint = GlassColors.AccentGlow)
                        Spacer(Modifier.width(12.dp))
                        Text("Volume Suara", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    TvSliderCustom(
                        value = volume,
                        onValueChange = { newVal ->
                            volume = newVal
                            try {
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val targetVol = ((newVal / 100f) * maxVol).toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }

            // --- KECERAHAN (BRIGHTNESS) ---
            item {
                Column(Modifier.fillMaxWidth().liquidGlass(cornerRadius = 16.dp).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BrightnessMedium, null, tint = GlassColors.AccentGlow)
                        Spacer(Modifier.width(12.dp))
                        Text("Kecerahan Layar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    TvSliderCustom(
                        value = brightness,
                        onValueChange = { newVal ->
                            brightness = newVal
                            onUpdateConfig(config.copy(manualBrightness = newVal.toInt()))
                            
                            // FIX: Ubah Window Attributes agar berefek instan ke layar
                            if (activity != null) {
                                try {
                                    val layoutParams = activity.window.attributes
                                    layoutParams.screenBrightness = (newVal / 100f).coerceIn(0.01f, 1f)
                                    activity.window.attributes = layoutParams
                                } catch (_: Exception) {}
                            }
                            
                            // Simpan juga ke system settings jika punya izin
                            if (hasWriteSettings) {
                                try {
                                    val targetBrightness = ((newVal / 100f) * 255).toInt()
                                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, targetBrightness)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                    if (!hasWriteSettings) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().clickable { 
                            PermissionHelper.openPermissionSettings(context, PermissionType.WRITE_SETTINGS)
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = GlassColors.AccentRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Klik di sini untuk memberi izin 'Ubah Pengaturan Sistem'", color = GlassColors.AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- PANEL TAMBAHAN ---
            item {
                SystemPanel(
                    title = "Buka Pengaturan Android TV",
                    icon = Icons.Default.Settings,
                    description = "Akses semua pengaturan sistem asli (Suara, Bahasa, Info TV, dll)",
                    onClick = { 
                        try { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } 
                        catch (_: Exception) { Toast.makeText(context, "Tidak bisa membuka pengaturan TV", Toast.LENGTH_SHORT).show() }
                    }
                )
            }

            item {
                SystemPanel(
                    title = "Kiosk Mode",
                    icon = Icons.Default.Lock,
                    tint = GlassColors.AccentRed,
                    description = if (config.isKioskModeEnabled) "Kiosk Aktif. Tekan OK untuk mematikan." else "Kiosk Nonaktif. Tekan OK untuk mengaktifkan.",
                    onClick = { onUpdateConfig(config.copy(isKioskModeEnabled = !config.isKioskModeEnabled)) }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun TvSliderCustom(value: Float, onValueChange: (Float) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x33FFFFFF)))
        Box(Modifier.fillMaxWidth(value / 100f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (isFocused) GlassColors.AccentGlow else GlassColors.AccentGlow.copy(alpha = 0.6f)))
        Row(Modifier.fillMaxWidth(value / 100f), horizontalArrangement = Arrangement.End) {
            Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(if (isFocused) Color.White else Color.Gray))
        }

        Box(
            Modifier.fillMaxSize()
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown) {
                        when (e.key) {
                            Key.DirectionLeft -> {
                                val newValue = (value - 5f).coerceIn(0f, 100f)
                                if (newValue != value) onValueChange(newValue)
                                true
                            }
                            Key.DirectionRight -> {
                                val newValue = (value + 5f).coerceIn(0f, 100f)
                                if (newValue != value) onValueChange(newValue)
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        )
        
        if (isFocused) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(GlassColors.AccentGlow))
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 12.dp, modifier = Modifier.size(48.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
            .onFocusChanged { isFocused = it.isFocused }.focusable()
            .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
            .clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ArrowBack, "Kembali", tint = if (isFocused) GlassColors.AccentGlow else Color.White)
        }
    }
}

@Composable
private fun SystemPanel(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, tint: Color = GlassColors.AccentGlow, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) GlassColors.AccentGlow else GlassColors.GlassBorder, RoundedCornerShape(16.dp))
            .onFocusChanged { isFocused = it.isFocused }.focusable()
            .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
            .clickable { onClick() }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(description, fontSize = 13.sp, color = GlassColors.TextSecondary)
            }
        }
    }
}
