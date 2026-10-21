package dev.andikune.homeio.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ResponsiveHelper — Helper Auto-Scale UI untuk TV & HP.
 */
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

    // FIX: Skala dibatasi maksimal 1.5x dan minimal 0.8x agar teks tidak terlalu kecil/besar
    val rawScale = minOf(widthDp / BASE_WIDTH_DP, heightDp / BASE_HEIGHT_DP)
    val scale = rawScale.coerceIn(0.8f, 1.5f)

    // FIX: Deteksi TV yang lebih akurat menggunakan uiMode
    val isTv = (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

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
@Composable
fun Dp.scaled(): Dp {
    val info = LocalScreenInfo.current
    return (this.value * info.scaleFactor).dp
}

@Composable
fun TextUnit.scaled(): TextUnit {
    val info = LocalScreenInfo.current
    return (this.value * info.scaleFactor).sp
}

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
