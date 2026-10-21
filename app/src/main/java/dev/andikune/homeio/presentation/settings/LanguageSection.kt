package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
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
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.TvToggle

@Composable
fun LanguageSection(
    settings: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("BAHASA & UMUM", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Preferensi bahasa, waktu, dan sinkronisasi", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        // Pilih Bahasa
        Text("BAHASA APLIKASI", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LangOption(
                label = "🇮🇩 Indonesia",
                isSelected = settings.languageCode == "in",
                modifier = Modifier.weight(1f),
                onClick = { onUpdate(settings.copy(languageCode = "in")) }
            )
            LangOption(
                label = "🇬🇧 English",
                isSelected = settings.languageCode == "en",
                modifier = Modifier.weight(1f),
                onClick = { onUpdate(settings.copy(languageCode = "en")) }
            )
        }

        // Format Jam
        Text("FORMAT JAM", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LangOption(
                label = "24 Jam (15:30)",
                isSelected = settings.timeFormat == "24H",
                modifier = Modifier.weight(1f),
                onClick = { onUpdate(settings.copy(timeFormat = "24H")) }
            )
            LangOption(
                label = "12 Jam (03:30 PM)",
                isSelected = settings.timeFormat == "12H",
                modifier = Modifier.weight(1f),
                onClick = { onUpdate(settings.copy(timeFormat = "12H")) }
            )
        }

        // Tampilkan Detik
        TvToggle(
            label = "Tampilkan Detik",
            description = "Tampilkan detik pada jam di Home Screen",
            isChecked = settings.showSeconds,
            onToggle = { onUpdate(settings.copy(showSeconds = it)) }
        )

        // Sinkronisasi NTP
        TvToggle(
            label = "NTP Time Check",
            description = "Sinkronisasi jam TV dengan internet secara otomatis",
            isChecked = settings.isNtpCheckEnabled,
            onToggle = { onUpdate(settings.copy(isNtpCheckEnabled = it)) }
        )
    }
}

@Composable
private fun LangOption(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 10.dp, modifier = modifier) {
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
                .onFocusChanged { isFocused = it.isFocused }
                .focusable().clickable { onClick() }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = if (isSelected) GlassColors.AccentGlow else GlassColors.TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
        }
    }
}
