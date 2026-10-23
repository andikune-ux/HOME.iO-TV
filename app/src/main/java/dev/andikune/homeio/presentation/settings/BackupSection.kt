package dev.andikune.homeio.presentation.settings

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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupSection(
    settings: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
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
                val msg = if (res.isFallbackPath) {
                    "Backup Aman tersimpan (fallback path).\nIzin storage belum diberikan."
                } else {
                    "Backup Aman berhasil:\n${res.filePath}"
                }
                Toast.makeText(ctx.applicationContext, msg, Toast.LENGTH_LONG).show()
            } else {
                if (res.isFallbackPath) {
                    // Jika gagal dan butuh izin, arahkan ke pengaturan
                    BackupManager.openPermissionSettings(ctx)
                }
                Toast.makeText(ctx.applicationContext, "Gagal: ${res.errorMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Save, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("BACKUP AMAN (TXT)", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Simpan semua info aplikasi + FULL SOURCE CODE", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        Text("Lokasi: /sdcard/HomeiO/Backup aman/", color = GlassColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        var isFocusedBtn by remember { mutableStateOf(false) }
        NeonFocusBorder(focused = isFocusedBtn, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (isFocusedBtn) GlassColors.AccentGlow.copy(alpha = 0.3f) else GlassColors.GlassTint)
                    .border(if (isFocusedBtn) 2.dp else 1.dp, GlassColors.AccentGlow, RoundedCornerShape(12.dp))
                    .onFocusChanged { isFocusedBtn = it.isFocused }
                    .focusable()
                    .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { if(!isBackingUp) doBackupAman(); true } else false }
                    .clickable(enabled = !isBackingUp) { doBackupAman() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isBackingUp) "MENYIMPAN..." else "BUAT BACKUP AMAN (.txt)",
                    color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }
        }

        if (lastBackupPath != null) {
            val color = if (wasFallback) GlassColors.AccentAmber else GlassColors.AccentGreen
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (wasFallback) Icons.Default.Warning else Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (wasFallback) "Tersimpan di fallback (izin storage belum ada)." else "Backup berhasil disimpan.",
                    color = color, fontSize = 11.sp
                )
            }
        }
    }
}
