package dev.andikune.homeio.presentation.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.homeio.R
import dev.andikune.homeio.data.AppKnowledge
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.liquidGlass
import kotlinx.coroutines.delay

@Composable
fun QrisDonationPopup(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val loadingFocusRequester = remember { FocusRequester() }

    // Timer 7 detik tanpa menampilkan angka
    LaunchedEffect(Unit) {
        // Paksa fokus ke box loading dulu agar tidak liar
        runCatching { loadingFocusRequester.requestFocus() }
        delay(7000)
        isLoading = false
        // Tarik fokus D-Pad setelah tombol muncul
        delay(100)
        runCatching { focusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = { /* Kunci back button */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.9f)
                    .liquidGlass(cornerRadius = 24.dp)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Donasi buat admin beli kopi",
                    color = GlassColors.AccentGlow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.qris_donasi),
                    contentDescription = "QRIS Donasi",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    // FIX BUG D-PAD: Box transparan yang bisa difokus agar D-Pad tidak liar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .focusRequester(loadingFocusRequester)
                            .focusable(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = GlassColors.AccentGlow,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Lagi menyiapkan aplikasi, mohon tunggu ya..",
                                color = GlassColors.TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Tombol Mampir
                        var isFocusedMampir by remember { mutableStateOf(false) }
                        NeonFocusBorder(
                            focused = isFocusedMampir,
                            cornerRadius = 12.dp,
                            borderWidth = 3.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isFocusedMampir) GlassColors.AccentGlow else GlassColors.GlassTint)
                                    .onFocusChanged { isFocusedMampir = it.isFocused }
                                    .focusable()
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppKnowledge.SOCIAL_WA))
                                            context.startActivity(intent)
                                            true
                                        } else false
                                    }
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppKnowledge.SOCIAL_WA))
                                        context.startActivity(intent)
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Mampir", color = if (isFocusedMampir) GlassColors.DeepBackground else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // Tombol Tutup
                        var isFocusedTutup by remember { mutableStateOf(false) }
                        NeonFocusBorder(
                            focused = isFocusedTutup,
                            cornerRadius = 12.dp,
                            borderWidth = 3.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isFocusedTutup) Color.White else GlassColors.GlassTint)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isFocusedTutup = it.isFocused }
                                    .focusable()
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                            onDismiss()
                                            true
                                        } else false
                                    }
                                    .clickable { onDismiss() }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tutup", color = if (isFocusedTutup) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
