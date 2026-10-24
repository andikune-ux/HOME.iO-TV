@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package dev.andikune.homeio.presentation.home

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import dev.andikune.homeio.R
import dev.andikune.homeio.domain.model.AppItem
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.domain.model.WeatherInfo
import dev.andikune.homeio.presentation.components.*
import dev.andikune.homeio.presentation.wallpaper.FileManagerDialog
import dev.andikune.homeio.presentation.wallpaper.WallpaperView
import dev.andikune.homeio.service.LauncherNotificationService
import dev.andikune.homeio.util.DateTimeFormatter
import dev.andikune.homeio.util.PermissionHelper
import dev.andikune.homeio.util.PermissionHelper.PermissionType
import dev.andikune.homeio.util.SoundFeedback
import dev.andikune.homeio.util.SystemUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.abs

// OPTIMASI CACHE MEMORI
private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
private val cacheSize = maxMemory / 8
private val iconCache = LruCache<String, Bitmap>(cacheSize)

@Composable
fun HomeScreen(
    config: SettingsConfig,
    allApps: List<AppItem>,
    weatherInfo: WeatherInfo?,
    onOpenSettings: () -> Unit,
    onOpenSystemControl: () -> Unit,
    onSaveConfig: (SettingsConfig) -> Unit,
    openFileManagerTrigger: Boolean = false,
    onFileManagerOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    var isPaneOpen by remember { mutableStateOf(false) }
    var showAllAppsOverlay by remember { mutableStateOf(false) }
    
    val paneFocusRequester = remember { FocusRequester() }
    val firstMenuItemFocusRequester = remember { FocusRequester() }
    val hamburgerFocusRequester = remember { FocusRequester() }
    val carouselFocusRequester = remember { FocusRequester() }

    var selectedNav by remember { mutableStateOf("home") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showFileManager by remember { mutableStateOf(false) }
    var selectedAppForContextMenu by remember { mutableStateOf<AppItem?>(null) }
    var isSideloadOnly by remember { mutableStateOf(false) }

    val notifications by LauncherNotificationService.notifications.collectAsState()

    BackHandler(enabled = isPaneOpen) { isPaneOpen = false }

    LaunchedEffect(isPaneOpen) {
        if (!isPaneOpen) {
            delay(400)
            runCatching { hamburgerFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(openFileManagerTrigger) {
        if (openFileManagerTrigger) {
            showFileManager = true
            onFileManagerOpened()
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
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))

        Box(Modifier.fillMaxSize().padding(safePadding)) {
            
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp, start = 16.dp).focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SystemControlIcon(icon = Icons.Rounded.Shield, tint = GlassColors.AccentRed, onClick = { 
                    val newKiosk = !config.isKioskModeEnabled
                    onSaveConfig(config.copy(isKioskModeEnabled = newKiosk)) 
                })
                SystemControlIcon(icon = Icons.Rounded.Wifi, tint = GlassColors.AccentGlow, onClick = onOpenSystemControl)
                SystemControlIcon(icon = Icons.Rounded.Bluetooth, tint = GlassColors.AccentGlow, onClick = onOpenSystemControl)
                SystemControlIcon(icon = Icons.Rounded.GraphicEq, tint = GlassColors.AccentGlow, onClick = onOpenSystemControl)
                SystemControlIcon(icon = Icons.Rounded.WbSunny, tint = GlassColors.AccentGlow, onClick = onOpenSystemControl)
                SystemControlIcon(icon = Icons.Rounded.Dashboard, tint = GlassColors.AccentGlow, onClick = onOpenSystemControl)
            }

            TopRightWidgets(
                config = config,
                weatherInfo = weatherInfo,
                memInfo = memInfo,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 24.dp)
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 80.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FavoriteAppsSection(
                    favoriteApps = favoriteApps, 
                    iconSizeMultiplier = config.appIconSizeMultiplier, 
                    onAppClick = { app -> SoundFeedback.playClick(context); safeLaunchApp(context, app) }, 
                    onAppLongClick = { app -> SoundFeedback.playClick(context); selectedAppForContextMenu = app },
                    modifier = Modifier.focusProperties { down = carouselFocusRequester }
                )
                
                if (displayApps.isNotEmpty()) {
                    AllAppsCenterCarousel(
                        apps = displayApps,
                        iconSizeMultiplier = config.appIconSizeMultiplier,
                        screenWidth = screenWidth,
                        focusRequester = carouselFocusRequester,
                        onAppClick = { app -> SoundFeedback.playClick(context); safeLaunchApp(context, app) },
                        onAppLongClick = { app -> SoundFeedback.playClick(context); selectedAppForContextMenu = app },
                        onEdgeLeftReached = { isPaneOpen = true },
                        onEdgeRightOrDownReached = { showAllAppsOverlay = true }
                    )
                }
            }
        }
        
        var isTriggerFocused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).size(48.dp).zIndex(10f).clip(CircleShape)
                .background(if (isTriggerFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                .focusRequester(hamburgerFocusRequester)
                .onFocusChanged { isTriggerFocused = it.isFocused }.focusable()
                .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    SoundFeedback.playClick(context)
                    isPaneOpen = !isPaneOpen
                    if (isPaneOpen) scope.launch { delay(350); runCatching { firstMenuItemFocusRequester.requestFocus() } }
                    true
                } else false }
                .combinedClickable(onClick = {
                    SoundFeedback.playClick(context)
                    isPaneOpen = !isPaneOpen
                    if (isPaneOpen) scope.launch { delay(350); runCatching { firstMenuItemFocusRequester.requestFocus() } }
                }),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Menu, "Menu", tint = if (isTriggerFocused) GlassColors.AccentGlow else Color.White)
        }

        AnimatedVisibility(
            visible = isPaneOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxHeight().width(280.dp).focusGroup().focusRequester(paneFocusRequester)) {
                    LeftNavigationGlassPanel(
                        selectedNav, isSideloadOnly,
                        notifications = notifications,
                        firstItemFocusRequester = firstMenuItemFocusRequester,
                        onSearchClick = { SoundFeedback.playClick(context); showSearchDialog = true },
                        // FIX: Gunakan PermissionHelper
                        onNotificationPermissionClick = { 
                            PermissionHelper.openPermissionSettings(context, PermissionType.NOTIF_LISTENER) 
                        },
                        onNotificationClick = { sbn -> SoundFeedback.playClick(context); try { sbn.notification.contentIntent?.send(); isPaneOpen = false } catch (e: PendingIntent.CanceledException) { Toast.makeText(context, "Gagal membuka", Toast.LENGTH_SHORT).show() } },
                        onClearNotification = { sbn -> LauncherNotificationService().removeNotification(sbn) },
                        onClearAllNotifications = { LauncherNotificationService().removeAllNotifications() },
                        onHomeClick = { SoundFeedback.playClick(context); selectedNav = "home"; isSideloadOnly = false },
                        onRamBoostClick = { SoundFeedback.playClick(context); memInfo = SystemUtils.boostRam(context); Toast.makeText(context, "RAM Dioptimalkan!", Toast.LENGTH_SHORT).show() },
                        onFileManagerClick = { SoundFeedback.playClick(context); showFileManager = true },
                        onSideloadToggleClick = { SoundFeedback.playClick(context); isSideloadOnly = !isSideloadOnly },
                        onSettingsClick = { SoundFeedback.playClick(context); onOpenSettings() },
                        onSystemControlClick = { SoundFeedback.playClick(context); isPaneOpen = false; onOpenSystemControl() },
                        onPaneClose = { isPaneOpen = false }
                    )
                }
                Box(Modifier.fillMaxHeight().weight(1f).background(Color.Black.copy(alpha = 0.5f)).clickable { isPaneOpen = false })
            }
        }

        if (showAllAppsOverlay) {
            AllAppsGridOverlay(
                apps = displayApps, iconSizeMultiplier = config.appIconSizeMultiplier,
                onDismiss = { showAllAppsOverlay = false },
                onAppClick = { app -> SoundFeedback.playClick(context); safeLaunchApp(context, app) },
                onAppLongClick = { app -> SoundFeedback.playClick(context); selectedAppForContextMenu = app }
            )
        }
    }
}

