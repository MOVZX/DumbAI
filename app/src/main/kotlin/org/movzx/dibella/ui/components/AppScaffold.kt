package org.movzx.dibella.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    gridState: LazyStaggeredGridState,
    isLoading: Boolean = false,
    hasMore: Boolean = false,
    showRefresh: Boolean = false,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var isBarsVisible by remember { mutableStateOf(true) }
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15f && isBarsVisible) isBarsVisible = false
                else if (available.y > 40f && !isBarsVisible) isBarsVisible = true

                return Offset.Zero
            }
        }
    }

    val barTranslation by
        animateFloatAsState(
            targetValue = if (isBarsVisible) 0f else 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "barTranslation",
        )

    val scrollProgress by
        remember(gridState) {
            derivedStateOf {
                val layoutInfo = gridState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount

                if (totalItems <= 0) 0f
                else {
                    val firstItem = layoutInfo.visibleItemsInfo.firstOrNull()

                    if (firstItem == null) 0f else firstItem.index.toFloat() / totalItems.toFloat()
                }
            }
        }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        content(
            PaddingValues(
                top = 64.dp + systemBarsPadding.calculateTopPadding(),
                bottom = 80.dp + systemBarsPadding.calculateBottomPadding(),
            )
        )

        val barColor = MaterialTheme.colorScheme.surface

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .graphicsLayer { translationY = -barTranslation * size.height }
                    .background(barColor)
                    .statusBarsPadding()
        ) {
            topBar()
        }

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val bottomBarHeight =
                            80.dp.toPx() + systemBarsPadding.calculateBottomPadding().toPx()

                        translationY = -(1f - barTranslation) * bottomBarHeight
                    }
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Box(
                modifier =
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(scrollProgress.coerceIn(0f, 1f))
                        .background(MaterialTheme.colorScheme.primary)
            )
        }

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { translationY = barTranslation * size.height }
                    .background(barColor)
                    .navigationBarsPadding()
        ) {
            bottomBar()
        }

        val fabAlpha by
            animateFloatAsState(
                targetValue = if (isBarsVisible) 1f else 0f,
                animationSpec = tween(200),
                label = "fabAlpha",
            )

        if (fabAlpha > 0.01f) {
            Box(
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = 80.dp + systemBarsPadding.calculateBottomPadding())
                        .graphicsLayer {
                            alpha = fabAlpha
                            scaleX = fabAlpha
                            scaleY = fabAlpha
                        }
            ) {
                AppFab(
                    gridState = gridState,
                    isLoading = isLoading,
                    hasMore = hasMore,
                    showRefresh = showRefresh,
                    onRefresh = onRefresh,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

@Composable
fun EmptyState(viewMode: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val (message, icon) =
            when (viewMode) {
                "favorites" ->
                    stringResource(R.string.empty_favorites) to Icons.Default.FavoriteBorder
                "gallery" -> stringResource(R.string.empty_gallery) to Icons.Outlined.Collections
                else -> stringResource(R.string.empty_feed) to Icons.Default.Search
            }

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline,
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )
    }
}
