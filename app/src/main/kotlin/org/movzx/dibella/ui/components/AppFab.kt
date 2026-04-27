package org.movzx.dibella.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        AnimatedVisibility(
            visible = showRefresh && !isLoading && !showScrollToTop,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            FloatingActionButton(
                onClick = onRefresh,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Refresh, stringResource(R.string.desc_refresh))
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            FloatingActionButton(
                onClick = { scope.launch { gridState.animateScrollToItem(0) } },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(Icons.Default.ArrowUpward, stringResource(R.string.desc_scroll_to_top))
            }
        }

        AnimatedVisibility(
            visible = showScrollToBottom,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        gridState.animateScrollToItem(gridState.layoutInfo.totalItemsCount - 1)
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(Icons.Default.ArrowDownward, stringResource(R.string.desc_scroll_to_bottom))
            }
        }

        AnimatedVisibility(
            visible = hasMore && !isLoading && !showScrollToBottom,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            FloatingActionButton(
                onClick = onLoadMore,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowDown, stringResource(R.string.desc_load_more))
            }
        }
    }
}
