package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.TvSlider
import dev.andikune.homeio.presentation.components.TvToggle

@Composable
fun TvDisplaySection(
    settings: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tv, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("TAMPILAN TV & SISTEM", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Safe Area, Kecerahan, Kiosk Mode, Ukuran Ikon", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        // === SAFE AREA ===
        Text("SAFE AREA (ANTI OVERSCAN)", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Padding di tepi layar jika UI terpotong bezel TV", color = GlassColors.TextSecondary, fontSize = 11.sp)
        TvSlider(
            label = "Safe Area Padding",
            value = settings.safeAreaPercent,
            onValueChange = { onUpdate(settings.copy(safeAreaPercent = it)) },
            valueRange = 0f..10f,
            steps = 19,
            formatter = { String.format("%.1f%%", it) }
        )

        // === UKURAN IKON APLIKASI ===
        Text("UKURAN IKON APLIKASI", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Perkecil atau perbesar ikon di Home Screen", color = GlassColors.TextSecondary, fontSize = 11.sp)
        TvSlider(
            label = "Skala Ikon",
            value = settings.appIconSizeMultiplier,
            onValueChange = { onUpdate(settings.copy(appIconSizeMultiplier = it)) },
            valueRange = 0.6f..1.5f,
            steps = 9,
            formatter = { String.format("%.1fx", it) }
        )

        // === KECERAHAN ===
        Text("KECERAHAN LAYAR", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MANUAL", "AUTO").forEach { mode ->
                val isSelected = settings.brightnessMode == mode
                BrightnessChip(
                    label = mode,
                    isSelected = isSelected,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdate(settings.copy(brightnessMode = mode)) }
                )
            }
        }

        if (settings.brightnessMode == "MANUAL") {
            TvSlider(
                label = "Kecerahan Manual",
                value = settings.manualBrightness.toFloat(),
                onValueChange = { onUpdate(settings.copy(manualBrightness = it.toInt())) },
                valueRange = 10f..100f,
                steps = 18,
                formatter = { "${it.toInt()}%" }
            )
        } else {
            Text("Mode AUTO: Terang di siang hari, redup di malam hari.", color = GlassColors.AccentGlow, fontSize = 11.sp)
        }

        // === KIOSK MODE ===
        Text("KIOSK & SYSTEM", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        TvToggle(
            label = "Kiosk Mode (Lock Task)",
            description = "Kunci aplikasi agar jamaah tidak bisa keluar dari Home.io",
            isChecked = settings.isKioskModeEnabled,
            onToggle = { onUpdate(settings.copy(isKioskModeEnabled = it)) }
        )

        TvToggle(
            label = "Auto-Start saat TV Menyala",
            description = "Home.io otomatis terbuka saat TV/STB dihidupkan",
            isChecked = settings.isAutoStartOnBoot,
            onToggle = { onUpdate(settings.copy(isAutoStartOnBoot = it)) }
        )
    }
}

@Composable
private fun BrightnessChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> GlassColors.AccentGlow.copy(alpha = 0.25f)
                    isFocused -> GlassColors.GlassTint.copy(alpha = 0.5f)
                    else -> GlassColors.GlassTint
                }
            )
            .border(if (isSelected) 1.5.dp else 0.dp, if (isSelected) GlassColors.AccentGlow else Color.Transparent, RoundedCornerShape(10.dp))
            .onFocusChanged { isFocused = it.isFocused }.focusable().clickable { onClick() }.padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isSelected) GlassColors.AccentGlow else GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