@Composable
fun TopRightWidgets(
    config: SettingsConfig,
    weatherInfo: WeatherInfo?,
    memInfo: SystemUtils.MemoryInfo,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    Column(modifier = modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currentTimeString, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(16.dp))
            var isSettingsFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(if (isSettingsFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                    .onFocusChanged { isSettingsFocused = it.isFocused }.focusable()
                    .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) { SoundFeedback.playClick(context); onOpenSettings(); true } else false }
                    .clickable { SoundFeedback.playClick(context); onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Settings, "Pengaturan", tint = if (isSettingsFocused) GlassColors.AccentGlow else Color.White, modifier = Modifier.size(28.dp))
            }
        }
        
        Text(currentDateString, color = GlassColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WbSunny, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("${weatherInfo?.temperature?.toInt() ?: "--"}°C • ${weatherInfo?.locationName ?: "Jakarta"}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Memory, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("${memInfo.freeMemMb} MB Free", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SystemControlIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 10.dp) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(if (isFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                .border(1.dp, if (isFocused) GlassColors.AccentGlow else GlassColors.GlassBorder, RoundedCornerShape(10.dp))
                .onFocusChanged { isFocused = it.isFocused }.focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun AllAppsCenterCarousel(
    apps: List<AppItem>, iconSizeMultiplier: Float, screenWidth: Dp, focusRequester: FocusRequester,
    onAppClick: (AppItem) -> Unit, onAppLongClick: (AppItem) -> Unit,
    onEdgeLeftReached: () -> Unit, onEdgeRightOrDownReached: () -> Unit
) {
    val initialIndex = remember { if (apps.size > 2) 2 else 0 }
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
    val scope = rememberCoroutineScope()
    
    val baseCardWidth = 110.dp
    val cardWidth = (baseCardWidth * iconSizeMultiplier).coerceIn(80.dp, 160.dp)
    val horizontalPadding = ((screenWidth - cardWidth) / 2f).coerceAtLeast(0.dp)

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf 0
            val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            visibleItems.minByOrNull { abs((it.offset + it.size / 2) - center) }?.index ?: 0
        }
    }

    var isProcessingKey by remember { mutableStateOf(false) }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) { delay(150); isProcessingKey = false }
    }

    Box(Modifier.fillMaxWidth().height(140.dp)) {
        LazyRow(
            state = lazyListState, flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().focusRequester(focusRequester).focusGroup()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (isProcessingKey || lazyListState.isScrollInProgress) return@onPreviewKeyEvent true
                                if (centeredIndex == 0) { onEdgeLeftReached(); true } 
                                else { 
                                    isProcessingKey = true
                                    scope.launch { lazyListState.animateScrollToItem(centeredIndex - 1) }
                                    true 
                                }
                            }
                            Key.DirectionRight -> {
                                if (isProcessingKey || lazyListState.isScrollInProgress) return@onPreviewKeyEvent true
                                if (centeredIndex == apps.size - 1) { onEdgeRightOrDownReached(); true } 
                                else { 
                                    isProcessingKey = true
                                    scope.launch { lazyListState.animateScrollToItem(centeredIndex + 1) }
                                    true 
                                }
                            }
                            Key.DirectionDown -> { onEdgeRightOrDownReached(); true }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (!isProcessingKey) {
                                    val app = apps.getOrNull(centeredIndex)
                                    if (app != null) { isProcessingKey = true; onAppClick(app) }
                                }
                                true
                            }
                            else -> false
                        }
                    } else if (event.type == KeyEventType.KeyUp) { isProcessingKey = false; false } else false
                }
        ) {
            items(apps) { app ->
                val isCentered = app == apps.getOrNull(centeredIndex)
                AppCardItem(
                    app = app, iconSizeMultiplier = iconSizeMultiplier,
                    modifier = Modifier.width(cardWidth).height(120.dp),
                    isCenterFocused = isCentered, useNativeFocus = false,
                    onClick = { }, onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

@Composable
fun FavoriteAppsSection(
    favoriteApps: List<AppItem>, 
    iconSizeMultiplier: Float, 
    onAppClick: (AppItem) -> Unit, 
    onAppLongClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("${stringResource(R.string.favorite_apps)} (3 Utama)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().focusGroup(), verticalAlignment = Alignment.CenterVertically) {
            favoriteApps.forEach { app -> 
                AppCardItem(
                    app = app, iconSizeMultiplier = iconSizeMultiplier,
                    modifier = Modifier.width(110.dp * iconSizeMultiplier).height(120.dp),
                    isCenterFocused = false, useNativeFocus = true,
                    onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) }
                ) 
            }
        }
    }
}

