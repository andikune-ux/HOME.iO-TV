@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package dev.andikune.homeio.presentation.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.presentation.wallpaper.FileManagerDialog
import dev.andikune.homeio.presentation.wallpaper.WallpaperView
import dev.andikune.homeio.util.SoundFeedback
import dev.andikune.homeio.util.SystemUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    config: SettingsConfig,
    allApps: List<AppItem>,
    weatherInfo: WeatherInfo?,
    onOpenSettings: () -> Unit,
    onSaveConfig: (SettingsConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val horizontalScrollState = rememberScrollState()

    var selectedNav by remember { mutableStateOf("home") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showFileManager by remember { mutableStateOf(false) }
    var selectedAppForContextMenu by remember { mutableStateOf<AppItem?>(null) }
    var isSideloadOnly by remember { mutableStateOf(false) }

    // Live Clock State
    var currentTimeString by remember { mutableStateOf(getCurrentTimeString()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = getCurrentTimeString()
            delay(1000)
        }
    }

    // Live RAM Status
    var memInfo by remember { mutableStateOf(SystemUtils.getMemoryInfo(context)) }

    // Auto-Boost RAM on Home if enabled in settings
    LaunchedEffect(config.isAutoRamBoostEnabled) {
        if (config.isAutoRamBoostEnabled) {
            memInfo = SystemUtils.boostRam(context)
        }
    }

    // App dialogs
    if (showSearchDialog) {
        SearchDialog(
            allApps = allApps,
            onAppSelected = { app -> launchApp(context, app) },
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showFileManager) {
        FileManagerDialog(
            onFileSelected = { file ->
                // Selected file can be opened or previewed
                showFileManager = false
            },
            onDismiss = { showFileManager = false }
        )
    }

    if (selectedAppForContextMenu != null) {
        val app = selectedAppForContextMenu!!
        val isFav = config.favoritePackages.contains(app.packageName)
        AppContextMenuDialog(
            app = app,
            isFavorite = isFav,
            onToggleFavorite = { pkg ->
                val newFavorites = if (isFav) {
                    config.favoritePackages.filter { it != pkg }
                } else {
                    config.favoritePackages + pkg
                }
                onSaveConfig(config.copy(favoritePackages = newFavorites))
            },
            onDismiss = { selectedAppForContextMenu = null }
        )
    }

    // Favorite apps (up to 3 primary)
    val favoriteApps = remember(allApps, config.favoritePackages) {
        val favs = allApps.filter { config.favoritePackages.contains(it.packageName) }
        if (favs.isNotEmpty()) favs.take(3)
        else allApps.take(3)
    }

    // Filtered apps based on Sideload toggle
    val displayApps = remember(allApps, isSideloadOnly) {
        if (isSideloadOnly) allApps.filter { it.isSideloaded }
        else allApps
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // == WALLPAPER ENGINE BACKGROUND ==
        WallpaperView(config = config)

        // == MAIN 16:9 LANDSCAPE LAYOUT ==
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // == LEFT LIQUID GLASS PANEL ==
            LeftNavigationGlassPanel(
                selectedNav = selectedNav,
                isSideloadActive = isSideloadOnly,
                onSearchClick = {
                    SoundFeedback.playClick(context)
                    showSearchDialog = true
                },
                onHomeClick = {
                    SoundFeedback.playClick(context)
                    selectedNav = "home"
                    isSideloadOnly = false
                    scope.launch { horizontalScrollState.animateScrollTo(0) }
                },
                onRamBoostClick = {
                    SoundFeedback.playClick(context)
                    memInfo = SystemUtils.boostRam(context)
                },
                onFileManagerClick = {
                    SoundFeedback.playClick(context)
                    showFileManager = true
                },
                onSideloadToggleClick = {
                    SoundFeedback.playClick(context)
                    isSideloadOnly = !isSideloadOnly
                },
                onSettingsClick = {
                    SoundFeedback.playClick(context)
                    onOpenSettings()
                }
            )

            Spacer(modifier = Modifier.width(14.dp))

            // == MAIN CONTENT (Horizontal Scroll Only - 16:9 Landscape TV) ==
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(780.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: Voice Search Mic + Realtime Clock + Status + Settings
                    TopStatusBar(
                        currentTime = currentTimeString,
                        isNtpSynced = SystemUtils.isTimeSynced(),
                        showNtpWarning = config.isNtpCheckEnabled && !SystemUtils.isTimeSynced(),
                        onMicClick = {
                            SoundFeedback.playClick(context)
                            showSearchDialog = true
                        },
                        onSettingsClick = {
                            SoundFeedback.playClick(context)
                            onOpenSettings()
                        }
                    )

                    // Row of Glass Widgets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LiveWeatherWidget(
                            weather = weatherInfo,
                            modifier = Modifier.weight(1f)
                        )
                        LiveSystemInfoWidget(
                            memInfo = memInfo,
                            onBoostClick = {
                                SoundFeedback.playClick(context)
                                memInfo = SystemUtils.boostRam(context)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // FAVORITE APPS ROW (Hanya 3 Aplikasi Utama)
                    FavoriteAppsSection(
                        favoriteApps = favoriteApps,
                        onAppClick = { app ->
                            SoundFeedback.playClick(context)
                            launchApp(context, app)
                        },
                        onAppLongClick = { app ->
                            SoundFeedback.playClick(context)
                            selectedAppForContextMenu = app
                        }
                    )

                    // ALL APPS ROW (Horizontal Scroll ke Kanan ->)
                    AllAppsHorizontalSection(
                        apps = displayApps,
                        isSideloadOnly = isSideloadOnly,
                        onAppClick = { app ->
                            SoundFeedback.playClick(context)
                            launchApp(context, app)
                        },
                        onAppLongClick = { app ->
                            SoundFeedback.playClick(context)
                            selectedAppForContextMenu = app
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LeftNavigationGlassPanel(
    selectedNav: String,
    isSideloadActive: Boolean,
    onSearchClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRamBoostClick: () -> Unit,
    onFileManagerClick: () -> Unit,
    onSideloadToggleClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .liquidGlass(cornerRadius = 28.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Brand header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .liquidGlass(cornerRadius = 10.dp, isFocused = true),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = GlassColors.AccentGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Home.io",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Liquid Glass",
                        color = GlassColors.TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Primary Navigation buttons
            GlassNavButton(
                icon = Icons.Default.Search,
                label = stringResource(R.string.nav_search),
                isSelected = false,
                onClick = onSearchClick
            )

            GlassNavButton(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                isSelected = selectedNav == "home" && !isSideloadActive,
                onClick = onHomeClick
            )

            // System & Utils Section Header
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.nav_system_utils),
                color = GlassColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )

            GlassNavButton(
                icon = Icons.Default.CleaningServices,
                label = stringResource(R.string.ram_booster),
                isSelected = false,
                onClick = onRamBoostClick
            )

            GlassNavButton(
                icon = Icons.Default.Folder,
                label = stringResource(R.string.file_manager),
                isSelected = false,
                onClick = onFileManagerClick
            )

            GlassNavButton(
                icon = Icons.Default.Build,
                label = stringResource(R.string.sideloaded_apps),
                isSelected = isSideloadActive,
                onClick = onSideloadToggleClick
            )
        }

        // Settings Entry
        GlassNavButton(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.settings_title),
            isSelected = false,
            onClick = onSettingsClick
        )
    }
}

@Composable
fun GlassNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 14.dp, isFocused = isSelected)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) GlassColors.AccentGlow else GlassColors.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else GlassColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun TopStatusBar(
    currentTime: String,
    isNtpSynced: Boolean,
    showNtpWarning: Boolean,
    onMicClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 20.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Voice Search Trigger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .liquidGlass(cornerRadius = 12.dp)
                .combinedClickable(onClick = onMicClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = GlassColors.AccentGlow, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.search_hint), color = GlassColors.TextSecondary, fontSize = 12.sp)
        }

        // Right Indicators (Time, Wi-Fi, NTP warning, Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showNtpWarning) {
                Icon(Icons.Default.Warning, contentDescription = "NTP Out of sync", tint = GlassColors.AccentAmber, modifier = Modifier.size(18.dp))
            }

            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi Connected", tint = GlassColors.AccentGlow, modifier = Modifier.size(18.dp))

            IconButton(onClick = onSettingsClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun LiveWeatherWidget(
    weather: WeatherInfo?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(112.dp)
            .liquidGlass(cornerRadius = 24.dp)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = weather?.locationName ?: "Jakarta, ID",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = weather?.description ?: "Open-Meteo Realtime",
                        color = GlassColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = if (weather != null) "${weather.temperature.toInt()}°C" else "--°C",
                    color = GlassColors.AccentGlow,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = "Live Forecast • Open-Meteo",
                color = GlassColors.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun LiveSystemInfoWidget(
    memInfo: SystemUtils.MemoryInfo,
    onBoostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(112.dp)
            .liquidGlass(cornerRadius = 24.dp)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sistem & RAM STB",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${memInfo.freeMemMb} MB Free / ${memInfo.totalMemMb} MB Total",
                        color = GlassColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onBoostClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Boost", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Memory usage progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x33FFFFFF))
            ) {
                val usedRatio = if (memInfo.totalMemMb > 0) memInfo.usedMemMb.toFloat() / memInfo.totalMemMb.toFloat() else 0.5f
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(usedRatio.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(GlassColors.AccentGlow)
                )
            }
        }
    }
}

