package dev.andikune.homeio.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.TvToggle

@Composable
fun WallpaperSection(
    settings: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
    val ctx = LocalContext.current

    // Launcher untuk pilih gambar dari galeri
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onUpdate(settings.copy(wallpaperPath = it.toString(), wallpaperType = "IMAGE")) }
    }

    // Launcher untuk pilih video dari galeri
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onUpdate(settings.copy(wallpaperPath = it.toString(), wallpaperType = "VIDEO")) }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Wallpaper, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("WALLPAPER & SKALA", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Atur latar belakang foto atau video", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        // Tipe Wallpaper
        TvToggle(
            label = "Gunakan Video Sebagai Wallpaper",
            description = "Jika aktif, wallpaper akan diputar sebagai video (ExoPlayer)",
            isChecked = settings.wallpaperType == "VIDEO",
            onToggle = { isVideo ->
                onUpdate(settings.copy(wallpaperType = if (isVideo) "VIDEO" else "IMAGE"))
            }
        )

        // Tombol Pilih File
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(
                icon = Icons.Default.PhotoLibrary,
                label = "PILIH GAMBAR",
                modifier = Modifier.weight(1f),
                onClick = { imagePicker.launch("image/*") }
            )
            ActionButton(
                icon = Icons.Default.Folder,
                label = "PILIH VIDEO",
                modifier = Modifier.weight(1f),
                onClick = { videoPicker.launch("video/*") }
            )
        }

        if (!settings.wallpaperPath.isNullOrBlank()) {
            Text(
                "File aktif: ${settings.wallpaperPath.substringAfterLast("/")}",
                color = GlassColors.AccentGreen, fontSize = 11.sp, maxLines = 1
            )
        }

        // Mode Skala
        Text("MODE SKALA", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("FIT", "FILL", "ZOOM", "CROP").forEach { mode ->
                val isSelected = settings.wallpaperScaleMode == mode
                SelectionChip(
                    label = mode,
                    isSelected = isSelected,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdate(settings.copy(wallpaperScaleMode = mode)) }
                )
            }
        }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 12.dp, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (isFocused) GlassColors.AccentGlow.copy(alpha = 0.2f) else GlassColors.GlassTint)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable().clickable { onClick() }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SelectionChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = if (isSelected) GlassColors.AccentGlow else GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
