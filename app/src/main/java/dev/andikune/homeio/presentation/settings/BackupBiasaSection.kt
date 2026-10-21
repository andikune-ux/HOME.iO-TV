package dev.andikune.homeio.presentation.settings

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.util.BackupBiasaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupBiasaSection(
    settings: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableStateOf(0) }
    var isBackingUp by remember { mutableStateOf(false) }

    val fileList = remember(refreshKey) { BackupBiasaManager.getBackupList(ctx) }

    fun doBackup() {
        if (isBackingUp) return
        isBackingUp = true
        scope.launch {
            val res = withContext(Dispatchers.IO) { BackupBiasaManager.createBackup(ctx, settings) }
            isBackingUp = false
            Toast.makeText(ctx, res.msg ?: if (res.success) "Backup berhasil" else "Backup gagal", Toast.LENGTH_LONG).show()
            if (res.success) refreshKey++
        }
    }

    fun doRestore(file: File) {
        scope.launch {
            val restored = withContext(Dispatchers.IO) { BackupBiasaManager.restoreBackup(file.absolutePath) }
            if (restored != null) {
                onUpdate(restored)
                Toast.makeText(ctx, "Tema berhasil dipulihkan!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(ctx, "Gagal membaca file backup", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun doDelete(file: File) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { BackupBiasaManager.deleteBackup(file.absolutePath) }
            if (ok) {
                Toast.makeText(ctx, "File dihapus", Toast.LENGTH_SHORT).show()
                refreshKey++
            } else {
                Toast.makeText(ctx, "Gagal menghapus file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Save, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("BACKUP BIASA (TEMA)", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Simpan & Pulihkan pengaturan aplikasi ke file .json", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        Text("Lokasi: /HomeiO/Backup/", color = GlassColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        var isFocusedBtn by remember { mutableStateOf(false) }
        NeonFocusBorder(focused = isFocusedBtn, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (isFocusedBtn) GlassColors.AccentGlow.copy(alpha = 0.3f) else GlassColors.GlassTint)
                    .border(if (isFocusedBtn) 2.dp else 1.dp, GlassColors.AccentGlow, RoundedCornerShape(12.dp))
                    .onFocusChanged { isFocusedBtn = it.isFocused }
                    .focusable()
                    .clickable(enabled = !isBackingUp) { doBackup() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Save, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isBackingUp) "MENYIMPAN..." else "BUAT BACKUP BARU",
                    color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }
        }

        Text("RIWAYAT FILE BACKUP", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        
        if (fileList.isEmpty()) {
            Text("Belum ada file backup.", color = GlassColors.TextSecondary, fontSize = 12.sp)
        } else {
            fileList.forEach { file ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date(file.lastModified()))

                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(GlassColors.GlassTint)
                        .border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name, color = GlassColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(dateStr, color = GlassColors.TextSecondary, fontSize = 10.sp)
                    }
                    
                    // Tombol Pulihkan
                    ActionIconButton(icon = Icons.Default.Restore, tint = GlassColors.AccentGreen, onClick = { doRestore(file) })
                    Spacer(Modifier.width(8.dp))
                    // Tombol Hapus
                    ActionIconButton(icon = Icons.Default.Delete, tint = GlassColors.AccentRed, onClick = { doDelete(file) })
                }
            }
        }
    }
}

@Composable
private fun ActionIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = focused, cornerRadius = 8.dp) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(if (focused) tint.copy(alpha = 0.4f) else tint.copy(alpha = 0.15f))
                .border(if (focused) 2.dp else 1.dp, tint, RoundedCornerShape(8.dp))
                .onFocusChanged { focused = it.isFocused }
                .focusable().clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}