@Composable
fun AppCardItem(
    app: AppItem, iconSizeMultiplier: Float, modifier: Modifier = Modifier,
    isCenterFocused: Boolean, useNativeFocus: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit
) {
    var isNativeFocused by remember { mutableStateOf(false) }
    var isPressedManual by remember { mutableStateOf(false) }
    val isEffectivelyFocused = if (useNativeFocus) isNativeFocused else isCenterFocused

    val scale by animateFloatAsState(targetValue = if (isEffectivelyFocused) 1.15f else 0.9f, label = "carousel_scale")
    val context = LocalContext.current

    val baseIconSize = 48.dp
    val currentIconSize = (baseIconSize * iconSizeMultiplier).coerceIn(24.dp, 80.dp)

    val bitmap by produceState<Bitmap?>(initialValue = iconCache.get(app.packageName), app.packageName) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) { try { drawableToBitmap(context.packageManager.getApplicationIcon(app.packageName)) } catch (e: Exception) { null } }
            if (loaded != null) { iconCache.put(app.packageName, loaded); value = loaded }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressedSystem by interactionSource.collectIsPressedAsState()
    val finalIsPressed = isPressedManual || isPressedSystem

    NeonFocusBorder(
        focused = isEffectivelyFocused, pressed = finalIsPressed, cornerRadius = 16.dp,
        borderWidth = 3.dp, glowRadius = 8.dp,
        modifier = modifier.zIndex(if (isEffectivelyFocused) 2f else 0f).scale(scale)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                .background(if (isEffectivelyFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                .then(
                    if (useNativeFocus) {
                        Modifier.onFocusChanged { isNativeFocused = it.isFocused }.focusable()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    isPressedManual = true; onClick(); true
                                } else if (event.type == KeyEventType.KeyUp) { isPressedManual = false; false } else false
                            }
                    } else Modifier
                )
                .combinedClickable(interactionSource = interactionSource, indication = null, onClick = { if (useNativeFocus) onClick() }, onLongClick = onLongClick)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (bitmap != null) {
                    Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = app.label, contentScale = ContentScale.Fit, modifier = Modifier.size(currentIconSize).clip(RoundedCornerShape(12.dp)))
                } else {
                    Box(Modifier.size(currentIconSize).background(Color(0x33FFFFFF), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(app.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun AllAppsGridOverlay(apps: List<AppItem>, iconSizeMultiplier: Float, onDismiss: () -> Unit, onAppClick: (AppItem) -> Unit, onAppLongClick: (AppItem) -> Unit) {
    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { firstFocusRequester.requestFocus() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xE60A0E1A)).padding(32.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SEMUA APLIKASI (${apps.size})", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(40.dp).clip(CircleShape).background(GlassColors.GlassTint).focusable()
                        .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) { onDismiss(); true } else false }
                        .clickable { onDismiss() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, "Tutup", tint = Color.White) }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = (100.dp * iconSizeMultiplier).coerceAtLeast(80.dp)),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().focusGroup()
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        var isFirst by remember { mutableStateOf(app == apps.firstOrNull()) }
                        Box(modifier = if (isFirst) Modifier.focusRequester(firstFocusRequester) else Modifier) {
                            AppCardItem(app = app, iconSizeMultiplier = iconSizeMultiplier, modifier = Modifier.height(120.dp), isCenterFocused = false, useNativeFocus = true, onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeftNavigationGlassPanel(
    selectedNav: String, isSideloadActive: Boolean, notifications: List<android.service.notification.StatusBarNotification>,
    firstItemFocusRequester: FocusRequester, onSearchClick: () -> Unit, onNotificationPermissionClick: () -> Unit,
    onNotificationClick: (android.service.notification.StatusBarNotification) -> Unit,
    onClearNotification: (android.service.notification.StatusBarNotification) -> Unit, onClearAllNotifications: () -> Unit,
    onHomeClick: () -> Unit, onRamBoostClick: () -> Unit, onFileManagerClick: () -> Unit,
    onSideloadToggleClick: () -> Unit, onSettingsClick: () -> Unit, onSystemControlClick: () -> Unit, onPaneClose: () -> Unit
) {
    Column(Modifier.fillMaxHeight().liquidGlass(cornerRadius = 0.dp, borderWidth = 0.dp).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 4.dp)) {
            Box(Modifier.size(34.dp).liquidGlass(cornerRadius = 10.dp, isFocused = true), contentAlignment = Alignment.Center) { Icon(Icons.Default.Home, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(10.dp))
            Column { Text("HOME.iO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Liquid Glass", color = GlassColors.TextSecondary, fontSize = 10.sp) }
        }
        
        Box(Modifier.focusRequester(firstItemFocusRequester)) { GlassNavButton(Icons.Default.Search, stringResource(R.string.nav_search), false, onSearchClick) }

        if (notifications.isEmpty()) {
            var isFocusedBtn by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isFocusedBtn) GlassColors.AccentAmber.copy(alpha = 0.3f) else GlassColors.AccentAmber.copy(alpha = 0.15f)).border(1.dp, GlassColors.AccentAmber, RoundedCornerShape(14.dp))
                .onFocusChanged { isFocusedBtn = it.isFocused }.focusable().onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onNotificationPermissionClick(); true } else false }
                .clickable { onNotificationPermissionClick() }.padding(12.dp)) {
                Column { Text("Izin Notifikasi Belum Aktif", color = GlassColors.AccentAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Klik untuk mengaktifkan", color = GlassColors.TextSecondary, fontSize = 10.sp) }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("NOTIFIKASI", color = GlassColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x33FF5252)).clickable { onClearAllNotifications() }.padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Hapus Semua", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            notifications.take(3).forEach { sbn -> NotificationItem(sbn = sbn, onClick = { onNotificationClick(sbn) }, onDeleteClick = { onClearNotification(sbn) }) }
        }

        GlassNavButton(Icons.Default.Home, stringResource(R.string.nav_home), selectedNav == "home" && !isSideloadActive, onHomeClick)
        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResource(R.string.nav_system_utils), color = GlassColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
        GlassNavButton(Icons.Default.CleaningServices, stringResource(R.string.ram_booster), false, onRamBoostClick)
        GlassNavButton(Icons.Default.Folder, stringResource(R.string.file_manager), false, onFileManagerClick)
        GlassNavButton(Icons.Default.Build, stringResource(R.string.sideloaded_apps), isSideloadActive, onSideloadToggleClick)
        
        Spacer(modifier = Modifier.height(8.dp))
        GlassNavButton(Icons.Default.Settings, "Kendali Sistem", false, onSystemControlClick)
        GlassNavButton(Icons.Default.Settings, stringResource(R.string.settings_title), false, onSettingsClick)
        
        Spacer(modifier = Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x33FF5252)).focusable().onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) { onPaneClose(); true } else false }.combinedClickable(onClick = onPaneClose).padding(12.dp), contentAlignment = Alignment.Center) {
            Text("TUTUP MENU", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NotificationItem(sbn: android.service.notification.StatusBarNotification, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    var isDeleteFocused by remember { mutableStateOf(false) }
    val title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: ""
    val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
    
    val context = LocalContext.current
    val appIcon by produceState<Bitmap?>(initialValue = iconCache.get(sbn.packageName), sbn.packageName) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) { try { drawableToBitmap(context.packageManager.getApplicationIcon(sbn.packageName)) } catch (e: Exception) { null } }
            if (loaded != null) { iconCache.put(sbn.packageName, loaded); value = loaded }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().liquidGlass(cornerRadius = 14.dp, isFocused = isFocused)
        .onFocusChanged { isFocused = it.isFocused }.focusable()
        .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
        .combinedClickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp)) {
        if (appIcon != null) {
            Image(bitmap = appIcon!!.asImageBitmap(), contentDescription = null, modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)))
        } else {
            Icon(Icons.Default.Notifications, "Notif", tint = if (isFocused) GlassColors.AccentGlow else GlassColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            if (title.isNotBlank()) Text(title, color = if (isFocused) Color.White else GlassColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (text.isNotBlank()) Text(text, color = GlassColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (isDeleteFocused) Color(0xFFFF5252) else Color.Transparent)
            .onFocusChanged { isDeleteFocused = it.isFocused }.focusable()
            .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onDeleteClick(); true } else false }
            .clickable { onDeleteClick() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, "Hapus", tint = if (isDeleteFocused) Color.White else GlassColors.TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun GlassNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().liquidGlass(cornerRadius = 14.dp, isFocused = isFocused || isSelected)
        .onFocusChanged { isFocused = it.isFocused }.focusable()
        .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) { onClick(); true } else false }
        .combinedClickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Icon(icon, label, tint = if (isFocused || isSelected) GlassColors.AccentGlow else GlassColors.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = if (isFocused || isSelected) Color.White else GlassColors.TextSecondary, fontSize = 13.sp, fontWeight = if (isFocused || isSelected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

fun safeLaunchApp(context: Context, app: AppItem) {
    try {
        val pm = context.packageManager
        val launchIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName) ?: pm.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) { launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(launchIntent) }
        else Toast.makeText(context, "Aplikasi tidak ditemukan.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) { Toast.makeText(context, "Gagal membuka aplikasi.", Toast.LENGTH_SHORT).show() }
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
