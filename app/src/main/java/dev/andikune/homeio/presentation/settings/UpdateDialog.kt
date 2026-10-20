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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// === Warna Tema (sementara — akan disesuaikan saat GlassTheme fix) ===
private val GLASS_BG = Color(0xE60B1720)
private val GLASS_BORDER = Color(0xFFFFD700)
private val GOLD_ACCENT = Color(0xFFFFD700)
private val GREEN_SUCCESS = Color(0xFF4CAF50)
private val RED_WARN = Color(0xFFFF5252)
private val TEXT_PRIMARY = Color(0xFFFFFFFF)
private val TEXT_SECONDARY = Color(0xB3FFFFFF)

/**
 * UpdateDialog — Skenario A (Manual)
 *
 * State Flow:
 *   IDLE        → Tombol [UPDATE] [NANTI] [SKIP]  (atau [UPDATE] [TIDAK] jika Force)
 *   DOWNLOADING → Progress bar + label %
 *   READY       → Tombol [INSTALL] [TUTUP]        (file APK sudah di folder Pembaruan)
 *   INSTALLING  → Tombol disable + label "Menginstall..."
 *
 * Force Update mode: tidak ada tombol NANTI/SKIP, tidak bisa dismiss.
 */
@Composable
fun UpdateDialog(
    currentVersion: String,
    latestVersion: String,
    releaseNotes: String?,
    forceUpdate: Boolean = false,
    downloadProgress: Float? = null,
    isDownloading: Boolean = false,
    downloadReady: Boolean = false,
    isInstalling: Boolean = false,
    onUpdateClick: () -> Unit,
    onLaterClick: () -> Unit,
    onSkipClick: () -> Unit,
    onTidakClick: () -> Unit = onSkipClick
) {
    val isLocked = isDownloading || isInstalling
    val updateFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(500L)
        runCatching { updateFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = {
            if (!forceUpdate && !isLocked && !downloadReady) onLaterClick()
        },
        properties = DialogProperties(
            dismissOnBackPress = !forceUpdate && !isLocked && !downloadReady,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.94f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GLASS_BG)
                    .border(2.dp, GLASS_BORDER, RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // HEADER
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = GOLD_ACCENT,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "UPDATE TERSEDIA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GOLD_ACCENT
                )
                Text(
                    text = "Versi baru Home.io sudah tersedia",
                    fontSize = 12.sp,
                    color = TEXT_SECONDARY,
                    textAlign = TextAlign.Center
                )

                // VERSI INFO
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Versi Saat Ini", fontSize = 10.sp, color = TEXT_SECONDARY)
                        Text(currentVersion, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TEXT_PRIMARY)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Versi Terbaru", fontSize = 10.sp, color = TEXT_SECONDARY)
                        Text(latestVersion, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GOLD_ACCENT)
                    }
                }

                // CHANGELOG
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .border(1.dp, GOLD_ACCENT.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "📋 Yang Baru di Versi $latestVersion:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GOLD_ACCENT
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val notesText = if (releaseNotes.isNullOrBlank()) "(Tidak ada catatan rilis)" else releaseNotes
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).verticalScroll(rememberScrollState())) {
                        Text(notesText, fontSize = 12.sp, color = TEXT_PRIMARY, lineHeight = 18.sp)
                    }
                }

                // PROGRESS BAR
                if ((isDownloading || downloadReady) && downloadProgress != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val label = when {
                            downloadReady -> "✅ Download selesai — siap dipasang"
                            else -> "⬇️ Mengunduh update... ${(downloadProgress * 100).toInt()}%"
                        }
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (downloadReady) GREEN_SUCCESS else GOLD_ACCENT,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(downloadProgress.coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (downloadReady) GREEN_SUCCESS else GOLD_ACCENT)
                            )
                        }
                    }
                }

                // STATUS INSTALLING
                if (isInstalling) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GREEN_SUCCESS.copy(alpha = 0.15f))
                            .border(1.dp, GREEN_SUCCESS.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📦 Membuka installer Android...",
                            fontSize = 12.sp,
                            color = GREEN_SUCCESS,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // =====================================================
                // TOMBOL — STATE MACHINE
                // =====================================================
                when {
                    // ---- STATE: INSTALLING ----
                    isInstalling -> {
                        DialogButton(
                            icon = Icons.Default.CheckCircle,
                            text = "MENUNGGAH PROSES INSTALL...",
                            backgroundColor = GREEN_SUCCESS.copy(alpha = 0.4f),
                            textColor = Color.White,
                            enabled = false,
                            onClick = {}
                        )
                    }

                    // ---- STATE: READY TO INSTALL (Skenario A) ----
                    downloadReady -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!forceUpdate) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DialogButton(
                                        icon = Icons.Default.Close,
                                        text = "TUTUP",
                                        backgroundColor = Color(0xFF142735),
                                        textColor = TEXT_PRIMARY,
                                        enabled = true,
                                        onClick = onLaterClick
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(if (forceUpdate) 1f else 1.5f)) {
                                DialogButton(
                                    icon = Icons.Default.InstallMobile,
                                    text = "INSTALL SEKARANG",
                                    backgroundColor = GREEN_SUCCESS,
                                    textColor = Color.White,
                                    enabled = true,
                                    onClick = onUpdateClick,
                                    focusRequester = updateFocusRequester
                                )
                            }
                        }
                        Text(
                            text = "File APK tersimpan di /HomeiO/Pembaharuan/ — bisa dipasang ulang kapan saja dari Pengaturan → Pembaruan Aplikasi.",
                            fontSize = 10.sp,
                            color = TEXT_SECONDARY,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }

                    // ---- STATE: DOWNLOADING ----
                    isDownloading -> {
                        DialogButton(
                            icon = Icons.Default.Download,
                            text = "MENGUNDUH... ${((downloadProgress ?: 0f) * 100).toInt()}%",
                            backgroundColor = GOLD_ACCENT.copy(alpha = 0.3f),
                            textColor = TEXT_PRIMARY,
                            enabled = false,
                            onClick = {}
                        )
                        Text(
                            text = "Download berjalan di background. Aman meskipun layar dimatikan atau tekan Back.",
                            fontSize = 10.sp,
                            color = TEXT_SECONDARY,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }

                    // ---- STATE: FORCE UPDATE (belum download) ----
                    forceUpdate -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DialogButton(
                                    icon = Icons.Default.Cancel,
                                    text = "TIDAK",
                                    backgroundColor = Color(0xFF142735),
                                    textColor = RED_WARN,
                                    enabled = true,
                                    onClick = onTidakClick
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DialogButton(
                                    icon = Icons.Default.Download,
                                    text = "UPDATE",
                                    backgroundColor = GOLD_ACCENT,
                                    textColor = Color(0xFF09141D),
                                    enabled = true,
                                    onClick = onUpdateClick,
                                    focusRequester = updateFocusRequester
                                )
                            }
                        }
                        Text(
                            text = "⚠️ Update ini WAJIB. Aplikasi tidak dapat digunakan sebelum update selesai.",
                            fontSize = 10.sp,
                            color = RED_WARN,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ---- STATE: IDLE (normal) ----
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DialogButton(
                                    icon = Icons.Default.SkipNext,
                                    text = "SKIP",
                                    backgroundColor = Color(0xFF142735),
                                    textColor = RED_WARN,
                                    enabled = true,
                                    onClick = onSkipClick
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DialogButton(
                                    icon = Icons.Default.Schedule,
                                    text = "NANTI",
                                    backgroundColor = Color(0xFF142735),
                                    textColor = TEXT_PRIMARY,
                                    enabled = true,
                                    onClick = onLaterClick
                                )
                            }
                            Box(modifier = Modifier.weight(1.2f)) {
                                DialogButton(
                                    icon = Icons.Default.Download,
                                    text = "UPDATE",
                                    backgroundColor = GOLD_ACCENT,
                                    textColor = Color(0xFF09141D),
                                    enabled = true,
                                    onClick = onUpdateClick,
                                    focusRequester = updateFocusRequester
                                )
                            }
                        }
                        Text(
                            text = "SKIP: tidak muncul lagi untuk versi ini · NANTI: muncul lagi besok",
                            fontSize = 10.sp,
                            color = TEXT_SECONDARY,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    textColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val bg = if (!enabled) backgroundColor.copy(alpha = 0.3f) else backgroundColor
    val rememberRequester = remember { FocusRequester() }
    val requester = focusRequester ?: rememberRequester

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .focusRequester(requester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled)
            .clickable(enabled) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, text, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}
