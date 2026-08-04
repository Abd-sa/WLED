package com.samroid.wled.presentation.components.core

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {

    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {

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


@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SecondaryButtonPreview() {

    WLEDTheme {

        SecondaryButton(
            text = "Cancel",
            onClick = {},
            leadingIcon = Icons.Rounded.Close
        )

    }

}


@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SecondaryButtonDarkPreview() {

    WLEDTheme {

        SecondaryButton(
            text = "Cancel",
            onClick = {},
            leadingIcon = Icons.Rounded.Close
        )

    }

}