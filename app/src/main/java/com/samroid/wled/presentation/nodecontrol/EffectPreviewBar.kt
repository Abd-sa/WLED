package com.samroid.wled.presentation.nodecontrol

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samroid.wled.domain.wled.EffectPreviewEngine

/**
 * Pixel-strip effect preview (WLED Liveview style).
 */
@Composable
fun EffectPreviewBar(
    effectId: Int,
    paletteId: Int = 6,
    speed: Float = 128f,
    intensity: Float = 128f,
    primary: Color = Color(0xFFFFAA00),
    secondary: Color = Color(0xFF000000),
    modifier: Modifier = Modifier,
    height: Dp = 22.dp,
    ledCount: Int = 48,
    animate: Boolean = true
) {
    val time by if (animate) {
        val tr = rememberInfiniteTransition(label = "fxTime")
        // 0..1000s linear — engine uses continuous seconds
        tr.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_000_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "t"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val params = remember(effectId, paletteId, speed, intensity, primary, secondary, ledCount) {
        EffectPreviewEngine.Params(
            effectId = effectId,
            paletteId = paletteId,
            speed = speed,
            intensity = intensity,
            primary = primary,
            secondary = secondary,
            ledCount = ledCount
        )
    }

    // Recompute colors every frame from animated time
    val colors = remember(params, time) {
        EffectPreviewEngine.render(params, timeSec = time)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || colors.isEmpty()) return@Canvas
        val slice = w / colors.size
        colors.forEachIndexed { i, c ->
            drawRect(
                color = c,
                topLeft = Offset(i * slice, 0f),
                size = Size(slice + 1f, h)
            )
        }
    }
}