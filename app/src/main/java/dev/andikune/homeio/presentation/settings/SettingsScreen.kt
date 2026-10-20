package dev.andikune.homeio.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.R
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.presentation.screensaver.ScreensaverActivity
import dev.andikune.homeio.presentation.wallpaper.FileManagerDialog
import dev.andikune.homeio.service.DownloadService
import dev.andikune.homeio.util.SoundFeedback
import dev.andikune.homeio.util.SystemUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SettingsCategory {
    WALLPAPER,
    SCREENSAVER,
    PERFORMANCE,
    GENERAL,
    ABOUT
}

@Composable
fun SettingsScreen(
    currentConfig: SettingsConfig,
    onSaveConfig: (SettingsConfig) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(SettingsCategory.WALLPAPER) }
    var config by remember(currentConfig) { mutableStateOf(currentConfig) }
    var showFileManager by remember { mutableStateOf(false) }

    // System gallery picker for Wallpaper
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = uri.path ?: uri.toString()
            val newConfig = config.copy(
                wallpaperPath = path,
                wallpaperType = if (path.endsWith(".mp4", ignoreCase = true)) "VIDEO" else "IMAGE"
            )
            config = newConfig
            onSaveConfig(newConfig)
            Toast.makeText(context, "Wallpaper updated", Toast.LENGTH_SHORT).show()
        }
    }

    if (showFileManager) {
        FileManagerDialog(
            onFileSelected = { file ->
                val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "webm")
                val newConfig = config.copy(
                    wallpaperPath = file.absolutePath,
                    wallpaperType = if (isVideo) "VIDEO" else "IMAGE"
                )
                config = newConfig
                onSaveConfig(newConfig)
                showFileManager = false
                Toast.makeText(context, "Wallpaper: ${file.name}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showFileManager = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0A0E1A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlass(cornerRadius = 28.dp)
                .padding(20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "2-Pane Glass Layout",
                        color = GlassColors.AccentGlow,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = {
                    SoundFeedback.playClick(context)
                    onClose()
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-PANE SPLIT VIEW
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // LEFT PANE: CATEGORIES
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .liquidGlass(cornerRadius = 20.dp)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryItem(
                        icon = Icons.Default.Wallpaper,
                        title = stringResource(R.string.cat_wallpaper),
                        isSelected = selectedCategory == SettingsCategory.WALLPAPER,
                        onClick = {
                            SoundFeedback.playClick(context)
                            selectedCategory = SettingsCategory.WALLPAPER
                        }
                    )
                    CategoryItem(
                        icon = Icons.Default.Brightness4,
                        title = stringResource(R.string.cat_screensaver),
                        isSelected = selectedCategory == SettingsCategory.SCREENSAVER,
                        onClick = {
                            SoundFeedback.playClick(context)
                            selectedCategory = SettingsCategory.SCREENSAVER
                        }
                    )
                    CategoryItem(
                        icon = Icons.Default.Speed,
                        title = stringResource(R.string.cat_performance),
                        isSelected = selectedCategory == SettingsCategory.PERFORMANCE,
                        onClick = {
                            SoundFeedback.playClick(context)
                            selectedCategory = SettingsCategory.PERFORMANCE
                        }
                    )
                    CategoryItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.cat_general),
                        isSelected = selectedCategory == SettingsCategory.GENERAL,
                        onClick = {
                            SoundFeedback.playClick(context)
                            selectedCategory = SettingsCategory.GENERAL
                        }
                    )
                    CategoryItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.cat_about),
                        isSelected = selectedCategory == SettingsCategory.ABOUT,
                        onClick = {
                            SoundFeedback.playClick(context)
                            selectedCategory = SettingsCategory.ABOUT
                        }
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // RIGHT PANE: DETAIL CONTROLS
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .liquidGlass(cornerRadius = 20.dp)
                        .padding(20.dp)
                ) {
                    when (selectedCategory) {
                        SettingsCategory.WALLPAPER -> WallpaperSettingsPane(
                            config = config,
                            onUpdate = { updated ->
                                config = updated
                                onSaveConfig(updated)
                            },
                            onPickGallery = { galleryPicker.launch("*/*") },
                            onPickInternal = { showFileManager = true }
                        )

                        SettingsCategory.SCREENSAVER -> ScreensaverSettingsPane(
                            config = config,
                            onUpdate = { updated ->
                                config = updated
                                onSaveConfig(updated)
                            },
                            onTestScreensaver = {
                                context.startActivity(Intent(context, ScreensaverActivity::class.java))
                            }
                        )

                        SettingsCategory.PERFORMANCE -> PerformanceSettingsPane(
                            config = config,
                            onUpdate = { updated ->
                                config = updated
                                onSaveConfig(updated)
                            }
                        )

                        SettingsCategory.GENERAL -> GeneralSettingsPane(
                            config = config,
                            onUpdate = { updated ->
                                config = updated
                                onSaveConfig(updated)
                            }
                        )

                        SettingsCategory.ABOUT -> AboutSettingsPane(
                            config = config,
                            onRestoreConfig = { restored ->
                                config = restored
                                onSaveConfig(restored)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 14.dp, isFocused = isSelected)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) GlassColors.AccentGlow else GlassColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = if (isSelected) Color.White else GlassColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun WallpaperSettingsPane(
    config: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit,
    onPickGallery: () -> Unit,
    onPickInternal: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Wallpaper Engine", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Text(
            text = "Current: ${config.wallpaperPath ?: "Default Ambient Glass Backdrop"}",
            color = GlassColors.TextSecondary,
            fontSize = 13.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPickInternal,
                colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.wallpaper_pick_internal), color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onPickGallery,
                colors = ButtonDefaults.buttonColors(containerColor = GlassColors.GlassTint)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.wallpaper_pick_gallery), color = Color.White)
            }

            if (config.wallpaperPath != null) {
                Button(
                    onClick = { onUpdate(config.copy(wallpaperPath = null)) },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.GlassTint)
                ) {
                    Text(text = "Reset Default", color = GlassColors.AccentRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.wallpaper_scale_mode), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("FIT", "FILL", "ZOOM", "CROP").forEach { mode ->
                val isSelected = config.wallpaperScaleMode.equals(mode, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .liquidGlass(cornerRadius = 12.dp, isFocused = isSelected)
                        .clickable { onUpdate(config.copy(wallpaperScaleMode = mode)) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = mode,
                        color = if (isSelected) GlassColors.AccentGlow else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ScreensaverSettingsPane(
    config: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit,
    onTestScreensaver: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "OLED Pure Black Screensaver", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Keeps OLED panels safe with 0% screen brightness and deep black background. Wake up instantly by touching screen or pressing any remote button.",
            color = GlassColors.TextSecondary,
            fontSize = 13.sp
        )

        Text(
            text = "${stringResource(R.string.screensaver_timer)}: ${config.screensaverTimeoutMinutes} Min",
            color = GlassColors.AccentGlow,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Slider(
            value = config.screensaverTimeoutMinutes.toFloat(),
            onValueChange = { onUpdate(config.copy(screensaverTimeoutMinutes = it.toInt())) },
            valueRange = 1f..60f,
            steps = 59,
            colors = SliderDefaults.colors(
                thumbColor = GlassColors.AccentGlow,
                activeTrackColor = GlassColors.AccentGlow
            )
        )

        Button(
            onClick = onTestScreensaver,
            colors = ButtonDefaults.buttonColors(containerColor = GlassColors.GlassTint),
            modifier = Modifier.border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(20.dp))
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Uji Screensaver Sekarang", color = Color.White)
        }
    }
}

@Composable
fun PerformanceSettingsPane(
    config: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
    val context = LocalContext.current
    var memInfo by remember { mutableStateOf(SystemUtils.getMemoryInfo(context)) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(text = "STB Performance & Glass Mode", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        // Blur vs Solid Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.glass_blur_mode), color = Color.White, fontSize = 14.sp)
                Text(
                    text = "High performance frosted glass blur on API 31+. Turn off for 1GB RAM low-end STB.",
                    color = GlassColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = config.isHighPerformanceBlurEnabled,
                onCheckedChange = { onUpdate(config.copy(isHighPerformanceBlurEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = GlassColors.AccentGlow)
            )
        }

        // Auto RAM Boost Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.auto_boost_ram), color = Color.White, fontSize = 14.sp)
                Text(
                    text = "Automatically optimize memory cache when returning to launcher home screen.",
                    color = GlassColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = config.isAutoRamBoostEnabled,
                onCheckedChange = { onUpdate(config.copy(isAutoRamBoostEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = GlassColors.AccentGlow)
            )
        }

        // Manual Boost RAM Action
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(cornerRadius = 16.dp)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Current RAM Status", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${memInfo.freeMemMb} MB Free / ${memInfo.totalMemMb} MB Total (${memInfo.freePercentage}% Available)",
                        color = GlassColors.AccentGlow,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        memInfo = SystemUtils.boostRam(context)
                        Toast.makeText(context, "RAM Optimized: ${memInfo.freeMemMb} MB Available", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Boost RAM", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsPane(
    config: SettingsConfig,
    onUpdate: (SettingsConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(text = "Language & General", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        // Language selection
        Text(text = "Language / Bahasa", color = Color.White, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val isIndo = config.languageCode == "in"
            Box(
                modifier = Modifier
                    .liquidGlass(cornerRadius = 12.dp, isFocused = isIndo)
                    .clickable { onUpdate(config.copy(languageCode = "in")) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(text = "Bahasa Indonesia", color = if (isIndo) GlassColors.AccentGlow else Color.White)
            }

            val isEng = config.languageCode == "en"
            Box(
                modifier = Modifier
                    .liquidGlass(cornerRadius = 12.dp, isFocused = isEng)
                    .clickable { onUpdate(config.copy(languageCode = "en")) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(text = "English", color = if (isEng) GlassColors.AccentGlow else Color.White)
            }
        }

        // NTP Time Check Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.ntp_time_check), color = Color.White, fontSize = 14.sp)
                Text(
                    text = "Warn when STB internal RTC is not synchronized with internet time.",
                    color = GlassColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = config.isNtpCheckEnabled,
                onCheckedChange = { onUpdate(config.copy(isNtpCheckEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = GlassColors.AccentGlow)
            )
        }
    }
}

@Composable
fun AboutSettingsPane(
    config: SettingsConfig,
    onRestoreConfig: (SettingsConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apkList by remember { mutableStateOf(getDownloadedApkFiles()) }
    var backupList by remember { mutableStateOf(getBackupFiles()) }
    var checkingUpdate by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(text = "App Information & OTA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "• Nama Aplikasi : Home.io (v1.0.0)", color = Color.White, fontSize = 13.sp)
            Text(text = "• Package ID    : dev.andikune.homeio", color = GlassColors.TextSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        checkingUpdate = true
                        scope.launch {
                            val container = (context.applicationContext as? dev.andikune.homeio.HomeIoApplication)?.container
                            try {
                                val release = container?.githubApiService?.getLatestRelease()
                                val latestTag = release?.tagName ?: "1.0.0"
                                if (latestTag != "1.0.0" && release?.assets?.isNotEmpty() == true) {
                                    val asset = release.assets.first()
                                    DownloadService.startDownload(context, asset.downloadUrl, asset.name)
                                    Toast.makeText(context, "Update found: $latestTag. Downloading...", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Home.io is already up to date (v1.0.0)", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "GitHub API: ${e.localizedMessage ?: "No new release"}", Toast.LENGTH_SHORT).show()
                            } finally {
                                checkingUpdate = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (checkingUpdate) "Checking..." else stringResource(R.string.btn_check_update), color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val container = (context.applicationContext as? dev.andikune.homeio.HomeIoApplication)?.container
                        val file = container?.backupManager?.createBackup(config)
                        backupList = getBackupFiles()
                        Toast.makeText(context, "Backup created: ${file?.name}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.GlassTint)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.btn_create_backup), color = Color.White)
                }
            }
        }

        // APK Updates Section
        item {
            Text(text = stringResource(R.string.apk_updates_section), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (apkList.isEmpty()) {
                Text(text = "No downloaded update APK files in /HomeiO/Pembaharuan/", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        items(apkList) { apkFile ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 12.dp)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = apkFile.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { installApk(context, apkFile) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGreen)
                    ) {
                        Text(text = stringResource(R.string.btn_install), color = Color.White, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            apkFile.delete()
                            apkList = getDownloadedApkFiles()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentRed)
                    ) {
                        Text(text = stringResource(R.string.btn_delete), color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // Backup Files Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.backup_files_section), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (backupList.isEmpty()) {
                Text(text = "No backup JSON files in /HomeiO/Backup/", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        }

        items(backupList) { jsonFile ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 12.dp)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = jsonFile.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val container = (context.applicationContext as? dev.andikune.homeio.HomeIoApplication)?.container
                            if (container != null) {
                                val restored = container.backupManager.restoreBackup(jsonFile)
                                onRestoreConfig(restored)
                                Toast.makeText(context, "Config restored successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow)
                    ) {
                        Text(text = stringResource(R.string.btn_restore), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            jsonFile.delete()
                            backupList = getBackupFiles()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentRed)
                    ) {
                        Text(text = stringResource(R.string.btn_delete), color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

fun getDownloadedApkFiles(): List<File> {
    val dir = File(Environment.getExternalStorageDirectory(), "HomeiO/Pembaharuan")
    return dir.listFiles { f -> f.extension.equals("apk", ignoreCase = true) }?.toList() ?: emptyList()
}

fun getBackupFiles(): List<File> {
    val dir = File(Environment.getExternalStorageDirectory(), "HomeiO/Backup")
    return dir.listFiles { f -> f.extension.equals("json", ignoreCase = true) }?.toList() ?: emptyList()
}

fun installApk(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "dev.andikune.homeio.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Install failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
