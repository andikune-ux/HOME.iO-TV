package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.AutoFocusPane
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
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
    val paneFocusRequester = remember { FocusRequester() }

    // FIX BUG #3: Force Save saat keluar (mencegah data loss karena delay)
    DisposableEffect(Unit) {
        onDispose {
            if (draftConfig != currentConfig) {
                onSaveConfig(draftConfig)
            }
        }
    }

    LaunchedEffect(draftConfig) {
        delay(500)
        if (draftConfig != currentConfig) {
            onSaveConfig(draftConfig)
        }
    }

    Row(Modifier.fillMaxSize().background(GlassColors.DeepBackground)) {
        // ============================================================
        // PANE KIRI (SIDEBAR)
        // ============================================================
        Column(
            Modifier.weight(0.28f).fillMaxHeight().background(GlassColors.DeepBackground.copy(alpha = 0.5f))
                .padding(16.dp).verticalScroll(rememberScrollState()).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(GlassColors.GlassTint)
                    .border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onBack() }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = GlassColors.AccentGlow)
                Spacer(Modifier.width(8.dp))
                Text("Kembali ke Home", color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            Text("KATEGORI", color = GlassColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp))

            SettingsCategory.entries.forEach { cat ->
                SidebarItem(
                    category = cat,
                    isSelected = selectedCategory == cat,
                    onClick = {
                        selectedCategory = cat
                        scope.launch {
                            delay(150)
                            // FIX BUG #11: Request focus ke pane kanan
                            try { paneFocusRequester.requestFocus() } catch (_: Exception) {}
                        }
                    }
                )
            }
        }

        // ============================================================
        // PANE KANAN (KONTEN)
        // ============================================================
        Box(Modifier.weight(0.72f).fillMaxHeight().padding(24.dp)) {
            AutoFocusPane(paneKey = selectedCategory, modifier = Modifier.fillMaxSize().focusRequester(paneFocusRequester)) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    when (selectedCategory) {
                        SettingsCategory.WALLPAPER -> WallpaperSection(draftConfig) { draftConfig = it }
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

@Composable
private fun SidebarItem(category: SettingsCategory, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    val bgColor = when {
        isSelected -> GlassColors.AccentGlow.copy(alpha = 0.2f)
        isFocused -> GlassColors.GlassTint.copy(alpha = 0.6f)
        else -> GlassColors.GlassTint
    }
    val borderColor = when {
        isSelected -> GlassColors.AccentGlow
        isFocused -> Color.White
        else -> GlassColors.GlassBorder
    }

    NeonFocusBorder(focused = isFocused, cornerRadius = 12.dp) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bgColor)
                .border(if (isSelected || isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                .onFocusChanged { isFocused = it.isFocused }.focusable().clickable { onClick() }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.icon, null,
                tint = if (isSelected || isFocused) GlassColors.AccentGlow else GlassColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                category.title,
                color = if (isSelected || isFocused) GlassColors.AccentGlow else GlassColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
