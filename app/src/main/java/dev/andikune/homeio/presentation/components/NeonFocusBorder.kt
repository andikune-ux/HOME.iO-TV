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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NeonFocusBorder(
    focused: Boolean,
    pressed: Boolean = false,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    glowRadius: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    // FIX UI: Menggunakan animasi Spring agar perpindahan skala terasa lebih "hidup" dan mewah
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "focus_scale"
    )

    val rotation = remember { Animatable(0f) }

    LaunchedEffect(focused) {
        if (!focused) {
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, tween(3000, easing = LinearEasing)) // Diperlambat jadi 3 detik agar elegan
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

    Box(modifier = modifier.scale(scale)) {
        if (focused) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawGlowBorder(
                    canvasSize = size,
                    strokePx = borderWidth.toPx(),
                    glowPx = glowRadius.toPx(),
                    radiusPx = cornerRadius.toPx(),
                    rotationDeg = rotation.value,
                    isPressed = pressed,
                    flash = flashAlpha.value
                )
            }
        }
        content()
    }
}

private fun DrawScope.drawGlowBorder(
    canvasSize: Size,
    strokePx: Float,
    glowPx: Float,
    radiusPx: Float,
    rotationDeg: Float,
    isPressed: Boolean,
    flash: Float
) {
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas

        val coreInset = strokePx / 2f
        val coreRect = RectF(coreInset, coreInset, canvasSize.width - coreInset, canvasSize.height - coreInset)
        val corePaint = AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.STROKE
            strokeWidth = strokePx
            color = if (isPressed) {
                val a = (flash * 255).toInt().coerceIn(0, 255)
                val c = GlassColors.AccentGlow
                android.graphics.Color.argb(a, (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
            } else {
                val c = GlassColors.AccentGlow
                android.graphics.Color.argb(80, (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
            }
        }
        native.drawRoundRect(coreRect, radiusPx, radiusPx, corePaint)

        val glowOutset = glowPx * 0.4f
        val glowRect = RectF(-glowOutset, -glowOutset, canvasSize.width + glowOutset, canvasSize.height + glowOutset)
        val glowPath = Path().apply {
            addRoundRect(glowRect, radiusPx + glowOutset, radiusPx + glowOutset, Path.Direction.CW)
        }

        val pathMeasure = PathMeasure(glowPath, false)
        val pathLength = pathMeasure.length

        val headColors = if (isPressed) listOf(android.graphics.Color.WHITE) else {
            val blue = GlassColors.AccentGlow
            val lightBlue = GlassColors.AccentGlowLight
            listOf(
                android.graphics.Color.argb(255, (lightBlue.red * 255).toInt(), (lightBlue.green * 255).toInt(), (lightBlue.blue * 255).toInt()),
                android.graphics.Color.argb(255, (blue.red * 255).toInt(), (blue.green * 255).toInt(), (blue.blue * 255).toInt())
            )
        }

        val tailLenFraction = 0.35f // Ekor lebih panjang agar lebih elegan
        val dotCount = 32 
        val blurRadius = glowPx * 1.5f

        headColors.forEachIndexed { idx, color ->
            val baseT = rotationDeg / 360f
            val headT = if (isPressed) baseT else (baseT + idx * 0.5f) % 1f

            for (i in 0 until dotCount) {
                val t = headT - (i.toFloat() / dotCount) * tailLenFraction
                val wrapped = ((t % 1f) + 1f) % 1f
                val pos = FloatArray(2)
                pathMeasure.getPosTan(wrapped * pathLength, pos, null)

                val linear = 1f - (i.toFloat() / dotCount)
                val alphaBase = linear * linear * 0.8f + linear * 0.2f
                val alpha = if (isPressed) alphaBase * flash else alphaBase
                val dotRadius = glowPx * (0.35f + 0.45f * linear)

                val paint = AndroidPaint().apply {
                    isAntiAlias = true
                    maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                    shader = RadialGradient(
                        pos[0], pos[1], dotRadius,
                        intArrayOf(
                            colorWithAlpha(color, alpha * 1.0f),
                            colorWithAlpha(color, alpha * 0.6f),
                            colorWithAlpha(color, alpha * 0.2f),
                            colorWithAlpha(color, 0f)
                        ),
                        floatArrayOf(0f, 0.3f, 0.7f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                native.drawCircle(pos[0], pos[1], dotRadius, paint)
            }
        }
    }
}

private fun colorWithAlpha(color: Int, alpha: Float): Int {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    return (color and 0x00FFFFFF) or (a shl 24)
}
