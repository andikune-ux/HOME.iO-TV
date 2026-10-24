package dev.andikune.homeio.presentation.onboarding

import android.content.Context
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.util.PermissionHelper
import dev.andikune.homeio.util.PermissionHelper.PermissionType
import kotlinx.coroutines.delay

@Composable
fun OnboardingPermissionDialog(
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // SharedPreferences untuk menyimpan status System Limit
    val prefs = remember { ctx.getSharedPreferences("homeio_onboarding", Context.MODE_PRIVATE) }

    var refresh by remember { mutableStateOf(0) }
    
    var systemLimitedStorage by remember { mutableStateOf(prefs.getBoolean("limit_storage", false)) }
    var systemLimitedNotif by remember { mutableStateOf(prefs.getBoolean("limit_notif", false)) }
    var systemLimitedInstall by remember { mutableStateOf(prefs.getBoolean("limit_install", false)) }
    var systemLimitedUsage by remember { mutableStateOf(prefs.getBoolean("limit_usage", false)) }
    var systemLimitedListener by remember { mutableStateOf(prefs.getBoolean("limit_listener", false)) }
    var systemLimitedWrite by remember { mutableStateOf(prefs.getBoolean("limit_write", false)) }

    var showGuideDialog by remember { mutableStateOf(false) }
    var guideTitle by remember { mutableStateOf("") }
    var guideMessage by remember { mutableStateOf("") }
    var guideAction by remember { mutableStateOf<() -> Unit>({}) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun handlePermission(type: PermissionType, titleStr: String, guideStr: String) {
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
            }
            Toast.makeText(ctx, "Sistem TV tidak mendukung izin ini. Menggunakan mode kompatibilitas.", Toast.LENGTH_LONG).show()
            refresh++
        } else {
            guideTitle = titleStr
            guideMessage = guideStr
            guideAction = { PermissionHelper.openPermissionSettings(ctx, type) }
            showGuideDialog = true
        }
    }

    LaunchedEffect(showGuideDialog) {
        if (!showGuideDialog) refresh++
    }

    val hasStorage = PermissionHelper.checkPermission(ctx, PermissionType.STORAGE) || systemLimitedStorage
    val hasNotif = PermissionHelper.checkPermission(ctx, PermissionType.NOTIFICATION) || systemLimitedNotif
    val hasInstall = PermissionHelper.checkPermission(ctx, PermissionType.INSTALL_APK) || systemLimitedInstall
    val hasUsage = PermissionHelper.checkPermission(ctx, PermissionType.USAGE_ACCESS) || systemLimitedUsage
    val hasListener = PermissionHelper.checkPermission(ctx, PermissionType.NOTIF_LISTENER) || systemLimitedListener
    val hasWrite = PermissionHelper.checkPermission(ctx, PermissionType.WRITE_SETTINGS) || systemLimitedWrite

    val allGranted = hasStorage && hasNotif && hasInstall && hasUsage

    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(400); runCatching { firstFocusRequester.requestFocus() } }

    Dialog(
        onDismissRequest = { if (allGranted) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = allGranted, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).clip(RoundedCornerShape(20.dp)).background(GlassColors.DeepBackground)
                    .border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(20.dp)).padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text("IZIN DIPERLUKAN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
                Text("HOME.iO butuh izin agar fitur berjalan normal", fontSize = 12.sp, color = GlassColors.TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))

                PermissionRow(icon = Icons.Default.Folder, title = "Akses File (Wajib)", desc = "Backup & Update", granted = hasStorage, modifier = Modifier.focusRequester(firstFocusRequester), onGrant = {
                    handlePermission(PermissionType.STORAGE, "Izin Akses File", "1. Pilih menu 'Izin' atau 'Permissions'.\n2. Cari 'Penyimpanan' atau 'Files and media'.\n3. Pilih 'Izinkan'.")
                })

                PermissionRow(icon = Icons.Default.Notifications, title = "Notifikasi Sistem (Wajib)", desc = "Progress download", granted = hasNotif, onGrant = {
                    handlePermission(PermissionType.NOTIFICATION, "Izin Notifikasi", "1. Cari 'Notifikasi'.\n2. Aktifkan 'Izinkan Notifikasi'.")
                })

                PermissionRow(icon = Icons.Default.Warning, title = "Install Aplikasi (Wajib)", desc = "Pasang update APK", granted = hasInstall, onGrant = {
                    handlePermission(PermissionType.INSTALL_APK, "Izin Install APK", "1. Aktifkan 'Izinkan dari sumber ini'.\n2. Kembali ke aplikasi.")
                })

                PermissionRow(icon = Icons.Default.CheckCircle, title = "Akses RAM (Wajib)", desc = "Fitur RAM Booster", granted = hasUsage, onGrant = {
                    handlePermission(PermissionType.USAGE_ACCESS, "Izin Akses Penggunaan", "1. Cari 'HOME.iO' di daftar.\n2. Aktifkan izinnya.")
                })

                PermissionRow(icon = Icons.Default.Notifications, title = "Baca Notifikasi (Opsional)", desc = "Notif di Panel Kiri", granted = hasListener, onGrant = {
                    handlePermission(PermissionType.NOTIF_LISTENER, "Izin Baca Notifikasi", "1. Cari 'HOME.iO'.\n2. Aktifkan izinnya.")
                })

                PermissionRow(icon = Icons.Default.Settings, title = "Ubah Sistem (Opsional)", desc = "Kecerahan & WiFi", granted = hasWrite, onGrant = {
                    handlePermission(PermissionType.WRITE_SETTINGS, "Izin Ubah Sistem", "1. Aktifkan 'Izinkan pengaturan sistem'.")
                })

                Spacer(Modifier.height(20.dp))
                
                var isFocusedBtn by remember { mutableStateOf(false) }
                NeonFocusBorder(focused = isFocusedBtn, cornerRadius = 12.dp, borderWidth = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isFocusedBtn) GlassColors.AccentGlow else if (allGranted) GlassColors.AccentGreen else GlassColors.SurfaceDark)
                            .onFocusChanged { isFocusedBtn = it.isFocused }.focusable()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    if (allGranted) onDismiss() else refresh++
                                    true
                                } else false
                            }
                            .clickable { if (allGranted) onDismiss() else refresh++ }.padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (allGranted) "MULAI GUNAKAN HOME.IO" else "CEK ULANG IZIN", color = if (isFocusedBtn) GlassColors.DeepBackground else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        PermissionGuideDialog(
            title = guideTitle,
            message = guideMessage,
            onOpenSettings = { guideAction(); showGuideDialog = false },
            onDismiss = { showGuideDialog = false }
        )
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
                Text(title, color = GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(desc, color = GlassColors.TextSecondary, fontSize = 11.sp)
            }
            Text(if (granted) "✓ OK" else "BERI", color = if (granted) GlassColors.AccentGreen else GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
