package com.samroid.wled.presentation.nodecontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samroid.wled.domain.wled.WledPalette
import com.samroid.wled.domain.wled.WledPaletteCatalog

@Composable
fun PalettePreviewBar(
    paletteId: Int,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    segments: Int = 24
) {
    val colors = remember(paletteId, segments) {
        WledPaletteCatalog.get(paletteId).sampleColors(segments)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
    ) {
        colors.forEach { c ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(c)
            )
        }
    }
}

@Composable
fun PalettePreviewBar(
    palette: WledPalette,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    segments: Int = 24
) {
    val colors = remember(palette.id, segments) { palette.sampleColors(segments) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
    ) {
        colors.forEach { c ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(c)
            )
        }
    }
}