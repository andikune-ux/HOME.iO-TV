package dev.andikune.homeio.presentation.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.util.PermissionHelper
import dev.andikune.homeio.util.PermissionHelper.PermissionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SystemControlScreen(
    config: SettingsConfig,
    onUpdateConfig: (SettingsConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }

    var brightness by remember { mutableFloatStateOf(50f) }
    var volume by remember { mutableFloatStateOf(50f) }
    var hasWriteSettings by remember { mutableStateOf(PermissionHelper.checkPermission(context, PermissionType.WRITE_SETTINGS)) }

    var wifiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanningWifi by remember { mutableStateOf(false) }

    var btList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanningBt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val currentBrightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            brightness = (currentBrightness / 255f) * 100f
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            volume = (currentVol.toFloat() / maxVol.toFloat()) * 100f
        } catch (_: Exception) {}
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Izin lokasi dibutuhkan untuk scan WiFi", Toast.LENGTH_SHORT).show()
    }
    val btPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { !it }) Toast.makeText(context, "Izin Bluetooth ditolak", Toast.LENGTH_SHORT).show()
    }

    fun startWifiScan() {
        if (wifiManager == null) {
            Toast.makeText(context, "TV ini tidak mendukung WiFi", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        if (!wifiManager.isWifiEnabled) {
            @Suppress("DEPRECATION") wifiManager.isWifiEnabled = true
            Toast.makeText(context, "Menyalakan WiFi...", Toast.LENGTH_SHORT).show()
        }
        
        isScanningWifi = true
        @Suppress("DEPRECATION") val success = wifiManager.startScan()
        if (!success) {
            Toast.makeText(context, "Gagal scan. Coba lagi.", Toast.LENGTH_SHORT).show()
            isScanningWifi = false
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val results = wifiManager.scanResults.map { it.SSID }.filter { it.isNotBlank() }.distinct()
                    wifiList = results.ifEmpty { listOf("(Tidak ada WiFi ditemukan)") }
                } catch (_: Exception) {
                    wifiList = listOf("(Gagal membaca hasil scan)")
                }
                isScanningWifi = false
            }, 2000)
        }
    }

    fun startBtScan() {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "TV ini tidak mendukung Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            btPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            @Suppress("DEPRECATION") bluetoothAdapter.enable()
            Toast.makeText(context, "Menyalakan Bluetooth...", Toast.LENGTH_SHORT).show()
        }

        isScanningBt = true
        btList = emptyList()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (BluetoothDevice.ACTION_FOUND == intent?.action) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val name = try { device?.name } catch (e: SecurityException) { null } ?: "Perangkat Tanpa Nama"
                    if (!btList.contains(name)) btList = btList + name
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == intent?.action) {
                    isScanningBt = false
                    if (btList.isEmpty()) btList = listOf("(Tidak ada perangkat ditemukan)")
                    try { context.unregisterReceiver(this) } catch (_: Exception) {}
                }
            }
        }
        
        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        })
        @Suppress("DEPRECATION") bluetoothAdapter.startDiscovery()
        
        Handler(Looper.getMainLooper()).postDelayed({
            if (isScanningBt) {
                @Suppress("DEPRECATION") bluetoothAdapter.cancelDiscovery()
                isScanningBt = false
                try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            }
        }, 10000)
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

            item {
                Column(Modifier.fillMaxWidth().liquidGlass(cornerRadius = 16.dp).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, null, tint = GlassColors.AccentGlow)
                            Spacer(Modifier.width(12.dp))
                            Text("WiFi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        SystemButton(text = if (isScanningWifi) "Scanning..." else "Scan", onClick = { startWifiScan() })
                    }
                    if (wifiList.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        wifiList.forEach { ssid ->
                            Text(ssid, color = GlassColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            item {
                Column(Modifier.fillMaxWidth().liquidGlass(cornerRadius = 16.dp).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, null, tint = GlassColors.AccentGlow)
                            Spacer(Modifier.width(12.dp))
                            Text("Bluetooth", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        SystemButton(text = if (isScanningBt) "Scanning..." else "Scan", onClick = { startBtScan() })
                    }
                    if (btList.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        btList.forEach { name ->
                            Text(name, color = GlassColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

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

// FIX: TvSliderCustom sekarang responsif terhadap D-Pad
@Composable
fun TvSliderCustom(value: Float, onValueChange: (Float) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.CenterStart) {
        // Track Belakang
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x33FFFFFF)))
        // Track Depan (Aktif)
        Box(Modifier.fillMaxWidth(value / 100f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (isFocused) GlassColors.AccentGlow else GlassColors.AccentGlow.copy(alpha = 0.6f)))
        // Thumb (Bulatan)
        Row(Modifier.fillMaxWidth(value / 100f), horizontalArrangement = Arrangement.End) {
            Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(if (isFocused) Color.White else Color.Gray))
        }

        // Layer deteksi fokus dan D-Pad
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
        
        // Garis bawah tanda fokus
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
private fun SystemButton(text: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 8.dp) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (isFocused) GlassColors.AccentGlow else GlassColors.GlassTint)
            .onFocusChanged { isFocused = it.isFocused }.focusable()
            .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
            .clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(text, color = if (isFocused) GlassColors.DeepBackground else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
