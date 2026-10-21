@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package dev.andikune.homeio.presentation.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.R
import dev.andikune.homeio.domain.model.AppItem
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.domain.model.WeatherInfo
import dev.andikune.homeio.presentation.components.*
import dev.andikune.homeio.presentation.wallpaper.FileManagerDialog
import dev.andikune.homeio.presentation.wallpaper.WallpaperView
import dev.andikune.homeio.util.DateTimeFormatter
import dev.andikune.homeio.util.SoundFeedback
import dev.andikune.homeio.util.SystemUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@Composable
fun HomeScreen(
    config: SettingsConfig,
    allApps: List<AppItem>,
    weatherInfo: WeatherInfo?,
    onOpenSettings: () -> Unit,
    onSaveConfig: (SettingsConfig) -> Unit,
    openFileManagerTrigger: Boolean = false,
    onFileManagerOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPaneOpen by remember { mutableStateOf(false) }
    val paneFocusRequester = remember { FocusRequester() }

    var selectedNav by remember { mutableStateOf("home") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showFileManager by remember { mutableStateOf(false) }
    var selectedAppForContextMenu by remember { mutableStateOf<AppItem?>(null) }
    var isSideloadOnly by remember { mutableStateOf(false) }

    LaunchedEffect(openFileManagerTrigger) {
        if (openFileManagerTrigger) {
            showFileManager = true
            onFileManagerOpened()
        }
    }

    var currentDateString by remember { mutableStateOf(DateTimeFormatter.formatDate(Date(), config.languageCode)) }
    var currentTimeString by remember { mutableStateOf(DateTimeFormatter.formatTime(Date(), config.timeFormat, config.showSeconds)) }

    LaunchedEffect(config.languageCode, config.timeFormat, config.showSeconds) {
        while (true) {
            val now = Date()
            currentDateString = DateTimeFormatter.formatDate(now, config.languageCode)
            currentTimeString = DateTimeFormatter.formatTime(now, config.timeFormat, config.showSeconds)
            delay(if (config.showSeconds) 1000L else 30_000L)
        }
    }

    var memInfo by remember { mutableStateOf(SystemUtils.getMemoryInfo(context)) }
    LaunchedEffect(Unit) {
        if (config.isAutoRamBoostEnabled) memInfo = SystemUtils.boostRam(context)
    }

    if (showSearchDialog) SearchDialog(allApps, { safeLaunchApp(context, it) }, { showSearchDialog = false })
    if (showFileManager) FileManagerDialog(onFileSelected = { showFileManager = false }, onDismiss = { showFileManager = false })
    if (selectedAppForContextMenu != null) {
        val app = selectedAppForContextMenu!!
        val isFav = config.favoritePackages.contains(app.packageName)
        AppContextMenuDialog(
            app = app, isFavorite = isFav,
            onToggleFavorite = { pkg ->
                if (isFav) onSaveConfig(config.copy(favoritePackages = config.favoritePackages.filter { it != pkg }))
                else onSaveConfig(config.copy(favoritePackages = config.favoritePackages + pkg))
                selectedAppForContextMenu = null
            },
            onDismiss = { selectedAppForContextMenu = null }
        )
    }

    val favoriteApps = remember(allApps, config.favoritePackages) {
        val favs = allApps.filter { config.favoritePackages.contains(it.packageName) }
        if (favs.isNotEmpty()) favs.take(3) else allApps.take(3)
    }
    val displayApps = remember(allApps, isSideloadOnly) {
        if (isSideloadOnly) allApps.filter { it.isSideloaded } else allApps
    }

    val safePadding = (config.safeAreaPercent * 10).dp

    Box(Modifier.fillMaxSize()) {
        WallpaperView(config = config)
        // FIX: Tambahkan scrim (gradien gelap) agar teks tetap terbaca di wallpaper terang
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

        Box(Modifier.fillMaxSize().padding(safePadding)) {
            // JAM, CUACA, RAM (Minimalis tanpa border di Kanan Atas)
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(currentTimeString, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
                Text(currentDateString, color = GlassColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${weatherInfo?.temperature?.toInt() ?: "--"}°C • ${weatherInfo?.locationName ?: "Jakarta"}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${memInfo.freeMemMb} MB Free", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            // APLIKASI (Terkunci di Bawah Kiri)
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 80.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FavoriteAppsSection(favoriteApps, { app -> SoundFeedback.playClick(context); safeLaunchApp(context, app) }, { app -> SoundFeedback.playClick(context); selectedAppForContextMenu = app })
                AllAppsHorizontalSection(displayApps, isSideloadOnly, { app -> SoundFeedback.playClick(context); safeLaunchApp(context, app) }, { app -> SoundFeedback.playClick(context); selectedAppForContextMenu = app })
            }
        }

        // TOMBOL PEMICU (Hamburger Menu) DI TENGAH KIRI
        var isTriggerFocused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart).padding(start = 16.dp).size(48.dp)
                .clip(CircleShape)
                .background(if (isTriggerFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                .onFocusChanged { 
                    isTriggerFocused = it.isFocused 
                    if (it.isFocused && !isPaneOpen) isPaneOpen = true
                }
                .focusable()
                .combinedClickable(onClick = {
                    SoundFeedback.playClick(context)
                    isPaneOpen = !isPaneOpen
                    if (isPaneOpen) scope.launch { delay(100); runCatching { paneFocusRequester.requestFocus() } }
                }),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Menu, "Menu", tint = if (isTriggerFocused) GlassColors.AccentGlow else Color.White)
        }

        // SLIDING PANE OVERLAY (KIRI)
        AnimatedVisibility(
            visible = isPaneOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(Modifier.fillMaxHeight().width(260.dp).focusRequester(paneFocusRequester)) {
                LeftNavigationGlassPanel(
                    selectedNav, isSideloadOnly,
                    onSearchClick = { SoundFeedback.playClick(context); showSearchDialog = true },
                    onHomeClick = { SoundFeedback.playClick(context); selectedNav = "home"; isSideloadOnly = false },
                    onRamBoostClick = { SoundFeedback.playClick(context); memInfo = SystemUtils.boostRam(context) },
                    onFileManagerClick = { SoundFeedback.playClick(context); showFileManager = true },
                    onSideloadToggleClick = { SoundFeedback.playClick(context); isSideloadOnly = !isSideloadOnly },
                    onSettingsClick = { SoundFeedback.playClick(context); onOpenSettings() },
                    onPaneClose = { isPaneOpen = false }
                )
            }
        }
    }
}

fun safeLaunchApp(context: Context, app: AppItem) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
        else Toast.makeText(context, "Aplikasi tidak ditemukan.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) { Toast.makeText(context, "Gagal membuka aplikasi.", Toast.LENGTH_SHORT).show() }
}

@Composable
fun LeftNavigationGlassPanel(
    selectedNav: String, isSideloadActive: Boolean,
    onSearchClick: () -> Unit, onHomeClick: () -> Unit,
    onRamBoostClick: () -> Unit, onFileManagerClick: () -> Unit,
    onSideloadToggleClick: () -> Unit, onSettingsClick: () -> Unit, onPaneClose: () -> Unit
) {
    Column(
        Modifier.fillMaxHeight().liquidGlass(cornerRadius = 0.dp, borderWidth = 0.dp).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 4.dp)) {
            Box(Modifier.size(34.dp).liquidGlass(cornerRadius = 10.dp, isFocused = true), contentAlignment = Alignment.Center) { Icon(Icons.Default.Home, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(10.dp))
            Column { Text("Home.io", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Liquid Glass", color = GlassColors.TextSecondary, fontSize = 10.sp) }
        }
        GlassNavButton(Icons.Default.Search, stringResource(R.string.nav_search), false, onSearchClick)
        GlassNavButton(Icons.Default.Home, stringResource(R.string.nav_home), selectedNav == "home" && !isSideloadActive, onHomeClick)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.nav_system_utils), color = GlassColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
        GlassNavButton(Icons.Default.CleaningServices, stringResource(R.string.ram_booster), false, onRamBoostClick)
        GlassNavButton(Icons.Default.Folder, stringResource(R.string.file_manager), false, onFileManagerClick)
        GlassNavButton(Icons.Default.Build, stringResource(R.string.sideloaded_apps), isSideloadActive, onSideloadToggleClick)
        Spacer(Modifier.height(16.dp))
        GlassNavButton(Icons.Default.Settings, stringResource(R.string.settings_title), false, onSettingsClick)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x33FF5252)).combinedClickable(onClick = onPaneClose).padding(12.dp), contentAlignment = Alignment.Center) {
            Text("TUTUP MENU", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlassNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().liquidGlass(cornerRadius = 14.dp, isFocused = isFocused || isSelected)
            .onFocusChanged { isFocused = it.isFocused }.combinedClickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(icon, label, tint = if (isFocused || isSelected) GlassColors.AccentGlow else GlassColors.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = if (isFocused || isSelected) Color.White else GlassColors.TextSecondary, fontSize = 13.sp, fontWeight = if (isFocused || isSelected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
fun FavoriteAppsSection(favoriteApps: List<AppItem>, onAppClick: (AppItem) -> Unit, onAppLongClick: (AppItem) -> Unit) {
    Column {
        Text("${stringResource(R.string.favorite_apps)} (3 Utama)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            favoriteApps.forEach { app -> AppCardItem(app, false, Modifier.weight(1f), { onAppClick(app) }, { onAppLongClick(app) }) }
        }
    }
}

@Composable
fun AllAppsHorizontalSection(apps: List<AppItem>, isSideloadOnly: Boolean, onAppClick: (AppItem) -> Unit, onAppLongClick: (AppItem) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (isSideloadOnly) stringResource(R.string.sideloaded_apps) else stringResource(R.string.all_apps), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${apps.size} Aplikasi", color = GlassColors.TextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            items(apps) { app -> AppCardItem(app, false, Modifier.width(90.dp), { onAppClick(app) }, { onAppLongClick(app) }) }
        }
    }
}

@Composable
fun AppCardItem(app: AppItem, isWide: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale = if (isFocused) 1.1f else 1f
    val context = LocalContext.current

    // FIX: Memuat ikon secara asinkron HANYA saat item ini dirender (mencegah OOM)
    val bitmap by produceState<Bitmap?>(initialValue = null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            try { drawableToBitmap(context.packageManager.getApplicationIcon(app.packageName)) } catch (e: Exception) { null }
        }
    }

    Box(
        modifier = modifier.scale(scale).liquidGlass(cornerRadius = 16.dp, isFocused = isFocused)
            .onFocusChanged { isFocused = it.isFocused }.combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (bitmap != null) {
                Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = app.label, contentScale = ContentScale.Fit, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
            } else {
                Box(Modifier.size(56.dp).background(Color(0x33FFFFFF), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(app.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
    val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bm)
    drawable.setBounds(0, 0, c.width, c.height)
    drawable.draw(c)
    return bm
}
