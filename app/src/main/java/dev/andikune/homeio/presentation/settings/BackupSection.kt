package dev.andikune.homeio.presentation.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Placeholder warna (akan disesuaikan saat Theme Liquid Glass selesai)
private val GLASS_BG = Color(0x33FFFFFF)
private val GLASS_BORDER = Color(0x66FFFFFF)
private val GOLD_ACCENT = Color(0xFFFFD700)
private val GREEN_SUCCESS = Color(0xFF4CAF50)
private val RED_WARN = Color(0xFFFF5252)

@Composable
fun BackupSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isFocused by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var needPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        needPermission = BackupManager.needsStoragePermission()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "💾 BACKUP AMAN",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = GOLD_ACCENT
        )
        Text(
            text = "Ekspor semua pengaturan + Memory Knowledge ke file .txt untuk backup atau handoff AI.",
            fontSize = 13.sp,
            color = Color(0xCCFFFFFF)
        )
        Text(
            text = "Lokasi: Internal/HomeiO/Backup aman/",
            fontSize = 11.sp,
            color = Color(0x99FFFFFF)
        )

        if (needPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RED_WARN.copy(alpha = 0.15f))
                    .border(2.dp, RED_WARN, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, tint = RED_WARN, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Izin Akses File Diperlukan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RED_WARN)
                    Text("Untuk simpan backup ke /HomeiO/Backup aman/", fontSize = 11.sp, color = Color(0xCCFFFFFF))
                }
            }

            SimpleButton(
                label = "BERI IZIN SEKARANG",
                icon = Icons.Default.Folder,
                backgroundColor = RED_WARN,
                onClick = { BackupManager.openPermissionSettings(context) }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFocused) GOLD_ACCENT.copy(alpha = 0.3f) else GLASS_BG)
                    .border(
                        width = if (isFocused) 3.dp else 1.dp,
                        color = if (isFocused) GOLD_ACCENT else GLASS_BORDER,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable(enabled = !isRunning) {
                        if (isRunning) return@clickable
                        isRunning = true
                        statusMessage = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                val content = BackupManager.generateBackupContent(context)
                                BackupManager.saveBackupToFile(context, content)
                            }
                            isRunning = false
                            if (result.success) {
                                isSuccess = true
                                statusMessage = "✅ Backup berhasil!\n${result.filePath}"
                            } else {
                                isSuccess = false
                                statusMessage = "❌ ${result.errorMessage ?: "Gagal backup"}"
                                needPermission = result.needPermission
                            }
                        }
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = if (isRunning) Color(0x99FFFFFF) else GOLD_ACCENT,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isRunning) "MEMBUAT BACKUP..." else "BUAT BACKUP AMAN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) Color(0x99FFFFFF) else GOLD_ACCENT
                    )
                }
            }
        }

        statusMessage?.let { msg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background((if (isSuccess) GREEN_SUCCESS else RED_WARN).copy(alpha = 0.15f))
                    .border(1.dp, if (isSuccess) GREEN_SUCCESS else RED_WARN, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSuccess) GREEN_SUCCESS else RED_WARN,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = if (isSuccess) GREEN_SUCCESS else RED_WARN,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SimpleButton(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor.copy(alpha = 0.9f))
            .border(if (focused) 3.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
