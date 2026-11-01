package dev.andikune.homeio.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.presentation.components.liquidGlass
import dev.andikune.homeio.util.SoundFeedback
import kotlinx.coroutines.delay

@Composable
fun TermsAndConditionsDialog(onAgree: () -> Unit) {
    val context = LocalContext.current
    var isChecked by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    val scrollFocusRequester = remember { FocusRequester() }
    val checkboxFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }

    // Fokus awal diarahkan ke area teks agar pengguna bisa langsung scroll
    LaunchedEffect(Unit) {
        delay(300)
        runCatching { scrollFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = { /* Wajib setuju, tidak bisa di-dismiss */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.9f)
                    .liquidGlass(cornerRadius = 24.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Description, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("SYARAT & KETENTUAN", color = GlassColors.AccentGlow, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(16.dp))

                // Area Teks Scrollable (SEKARANG BISA DI-FOKUS DAN DI-SCROLL DENGAN D-PAD)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(12.dp))
                        .focusRequester(scrollFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                if (event.key == Key.DirectionDown) {
                                    if (!scrollState.canScrollForward) {
                                        // Jika scroll sudah mentok bawah, pindah fokus ke Checkbox
                                        checkboxFocusRequester.requestFocus()
                                        true
                                    } else false // Biarkan scroll berjalan
                                } else if (event.key == Key.DirectionUp) {
                                    if (!scrollState.canScrollBackward) {
                                        true // Tahan fokus di atas
                                    } else false // Biarkan scroll berjalan
                                } else false
                            } else false
                        }
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = """
                            Selamat datang di HOME.iO TV. Sebelum melanjutkan, harap baca dan setujui ketentuan berikut:

                            1. 100% Gratis: Aplikasi ini sepenuhnya gratis untuk digunakan. Tidak ada biaya tersembunyi, langganan, atau iklan yang mengganggu. Fitur donasi (QRIS) bersifat opsional dan sukarela.

                            2. Izin Akses: Aplikasi memerlukan izin Penyimpanan (Storage), Internet, dan Notifikasi agar fitur Launcher, Wallpaper, serta Pembaruan (OTA) dapat berjalan optimal.

                            3. Privasi Data: Kami tidak mengumpulkan, menyimpan, atau menjual data pribadi Anda ke pihak ketiga. Seluruh data (pengaturan dan backup) disimpan 100% secara lokal di perangkat Anda.

                            4. Tanggung Jawab Pengguna: Anda bertanggung jawab penuh atas aplikasi pihak ketiga yang Anda pasang, hapus (Force Stop/Uninstall), atau modifikasi melalui launcher ini.

                            5. Mode Kiosk: Fitur Kiosk Mode akan mengunci perangkat. Pastikan Anda mengetahui cara keluar dari mode ini (tombol darurat) sebelum mengaktifkannya.

                            6. Pembaruan Sistem: Aplikasi dapat memeriksa pembaruan secara otomatis melalui repositori GitHub.

                            Dengan mencentang kotak di bawah dan menekan tombol "Saya Setuju", Anda menyatakan telah membaca, memahami, dan menyetujui seluruh Syarat & Ketentuan ini.
                        """.trimIndent(),
                        color = GlassColors.TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Checkbox Area
                var isCheckboxFocused by remember { mutableStateOf(false) }
                NeonFocusBorder(focused = isCheckboxFocused, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCheckboxFocused) GlassColors.GlassTintLighter else GlassColors.GlassTint)
                            .onFocusChanged { isCheckboxFocused = it.isFocused }
                            .focusRequester(checkboxFocusRequester)
                            .focusable()
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                                    SoundFeedback.playClick(context)
                                    isChecked = !isChecked
                                    if (isChecked) {
                                        buttonFocusRequester.requestFocus()
                                    }
                                    true
                                } else if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionUp) {
                                    scrollFocusRequester.requestFocus()
                                    true
                                } else false
                            }
                            .clickable {
                                SoundFeedback.playClick(context)
                                isChecked = !isChecked
                                if (isChecked) buttonFocusRequester.requestFocus()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isChecked) GlassColors.AccentGlow else Color.Transparent)
                                .border(2.dp, if (isChecked) GlassColors.AccentGlow else GlassColors.TextSecondary, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(Icons.Default.Check, null, tint = GlassColors.DeepBackground, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Saya telah membaca dan menyetujui Syarat & Ketentuan.",
                            color = if (isCheckboxFocused || isChecked) Color.White else GlassColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tombol Setuju
                var isButtonFocused by remember { mutableStateOf(false) }
                NeonFocusBorder(focused = isButtonFocused, cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    !isChecked -> GlassColors.SurfaceDark
                                    isButtonFocused -> GlassColors.AccentGreen
                                    else -> GlassColors.AccentGreen.copy(alpha = 0.8f)
                                }
                            )
                            .onFocusChanged { isButtonFocused = it.isFocused }
                            .focusRequester(buttonFocusRequester)
                            .focusable(enabled = isChecked)
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                                    if (isChecked) {
                                        SoundFeedback.playClick(context)
                                        onAgree()
                                        true
                                    } else {
                                        false
                                    }
                                } else if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionUp) {
                                    checkboxFocusRequester.requestFocus()
                                    true
                                } else false
                            }
                            .clickable(enabled = isChecked) {
                                if (isChecked) {
                                    SoundFeedback.playClick(context)
                                    onAgree()
                                }
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isChecked) {
                                Icon(Icons.Default.CheckCircle, null, tint = if (isButtonFocused) GlassColors.DeepBackground else Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                "SAYA SETUJU",
                                color = if (!isChecked) GlassColors.TextSecondary else if (isButtonFocused) GlassColors.DeepBackground else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
