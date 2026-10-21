package dev.andikune.homeio.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NeonFocusBorder(
    focused: Boolean,
    pressed: Boolean = false,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
    glowRadius: Dp = 8.dp, // Tidak dipakai, untuk backward compatibility
    content: @Composable () -> Unit
) {
    val scale = if (focused) 1.05f else 1f
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    
    // FIX: Animasi alpha ringan, tidak membebani GPU
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val borderColor = if (focused) {
        GlassColors.AccentGlow.copy(alpha = if (pressed) 1f else glowAlpha)
    } else Color.Transparent

    Box(
        modifier = modifier
            .scale(scale)
            .border(
                width = if (focused) borderWidth else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}
