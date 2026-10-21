package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.TvSlider
import dev.andikune.homeio.presentation.components.TvToggle

@Composable
fun ScreensaverSection(
    settings: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Monitor, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("SCREENSAVER OLED", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Layar hitam pekat saat idle (anti burn-in)", color = GlassColors.TextSecondary, fontSize = 12.sp)
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
                valueRange = 1f..60f,
                steps = 59,
                unit = " Menit",
                formatter = { "${it.toInt()} Menit" }
            )
            Text(
                "Screensaver akan muncul jika tidak ada input remote atau sentuhan selama waktu di atas.",
                color = GlassColors.TextSecondary, fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}
