package dev.andikune.homeio.presentation.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.*
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
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Transformer
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
fun WallpaperSection(
    settings: SettingsConfig, 
    onUpdate: (SettingsConfig) -> Unit,
    onSave: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var showFileManager by remember { mutableStateOf(false) }
    var pickerModeIsVideo by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) }

    // FUNGSI AUTO-DELETE FILE LAMA
    fun deleteOldFile(oldPath: String?) {
        if (!oldPath.isNullOrBlank()) {
            try {
                val oldFile = File(oldPath)
                if (oldFile.exists() && oldFile.absolutePath.contains(ctx.packageName)) {
                    if (oldFile.delete()) Log.d("Wallpaper", "File lama dihapus: $oldPath")
                }
            } catch (e: Exception) { Log.e("Wallpaper", "Gagal hapus file lama", e) }
        }
    }

    // FUNGSI COPY + AUTO-CONVERT VIDEO
    suspend fun processNewMedia(uri: Uri, isVideo: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = if (isVideo) "wallpaper_${System.currentTimeMillis()}.mp4" else "wallpaper_${System.currentTimeMillis()}.jpg"
            val destFile = File(ctx.filesDir, fileName)

            if (isVideo) {
                // AUTO-CONVERT PAKAI MEDIA3 TRANSFORMER (Turunin resolusi ke 720p H.264)
                val transformer = Transformer.Builder(ctx)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult) {
                            Log.d("Wallpaper", "Video Convert Berhasil")
                        }
                        override fun onError(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult, exportException: androidx.media3.transformer.ExportException) {
                            Log.e("Wallpaper", "Video Convert Gagal: ${exportException.message}")
                        }
                    })
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().build())
                    .build()
                
                // Mulai transcoding (ini akan jalan di background)
                transformer.start(mediaItem, destFile.absolutePath)
                // Tunggu sebentar agar proses transcode berjalan (bisa disesuaikan)
                Thread.sleep(2000) 
            } else {
                // FOTO: Cukup copy langsung
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e("Wallpaper", "Gagal proses media: ${e.message}")
            null
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            scope.launch {
                isConverting = true
                val localPath = processNewMedia(it, pickerModeIsVideo)
                isConverting = false
                if (localPath != null) {
                    // Hapus file lama
                    deleteOldFile(settings.customVideoPath)
                    deleteOldFile(settings.customPhotoPath)
                    
                    if (pickerModeIsVideo) onUpdate(settings.copy(wallpaperMode = "KUSTOM", customVideoPath = localPath, customActiveType = "VIDEO"))
                    else onUpdate(settings.copy(wallpaperMode = "KUSTOM", customPhotoPath = localPath, customActiveType = "PHOTO"))
                } else {
                    Toast.makeText(ctx, "Gagal memproses media", Toast.LENGTH_SHORT).show()
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

            if (!settings.customPhotoPath.isNullOrBlank() || !settings.customVideoPath.isNullOrBlank()) {
                Text("File Aktif:", color = GlassColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                if (!settings.customPhotoPath.isNullOrBlank()) Text("📸 ${settings.customPhotoPath.substringAfterLast("/")}", color = GlassColors.AccentGreen, fontSize = 11.sp, maxLines = 1)
                if (!settings.customVideoPath.isNullOrBlank()) Text("🎬 ${settings.customVideoPath.substringAfterLast("/")}", color = GlassColors.AccentGlow, fontSize = 11.sp, maxLines = 1)
                
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

        // SWITCH FORCE SOFTWARE DECODER
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Paksa Software Decoder", color = GlassColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Aktifkan jika video tidak muncul (bisa bikin lag)", color = GlassColors.TextSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = settings.forceSoftwareDecoder,
                onCheckedChange = { onUpdate(settings.copy(forceSoftwareDecoder = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = GlassColors.AccentGlow)
            )
        }

        // TOMBOL SIMPAN (FORCE SAVE)
        Spacer(Modifier.height(16.dp))
        var isSaveFocused by remember { mutableStateOf(false) }
        NeonFocusBorder(focused = isSaveFocused, cornerRadius = 12.dp) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (isSaveFocused) GlassColors.AccentGreen else GlassColors.AccentGreen.copy(alpha = 0.6f))
                    .onFocusChanged { isSaveFocused = it.isFocused }.focusable()
                    .onPreviewKeyEvent { e -> 
                        if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { 
                            onSave() // Panggil simpan langsung
                            Toast.makeText(ctx, "Wallpaper berhasil disimpan permanen!", Toast.LENGTH_SHORT).show()
                            true 
                        } else false 
                    }
                    .clickable { 
                        onSave()
                        Toast.makeText(ctx, "Wallpaper berhasil disimpan permanen!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isConverting) "MEMPROSES VIDEO..." else "SIMPAN WALLPAPER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    // Hapus file lama
                    deleteOldFile(settings.customVideoPath)
                    deleteOldFile(settings.customPhotoPath)

                    if (isVideo) onUpdate(settings.copy(wallpaperMode = "KUSTOM", customVideoPath = file.absolutePath, customActiveType = "VIDEO"))
                    else onUpdate(settings.copy(wallpaperMode = "KUSTOM", customPhotoPath = file.absolutePath, customActiveType = "PHOTO"))
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
