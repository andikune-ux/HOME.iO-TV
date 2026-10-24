package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.AutoFocusPane
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.util.ApkFileListSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    WALLPAPER("Wallpaper & Skala", Icons.Default.Wallpaper),
    SCREENSAVER("Screensaver OLED", Icons.Default.Monitor),
    PERFORMANCE("Performa STB", Icons.Default.Speed),
    LANGUAGE("Bahasa & Umum", Icons.Default.Language),
    TV_DISPLAY("Tampilan TV & Sistem", Icons.Default.Tv),
    UPDATER("Pembaruan Aplikasi", Icons.Default.SystemUpdate),
    BACKUP("Backup Biasa", Icons.Default.Save),
    INFO("Info Aplikasi", Icons.Default.Info)
}

@Composable
fun SettingsScreen(
    currentConfig: SettingsConfig,
    onSaveConfig: (SettingsConfig) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.WALLPAPER) }
    var draftConfig by remember { mutableStateOf(currentConfig) }
    val scope = rememberCoroutineScope()
    
    val contentFocusRequester = remember { FocusRequester() }

    // STATE UNTUK DIALOG KONFIRMASI
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Auto-save HANYA untuk menu selain WALLPAPER
    LaunchedEffect(draftConfig, selectedCategory) {
        if (selectedCategory != SettingsCategory.WALLPAPER) {
            delay(500)
            if (draftConfig != currentConfig) {
                onSaveConfig(draftConfig)
            }
        }
    }

    // Fungsi navigasi dengan intersepsi
    fun requestNavigation(action: () -> Unit) {
        if (selectedCategory == SettingsCategory.WALLPAPER && draftConfig != currentConfig) {
            pendingAction = action
            showSaveDialog = true
        } else {
            action()
        }
    }

    Box(Modifier.fillMaxSize().background(GlassColors.DeepBackgroundGradient)) {
        Row(Modifier.fillMaxSize().padding(16.dp)) {
            
            Column(
                Modifier.weight(0.28f).fillMaxHeight().liquidGlass(cornerRadius = 24.dp)
                    .padding(16.dp).verticalScroll(rememberScrollState()).focusGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(GlassColors.GlassTintLighter)
                        .border(1.dp, GlassColors.AccentGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                requestNavigation { onBack() }
                                true
                            } else false
                        }
                        .clickable { requestNavigation { onBack() } }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = GlassColors.AccentGlow)
                    Spacer(Modifier.width(8.dp))
                    Text("Kembali ke Home", color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text("PENGATURAN", color = GlassColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(start = 8.dp))

                SettingsCategory.entries.forEach { cat ->
                    SidebarItem(
                        category = cat,
                        isSelected = selectedCategory == cat,
                        onClick = {
                            requestNavigation {
                                selectedCategory = cat
                                scope.launch {
                                    delay(250)
                                    runCatching { contentFocusRequester.requestFocus() }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.width(24.dp))

            Box(Modifier.weight(0.72f).fillMaxHeight().liquidGlass(cornerRadius = 24.dp).padding(24.dp)) {
                AutoFocusPane(paneKey = selectedCategory, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.fillMaxSize().focusRequester(contentFocusRequester)) {
                        item {
                            when (selectedCategory) {
                                SettingsCategory.WALLPAPER -> WallpaperSection(
                                    settings = draftConfig, 
                                    onUpdate = { draftConfig = it },
                                    onSave = { onSaveConfig(draftConfig) }
                                )
                                SettingsCategory.SCREENSAVER -> ScreensaverSection(draftConfig) { draftConfig = it }
                                SettingsCategory.PERFORMANCE -> PerformanceSection(draftConfig) { draftConfig = it }
                                SettingsCategory.LANGUAGE -> LanguageSection(draftConfig) { draftConfig = it }
                                SettingsCategory.TV_DISPLAY -> TvDisplaySection(draftConfig) { draftConfig = it }
                                SettingsCategory.UPDATER -> ApkFileListSection()
                                SettingsCategory.BACKUP -> BackupBiasaSection(draftConfig) { draftConfig = it }
                                SettingsCategory.INFO -> InfoAppSection()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        Dialog(
            onDismissRequest = { showSaveDialog = false; pendingAction = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth(0.7f).liquidGlass(cornerRadius = 20.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Simpan Perubahan?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Kamu memiliki perubahan wallpaper yang belum disimpan.", color = GlassColors.TextSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        var isDiscardFocused by remember { mutableStateOf(false) }
                        NeonFocusBorder(focused = isDiscardFocused, cornerRadius = 12.dp, modifier = Modifier.weight(1f)) {
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x33FF5252))
                                .onFocusChanged { isDiscardFocused = it.isFocused }.focusable()
                                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { 
                                    draftConfig = currentConfig; pendingAction?.invoke(); showSaveDialog = false; pendingAction = null; true 
                                } else false }
                                .clickable { draftConfig = currentConfig; pendingAction?.invoke(); showSaveDialog = false; pendingAction = null }
                                .padding(14.dp), contentAlignment = Alignment.Center) {
                                Text("Buang", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        var isSaveFocused by remember { mutableStateOf(false) }
                        NeonFocusBorder(focused = isSaveFocused, cornerRadius = 12.dp, modifier = Modifier.weight(1f)) {
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isSaveFocused) GlassColors.AccentGreen else GlassColors.AccentGreen.copy(alpha = 0.8f))
                                .onFocusChanged { isSaveFocused = it.isFocused }.focusable()
                                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { 
                                    onSaveConfig(draftConfig); pendingAction?.invoke(); showSaveDialog = false; pendingAction = null; true 
                                } else false }
                                .clickable { onSaveConfig(draftConfig); pendingAction?.invoke(); showSaveDialog = false; pendingAction = null }
                                .padding(14.dp), contentAlignment = Alignment.Center) {
                                Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(category: SettingsCategory, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    NeonFocusBorder(focused = isFocused, cornerRadius = 14.dp) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        isSelected -> GlassColors.AccentGlow.copy(alpha = 0.15f)
                        isFocused -> GlassColors.GlassTintLighter
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = if (isSelected) 1.5.dp else 0.dp,
                    color = if (isSelected) GlassColors.AccentGlow else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                        onClick(); true
                    } else false
                }
                .clickable { onClick() }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.icon, null,
                tint = if (isSelected || isFocused) GlassColors.AccentGlow else GlassColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                category.title,
                color = if (isSelected || isFocused) Color.White else GlassColors.TextSecondary,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
