package dev.andikune.homeio.presentation.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import kotlinx.coroutines.delay

@Composable
fun OnboardingPermissionDialog(
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var hasStorage by remember { mutableStateOf(false) }
    var hasNotif by remember { mutableStateOf(false) }
    var hasInstall by remember { mutableStateOf(false) }
    var hasUsage by remember { mutableStateOf(false) }
    var hasNotifListener by remember { mutableStateOf(false) }
    var hasWriteSettings by remember { mutableStateOf(false) } // Izin Sistem Baru
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
        hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true
        hasInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.packageManager.canRequestPackageInstalls() else true
        
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION") appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        }
        hasUsage = mode == AppOpsManager.MODE_ALLOWED
        hasNotifListener = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        hasWriteSettings = Settings.System.canWrite(ctx)
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val allGranted = hasStorage && hasNotif && hasInstall && hasUsage && hasNotifListener && hasWriteSettings

    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(400); runCatching { firstFocusRequester.requestFocus() } }

    Dialog(
        onDismissRequest = { if (allGranted) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = allGranted, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth(0.9f).fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(20.dp)).background(GlassColors.DeepBackground)
                    .border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(20.dp))
                    .padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text("IZIN DIPERLUKAN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
                Text("HOME.iO butuh izin agar semua fitur berjalan normal", fontSize = 12.sp, color = GlassColors.TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))

                // 1. Storage
                PermissionRow(icon = Icons.Default.Folder, title = "Akses File", desc = "Simpan Backup & download Update", granted = hasStorage, modifier = Modifier.focusRequester(firstFocusRequester), onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try { ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply { data = Uri.parse("package:${ctx.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } 
                        catch (_: Exception) { ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
                    }
                })

                // 2. Notifikasi
                PermissionRow(icon = Icons.Default.Notifications, title = "Notifikasi Sistem", desc = "Progress download di status bar", granted = hasNotif, onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                })

                // 3. Install APK
                PermissionRow(icon = Icons.Default.Warning, title = "Install Aplikasi", desc = "Pasang update APK HOME.iO", granted = hasInstall, onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) try { ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = Uri.parse("package:${ctx.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                })

                // 4. Usage Stats
                PermissionRow(icon = Icons.Default.CheckCircle, title = "Akses Penggunaan (RAM)", desc = "Fitur RAM Booster & bersihkan cache", granted = hasUsage, onGrant = {
                    try { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                })

                // 5. Notif Listener
                PermissionRow(icon = Icons.Default.Notifications, title = "Baca Notifikasi", desc = "Notifikasi HP/sistem di Panel Kiri", granted = hasNotifListener, onGrant = {
                    try { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                })

                // 6. WRITE_SETTINGS (System Control)
                PermissionRow(icon = Icons.Default.Settings, title = "Ubah Pengaturan Sistem", desc = "Atur Kecerahan, WiFi, & Volume TV", granted = hasWriteSettings, onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) try { ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { data = Uri.parse("package:${ctx.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                })

                Spacer(Modifier.height(20.dp))
                Text("Izin dicek ulang saat Anda kembali.", fontSize = 11.sp, color = GlassColors.TextSecondary)
                Spacer(Modifier.height(12.dp))

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
