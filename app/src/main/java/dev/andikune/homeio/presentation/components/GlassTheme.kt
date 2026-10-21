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
    val DeepBackground = Color(0xFF0A0E1A)
    // Tint kaca transparan (untuk fallback di bawah API 31)
    val GlassTint = Color(0x26FFFFFF) // 15% white frosted tint
    val GlassSolidFallback = Color(0xCC1A1A2E) // Low-end solid fallback
    
    val GlassBorder = Color(0x33FFFFFF) // 20% white border
    val AccentGlow = Color(0xFF4DA3FF) // Neon Cyan Blue glow
    val FocusedBorder = Color(0xFF4DA3FF)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val SurfaceDark = Color(0xFF111827)
    val AccentGreen = Color(0xFF10B981)
    val AccentRed = Color(0xFFEF4444)
    val AccentAmber = Color(0xFFF59E0B)

    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0E1A),
            Color(0xFF0F172A),
            Color(0xFF0A0E1A)
        )
    )
}

/**
 * iOS 26 Liquid Glass card modifier.
 * Mendukung efek blur asli (API 31+) dan fallback solid transparan (API < 31).
 */
@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = 28.dp,
    isFocused: Boolean = false,
    useBlur: Boolean = true,
    borderWidth: Dp = 1.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    
    // Cek apakah perangkat mendukung RenderEffect (API 31+)
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    
    // Warna background: Jika blur aktif, gunakan tint transparan. Jika tidak, gunakan solid.
    val backgroundColor = if (useBlur && supportsBlur) GlassColors.GlassTint else GlassColors.GlassSolidFallback
    
    val currentBorderColor = if (isFocused) GlassColors.AccentGlow else GlassColors.GlassBorder
    val effectiveBorderWidth = if (isFocused) 2.dp else borderWidth

    return this
        .clip(shape)
        .then(
            if (isFocused) {
                Modifier.drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GlassColors.AccentGlow.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                }
            } else Modifier
        )
        .then(
            // Efek blur asli hanya jika API >= 31 dan useBlur = true
            if (useBlur && supportsBlur) Modifier.blur(20.dp) else Modifier
        )
        .background(backgroundColor, shape)
        .border(
            BorderStroke(effectiveBorderWidth, currentBorderColor),
            shape
        )
}
