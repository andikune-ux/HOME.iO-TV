package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.util.CacheCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppCacheDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var appList by remember { mutableStateOf<List<CacheCleaner.AppCacheInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val hasAccess = remember { CacheCleaner.hasUsageAccess(ctx) }

    LaunchedEffect(Unit) {
        if (hasAccess) {
            isLoading = true
            appList = withContext(Dispatchers.IO) { CacheCleaner.getAllAppsCache(ctx) }
            isLoading = false
        } else isLoading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).clip(RoundedCornerShape(20.dp))
                    .background(GlassColors.DeepBackground).border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(20.dp)).padding(20.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apps, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("KELOLA CACHE APLIKASI LAIN", color = GlassColors.AccentGlow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Tap app → Clear cache", color = GlassColors.TextSecondary, fontSize = 11.sp)
                        }
                    }
                    CloseButton(onClick = onDismiss)
                }

                Spacer(Modifier.height(14.dp))

                if (!hasAccess) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(GlassColors.AccentAmber.copy(alpha = 0.15f))
                            .border(1.dp, GlassColors.AccentAmber, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️ Izin Diperlukan", color = GlassColors.AccentAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Untuk melihat ukuran cache aplikasi lain, Home.io butuh izin Usage Access.", color = GlassColors.TextPrimary, fontSize = 12.sp)
                        PermissionButton(label = "AKTIFKAN IZIN USAGE ACCESS", onClick = { CacheCleaner.openUsageAccessSettings(ctx) })
                        Spacer(Modifier.height(8.dp))
                        Text("Atau tanpa izin:", color = GlassColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        PermissionButton(label = "BUKA STORAGE TV (MANUAL)", onClick = { CacheCleaner.openStorageSettings(ctx) })
                    }
                } else if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Memindai cache aplikasi...", color = GlassColors.TextSecondary, fontSize = 13.sp)
                    }
                } else if (appList.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✅ Tidak ada aplikasi dengan cache > 1 MB", color = GlassColors.AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Cache TV/HP Anda sudah bersih", color = GlassColors.TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    Text("${appList.size} aplikasi dengan cache terbesar", color = GlassColors.TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(appList, key = { it.packageName }) { app ->
                            AppCacheRow(app = app, onClick = { CacheCleaner.openAppInfoSettings(ctx, app.packageName) })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Setelah tap app → Anda akan masuk ke App Info → tap 'Clear cache' di sana.", color = GlassColors.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AppCacheRow(app: CacheCleaner.AppCacheInfo, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (focused) GlassColors.AccentGlow.copy(alpha = 0.15f) else GlassColors.GlassTint)
            .border(if (focused) 2.dp else 1.dp, if (focused) GlassColors.AccentGlow else GlassColors.GlassBorder, RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.appName, color = GlassColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (app.isSystemApp) "Sistem · ${app.packageName}" else app.packageName, color = GlassColors.TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
        Text(CacheCleaner.formatBytes(app.cacheBytes), color = GlassColors.AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Default.FolderOpen, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PermissionButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = focused, cornerRadius = 10.dp) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(GlassColors.AccentGlow.copy(alpha = 0.85f))
                .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                .onFocusChanged { focused = it.isFocused }.focusable().clickable { onClick() }.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = GlassColors.DeepBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            .background(if (focused) GlassColors.AccentRed.copy(alpha = 0.4f) else GlassColors.AccentRed.copy(alpha = 0.2f))
            .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, null, tint = GlassColors.AccentRed, modifier = Modifier.size(18.dp))
    }
}
