package dev.andikune.homeio.presentation.settings

import android.content.Context
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
import dev.andikune.homeio.presentation.wallpaper.FileManagerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun WallpaperSection(settings: SettingsConfig, onUpdate: (SettingsConfig) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var showFileManager by remember { mutableStateOf(false) }
    var pickerModeIsVideo by remember { mutableStateOf(false) }

    suspend fun copyUriToInternal(uri: Uri, isVideo: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            val ext = if (isVideo) ".mp4" else ".jpg"
            val fileName = "wallpaper_${System.currentTimeMillis()}$ext"
            val file = File(ctx.filesDir, fileName)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            scope.launch {
                val localPath = copyUriToInternal(it, pickerModeIsVideo)
                if (localPath != null) {
                    if (pickerModeIsVideo) onUpdate(settings.copy(customVideoPath = localPath, customActiveType = "VIDEO"))
                    else onUpdate(settings.copy(customPhotoPath = localPath, customActiveType = "PHOTO"))
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Wallpaper, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("WALLPAPER & SKALA", color = GlassColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Atur latar belakang layar utama", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        Text("SUMBER WALLPAPER", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WallpaperModeButton("Sinematik", "🎬", settings.wallpaperMode == "SINEMATIK", Modifier.weight(1f)) { onUpdate(settings.copy(wallpaperMode = "SINEMATIK")) }
            WallpaperModeButton("Estetik", "📸", settings.wallpaperMode == "ESTETIK", Modifier.weight(1f)) { onUpdate(settings.copy(wallpaperMode = "ESTETIK")) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WallpaperModeButton("Mode OLED", "⬛", settings.wallpaperMode == "OLED", Modifier.weight(1f)) { onUpdate(settings.copy(wallpaperMode = "OLED")) }
            WallpaperModeButton("Kustomisasi", "📂", settings.wallpaperMode == "KUSTOM", Modifier.weight(1f)) { onUpdate(settings.copy(wallpaperMode = "KUSTOM")) }
        }

        if (settings.wallpaperMode == "KUSTOM") {
            Spacer(Modifier.height(8.dp))
            Text("PILIH FILE KUSTOM", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(icon = Icons.Default.PhotoLibrary, label = "Foto Galeri", modifier = Modifier.weight(1f)) {
                    pickerModeIsVideo = false; galleryPicker.launch("image/*")
                }
                ActionButton(icon = Icons.Default.VideoLibrary, label = "Video Galeri", modifier = Modifier.weight(1f)) {
                    pickerModeIsVideo = true; galleryPicker.launch("video/*")
                }
            }
            ActionButton(icon = Icons.Default.Folder, label = "File TV / USB", modifier = Modifier.fillMaxWidth()) {
                showFileManager = true 
            }

            // Tampilkan info file aktif
            if (!settings.customPhotoPath.isNullOrBlank() || !settings.customVideoPath.isNullOrBlank()) {
                Text("File Aktif:", color = GlassColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                if (!settings.customPhotoPath.isNullOrBlank()) Text("📸 ${settings.customPhotoPath.substringAfterLast("/")}", color = GlassColors.AccentGreen, fontSize = 11.sp, maxLines = 1)
                if (!settings.customVideoPath.isNullOrBlank()) Text("🎬 ${settings.customVideoPath.substringAfterLast("/")}", color = GlassColors.AccentGlow, fontSize = 11.sp, maxLines = 1)
                
                // Switch Foto / Video
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionChip("Gunakan Foto", settings.customActiveType == "PHOTO", Modifier.weight(1f)) { onUpdate(settings.copy(customActiveType = "PHOTO")) }
                    SelectionChip("Gunakan Video", settings.customActiveType == "VIDEO", Modifier.weight(1f)) { onUpdate(settings.copy(customActiveType = "VIDEO")) }
                }
            }
        }

        if (settings.wallpaperMode != "OLED") {
            Spacer(Modifier.height(8.dp))
            Text("MODE SKALA", color = GlassColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("FIT", "FILL", "ZOOM", "CROP").forEach { mode ->
                    SelectionChip(mode, settings.wallpaperScaleMode == mode, Modifier.weight(1f)) { onUpdate(settings.copy(wallpaperScaleMode = mode)) }
                }
            }
        }
    }

    if (showFileManager) {
        FileManagerDialog(
            onFileSelected = { file: File ->
                showFileManager = false
                val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "webm", "avi")
                scope.launch {
                    if (isVideo) onUpdate(settings.copy(customVideoPath = file.absolutePath, customActiveType = "VIDEO"))
                    else onUpdate(settings.copy(customPhotoPath = file.absolutePath, customActiveType = "PHOTO"))
                }
            },
            onDismiss = { showFileManager = false }
        )
    }
}

@Composable
private fun WallpaperModeButton(label: String, emoji: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 12.dp, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) GlassColors.AccentGlow.copy(alpha = 0.2f) else if (isFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                .border(if (isSelected) 2.dp else 0.dp, if (isSelected) GlassColors.AccentGlow else Color.Transparent, RoundedCornerShape(12.dp))
                .onFocusChanged { isFocused = it.isFocused }.focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() }.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = if (isSelected) GlassColors.AccentGlow else GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                .border(if (isFocused) 2.dp else 1.dp, if (isFocused) GlassColors.AccentGlow else GlassColors.GlassBorder, RoundedCornerShape(12.dp))
                .onFocusChanged { isFocused = it.isFocused }.focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() }.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SelectionChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 10.dp, modifier = modifier) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) GlassColors.AccentGlow.copy(alpha = 0.25f) else GlassColors.GlassTint)
                .border(if (isSelected) 1.5.dp else 0.dp, if (isSelected) GlassColors.AccentGlow else Color.Transparent, RoundedCornerShape(10.dp))
                .onFocusChanged { isFocused = it.isFocused }.focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() }.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = if (isSelected) GlassColors.AccentGlow else GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
