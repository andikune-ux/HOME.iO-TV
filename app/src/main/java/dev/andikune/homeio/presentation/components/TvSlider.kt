package dev.andikune.homeio.presentation.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun TvSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    unit: String = "",
    formatter: ((Float) -> String)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var isSliderActive by remember { mutableStateOf(false) }

    val stepSize: Float = if (steps > 0) {
        (valueRange.endInclusive - valueRange.start) / (steps + 1)
    } else {
        (valueRange.endInclusive - valueRange.start) / 20f
    }.coerceAtLeast(0.001f)

    fun slideLeft() {
        val rawNew = value - stepSize
        val nextVal = if (steps > 0) {
            val stepIdx = ((rawNew - valueRange.start) / stepSize).roundToInt()
            (valueRange.start + stepIdx * stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
        } else rawNew.coerceIn(valueRange.start, valueRange.endInclusive)
        onValueChange(nextVal)
    }

    fun slideRight() {
        val rawNew = value + stepSize
        val nextVal = if (steps > 0) {
            val stepIdx = ((rawNew - valueRange.start) / stepSize).roundToInt()
            (valueRange.start + stepIdx * stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
        } else rawNew.coerceIn(valueRange.start, valueRange.endInclusive)
        onValueChange(nextVal)
    }

    val displayValue = formatter?.invoke(value) ?: "${value.toInt()}$unit"

    NeonFocusBorder(
        focused = isFocused,
        pressed = isSliderActive,
        cornerRadius = 14.dp,
        borderWidth = 3.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSliderActive) GlassColors.AccentGreen.copy(alpha = 0.15f) else GlassColors.GlassTint)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (!it.isFocused) isSliderActive = false
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val keyCode = event.key.nativeKeyCode
                        when {
                            event.key == Key.DirectionCenter || event.key == Key.Enter || keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER || keyCode == AndroidKeyEvent.KEYCODE_ENTER -> {
                                isSliderActive = !isSliderActive; true
                            }
                            event.key == Key.DirectionLeft || keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                isSliderActive = true; slideLeft(); true
                            }
                            event.key == Key.DirectionRight || keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                isSliderActive = true; slideRight(); true
                            }
                            else -> false
                        }
                    } else false
                }
                .focusable()
                .clickable { isSliderActive = !isSliderActive }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = when { isSliderActive -> GlassColors.AccentGreen; isFocused -> GlassColors.AccentGlow; else -> GlassColors.TextPrimary }
                )
                Text(
                    text = displayValue, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = when { isSliderActive -> GlassColors.AccentGreen; isFocused -> GlassColors.AccentGlow; else -> GlassColors.TextPrimary }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(valueRange) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isSliderActive = true
                            val initialFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                            onValueChange(valueRange.start + initialFraction * (valueRange.endInclusive - valueRange.start))
                            down.consume()
                            var pointer = down
                            while (pointer.pressed) {
                                val event = awaitPointerEvent()
                                pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (pointer.positionChanged()) {
                                    val fraction = (pointer.position.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                                    pointer.consume()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x33FFFFFF)))
                val rangeSpan = valueRange.endInclusive - valueRange.start
                val progressFraction = if (rangeSpan > 0f) ((value - valueRange.start) / rangeSpan).coerceIn(0.005f, 1f) else 0.005f
                Box(Modifier.fillMaxWidth(progressFraction).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (isSliderActive) GlassColors.AccentGreen else GlassColors.AccentGlow))
                Box(Modifier.fillMaxWidth(progressFraction).height(28.dp), contentAlignment = Alignment.CenterEnd) {
                    Box(Modifier.size(if (isSliderActive) 30.dp else 26.dp).clip(CircleShape).background(when { isSliderActive -> GlassColors.AccentGreen; isFocused -> GlassColors.AccentGlow; else -> GlassColors.TextSecondary }))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).clickable { isSliderActive = true; slideLeft() }.padding(4.dp)) {
                        Text("◀", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSliderActive) GlassColors.AccentGreen else GlassColors.TextSecondary)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSliderActive) "Geser KIRI-KANAN" else "OK / KIRI-KANAN", fontSize = 11.sp, color = if (isSliderActive) GlassColors.AccentGreen else GlassColors.TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).clickable { isSliderActive = true; slideRight() }.padding(4.dp)) {
                        Text("▶", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSliderActive) GlassColors.AccentGreen else GlassColors.TextSecondary)
                    }
                }
                if (isSliderActive) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(GlassColors.AccentGreen.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("AKTIF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGreen)
                    }
                }
            }
        }
    }
}
