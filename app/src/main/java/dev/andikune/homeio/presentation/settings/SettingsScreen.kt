package dev.andikune.homeio.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.util.ApkFileListSection // <-- IMPORT DITAMBAHKAN

private val GLASS_BG = Color(0x0DFFFFFF)
private val GLASS_BORDER = Color(0x1AFFFFFF)
private val GLASS_FOCUS = Color(0x33FFFFFF)
private val GOLD_ACCENT = Color(0xFFFFD700)
private val TEXT_PRIMARY = Color(0xFFFFFFFF)
private val TEXT_SECONDARY = Color(0xB3FFFFFF)

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    GENERAL("Umum", Icons.Default.Settings),
    UPDATER("Pembaruan Aplikasi", Icons.Default.SystemUpdate),
    BACKUP("Backup Aman", Icons.Default.Save),
    ABOUT("Tentang Aplikasi", Icons.Default.Info)
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GENERAL) }

    Row(
        modifier = modifier.fillMaxSize().background(Color(0xFF0A0E1A))
    ) {
        // PANE KIRI (SIDEBAR)
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(Color(0xFF070A12))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GLASS_BG)
                    .border(1.dp, GLASS_BORDER, RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = GOLD_ACCENT)
                Spacer(Modifier.width(8.dp))
                Text("Kembali ke Home", color = GOLD_ACCENT, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(16.dp))

            Text(
                text = "PENGATURAN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TEXT_SECONDARY,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )

            SettingsCategory.entries.forEach { category ->
                SidebarItem(
                    category = category,
                    isSelected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }

        // PANE KANAN (KONTEN)
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedCategory) {
                    SettingsCategory.GENERAL -> GeneralSettingsPlaceholder()
                    SettingsCategory.UPDATER -> ApkFileListSection()
                    SettingsCategory.BACKUP -> BackupSection()
                    SettingsCategory.ABOUT -> AboutPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isSelected -> GOLD_ACCENT.copy(alpha = 0.15f)
        isFocused -> GLASS_FOCUS
        else -> GLASS_BG
    }
    val borderColor = when {
        isSelected -> GOLD_ACCENT
        isFocused -> Color.White
        else -> GLASS_BORDER
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = if (isSelected || isFocused) GOLD_ACCENT else TEXT_PRIMARY,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = category.title,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected || isFocused) GOLD_ACCENT else TEXT_PRIMARY
        )
    }
}

@Composable
private fun GeneralSettingsPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("⚙️ PENGATURAN UMUM", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GOLD_ACCENT)
        Text("Menu ini akan diisi dengan opsi Wallpaper, Performa, Bahasa, dll.", color = TEXT_SECONDARY)
    }
}

@Composable
private fun AboutPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ℹ️ TENTANG APLIKASI", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GOLD_ACCENT)
        Text("Home.io\nPackage: dev.andikune.homeio", color = TEXT_SECONDARY)
    }
}
