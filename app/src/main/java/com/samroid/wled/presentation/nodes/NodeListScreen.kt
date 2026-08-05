package com.samroid.wled.presentation.nodes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.R
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.presentation.theme.AppColors.Brand.Green
import com.samroid.wled.presentation.theme.AppColors.Brand.Red

@Composable
fun NodeListScreen(
    onOpenNode: (Int) -> Unit,
    onOpenControl: (Int) -> Unit = {},
    onAddNode: () -> Unit = {},
    viewModel: NodeListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.bluetoothConnected) {
        if (state.bluetoothConnected && state.nodes.isEmpty()) {
            viewModel.refresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.nodes),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineLarge
            )
            Row {
                IconButton(
                    onClick = viewModel::refresh,
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onAddNode) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.provision),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (!state.bluetoothConnected) {
            Text(
                stringResource(R.string.for_seeing_nodes_bluetooth_must_be_connected),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (!state.message.isNullOrBlank()) {
            Text(
                state.message.orEmpty(),
                color = Green,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!state.isLoading && state.nodes.isEmpty() && state.bluetoothConnected) {
            Text(
                stringResource(R.string.no_nodes_found),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.nodes, key = { it.nodeId }) { node ->
                NodeCard(
                    node = node,
                    onOpenInfo = { onOpenNode(node.nodeId) },
                    onOpenControl = { onOpenControl(node.nodeId) }
                )
            }
        }
    }
}

@Composable
private fun NodeCard(
    node: NodeListItem,
    onOpenInfo: () -> Unit,
    onOpenControl: () -> Unit
) {
    val title = node.nodeName.ifBlank { stringResource(R.string.node, node.nodeId) }
    val subtitle = stringResource(R.string.id_nodeid, node.nodeId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpenInfo)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Memory, null, tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(node.online)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(onClick = onOpenControl) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = stringResource(R.string.control),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusBadge(online: Boolean) {
    Text(
        text = if (online) stringResource(R.string.online) else stringResource(R.string.offline),
        color = if (online) Green else Red,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (online) Green.copy(alpha = 0.12f) else Red.copy(alpha = 0.12f)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}