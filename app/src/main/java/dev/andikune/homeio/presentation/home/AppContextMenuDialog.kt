package dev.andikune.homeio.presentation.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.homeio.R
import dev.andikune.homeio.domain.model.AppItem
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.util.SoundFeedback
import kotlinx.coroutines.delay

@Composable
fun AppContextMenuDialog(
    app: AppItem,
    isFavorite: Boolean,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val firstItemFocusRequester = remember { FocusRequester() }

    // Auto fokus ke tombol pertama saat dialog muncul
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { firstItemFocusRequester.requestFocus() }
    }

    // FIX: Ganti AlertDialog standar dengan Dialog kustom agar TV friendly
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .liquidGlass(cornerRadius = 24.dp, isFocused = true)
                    .padding(24.dp)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ContextMenuOption(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.action_open),
                    focusReq = firstItemFocusRequester,
                    onClick = {
                        launchApp(context, app)
                        onDismiss()
                    }
                )

                ContextMenuOption(
                    icon = if (isFavorite) Icons.Default.StarOutline else Icons.Default.Star,
                    title = if (isFavorite) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin),
                    tint = if (isFavorite) GlassColors.AccentAmber else GlassColors.AccentGlow,
                    onClick = {
                        onToggleFavorite(app.packageName)
                        onDismiss()
                    }
                )

                ContextMenuOption(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.action_info),
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${app.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }
                )

                if (!app.isSystemApp) {
                    ContextMenuOption(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.action_uninstall),
                        tint = GlassColors.AccentRed,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.parse("package:${app.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Tombol Tutup
                var isFocusedTutup by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isFocusedTutup) Color.White else GlassColors.GlassTint)
                        .border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .onFocusChanged { isFocusedTutup = it.isFocused }
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                onDismiss(); true
                            } else false
                        }
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tutup", color = if (isFocusedTutup) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ContextMenuOption(
    icon: ImageVector,
    title: String,
    tint: Color = Color.White,
    focusReq: FocusRequester? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    
    // Hapus playClick di onClick, pindahkan ke sini agar tidak dobel
    val handleClick = {
        SoundFeedback.playClick(context)
        onClick()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) GlassColors.GlassTintLighter else Color.Transparent)
            .border(if (isFocused) 1.5.dp else 0.dp, if (isFocused) GlassColors.AccentGlow else Color.Transparent, RoundedCornerShape(14.dp))
            .then(if (focusReq != null) Modifier.focusRequester(focusReq) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    handleClick(); true
                } else false
            }
            .clickable { handleClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

fun launchApp(context: Context, app: AppItem) {
    val pm = context.packageManager
    val launchIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName)
        ?: pm.getLaunchIntentForPackage(app.packageName)
    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
