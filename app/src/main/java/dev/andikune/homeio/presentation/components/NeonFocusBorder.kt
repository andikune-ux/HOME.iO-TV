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
    HIDDEN,
    IDLE,
    PRESSED
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
    // SOLUSI BUG: Modifier.scale() dihapus total dari sini.
    // Biarkan container di luar yang mengurus scale agar tidak terjadi double scale.
    val rotation = remember { Animatable(0f) }
    var currentPhase by remember { mutableStateOf(BorderPhase.HIDDEN) }

    // Fokus -> langsung loop 2 warna (Putih & Biru HOME.iO)
    LaunchedEffect(focused) {
        if (!focused) {
            currentPhase = BorderPhase.HIDDEN
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        currentPhase = BorderPhase.IDLE
        while (true) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, tween(2500, easing = LinearEasing)) // 2.5 detik per putaran
        }
    }

    // SOLUSI BUG: Pressed -> kedip ON/OFF (deteksi dari interaksi luar)
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
        // SOLUSI BUG: Canvas hanya digambar jika fokus (hemat RAM/GPU)
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

        // 1. CORE BORDER (Kerangka)
        val coreInset = strokePx / 2f
        val coreRect = RectF(
            coreInset,
            coreInset,
            canvasSize.width - coreInset,
            canvasSize.height - coreInset
        )
        val corePaint = AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.STROKE
            strokeWidth = strokePx
            color = if (isPressed) {
                val a = (flash * 255).toInt().coerceIn(0, 255)
                (0xFF80C3FF.toInt() and 0x00FFFFFF) or (a shl 24) // AccentGlowLight nyala terang
            } else {
                0x334DA3FF // AccentGlow redup (20%)
            }
        }
        native.drawRoundRect(coreRect, radiusPx, radiusPx, corePaint)

        // 2. GLOW TAIL (Animasi Berputar)
        val glowOutset = glowPx * 0.4f
        val glowRect = RectF(
            -glowOutset,
            -glowOutset,
            canvasSize.width + glowOutset,
            canvasSize.height + glowOutset
        )
        val glowPath = Path().apply {
            addRoundRect(
                glowRect,
                radiusPx + glowOutset,
                radiusPx + glowOutset,
                Path.Direction.CW
            )
        }

        val pathMeasure = PathMeasure(glowPath, false)
        val pathLength = pathMeasure.length

        // Warna HOME.iO: Putih dan Biru AccentGlowLight
        val headColors = if (isPressed) {
            listOf(0xFF80C3FF.toInt()) // Biru terang
        } else {
            listOf(0xFFFFFFFF.toInt(), 0xFF80C3FF.toInt()) // Putih & Biru
        }

        val tailLenFraction = 0.30f
        val dotCount = 48
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
                val alphaBase = linear * linear * 0.7f + linear * 0.3f
                val alpha = if (isPressed) alphaBase * flash else alphaBase

                val dotRadius = glowPx * (0.35f + 0.35f * linear)

                val paint = AndroidPaint().apply {
                    isAntiAlias = true
                    maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                    shader = RadialGradient(
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
