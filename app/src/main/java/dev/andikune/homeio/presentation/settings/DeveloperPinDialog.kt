package dev.andikune.homeio.presentation.settings

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.homeio.data.local.DeveloperPrefs
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.util.CrashHandler
import dev.andikune.homeio.util.SoundFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun DeveloperPinDialog(
    onDismiss: () -> Unit,
    onSuccess: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var lockTimer by remember { mutableIntStateOf(0) }

    val isCurrentlyDevMode = DeveloperPrefs.isDevModeActive(context)

    LaunchedEffect(isLocked) {
        if (isLocked) {
            lockTimer = 10
            while (lockTimer > 0) {
                delay(1000)
                lockTimer--
            }
            isLocked = false
            failedAttempts = 0
        }
    }

    fun verifyPin() {
        if (pin.length < 6) return
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val url = URL("https://raw.githubusercontent.com/andikune-ux/dev.andikune-apps/main/system_config.txt")
                val expectedBase64 = withContext(Dispatchers.IO) { url.readText().trim() }
                
                // LOGIKA BASE64: Ubah PIN yang diketik menjadi Base64
                val actualBase64 = Base64.encodeToString(pin.toByteArray(), Base64.NO_WRAP)

                if (actualBase64 == expectedBase64) {
                    val newState = !isCurrentlyDevMode
                    DeveloperPrefs.setDevModeActive(context, newState)
                    onSuccess(newState)
                    onDismiss()
                } else {
                    failedAttempts++
                    if (failedAttempts >= 3) {
                        isLocked = true
                    } else {
                        errorMessage = "PIN Salah! Sisa percobaan: ${3 - failedAttempts}"
                    }
                    pin = ""
                }
            } catch (e: Exception) {
                errorMessage = "Gagal verifikasi. Cek internet TV/HP Anda."
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxHeight(0.9f)
                    .liquidGlass(cornerRadius = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text("MASUKKAN PIN DEVELOPER", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(if (isCurrentlyDevMode) "Masukkan PIN untuk MEMATIKAN Dev Mode" else "Masukkan PIN untuk MENGAKTIFKAN Dev Mode", color = GlassColors.TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        val isFilled = index < pin.length
                        Box(
                            Modifier.size(14.dp).clip(RoundedCornerShape(4.dp))
                                .background(if (isFilled) GlassColors.AccentGlow else Color.Transparent)
                                .border(1.dp, if (isFilled) GlassColors.AccentGlow else GlassColors.TextSecondary, RoundedCornerShape(4.dp))
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = GlassColors.AccentRed, fontSize = 11.sp, textAlign = TextAlign.Center)
                    } else if (isLocked) {
                        Text("Terkunci! Tunggu $lockTimer detik...", color = GlassColors.AccentAmber, fontSize = 11.sp, textAlign = TextAlign.Center)
                    } else if (isLoading) {
                        Text("Memverifikasi PIN...", color = GlassColors.AccentGlow, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }

                Spacer(Modifier.height(12.dp))

                val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Hapus", "0", "Batal")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 3) {
                                val item = numbers[row * 3 + col]
                                val isSpecial = item == "Hapus" || item == "Batal"
                                var isFocused by remember { mutableStateOf(false) }
                                
                                NeonFocusBorder(focused = isFocused, cornerRadius = 8.dp, modifier = Modifier.weight(1f)) {
                                    Box(
                                        Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSpecial -> Color(0x33FF5252)
                                                    isFocused -> GlassColors.AccentGlow
                                                    else -> GlassColors.GlassTint
                                                }
                                            )
                                            .onFocusChanged { isFocused = it.isFocused }
                                            .focusable(enabled = !isLocked && !isLoading)
                                            .onPreviewKeyEvent { e ->
                                                if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                                                    SoundFeedback.playClick(context)
                                                    when (item) {
                                                        "Hapus" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                        "Batal" -> onDismiss()
                                                        else -> if (pin.length < 6) {
                                                            pin += item
                                                            if (pin.length == 6) verifyPin()
                                                        }
                                                    }
                                                    true
                                                } else false
                                            }
                                            .clickable(enabled = !isLocked && !isLoading) {
                                                SoundFeedback.playClick(context)
                                                when (item) {
                                                    "Hapus" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                    "Batal" -> onDismiss()
                                                    else -> if (pin.length < 6) {
                                                        pin += item
                                                        if (pin.length == 6) verifyPin()
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            item, 
                                            color = if (isFocused && !isSpecial) GlassColors.DeepBackground else Color.White, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = if (isSpecial) 11.sp else 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                var isFocusedLog by remember { mutableStateOf(false) }
                NeonFocusBorder(focused = isFocusedLog, cornerRadius = 10.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isFocusedLog) GlassColors.AccentAmber else Color(0x33FFFFFF))
                            .onFocusChanged { isFocusedLog = it.isFocused }.focusable()
                            .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { 
                                CrashHandler.clearCrashLog(context)
                                android.widget.Toast.makeText(context, "Crash log dibersihkan", android.widget.Toast.LENGTH_SHORT).show()
                                true 
                            } else false }
                            .clickable { 
                                CrashHandler.clearCrashLog(context)
                                android.widget.Toast.makeText(context, "Crash log dibersihkan", android.widget.Toast.LENGTH_SHORT).show()
                            }.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, null, tint = if (isFocusedLog) GlassColors.DeepBackground else GlassColors.AccentAmber, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hapus Crash Log", color = if (isFocusedLog) GlassColors.DeepBackground else GlassColors.AccentAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
