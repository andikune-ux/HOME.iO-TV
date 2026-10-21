package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.TvToggle
import dev.andikune.homeio.util.SystemUtils

@Composable
fun PerformanceSection(
    settings: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
    val ctx = LocalContext.current
    // Ambil info RAM untuk ditampilkan
    val memInfo by remember { mutableStateOf(SystemUtils.getMemoryInfo(ctx)) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("PERFORMA STB", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Optimasi tampilan & manajemen RAM", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        TvToggle(
            label = "Efek Kaca Blur (High Performance)",
            description = "Matikan (OFF) jika STB RAM 1GB terasa lambat. Akan memakai efek solid transparan.",
            isChecked = settings.isHighPerformanceBlurEnabled,
            onToggle = { onUpdate(settings.copy(isHighPerformanceBlurEnabled = it)) }
        )

        TvToggle(
            label = "Auto-Boost RAM saat Kembali ke Home",
            description = "Membersihkan cache & proses background secara otomatis",
            isChecked = settings.isAutoRamBoostEnabled,
            onToggle = { onUpdate(settings.copy(isAutoRamBoostEnabled = it)) }
        )

        // Info & Tombol Boost Manual
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(GlassColors.GlassTint).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("RAM Tersedia", color = GlassColors.TextSecondary, fontSize = 11.sp)
                Text("${memInfo.freeMemMb} MB / ${memInfo.totalMemMb} MB", color = GlassColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            
            BoostButton(onClick = { SystemUtils.boostRam(ctx) })
        }
    }
}

@Composable
private fun BoostButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 10.dp) {
        Row(
            Modifier.clip(RoundedCornerShape(10.dp))
                .background(if (isFocused) GlassColors.AccentGlow else GlassColors.AccentGlow.copy(alpha = 0.8f))
                .onFocusChanged { isFocused = it.isFocused }
                .focusable().clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CleaningServices, null, tint = GlassColors.DeepBackground, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("BOOST NOW", color = GlassColors.DeepBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
