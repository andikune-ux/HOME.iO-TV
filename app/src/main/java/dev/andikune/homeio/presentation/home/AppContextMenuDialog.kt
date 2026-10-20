package dev.andikune.homeio.presentation.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import dev.andikune.homeio.domain.model.AppItem
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.util.SoundFeedback

@Composable
fun AppContextMenuDialog(
    app: AppItem,
    isFavorite: Boolean,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF0111827),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        title = {
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContextMenuOption(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.action_open),
                    onClick = {
                        SoundFeedback.playClick(context)
                        launchApp(context, app)
                        onDismiss()
                    }
                )

                ContextMenuOption(
                    icon = if (isFavorite) Icons.Default.StarOutline else Icons.Default.Star,
                    title = if (isFavorite) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin),
                    tint = if (isFavorite) GlassColors.AccentAmber else GlassColors.AccentGlow,
                    onClick = {
                        SoundFeedback.playClick(context)
                        onToggleFavorite(app.packageName)
                        onDismiss()
                    }
                )

                ContextMenuOption(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.action_info),
                    onClick = {
                        SoundFeedback.playClick(context)
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${app.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }
                )

                ContextMenuOption(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.action_uninstall),
                    tint = GlassColors.AccentRed,
                    onClick = {
                        SoundFeedback.playClick(context)
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:${app.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Tutup", color = GlassColors.TextSecondary)
            }
        }
    )
}

@Composable
fun ContextMenuOption(
    icon: ImageVector,
    title: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 14.dp)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
