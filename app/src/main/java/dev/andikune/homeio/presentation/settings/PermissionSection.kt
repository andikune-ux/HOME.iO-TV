package dev.andikune.homeio.presentation.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.util.PermissionHelper
import dev.andikune.homeio.util.PermissionHelper.PermissionType

@Composable
fun PermissionSection() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // SharedPreferences untuk menyimpan status System Limit (sama seperti Onboarding)
    val prefs = remember { ctx.getSharedPreferences("homeio_onboarding", Context.MODE_PRIVATE) }

    var refresh by remember { mutableStateOf(0) }

    var systemLimitedStorage by remember { mutableStateOf(prefs.getBoolean("limit_storage", false)) }
    var systemLimitedNotif by remember { mutableStateOf(prefs.getBoolean("limit_notif", false)) }
    var systemLimitedInstall by remember { mutableStateOf(prefs.getBoolean("limit_install", false)) }
    var systemLimitedUsage by remember { mutableStateOf(prefs.getBoolean("limit_usage", false)) }
    var systemLimitedListener by remember { mutableStateOf(prefs.getBoolean("limit_listener", false)) }
    var systemLimitedWrite by remember { mutableStateOf(prefs.getBoolean("limit_write", false)) }
    var systemLimitedOverlay by remember { mutableStateOf(prefs.getBoolean("limit_overlay", false)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun handlePermission(type: PermissionType) {
        if (PermissionHelper.checkPermission(ctx, type)) {
            Toast.makeText(ctx, "Izin sudah aktif.", Toast.LENGTH_SHORT).show()
            refresh++
            return
        }
        
        val opened = PermissionHelper.openPermissionSettings(ctx, type)
        
        if (!opened) {
            when(type) {
                PermissionType.STORAGE -> { systemLimitedStorage = true; prefs.edit().putBoolean("limit_storage", true).apply() }
                PermissionType.NOTIFICATION -> { systemLimitedNotif = true; prefs.edit().putBoolean("limit_notif", true).apply() }
                PermissionType.INSTALL_APK -> { systemLimitedInstall = true; prefs.edit().putBoolean("limit_install", true).apply() }
                PermissionType.USAGE_ACCESS -> { systemLimitedUsage = true; prefs.edit().putBoolean("limit_usage", true).apply() }
                PermissionType.NOTIF_LISTENER -> { systemLimitedListener = true; prefs.edit().putBoolean("limit_listener", true).apply() }
                PermissionType.WRITE_SETTINGS -> { systemLimitedWrite = true; prefs.edit().putBoolean("limit_write", true).apply() }
                PermissionType.OVERLAY -> { systemLimitedOverlay = true; prefs.edit().putBoolean("limit_overlay", true).apply() }
            }
            Toast.makeText(ctx, "Sistem TV tidak mendukung izin ini. Menggunakan mode kompatibilitas.", Toast.LENGTH_LONG).show()
            refresh++
        }
    }

    val hasStorage = PermissionHelper.checkPermission(ctx, PermissionType.STORAGE) || systemLimitedStorage
    val hasNotif = PermissionHelper.checkPermission(ctx, PermissionType.NOTIFICATION) || systemLimitedNotif
    val hasInstall = PermissionHelper.checkPermission(ctx, PermissionType.INSTALL_APK) || systemLimitedInstall
    val hasUsage = PermissionHelper.checkPermission(ctx, PermissionType.USAGE_ACCESS) || systemLimitedUsage
    val hasListener = PermissionHelper.checkPermission(ctx, PermissionType.NOTIF_LISTENER) || systemLimitedListener
    val hasWrite = PermissionHelper.checkPermission(ctx, PermissionType.WRITE_SETTINGS) || systemLimitedWrite
    val hasOverlay = PermissionHelper.checkPermission(ctx, PermissionType.OVERLAY) || systemLimitedOverlay

    Column(Modifier.fillMaxWidth()) {
        Text(
            "IZIN APLIKASI",
            color = GlassColors.AccentGlow,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Kelola izin yang dibutuhkan HOME.iO agar semua fitur berjalan normal. Jika status izin belum aktif, klik tombol BERI untuk membuka pengaturan.",
            color = GlassColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        PermissionRow(
            icon = Icons.Default.Folder, 
            title = "Akses File (Wajib)", 
            desc = "Untuk Backup & Update APK", 
            granted = hasStorage, 
            onGrant = { handlePermission(PermissionType.STORAGE) }
        )

        Spacer(Modifier.height(8.dp))

        PermissionRow(
            icon = Icons.Default.Notifications, 
            title = "Notifikasi Sistem (Wajib)", 
            desc = "Untuk progress download", 
            granted = hasNotif, 
            onGrant = { handlePermission(PermissionType.NOTIFICATION) }
        )

        Spacer(Modifier.height(8.dp))

        PermissionRow(
            icon = Icons.Default.Warning, 
            title = "Install Aplikasi (Wajib)", 
            desc = "Untuk memasang update APK", 
            granted = hasInstall, 
            onGrant = { handlePermission(PermissionType.INSTALL_APK) }
        )

        Spacer(Modifier.height(8.dp))

        PermissionRow(
            icon = Icons.Default.CheckCircle, 
            title = "Akses Penggunaan (Wajib)", 
            desc = "Untuk fitur RAM Booster (Usage Stats)", 
            granted = hasUsage, 
            onGrant = { handlePermission(PermissionType.USAGE_ACCESS) }
        )

        Spacer(Modifier.height(8.dp))

        PermissionRow(
            icon = Icons.Default.Notifications, 
            title = "Baca Notifikasi (Opsional)", 
            desc = "Untuk Panel Notifikasi di Sidebar", 
            granted = hasListener, 
            onGrant = { handlePermission(PermissionType.NOTIF_LISTENER) }
        )

        Spacer(Modifier.height(8.dp))

        PermissionRow(
            icon = Icons.Default.Settings, 
            title = "Ubah Sistem (Opsional)", 
            desc = "Untuk Kecerahan & WiFi", 
            granted = hasWrite, 
            onGrant = { handlePermission(PermissionType.WRITE_SETTINGS) }
        )

        Spacer(Modifier.height(8.dp))

        PermissionRow(
            icon = Icons.Default.Layers, 
            title = "Gambar di Atas Aplikasi (Opsional)", 
            desc = "Untuk Overlay & Pop-up", 
            granted = hasOverlay, 
            onGrant = { handlePermission(PermissionType.OVERLAY) }
        )
        
        Spacer(Modifier.height(24.dp))

        // Tombol Cek Ulang
        var isFocusedBtn by remember { mutableStateOf(false) }
        NeonFocusBorder(focused = isFocusedBtn, cornerRadius = 12.dp, borderWidth = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isFocusedBtn) GlassColors.AccentGlow else GlassColors.SurfaceDark)
                    .onFocusChanged { isFocusedBtn = it.isFocused }.focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                            refresh++
                            true
                        } else false
                    }
                    .clickable { refresh++ }.padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("CEK ULANG IZIN", color = if (isFocusedBtn) GlassColors.DeepBackground else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, desc: String, granted: Boolean, modifier: Modifier = Modifier, onGrant: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = focused, cornerRadius = 12.dp, borderWidth = 4.dp, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (focused) GlassColors.GlassTintLighter else if (granted) GlassColors.AccentGreen.copy(alpha = 0.15f) else GlassColors.GlassTint)
                .onFocusChanged { focused = it.isFocused }.focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                        if (!granted) onGrant()
                        true
                    } else false
                }
                .clickable { if (!granted) onGrant() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (granted) Icons.Default.CheckCircle else icon, null, tint = if (granted) GlassColors.AccentGreen else GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
            Text(if (granted) "✓ OK" else "BERI", color = if (granted) GlassColors.AccentGreen else GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
