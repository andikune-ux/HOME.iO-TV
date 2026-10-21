package dev.andikune.homeio.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    // FIX: Animasi scale ringan (tanpa shadow yang membebani GPU)
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "toggle_scale"
    )

    val borderColor = when {
        !enabled -> Color(0x33FFFFFF)
        isHighlighted -> GlassColors.AccentGlow
        else -> GlassColors.GlassBorder
    }

    val bgColor = when {
        isHighlighted -> GlassColors.GlassTintLighter
        !enabled -> Color(0x11000000)
        else -> GlassColors.GlassTint
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isHighlighted) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
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
