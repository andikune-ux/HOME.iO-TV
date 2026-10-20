package dev.andikune.homeio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.util.CrashHandler
import dev.andikune.homeio.util.SoundFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            var crashLog by remember { mutableStateOf(CrashHandler.getLastCrashLog(context)) }
            var isSplashFinished by remember { mutableStateOf(false) }

            if (crashLog != null) {
                // Display Crash Log UI
                CrashLogScreen(
                    crashLog = crashLog!!,
                    onClearAndReload = {
                        CrashHandler.clearCrashLog(context)
                        crashLog = null
                    }
                )
            } else if (!isSplashFinished) {
                // Splash screen with fade-in 400ms -> hold 600ms -> fade-out 400ms
                SplashScreen(onFinish = { isSplashFinished = true })
            } else {
                // Main Launcher Home UI
                HomeLauncherMainScreen()
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in: 400ms
        alphaAnim.animateTo(1f, animationSpec = tween(400))
        // Hold: 600ms
        delay(600)
        // Fade out: 400ms
        alphaAnim.animateTo(0f, animationSpec = tween(400))
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassColors.DeepBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .liquidGlass(cornerRadius = 32.dp, isFocused = true),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home.io Logo",
                    tint = GlassColors.AccentGlow,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Home.io",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Liquid Glass Launcher",
                color = GlassColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CrashLogScreen(crashLog: String, onClearAndReload: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassColors.DeepBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlass(cornerRadius = 24.dp)
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = GlassColors.AccentRed,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.crash_detected_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.crash_detected_desc),
                        color = GlassColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stack trace text area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF000000))
                    .border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = crashLog,
                    color = Color(0xFFFF8080),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        SoundFeedback.playClick(context)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Home.io Crash Log", crashLog)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.GlassTint),
                    modifier = Modifier
                        .testTag("copy_crash_log_button")
                        .border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(20.dp))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_quick_copy), color = Color.White)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        SoundFeedback.playClick(context)
                        onClearAndReload()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.AccentGlow),
                    modifier = Modifier.testTag("reload_launcher_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_return_home), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HomeLauncherMainScreen() {
    val context = LocalContext.current
    var selectedNav by remember { mutableStateOf("home") }
    val horizontalScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassColors.BackgroundGradient)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // == LEFT LIQUID GLASS PANEL ==
            LeftGlassPanel(
                selectedNav = selectedNav,
                onNavSelect = { nav ->
                    SoundFeedback.playClick(context)
                    selectedNav = nav
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            // == MAIN CONTENT (Horizontal Scroll Only - 16:9 TV Landscape) ==
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(760.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar (Voice Search + Time & Status)
                    TopHeaderBar()

                    // Widgets Row (Glass Weather Widget + Glass System Info Widget)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WeatherWidget(modifier = Modifier.weight(1f))
                        SystemInfoWidget(modifier = Modifier.weight(1f))
                    }

                    // Favorite Apps Row (3 Primary Apps)
                    FavoriteAppsRow()

                    // All Apps Row (Horizontal scroll list)
                    AllAppsRow()
                }
            }
        }
    }
}

@Composable
fun LeftGlassPanel(
    selectedNav: String,
    onNavSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .liquidGlass(cornerRadius = 28.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // App Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
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
                Text(
                    text = "Home.io",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Navigation Items
            NavItem(
                icon = Icons.Default.Search,
                label = stringResource(R.string.nav_search),
                isSelected = selectedNav == "search",
                onClick = { onNavSelect("search") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            NavItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                isSelected = selectedNav == "home",
                onClick = { onNavSelect("home") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            NavItem(
                icon = Icons.Default.Build,
                label = stringResource(R.string.nav_system_utils),
                isSelected = selectedNav == "utils",
                onClick = { onNavSelect("utils") }
            )
        }

        // Settings Entry at bottom
        NavItem(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.settings_title),
            isSelected = selectedNav == "settings",
            onClick = { onNavSelect("settings") }
        )
    }
}

@Composable
fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 16.dp, isFocused = isSelected)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) GlassColors.AccentGlow else GlassColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else GlassColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun TopHeaderBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 20.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GlassColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.search_hint),
                color = GlassColors.TextSecondary,
                fontSize = 14.sp
            )
        }

        Text(
            text = "Home.io Liquid Glass • 1.0.0",
            color = GlassColors.AccentGlow,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun WeatherWidget(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(110.dp)
            .liquidGlass(cornerRadius = 24.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Jakarta, ID",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "29°C",
                    color = GlassColors.AccentGlow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Partly Cloudy • Open-Meteo",
                color = GlassColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SystemInfoWidget(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(110.dp)
            .liquidGlass(cornerRadius = 24.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "System Status",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Optimal",
                    color = GlassColors.AccentGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "RAM Available • High Performance Glass",
                color = GlassColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun FavoriteAppsRow() {
    Column {
        Text(
            text = stringResource(R.string.favorite_apps),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(86.dp)
                        .liquidGlass(cornerRadius = 22.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Favorite App ${index + 1}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AllAppsRow() {
    val scrollState = rememberScrollState()
    Column {
        Text(
            text = stringResource(R.string.all_apps),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            repeat(8) { index ->
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 80.dp)
                        .liquidGlass(cornerRadius = 20.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "App ${index + 1}",
                        color = GlassColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
