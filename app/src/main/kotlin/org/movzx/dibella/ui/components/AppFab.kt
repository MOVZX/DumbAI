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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.movzx.dibella.R

@Composable
fun AppNavigationFab(gridState: LazyStaggeredGridState) {
    val scope = rememberCoroutineScope()
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
            FabButton(
                onClick = { scope.launch { gridState.animateScrollToItem(0) } },
                containerColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.ArrowUpward,
                contentDescription = stringResource(R.string.desc_scroll_to_top),
                fabScale = fabScale,
            )
        }

        if (showScrollToBottom) {
            FabButton(
                onClick = {
                    scope.launch {
                        gridState.animateScrollToItem(gridState.layoutInfo.totalItemsCount - 1)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.ArrowDownward,
                contentDescription = stringResource(R.string.desc_scroll_to_bottom),
                fabScale = fabScale,
            )
        }
    }
}

@Composable
fun AppActionFab(
    showBookmarkJump: Boolean = true,
    onJumpClicked: () -> Unit = {},
    onBookmarkClicked: () -> Unit = {},
) {
    if (!showBookmarkJump) return

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
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        FabButton(
            onClick = onBookmarkClicked,
            containerColor = colorResource(R.color.accent_variant),
            icon = Icons.Default.BookmarkAdd,
            contentDescription = stringResource(R.string.desc_add_bookmark),
            fabScale = fabScale,
        )

        FabButton(
            onClick = onJumpClicked,
            containerColor = colorResource(R.color.secondary),
            icon = Icons.Default.FlightTakeoff,
            contentDescription = stringResource(R.string.desc_jump_to_cursor),
            fabScale = fabScale,
        )
    }
}

@Composable
private fun FabButton(
    onClick: () -> Unit,
    containerColor: Color,
    icon: ImageVector,
    contentDescription: String,
    fabScale: Float,
) {
    val view = LocalView.current

    FloatingActionButton(
        onClick = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onClick()
        },
        containerColor = containerColor,
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
        Icon(icon, contentDescription, modifier = Modifier.size(20.dp))
    }
}
