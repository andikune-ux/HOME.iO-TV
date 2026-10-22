package dev.andikune.homeio.presentation.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private enum class BorderPhase {
    HIDDEN, IDLE, PRESSED
}

@Composable
fun NeonFocusBorder(
    focused: Boolean,
    pressed: Boolean = false,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
    glowRadius: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    var currentPhase by remember { mutableStateOf(BorderPhase.HIDDEN) }

    LaunchedEffect(focused) {
        if (!focused) {
            currentPhase = BorderPhase.HIDDEN
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        currentPhase = BorderPhase.IDLE
        while (true) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, tween(2500, easing = LinearEasing))
        }
    }

    val flashAlpha = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            while (true) {
                flashAlpha.animateTo(1f, tween(100, easing = LinearEasing))
                flashAlpha.animateTo(0f, tween(100, easing = LinearEasing))
            }
        } else {
            flashAlpha.snapTo(1f)
        }
    }

    Box(modifier = modifier) {
        if (focused) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // OPTIMASI: Hanya buat objek Paint sekali di luar loop
                val corePaint = AndroidPaint().apply {
                    isAntiAlias = true
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = borderWidth.toPx()
                }
                val glowPaint = AndroidPaint().apply {
                    isAntiAlias = true
                    maskFilter = BlurMaskFilter(glowRadius.toPx() * 1.5f, BlurMaskFilter.Blur.NORMAL)
                }

                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas

                    // 1. CORE BORDER
                    val coreInset = borderWidth.toPx() / 2f
                    val coreRect = RectF(coreInset, coreInset, size.width - coreInset, size.height - coreInset)
                    corePaint.color = if (pressed) {
                        val a = (flashAlpha.value * 255).toInt().coerceIn(0, 255)
                        (0xFF80C3FF.toInt() and 0x00FFFFFF) or (a shl 24)
                    } else {
                        0x334DA3FF
                    }
                    native.drawRoundRect(coreRect, cornerRadius.toPx(), cornerRadius.toPx(), corePaint)

                    // 2. GLOW TAIL
                    val glowOutset = glowRadius.toPx() * 0.4f
                    val glowRect = RectF(-glowOutset, -glowOutset, size.width + glowOutset, size.height + glowOutset)
                    val glowPath = Path().apply {
                        addRoundRect(glowRect, cornerRadius.toPx() + glowOutset, cornerRadius.toPx() + glowOutset, Path.Direction.CW)
                    }

                    val pathMeasure = PathMeasure(glowPath, false)
                    val pathLength = pathMeasure.length

                    val headColors = if (pressed) listOf(0xFF80C3FF.toInt()) else listOf(0xFFFFFFFF.toInt(), 0xFF80C3FF.toInt())
                    val tailLenFraction = 0.30f
                    val dotCount = 24 // Dikurangi dari 48 ke 24 agar lebih ringan

                    headColors.forEachIndexed { idx, color ->
                        val baseT = rotation.value / 360f
                        val headT = if (pressed) baseT else (baseT + idx * 0.5f) % 1f

                        for (i in 0 until dotCount) {
                            val t = headT - (i.toFloat() / dotCount) * tailLenFraction
                            val wrapped = ((t % 1f) + 1f) % 1f
                            val pos = FloatArray(2)
                            pathMeasure.getPosTan(wrapped * pathLength, pos, null)

                            val linear = 1f - (i.toFloat() / dotCount)
                            val alphaBase = linear * linear * 0.7f + linear * 0.3f
                            val alpha = if (pressed) alphaBase * flashAlpha.value else alphaBase
                            val dotRadius = glowRadius.toPx() * (0.35f + 0.35f * linear)

                            glowPaint.shader = RadialGradient(
                                pos[0], pos[1], dotRadius,
                                intArrayOf(
                                    colorWithAlpha(color, alpha * 1.0f),
                                    colorWithAlpha(color, alpha * 0.7f),
                                    colorWithAlpha(color, alpha * 0.3f),
                                    colorWithAlpha(color, 0f)
                                ),
                                floatArrayOf(0f, 0.3f, 0.7f, 1f),
                                Shader.TileMode.CLAMP
                            )
                            native.drawCircle(pos[0], pos[1], dotRadius, glowPaint)
                        }
                    }
                }
            }
        }
        content()
    }
}

private fun colorWithAlpha(color: Int, alpha: Float): Int {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    return (color and 0x00FFFFFF) or (a shl 24)
}
