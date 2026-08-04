package com.samroid.wled.presentation.components
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun AppScaffold(

    modifier: Modifier = Modifier,

    topBar: @Composable () -> Unit = {},

    bottomBar: @Composable () -> Unit = {},

    floatingActionButton: @Composable () -> Unit = {},

    content: @Composable (paddingValues: androidx.compose.foundation.layout.PaddingValues) -> Unit

) {


    Scaffold(

        modifier = modifier,

        topBar = topBar,

        bottomBar = bottomBar,

        floatingActionButton = floatingActionButton,

        content = content

    )

}