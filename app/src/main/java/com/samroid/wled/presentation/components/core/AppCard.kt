package com.samroid.wled.presentation.components.core


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.samroid.wled.presentation.theme.AppDimens
import com.samroid.wled.presentation.theme.AppElevation

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = AppDimens.PaddingLarge,
    content: @Composable ColumnScope.() -> Unit
) {

    val interactionSource = remember { MutableInteractionSource() }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateDpAsState(
        targetValue = if (pressed) 0.985.dp else 1.dp,
        animationSpec = tween(
            durationMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "CardScale"
    )

    val elevation by animateDpAsState(
        targetValue = if (pressed)
            AppElevation.Floating
        else
            AppElevation.Card,
        animationSpec = tween(180),
        label = "CardElevation"
    )

    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(300),
        label = "CardColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale.value)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.extraLarge,
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}