package dev.andikune.homeio.presentation.wallpaper

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.util.PermissionHelper
import dev.andikune.homeio.util.PermissionHelper.PermissionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileManagerDialog(
    onFileSelected: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isVideoMode by remember { mutableStateOf(true) }
    
    var hasStoragePermission by remember { mutableStateOf(PermissionHelper.checkPermission(context, PermissionType.STORAGE)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission = PermissionHelper.checkPermission(context, PermissionType.STORAGE)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = true) {
        if (currentDir.absolutePath != Environment.getExternalStorageDirectory().absolutePath) {
            currentDir = currentDir.parentFile ?: currentDir
        } else {
            onDismiss()
        }
    }

    fun loadFiles(dir: File) {
        isLoading = true
        files = try {
            dir.listFiles()?.filter { file ->
                if (file.isDirectory) {
                    !file.name.startsWith(".") && file.name != "Android"
                } else {
                    val ext = file.extension.lowercase()
                    if (isVideoMode) ext in listOf("mp4", "mkv", "webm", "3gp", "avi")
                    else ext in listOf("jpg", "jpeg", "png", "webp")
                }
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        } catch (e: SecurityException) {
            emptyList() 
        } catch (e: Exception) {
            emptyList()
        }
        isLoading = false
    }

    LaunchedEffect(currentDir, isVideoMode, hasStoragePermission) {
        if (hasStoragePermission) {
            withContext(Dispatchers.IO) { loadFiles(currentDir) }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(GlassColors.DeepBackground.copy(alpha = 0.98f)).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(GlassColors.DeepBackground).border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(20.dp)).padding(16.dp).focusGroup()) {
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("PILIH FILE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
                            Text(currentDir.absolutePath, fontSize = 11.sp, color = GlassColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    
                    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(GlassColors.GlassTint).padding(4.dp)) {
                        Text("VIDEO", color = if (isVideoMode) GlassColors.DeepBackground else GlassColors.TextPrimary, 
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isVideoMode) GlassColors.AccentGlow else Color.Transparent).clickable { isVideoMode = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("FOTO", color = if (!isVideoMode) GlassColors.DeepBackground else GlassColors.TextPrimary, 
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (!isVideoMode) GlassColors.AccentGlow else Color.Transparent).clickable { isVideoMode = false }.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (!hasStoragePermission) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Lock, null, tint = GlassColors.AccentAmber, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Izin Storage Diperlukan", color = GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Aplikasi butuh akses file untuk menampilkan video/foto.", color = GlassColors.TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(GlassColors.AccentGlow).clickable {
                            PermissionHelper.openPermissionSettings(context, PermissionType.STORAGE)
                        }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            Text("BERI IZIN", color = GlassColors.DeepBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    if (currentDir.absolutePath != Environment.getExternalStorageDirectory().absolutePath) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GlassColors.GlassTint).clickable { currentDir = currentDir.parentFile ?: currentDir }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GlassColors.TextPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Kembali ke folder sebelumnya", color = GlassColors.TextPrimary, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Memindai folder...", color = GlassColors.TextSecondary)
                        }
                    } else if (files.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (isVideoMode) "Tidak ada video di folder ini." else "Tidak ada foto di folder ini.", color = GlassColors.TextSecondary)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(files) { file ->
                                FileItemRow(file = file, isVideoMode = isVideoMode, onFolderClick = { currentDir = file }, onFileSelected = { onFileSelected(file) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(file: File, isVideoMode: Boolean, onFolderClick: () -> Unit, onFileSelected: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val isDir = file.isDirectory

    NeonFocusBorder(focused = isFocused, cornerRadius = 10.dp) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(if (isFocused) GlassColors.GlassTint.copy(alpha = 0.6f) else GlassColors.GlassTint)
                .border(if (isFocused) 2.dp else 1.dp, if (isFocused) GlassColors.AccentGlow else GlassColors.GlassBorder, RoundedCornerShape(10.dp))
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .clickable { if (isDir) onFolderClick() else onFileSelected() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDir) {
                Icon(Icons.Default.Folder, null, tint = GlassColors.AccentAmber, modifier = Modifier.size(32.dp))
            } else {
                Box(Modifier.size(64.dp, 36.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    if (isVideoMode) {
                        VideoThumbnail(file = file)
                    } else {
                        AsyncImage(model = file, contentDescription = "Foto", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, color = GlassColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!isDir) {
                    val sizeMb = file.length() / (1024 * 1024)
                    Text("$sizeMb MB", color = GlassColors.TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(file: File) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, file) {
        value = withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (e: Exception) { null } 
            finally { retriever?.release() }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Thumbnail Video",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
    } else {
        Icon(Icons.Default.PlayArrow, null, tint = GlassColors.TextSecondary, modifier = Modifier.size(24.dp))
    }
}
