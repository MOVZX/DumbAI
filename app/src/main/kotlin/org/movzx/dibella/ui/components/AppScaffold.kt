package org.movzx.dibella.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
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
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    gridState: LazyStaggeredGridState,
    isLoading: Boolean = false,
    amoledMode: Boolean = false,
    hasMore: Boolean = false,
    showRefresh: Boolean = false,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var isBarsVisible by remember { mutableStateOf(true) }
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    var pullOffset by remember { mutableFloatStateOf(0f) }
    val pullThreshold = 80.dp
    val pullLimit = 140.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pullThresholdPx = with(density) { pullThreshold.toPx() }
    val pullLimitPx = with(density) { pullLimit.toPx() }

    val nestedScrollConnection =
        remember(isBarsVisible, hasMore, isLoading, showRefresh) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0 && pullOffset < 0) {
                        val consumed =
                            if (available.y + pullOffset > 0) -pullOffset else available.y

                        pullOffset += consumed

                        return Offset(0f, consumed)
                    }

                    if (available.y < -15f && isBarsVisible) isBarsVisible = false
                    else if (available.y > 15f && !isBarsVisible) isBarsVisible = true

                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (isLoading) return Offset.Zero

                    if (available.y > 0 && showRefresh) {
                        val progress = (pullOffset / pullLimitPx).coerceIn(0f, 1f)
                        val resistance = 1f - progress
                        pullOffset =
                            (pullOffset + available.y * 0.5f * resistance).coerceAtMost(pullLimitPx)

                        return Offset(0f, available.y)
                    }

                    if (available.y < 0 && hasMore) {
                        val progress = (Math.abs(pullOffset) / pullLimitPx).coerceIn(0f, 1f)
                        val resistance = 1f - progress
                        pullOffset =
                            (pullOffset + available.y * 0.5f * resistance).coerceAtLeast(
                                -pullLimitPx
                            )

                        return Offset(0f, available.y)
                    }

                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    if (pullOffset > pullThresholdPx) onRefresh()
                    else if (pullOffset < -pullThresholdPx) onLoadMore()

                    pullOffset = 0f

                    return Velocity.Zero
                }
            }
        }

    val barTranslation by
        animateFloatAsState(
            targetValue = if (isBarsVisible) 0f else 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "barTranslation",
        )

    val animatedPullOffset by
        animateFloatAsState(
            targetValue = pullOffset,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "pullOffset",
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
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationY = animatedPullOffset }) {
            content(
                PaddingValues(
                    top = 64.dp + systemBarsPadding.calculateTopPadding(),
                    bottom = 80.dp + systemBarsPadding.calculateBottomPadding(),
                )
            )
        }

        if (pullOffset != 0f && !isLoading) {
            val isTop = pullOffset > 0
            val rawProgress = (Math.abs(pullOffset) / pullThresholdPx).coerceIn(0f, 1.5f)
            val bounceTransition = rememberInfiniteTransition(label = "refreshBounce")

            val bounceOffset by
                bounceTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 10f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "bounce",
                )

            Box(
                modifier =
                    Modifier.align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
                        .padding(
                            top =
                                if (isTop) 100.dp + systemBarsPadding.calculateTopPadding()
                                else 0.dp,
                            bottom =
                                if (!isTop) 100.dp + systemBarsPadding.calculateBottomPadding()
                                else 0.dp,
                        )
                        .graphicsLayer {
                            alpha = (rawProgress / 1f).coerceIn(0f, 1f)
                            scaleX = rawProgress.coerceIn(0.5f, 1.2f)
                            scaleY = rawProgress.coerceIn(0.5f, 1.2f)

                            translationY =
                                animatedPullOffset * 0.4f +
                                    (if (isTop) bounceOffset else -bounceOffset)
                        },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { (rawProgress).coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp),
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    shadowElevation = 4.dp,
                ) {
                    Icon(
                        Icons.Default.KeyboardDoubleArrowDown,
                        contentDescription = null,
                        modifier =
                            Modifier.padding(6.dp).size(20.dp).graphicsLayer {
                                rotationZ = if (isTop) 0f else 180f
                            },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        val barColor =
            if (amoledMode) androidx.compose.ui.graphics.Color.Black
            else MaterialTheme.colorScheme.surface

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
                AppFab(gridState = gridState, isLoading = isLoading, hasMore = hasMore)
            }
        }
    }
}

@Composable
fun EmptyState(viewMode: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyState")

    val floatOffset by
        infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "float",
        )

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
            modifier = Modifier.size(64.dp).graphicsLayer { translationY = floatOffset },
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    brush =
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.onSurface,
                                    MaterialTheme.colorScheme.outline,
                                )
                        )
                ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp).graphicsLayer { alpha = 0.8f },
        )
    }
}
