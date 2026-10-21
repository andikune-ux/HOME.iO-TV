package dev.andikune.homeio.presentation.onboarding

import android.Manifest
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import dev.andikune.homeio.presentation.components.GlassColors

/**
 * OnboardingPermissionDialog — Dialog izin runtime saat pertama buka.
 * Fix Bug #3 (MANAGE_EXTERNAL_STORAGE), #23 (POST_NOTIFICATIONS), #46 (install).
 */
@Composable
fun OnboardingPermissionDialog(
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var hasStorage by remember { mutableStateOf(false) }
    var hasNotif by remember { mutableStateOf(false) }
    var hasInstall by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }

    // Refresh status saat kembali dari settings
    LaunchedEffect(refresh) {
        hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager() else true
        hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true
        hasInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ctx.packageManager.canRequestPackageInstalls() else true
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    val allGranted = hasStorage && hasNotif && hasInstall

    Dialog(
        onDismissRequest = { if (allGranted) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = allGranted,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassColors.DeepBackground)
                    .border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(20.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text("IZIN DIPERLUKAN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
                Text("Home.io butuh beberapa izin agar semua fitur berjalan normal", fontSize = 12.sp, color = GlassColors.TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))

                // Storage
                PermissionRow(
                    icon = Icons.Default.Folder,
                    title = "Akses File",
                    desc = "Untuk simpan Backup & download Update ke /sdcard/HomeiO/",
                    granted = hasStorage,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${ctx.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (_: Exception) {
                                ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }
                    }
                )

                // Notifikasi
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifikasi",
                    desc = "Untuk tampilkan progress download update di status bar",
                    granted = hasNotif,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                // Install APK
                PermissionRow(
                    icon = Icons.Default.Warning,
                    title = "Install Aplikasi",
                    desc = "Untuk pasang update APK Home.io yang diunduh",
                    granted = hasInstall,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            try {
                                ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                    data = Uri.parse("package:${ctx.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (_: Exception) {}
                        }
                    }
                )

                Spacer(Modifier.height(20.dp))
                Text(
                    "Izin akan dicek ulang saat Anda kembali ke aplikasi.",
                    fontSize = 11.sp, color = GlassColors.TextSecondary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))

                // Tombol Selesai
                var isFocused by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (allGranted) GlassColors.AccentGreen else GlassColors.SurfaceDark)
                        .border(if (isFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable {
                            if (allGranted) onDismiss() else refresh++
                        }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (allGranted) "MULAI GUNAKAN HOME.IO" else "CEK ULANG IZIN",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    desc: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (granted) GlassColors.AccentGreen.copy(alpha = 0.15f) else GlassColors.GlassTint)
            .border(
                if (focused) 2.dp else 1.dp,
                if (granted) GlassColors.AccentGreen else GlassColors.GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { if (!granted) onGrant() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (granted) Icons.Default.CheckCircle else icon, null,
            tint = if (granted) GlassColors.AccentGreen else GlassColors.AccentGlow,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, color = GlassColors.TextSecondary, fontSize = 11.sp)
        }
        Text(
            if (granted) "✓ OK" else "BERI",
            color = if (granted) GlassColors.AccentGreen else GlassColors.AccentGlow,
            fontWeight = FontWeight.Bold, fontSize = 12.sp
        )
    }
}
