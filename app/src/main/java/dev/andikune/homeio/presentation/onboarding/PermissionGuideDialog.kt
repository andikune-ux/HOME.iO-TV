package dev.andikune.homeio.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.liquidGlass

@Composable
fun PermissionGuideDialog(
    title: String,
    message: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth(0.7f).liquidGlass(cornerRadius = 24.dp).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Warning, null, tint = GlassColors.AccentAmber, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(title, color = GlassColors.AccentAmber, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(message, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var isCloseFocused by remember { mutableStateOf(false) }
                    NeonFocusBorder(focused = isCloseFocused, cornerRadius = 12.dp, modifier = Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GlassColors.GlassTint)
                            .onFocusChanged { isCloseFocused = it.isFocused }.focusable()
                            .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onDismiss(); true } else false }
                            .clickable { onDismiss() }.padding(14.dp), contentAlignment = Alignment.Center) {
                            Text("TUTUP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    var isOpenFocused by remember { mutableStateOf(false) }
                    NeonFocusBorder(focused = isOpenFocused, cornerRadius = 12.dp, modifier = Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isOpenFocused) GlassColors.AccentGlow else GlassColors.AccentGlow.copy(alpha = 0.8f))
                            .onFocusChanged { isOpenFocused = it.isFocused }.focusable()
                            .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onOpenSettings(); true } else false }
                            .clickable { onOpenSettings() }.padding(14.dp), contentAlignment = Alignment.Center) {
                            Text("BUKA PENGATURAN", color = GlassColors.DeepBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
