package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.TvSlider
import dev.andikune.homeio.presentation.components.TvToggle

@Composable
fun ScreensaverSection(settings: SettingsConfig, onUpdate: (SettingsConfig) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Monitor, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("SCREENSAVER OLED", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Hemat listrik & anti burn-in", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        // Visual Preview Box
        Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌙", fontSize = 24.sp)
                Text("Mode Layar Hitam Total", color = GlassColors.TextSecondary, fontSize = 11.sp)
            }
        }

        TvToggle(
            label = "Aktifkan Screensaver",
            description = "Matikan opsi ini jika TV tidak ingin pernah redup otomatis",
            isChecked = settings.screensaverEnabled,
            onToggle = { onUpdate(settings.copy(screensaverEnabled = it)) }
        )

        if (settings.screensaverEnabled) {
            TvSlider(
                label = "Waktu Aktif (Idle Timeout)",
                value = settings.screensaverTimeoutMinutes.toFloat(),
                onValueChange = { onUpdate(settings.copy(screensaverTimeoutMinutes = it.toInt())) },
                valueRange = 1f..60f, steps = 59, unit = " Menit",
                formatter = { "${it.toInt()} Menit" }
            )
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GlassColors.AccentGlow.copy(alpha = 0.1f)).padding(10.dp)) {
                Text("💡 Layar akan menjadi hitam total jika tidak ada input remote selama waktu di atas. Sentuh remote untuk bangun.", color = GlassColors.TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}