@Composable
fun FavoriteAppsSection(
    favoriteApps: List<AppItem>,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit
) {
    Column {
        Text(
            text = "${stringResource(R.string.favorite_apps)} (3 Utama)",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            favoriteApps.forEach { app ->
                AppCardItem(
                    app = app,
                    isWide = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

@Composable
fun AllAppsHorizontalSection(
    apps: List<AppItem>,
    isSideloadOnly: Boolean,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit
) {
    val scrollState = rememberScrollState()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSideloadOnly) stringResource(R.string.sideloaded_apps) else stringResource(R.string.all_apps),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${apps.size} Aplikasi",
                color = GlassColors.TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            apps.forEach { app ->
                AppCardItem(
                    app = app,
                    isWide = app.isTvApp && app.banner != null,
                    modifier = Modifier.width(if (app.isTvApp && app.banner != null) 160.dp else 105.dp),
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

@Composable
fun AppCardItem(
    app: AppItem,
    isWide: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bitmap = remember(app) {
        val targetDrawable = if (isWide && app.banner != null) app.banner else app.icon
        targetDrawable?.let { drawableToBitmap(it) }
    }

    Box(
        modifier = modifier
            .height(84.dp)
            .liquidGlass(cornerRadius = 20.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.label,
                    contentScale = if (isWide) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .size(if (isWide) 48.dp else 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = app.label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

fun getCurrentTimeString(): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
