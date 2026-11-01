package dev.andikune.homeio.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.homeio.BuildConfig
import dev.andikune.homeio.data.AppKnowledge
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.util.SoundFeedback

@Composable
fun HelpSection() {
    val ctx = LocalContext.current

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PANDUAN LENGKAP HOME.iO TV", color = GlassColors.AccentGlow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Panduan dari nol hingga mahir untuk TV & HP.", color = GlassColors.TextSecondary, fontSize = 13.sp)

        // 1. INSTALL
        HelpCard(icon = Icons.Default.Download, title = "1. CARA INSTALL APLIKASI") {
            HelpText("Di HP/Tablet: Unduh file APK dari GitHub. Buka File Manager bawaan HP, cari file APK tersebut, lalu ketuk untuk Install.")
            HelpText("Di Android TV/STB: Unduh melalui aplikasi 'Downloader' (ketik URL GitHub), atau pindahkan file APK via USB Flashdisk. Buka File Manager TV dan klik Install.")
            HelpText("Penting: Jika muncul peringatan, Anda harus mengizinkan 'Instal dari sumber tidak dikenal' (Unknown Sources) di pengaturan keamanan perangkat Anda.", isHighlight = true)
        }

        // 2. LAUNCHER TANPA PC
        HelpCard(icon = Icons.Default.Home, title = "2. JADIKAN LAUNCHER UTAMA (TANPA PC)") {
            HelpText("Pengguna Android TV:")
            HelpText("Buka Pengaturan (Settings) > Pilih menu Aplikasi > Aplikasi Default > Aplikasi Home. Pilih HOME.iO TV dari daftar.")
            HelpText("Pengguna HP/Tablet:")
            HelpText("Buka Setelan (Settings) > Aplikasi > Aplikasi Default (atau Aplikasi Layar Utama / Home). Pilih HOME.iO TV.")
        }

        // 3. LAUNCHER VIA ADB
        HelpCard(icon = Icons.Default.Terminal, title = "3. JADIKAN LAUNCHER (VIA ADB)") {
            HelpText("Jika sistem memblokir menu default di atas, gunakan ADB:")
            HelpText("1. Buka Pengaturan > Tentang Perangkat > Ketuk 7x 'Nomor Bentukan/Build' untuk membuka Opsi Pengembang.")
            HelpText("2. Nyalakan 'USB Debugging' atau 'Wireless Debugging'.")
            HelpText("3. Hubungkan perangkat ke PC/HP lain yang memiliki aplikasi ADB.")
            HelpText("4. Salin dan jalankan perintah ini:")
            
            // Kotak Kode ADB
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(8.dp)).padding(12.dp)) {
                Text("adb shell cmd package set-home-activity dev.andikune.homeio/.MainActivity", color = GlassColors.AccentGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(Modifier.height(8.dp))
            
            var isFocusedCopy by remember { mutableStateOf(false) }
            NeonFocusBorder(focused = isFocusedCopy, cornerRadius = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (isFocusedCopy) GlassColors.AccentGlow else GlassColors.GlassTint)
                    .onFocusChanged { isFocusedCopy = it.isFocused }.focusable()
                    .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { 
                        SoundFeedback.playClick(ctx)
                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", "adb shell cmd package set-home-activity dev.andikune.homeio/.MainActivity"))
                        Toast.makeText(ctx.applicationContext, "Perintah ADB disalin", Toast.LENGTH_SHORT).show()
                        true 
                    } else false }
                    .clickable { 
                        SoundFeedback.playClick(ctx)
                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", "adb shell cmd package set-home-activity dev.andikune.homeio/.MainActivity"))
                        Toast.makeText(ctx.applicationContext, "Perintah ADB disalin", Toast.LENGTH_SHORT).show()
                    }.padding(10.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, null, tint = if (isFocusedCopy) GlassColors.DeepBackground else Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SALIN PERINTAH ADB", color = if (isFocusedCopy) GlassColors.DeepBackground else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // 4. CARA PAKAI
        HelpCard(icon = Icons.Default.Gamepad, title = "4. CARA MENGGUNAKAN (TV vs HP)") {
            HelpText("Di Android TV (Remote): Navigasi menggunakan D-Pad. Tekan OK untuk memilih. Tekan dan Tahan OK pada ikon aplikasi untuk memunculkan menu (Paksa Hentikan / Hapus).")
            HelpText("Di HP/Tablet (Layar Sentuh): Ketuk ikon aplikasi untuk membuka. Tekan dan Tahan (Long Press) untuk memunculkan menu opsi.")
        }

        // 5. KIOSK
        HelpCard(icon = Icons.Default.Warning, title = "5. CARA KELUAR DARI MODE KIOSK (PENTING)") {
            HelpText("Jika aplikasi terkunci di Mode Kiosk dan Anda tidak bisa keluar:", isHighlight = true)
            HelpText("1. Tekan ikon Tameng Merah (Shield) di pojok kiri atas layar Home.")
            HelpText("2. Atau, buka menu Kendali Sistem (ikon Dashboard) lalu tekan ikon Tameng Merah yang menyala.")
            HelpText("3. Kiosk Mode akan langsung mati, dan Anda bisa menekan tombol Home di remote untuk keluar.")
        }

        // 6. TROUBLESHOOTING
        HelpCard(icon = Icons.Default.Build, title = "6. TROUBLESHOOTING (MASALAH UMUM)") {
            HelpText("- Layar TV menjadi Hitam/Blank? Kemungkinan besar perangkat Anda tidak kuat memutar video wallpaper. Masuk ke Pengaturan aplikasi dan ubah Wallpaper ke mode Gambar/Statis.")
            HelpText("- Aplikasi Crash/Force Close? Aplikasi memiliki sistem pendeteksi error. Jika layar memerah, tekan 'Copy Log' dan kirimkan teks tersebut ke Developer via GitHub.")
            HelpText("- Tombol Remote Tidak Berfungsi? Buka Pengaturan Aplikasi > Izin Aplikasi. Pastikan semua izin sudah diberikan.")
        }

        // 7. BANTUAN LAIN (MEDSOS)
        HelpCard(icon = Icons.Default.SupportAgent, title = "7. BUTUH BANTUAN LAIN?") {
            HelpText("Jika panduan di atas belum menjawab masalah Anda, silakan hubungi Developer melalui media sosial berikut:")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SocialBtn("WhatsApp", Color(0xFF25D366), Modifier.weight(1f)) { openUrl(ctx, AppKnowledge.SOCIAL_WA) }
                SocialBtn("TikTok", Color(0xFFEE1D52), Modifier.weight(1f)) { openUrl(ctx, AppKnowledge.SOCIAL_TIKTOK) }
                SocialBtn("Instagram", Color(0xFFE1306C), Modifier.weight(1f)) { openUrl(ctx, AppKnowledge.SOCIAL_IG) }
            }
        }

        // 8. INFO APLIKASI (Paling Bawah)
        HelpCard(icon = Icons.Default.Info, title = "8. INFORMASI APLIKASI") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Versi Aplikasi", color = GlassColors.TextSecondary, fontSize = 12.sp)
                Text(BuildConfig.VERSION_NAME, color = GlassColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Package ID", color = GlassColors.TextSecondary, fontSize = 12.sp)
                Text("dev.andikune.homeio", color = GlassColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Developer", color = GlassColors.TextSecondary, fontSize = 12.sp)
                Text("Andi (andikune)", color = GlassColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HelpCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GlassColors.GlassTint).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun HelpText(text: String, isHighlight: Boolean = false) {
    Text(
        text = text,
        color = if (isHighlight) GlassColors.AccentAmber else GlassColors.TextPrimary,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SocialBtn(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    NeonFocusBorder(focused = isFocused, cornerRadius = 10.dp, modifier = modifier) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(if (isFocused) color.copy(alpha = 0.4f) else color.copy(alpha = 0.15f))
                .border(if (isFocused) 2.dp else 1.dp, color, RoundedCornerShape(10.dp))
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { onClick(); true } else false }
                .clickable { onClick() }.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

private fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (e: Exception) {
        Toast.makeText(ctx.applicationContext, "Tidak bisa membuka link", Toast.LENGTH_SHORT).show()
    }
}
