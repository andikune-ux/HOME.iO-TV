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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.TvToggle
import dev.andikune.homeio.util.CacheCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PerformanceSection(settings: SettingsConfig, onUpdate: (SettingsConfig) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var cacheSize by remember { mutableStateOf(0L) }
    var freeRam by remember { mutableStateOf(0L) }
    var totalRam by remember { mutableStateOf(0L) }
    var freeStorage by remember { mutableStateOf(0L) }
    var isBoostRunning by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<CacheCleaner.BoostResult?>(null) }
    var showAppCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cacheSize = CacheCleaner.getOwnCacheSize(ctx)
            freeRam = CacheCleaner.getFreeRamMb(ctx)
            totalRam = CacheCleaner.getTotalRamMb(ctx)
            freeStorage = CacheCleaner.getFreeStorageMb()
        }
    }

    suspend fun refreshInfo() {
        withContext(Dispatchers.IO) {
            cacheSize = CacheCleaner.getOwnCacheSize(ctx)
            freeRam = CacheCleaner.getFreeRamMb(ctx)
            freeStorage = CacheCleaner.getFreeStorageMb()
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("PERFORMA STB", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Optimasi memori & bersihkan cache", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GlassColors.GlassTint).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(12.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("RAM TERSEDIA", color = GlassColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$freeRam MB", color = GlassColors.AccentGlow, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("/ $totalRam MB Total", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
            
            val usedRatio = if (totalRam > 0) (totalRam - freeRam).toFloat() / totalRam.toFloat() else 0f
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x33FFFFFF))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(usedRatio.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(GlassColors.AccentGlow))
            }
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cache App Ini: ${CacheCleaner.formatBytes(cacheSize)}", color = GlassColors.AccentGreen, fontSize = 11.sp)
                Text("Storage Free: $freeStorage MB", color = GlassColors.TextSecondary, fontSize = 11.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard(icon = Icons.Default.CleaningServices, title = if (isBoostRunning) "MEMPROSES..." else "BOOST & CLEAR", desc = "Cache app ini + RAM", backgroundColor = GlassColors.AccentGreen, modifier = Modifier.weight(1f), enabled = !isBoostRunning, onClick = {
                if (isBoostRunning) return@ActionCard
                isBoostRunning = true; lastResult = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { CacheCleaner.boostAll(ctx) }
                    lastResult = result; isBoostRunning = false; refreshInfo()
                    Toast.makeText(ctx.applicationContext, result.message, Toast.LENGTH_LONG).show()
                }
            })
            ActionCard(icon = Icons.Default.Apps, title = "KELOLA CACHE LAIN", desc = "List app dengan cache", backgroundColor = GlassColors.AccentAmber, modifier = Modifier.weight(1f), enabled = true, onClick = { showAppCacheDialog = true })
        }

        ActionCard(icon = Icons.Default.Folder, title = "BUKA STORAGE TV", desc = "Clear cached data manual", backgroundColor = GlassColors.AccentGlow, modifier = Modifier.fillMaxWidth(), enabled = true, onClick = {
            CacheCleaner.openStorageSettings(ctx)
            Toast.makeText(ctx.applicationContext, "Tap 'Cached data' → OK untuk clear", Toast.LENGTH_LONG).show()
        })

        lastResult?.let { r ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(GlassColors.AccentGreen.copy(alpha = 0.12f)).border(1.dp, GlassColors.AccentGreen, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("✅ BOOST BERHASIL", color = GlassColors.AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Cache dibersihkan: ${CacheCleaner.formatBytes(r.cacheFreedBytes)}", color = GlassColors.TextPrimary, fontSize = 11.sp)
                Text("RAM dibebaskan: ${r.ramFreedMb} MB (${r.ramBefore} → ${r.ramAfter})", color = GlassColors.TextPrimary, fontSize = 11.sp)
            }
        }

        TvToggle(label = "Efek Kaca Blur (High Performance)", description = "Matikan (OFF) jika STB RAM 1GB terasa lambat", isChecked = settings.isHighPerformanceBlurEnabled, onToggle = { onUpdate(settings.copy(isHighPerformanceBlurEnabled = it)) })
        TvToggle(label = "Auto-Boost RAM saat Kembali ke Home", description = "Otomatis bersihkan RAM setiap kembali ke Home", isChecked = settings.isAutoRamBoostEnabled, onToggle = { onUpdate(settings.copy(isAutoRamBoostEnabled = it)) })
    }

    if (showAppCacheDialog) { AppCacheDialog(onDismiss = { showAppCacheDialog = false }) }
}

@Composable
private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, backgroundColor: Color, modifier: Modifier = Modifier, enabled: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val effectiveColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f)

    NeonFocusBorder(focused = isFocused, cornerRadius = 12.dp, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(effectiveColor.copy(alpha = 0.15f))
                .border(if (isFocused && enabled) 2.dp else 1.dp, effectiveColor, RoundedCornerShape(12.dp))
                .onFocusChanged { isFocused = it.isFocused }.focusable(enabled)
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { if(enabled) onClick(); true } else false }
                .clickable(enabled = enabled) { onClick() }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = effectiveColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, color = effectiveColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(desc, color = GlassColors.TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
        }
    }
}
