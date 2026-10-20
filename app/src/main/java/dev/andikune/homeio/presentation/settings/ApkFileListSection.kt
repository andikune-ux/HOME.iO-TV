package dev.andikune.homeio.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.util.ApkDownloader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GLASS_BG = Color(0x0DFFFFFF)
private val GLASS_BORDER = Color(0x1AFFFFFF)
private val GOLD_ACCENT = Color(0xFFFFD700)
private val GREEN_SUCCESS = Color(0xFF4CAF50)
private val RED_WARN = Color(0xFFFF5252)
private val TEXT_PRIMARY = Color(0xFFFFFFFF)
private val TEXT_SECONDARY = Color(0xB3FFFFFF)

@Composable
fun ApkFileListSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    val apkList = remember(refreshKey) { ApkDownloader.getDownloadedApkList(context) }

    // Refresh otomatis setiap 2 detik selama ada download aktif
    LaunchedEffect(refreshKey) {
        kotlinx.coroutines.delay(2000)
        refreshKey++
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "📦 FILE PEMBARUAN TERSIMPAN",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = GOLD_ACCENT
        )
        Text(
            text = "File APK yang sudah diunduh tidak terhapus otomatis. Klik PASANG untuk install manual.",
            fontSize = 12.sp,
            color = TEXT_SECONDARY,
            lineHeight = 16.sp
        )
        Text(
            text = "Lokasi: Internal/HomeiO/Pembaharuan/",
            fontSize = 11.sp,
            color = TEXT_SECONDARY.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace
        )

        if (apkList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GLASS_BG)
                    .border(1.dp, GLASS_BORDER, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Folder, null, tint = TEXT_SECONDARY, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Belum ada file APK tersimpan", fontSize = 13.sp, color = TEXT_SECONDARY)
                    Text("File akan muncul di sini setelah download update", fontSize = 11.sp, color = TEXT_SECONDARY.copy(alpha = 0.7f))
                }
            }
        } else {
            apkList.forEach { file ->
                ApkFileItem(
                    file = file,
                    onInstall = {
                        if (!ApkDownloader.canInstallApk(context)) {
                            Toast.makeText(context, "Beri izin 'Install unknown apps' dulu", Toast.LENGTH_LONG).show()
                            ApkDownloader.openInstallPermissionSettings(context)
                        } else {
                            val ok = ApkDownloader.installApk(context, file.absolutePath)
                            if (!ok) Toast.makeText(context, "Gagal membuka installer", Toast.LENGTH_LONG).show()
                        }
                    },
                    onDelete = {
                        val deleted = ApkDownloader.deleteApk(file.absolutePath)
                        if (deleted) {
                            Toast.makeText(context, "File dihapus: ${file.name}", Toast.LENGTH_SHORT).show()
                            refreshKey++
                        } else {
                            Toast.makeText(context, "Gagal menghapus file", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ApkFileItem(
    file: File,
    onInstall: () -> Unit,
    onDelete: () -> Unit
) {
    var isFocusedInstall by remember { mutableStateOf(false) }
    var isFocusedDelete by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val sizeText = formatSize(file.length())
    val dateText = dateFormat.format(Date(file.lastModified()))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GLASS_BG)
            .border(
                if (isFocusedInstall) 2.dp else 1.dp,
                if (isFocusedInstall) GREEN_SUCCESS else GLASS_BORDER,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocusedInstall) GREEN_SUCCESS.copy(alpha = 0.15f) else Color.Transparent)
                .onFocusChanged { isFocusedInstall = it.isFocused }
                .focusable()
                .clickable { onInstall() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isFocusedInstall) Icons.Default.CheckCircle else Icons.Default.Download,
                null,
                tint = GREEN_SUCCESS,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TEXT_PRIMARY)
                Spacer(Modifier.height(2.dp))
                Text("$sizeText · $dateText", fontSize = 11.sp, color = TEXT_SECONDARY)
                if (isFocusedInstall) {
                    Spacer(Modifier.height(2.dp))
                    Text("TAP UNTUK PASANG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GREEN_SUCCESS)
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocusedDelete) RED_WARN.copy(alpha = 0.3f) else RED_WARN.copy(alpha = 0.15f))
                .border(
                    if (isFocusedDelete) 2.dp else 1.dp,
                    RED_WARN, RoundedCornerShape(8.dp)
                )
                .onFocusChanged { isFocusedDelete = it.isFocused }
                .focusable()
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Delete, "Hapus", tint = RED_WARN, modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
