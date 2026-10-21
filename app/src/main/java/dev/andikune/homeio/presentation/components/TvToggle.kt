package dev.andikune.homeio.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TvToggle(
    label: String,
    description: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHighlighted = (isFocused || isPressed) && enabled

    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "toggle_scale"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isHighlighted) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "toggle_border_width"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isHighlighted) 10.dp else 0.dp,
        animationSpec = tween(200),
        label = "toggle_shadow"
    )

    val borderColor = when {
        !enabled -> Color(0x33FFFFFF)
        isHighlighted -> GlassColors.AccentGlow
        else -> GlassColors.GlassBorder
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(14.dp),
                ambientColor = GlassColors.AccentGlow.copy(alpha = 0.5f),
                spotColor = GlassColors.AccentGlow.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isHighlighted -> GlassColors.GlassTint.copy(alpha = 0.5f)
                    !enabled -> Color(0x11000000)
                    else -> GlassColors.GlassTint
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onToggle(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    !enabled -> GlassColors.TextSecondary.copy(alpha = 0.5f)
                    isHighlighted -> GlassColors.AccentGlow
                    else -> GlassColors.TextPrimary
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = GlassColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        !enabled -> Color(0x33FFFFFF)
                        isChecked -> GlassColors.AccentGreen
                        else -> Color(0x55FFFFFF)
                    }
                )
                .padding(3.dp),
            contentAlignment = if (isChecked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White)
            )
        }
    }
}
