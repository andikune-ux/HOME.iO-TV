package dev.andikune.homeio.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import dev.andikune.homeio.BuildConfig
import dev.andikune.homeio.data.AppKnowledge
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InfoAppSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBackingUp by remember { mutableStateOf(false) }
    var lastBackupPath by remember { mutableStateOf<String?>(null) }
    var wasFallback by remember { mutableStateOf(false) }

    fun doBackupAman() {
        if (isBackingUp) return
        isBackingUp = true
        scope.launch {
            val content = withContext(Dispatchers.IO) { BackupManager.generateBackupContent(ctx) }
            val res = withContext(Dispatchers.IO) { BackupManager.saveBackupToFile(ctx, content) }
            isBackingUp = false
            if (res.success) {
                lastBackupPath = res.filePath
                wasFallback = res.isFallbackPath
                val msg = if (res.isFallbackPath) "Backup Aman tersimpan (fallback path)." else "Backup Aman berhasil:\n${res.filePath}"
                Toast.makeText(ctx.applicationContext, msg, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(ctx.applicationContext, "Gagal: ${res.errorMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // FIX BUG IKON: Ambil ikon dari PackageManager
    val iconBitmap = remember {
        try {
            ctx.packageManager.getApplicationIcon(ctx.packageName).toBitmap().asImageBitmap()
        } catch (e: Exception) { null }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (iconBitmap != null) {
            Image(bitmap = iconBitmap, contentDescription = "Icon", modifier = Modifier.size(90.dp).clip(RoundedCornerShape(20.dp)).border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(20.dp)))
        } else {
            Box(Modifier.size(90.dp).clip(RoundedCornerShape(20.dp)).background(GlassColors.GlassSolidFallback).border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Home, contentDescription = "Icon", tint = GlassColors.AccentGlow, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("HOME.IO", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = GlassColors.TextPrimary, letterSpacing = 1.sp)
        Text("Liquid Glass Launcher", fontSize = 13.sp, color = GlassColors.TextSecondary)

        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(GlassColors.AccentGlow.copy(alpha = 0.15f)).border(1.dp, GlassColors.AccentGlow, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
            Text("Versi ${BuildConfig.VERSION_NAME}", color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow("Package ID", BuildConfig.APPLICATION_ID)
            InfoRow("Platform", "Android TV / STB")
            InfoRow("Developer", "Andi (andikune@gmail.com)")
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle("BACKUP AMAN (FULL TXT)")
        Text("Menyimpan semua file + memory AI (AppKnowledge.kt) ke format .txt untuk backup lengkap.", color = GlassColors.TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        Text("Lokasi: /sdcard/HomeiO/Backup aman/", color = GlassColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        
        ActionButtonLarge(
            icon = Icons.Default.Folder,
            label = if (isBackingUp) "MEMPROSES..." else "BUAT BACKUP AMAN (.txt)",
            color = GlassColors.AccentGreen,
            onClick = { doBackupAman() }
        )

        if (lastBackupPath != null) {
            val color = if (wasFallback) GlassColors.AccentAmber else GlassColors.AccentGreen
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)).border(1.dp, color, RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (wasFallback) Icons.Default.Warning else Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (wasFallback) "Tersimpan di fallback." else "Backup berhasil disimpan.", color = color, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle("MEDIA SOSIAL")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SocialButton("WhatsApp", Color(0xFF25D366), Modifier.weight(1f)) { openUrl(ctx, AppKnowledge.SOCIAL_WA) }
            SocialButton("TikTok", Color(0xFFEE1D52), Modifier.weight(1f)) { openUrl(ctx, AppKnowledge.SOCIAL_TIKTOK) }
            SocialButton("Instagram", Color(0xFFE1306C), Modifier.weight(1f)) { openUrl(ctx, AppKnowledge.SOCIAL_IG) }
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle("PROMPT AI")
        Text("Salin instruksi untuk AI baru (handoff) ke clipboard.", color = GlassColors.TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        
        ActionButtonLarge(
            icon = Icons.Default.ContentCopy,
            label = "SALIN PROMPT AI",
            color = GlassColors.AccentGlow,
            onClick = {
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("AI Prompt", AppKnowledge.AI_PROMPT))
                Toast.makeText(ctx.applicationContext, "✅ Prompt tersalin ke clipboard", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(GlassColors.GlassBorder))
        Text(title, color = GlassColors.AccentGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(GlassColors.GlassBorder))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = GlassColors.TextSecondary, fontSize = 12.sp)
        Text(value, color = GlassColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ActionButtonLarge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (isFocused) color.copy(alpha = 0.3f) else color.copy(alpha = 0.15f))
                .border(if (isFocused) 2.dp else 1.dp, color, RoundedCornerShape(12.dp))
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() }.padding(14.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SocialButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 10.dp, modifier = modifier) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(if (isFocused) color.copy(alpha = 0.4f) else color.copy(alpha = 0.15f))
                .border(if (isFocused) 2.dp else 1.dp, color, RoundedCornerShape(10.dp))
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() }.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

private fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (e: Exception) {
        Toast.makeText(ctx.applicationContext, "Tidak bisa membuka link", Toast.LENGTH_SHORT).show()
    }
}
