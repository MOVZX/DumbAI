package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.movzx.dibella.R

@Composable
fun AppFab(
    gridState: LazyStaggeredGridState,
    isLoading: Boolean = false,
    hasMore: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current
    val showScrollToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
    val showScrollToBottom by remember { derivedStateOf { gridState.canScrollForward } }

    val fabScale by
        animateFloatAsState(
            targetValue = 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "fabScale",
        )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        if (showScrollToTop) {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                    scope.launch { gridState.animateScrollToItem(0) }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation =
                    FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp,
                    ),
                modifier =
                    Modifier.size(48.dp).graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    },
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    stringResource(R.string.desc_scroll_to_top),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (showScrollToBottom) {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                    scope.launch {
                        gridState.animateScrollToItem(gridState.layoutInfo.totalItemsCount - 1)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation =
                    FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp,
                    ),
                modifier =
                    Modifier.size(48.dp).graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    },
            ) {
                Icon(
                    Icons.Default.ArrowDownward,
                    stringResource(R.string.desc_scroll_to_bottom),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
