package com.samroid.wled.presentation.components.core


import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samroid.wled.presentation.theme.AppDimens
import com.samroid.wled.presentation.theme.WLEDTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = Icons.Rounded.ArrowForward
) {

    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.large,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {

        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )

        }

        if (loading) {

            Row(
                modifier = Modifier.padding(start = AppDimens.Space12),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )

            }

        } else {

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8),
                verticalAlignment = Alignment.CenterVertically
            ) {

                leadingIcon?.let {

                    Icon(
                        imageVector = it,
                        contentDescription = null
                    )

                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )

                trailingIcon?.let {

                    Icon(
                        imageVector = it,
                        contentDescription = null
                    )

                }

            }

        }

    }

}
@Preview(showBackground = true,uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun PrimaryButtonPreview() {

    WLEDTheme {

        PrimaryButton(
            text = "Connect",
            onClick = {}
        )

    }

}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PrimaryButtonDarkPreview() {

    WLEDTheme {

        PrimaryButton(
            text = "Connecting...",
            loading = true,
            onClick = {}
        )

    }

}