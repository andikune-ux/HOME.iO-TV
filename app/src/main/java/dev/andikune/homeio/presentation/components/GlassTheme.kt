package dev.andikune.homeio.presentation.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GlassColors {
    // Latar belakang lebih gelap dan dalam
    val DeepBackground = Color(0xFF050810)
    val DeepBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0E1A),
            Color(0xFF0F172A),
            Color(0xFF050810)
        )
    )
    
    // Tint kaca yang lebih realistis
    val GlassTint = Color(0x1AFFFFFF) // 10% white
    val GlassTintLighter = Color(0x33FFFFFF) // 20% white (saat fokus)
    val GlassSolidFallback = Color(0xE60F172A) 
    
    val GlassBorder = Color(0x26FFFFFF) // 15% white
    val AccentGlow = Color(0xFF4DA3FF) // Neon Cyan Blue
    val AccentGlowLight = Color(0xFF80C3FF)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val SurfaceDark = Color(0xFF1E293B)
    val AccentGreen = Color(0xFF10B981)
    val AccentRed = Color(0xFFEF4444)
    val AccentAmber = Color(0xFFF59E0B)
}

@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = 28.dp,
    isFocused: Boolean = false,
    useBlur: Boolean = true,
    borderWidth: Dp = 1.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    
    // Gunakan tint yang lebih terang saat fokus
    val backgroundColor = if (useBlur && supportsBlur) {
        if (isFocused) GlassTintLighter else GlassTint
    } else GlassSolidFallback
    
    // Border warna biru neon saat fokus, abu transparan saat idle
    val currentBorderColor = if (isFocused) AccentGlow else GlassBorder
    val effectiveBorderWidth = if (isFocused) 2.dp else borderWidth

    return this
        .clip(shape)
        .then(
            // Tambahkan efek glow shadow (inner & outer)
            if (isFocused) {
                Modifier.drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(AccentGlow.copy(alpha = 0.25f), Color.Transparent),
                            radius = size.maxDimension * 0.8f
                        )
                    )
                }
            } else Modifier
        )
        .then(
            if (useBlur && supportsBlur) Modifier.blur(24.dp) else Modifier
        )
        .background(backgroundColor, shape)
        .border(BorderStroke(effectiveBorderWidth, currentBorderColor), shape)
}
