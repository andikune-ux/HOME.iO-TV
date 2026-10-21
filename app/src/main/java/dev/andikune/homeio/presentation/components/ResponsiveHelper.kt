package dev.andikune.homeio.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ResponsiveHelper — Helper Auto-Scale UI untuk TV & HP.
 *
 * Masalah: 1 Dp di TV 32 inci (720p) dan 1 Dp di TV 65 inci (4K) ukuran fisiknya beda jauh.
 * Solusi: Deteksi ukuran layar, hitung rasio terhadap base 1080p, kalikan semua ukuran.
 */

// Base design kita adalah TV 1920x1080 (FHD), yang kira-kira setara 960dp x 540dp
private const val BASE_WIDTH_DP = 960f
private const val BASE_HEIGHT_DP = 540f

data class ScreenInfo(
    val screenWidthDp: Float,
    val screenHeightDp: Float,
    val scaleFactor: Float,
    val isTv: Boolean
)

val LocalScreenInfo = compositionLocalOf<ScreenInfo> {
    error("ScreenInfo belum dihitung. Gunakan ResponsiveProvider { } di root aplikasi.")
}

@Composable
fun rememberScreenInfo(): ScreenInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.toFloat()
    val heightDp = configuration.screenHeightDp.toFloat()

    // Hitung rasio (scale factor) dengan base 960x540
    // Gunakan minOf agar UI tidak terlalu besar jika layar HP memanjang
    val rawScale = minOf(widthDp / BASE_WIDTH_DP, heightDp / BASE_HEIGHT_DP)
    
    // Batasi scale minimal 0.6x dan maksimal 2.5x agar UI tidak terlalu kecil/besar
    val scale = rawScale.coerceIn(0.6f, 2.5f)

    val isTv = configuration.smallestScreenWidthDp >= 600

    return ScreenInfo(
        screenWidthDp = widthDp,
        screenHeightDp = heightDp,
        scaleFactor = scale,
        isTv = isTv
    )
}

@Composable
fun ResponsiveProvider(content: @Composable () -> Unit) {
    val screenInfo = rememberScreenInfo()
    CompositionLocalProvider(LocalScreenInfo provides screenInfo) {
        content()
    }
}

// ============================================================
// EXTENSION UNTUK UKURAN
// ============================================================

/**
 * Mengubah angka Dp statis menjadi Dp yang sudah di-scale.
 * Contoh: 16.scaledDp() -> jika scale 1.5x, hasilnya jadi 24.dp
 */
@Composable
fun Dp.scaled(): Dp {
    val info = LocalScreenInfo.current
    return (this.value * info.scaleFactor).dp
}

/**
 * Mengubah angka TextUnit (Sp) statis menjadi Sp yang sudah di-scale.
 * Contoh: 14.scaledSp() -> jika scale 1.5x, hasilnya jadi 21.sp
 */
@Composable
fun TextUnit.scaled(): TextUnit {
    val info = LocalScreenInfo.current
    return (this.value * info.scaleFactor).sp
}

/**
 * Modifier untuk memberikan padding responsif.
 */
@Composable
fun Modifier.responsivePadding(all: Dp): Modifier {
    val info = LocalScreenInfo.current
    return this.padding((all.value * info.scaleFactor).dp)
}

@Composable
fun Modifier.responsivePadding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier {
    val info = LocalScreenInfo.current
    return this.padding(
        horizontal = (horizontal.value * info.scaleFactor).dp,
        vertical = (vertical.value * info.scaleFactor).dp
    )
}
