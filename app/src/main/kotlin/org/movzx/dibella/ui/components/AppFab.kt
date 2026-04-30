package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.movzx.dibella.R

@Composable
fun AppFab(
    gridState: LazyStaggeredGridState,
    isLoading: Boolean = false,
    hasMore: Boolean = false,
    showRefresh: Boolean = false,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
    val showScrollToBottom by remember { derivedStateOf { gridState.canScrollForward } }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        if (showScrollToTop) {
            FloatingActionButton(
                onClick = { scope.launch { gridState.animateScrollToItem(0) } },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    stringResource(R.string.desc_scroll_to_top),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (showRefresh && !isLoading && !showScrollToTop) {
            FloatingActionButton(
                onClick = onRefresh,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.Refresh,
                    stringResource(R.string.desc_refresh),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (showScrollToBottom) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        gridState.animateScrollToItem(gridState.layoutInfo.totalItemsCount - 1)
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.ArrowDownward,
                    stringResource(R.string.desc_scroll_to_bottom),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (hasMore && !isLoading && !showScrollToBottom) {
            FloatingActionButton(
                onClick = onLoadMore,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowDown,
                    stringResource(R.string.desc_load_more),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
