package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import kotlinx.coroutines.flow.Flow
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

@Composable
fun ImageGrid(
    images: List<CivitaiImage>,
    imageLoader: ImageLoader,
    state: LazyStaggeredGridState,
    isLoading: Boolean,
    favoriteIds: Set<Long>,
    downloadProgresses: Map<Long, Float>,
    columnCount: Int,
    showFavorite: Boolean,
    viewMode: String,
    favoritesPath: String? = null,
    isSelected: (Long) -> Boolean = { false },
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    onGetFavoriteFlow: (Long) -> Flow<FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onEnsureFavoriteResourcesThrottled: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onImageClick: (CivitaiImage) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit,
    onRetryThumbnail: (String, () -> Unit) -> Unit = { _, _ -> },
    onToggleSelection: (Long) -> Unit = {},
    onLongClick: (Long) -> Unit = {},
    onUpdateGridColumns: (Int) -> Unit = {},
    autoplayEnabled: Boolean = false,
    isPreviewOpen: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    showVideoIcon: Boolean = true,
) {
    var pressedId by remember { mutableStateOf<Long?>(null) }
    val animatedItems = remember { mutableSetOf<Int>() }
    val uniqueImages = remember(images) { images.distinctBy { "${it.type ?: "image"}:${it.id}" } }
    val pullRefreshState = rememberPullToRefreshState()

    val visibleItemIds by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

            if (viewportHeight <= 0) return@derivedStateOf emptySet<Int>()

            layoutInfo.visibleItemsInfo
                .filter { item ->
                    val itemHeight = item.size.height

                    if (itemHeight <= 0) return@filter false

                    val top = item.offset.y
                    val bottom = top + itemHeight

                    bottom > 0 && top < viewportHeight
                }
                .mapNotNull { it.key as? Int }
                .toSet()
        }
    }

    val focusedItemIndex by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo

            val viewportCenter =
                (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2f

            layoutInfo.visibleItemsInfo
                .filter { it.key is Int }
                .minByOrNull { item ->
                    val itemCenter = item.offset.y + (item.size.height / 2f)

                    Math.abs(itemCenter - viewportCenter)
                }
                ?.key as? Int
        }
    }

    if (onRefresh != null) {
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            GridContent(
                images = uniqueImages,
                imageLoader = imageLoader,
                state = state,
                isLoading = isLoading,
                favoriteIds = favoriteIds,
                downloadProgresses = downloadProgresses,
                columnCount = columnCount,
                showFavorite = showFavorite,
                viewMode = viewMode,
                favoritesPath = favoritesPath,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                selectedIds = selectedIds,
                contentPadding = contentPadding,
                onGetFavoriteFlow = onGetFavoriteFlow,
                onEnsureFavoriteResources = onEnsureFavoriteResources,
                onEnsureFavoriteResourcesThrottled = onEnsureFavoriteResourcesThrottled,
                onImageClick = onImageClick,
                onToggleFavorite = onToggleFavorite,
                onRetryThumbnail = onRetryThumbnail,
                onToggleSelection = onToggleSelection,
                onLongClick = onLongClick,
                onUpdateGridColumns = onUpdateGridColumns,
                autoplayEnabled = autoplayEnabled,
                isPreviewOpen = isPreviewOpen,
                isRefreshing = isRefreshing,
                showVideoIcon = showVideoIcon,
                pressedId = pressedId,
                animatedItems = animatedItems,
                visibleItemIds = visibleItemIds,
                focusedItemIndex = focusedItemIndex,
                pressedIdState = { pressedId },
                pressedIdChange = { pressedId = it },
            )
        }
    } else {
        GridContent(
            images = uniqueImages,
            imageLoader = imageLoader,
            state = state,
            isLoading = isLoading,
            favoriteIds = favoriteIds,
            downloadProgresses = downloadProgresses,
            columnCount = columnCount,
            showFavorite = showFavorite,
            viewMode = viewMode,
            favoritesPath = favoritesPath,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            contentPadding = contentPadding,
            onGetFavoriteFlow = onGetFavoriteFlow,
            onEnsureFavoriteResources = onEnsureFavoriteResources,
            onEnsureFavoriteResourcesThrottled = onEnsureFavoriteResourcesThrottled,
            onImageClick = onImageClick,
            onToggleFavorite = onToggleFavorite,
            onRetryThumbnail = onRetryThumbnail,
            onToggleSelection = onToggleSelection,
            onLongClick = onLongClick,
            onUpdateGridColumns = onUpdateGridColumns,
            autoplayEnabled = autoplayEnabled,
            isPreviewOpen = isPreviewOpen,
            isRefreshing = isRefreshing,
            showVideoIcon = showVideoIcon,
            pressedId = pressedId,
            animatedItems = animatedItems,
            visibleItemIds = visibleItemIds,
            focusedItemIndex = focusedItemIndex,
            pressedIdState = { pressedId },
            pressedIdChange = { pressedId = it },
        )
    }
}

@Composable
private fun GridContent(
    images: List<CivitaiImage>,
    imageLoader: ImageLoader,
    state: LazyStaggeredGridState,
    isLoading: Boolean,
    favoriteIds: Set<Long>,
    downloadProgresses: Map<Long, Float>,
    columnCount: Int,
    showFavorite: Boolean,
    viewMode: String,
    favoritesPath: String? = null,
    isSelected: (Long) -> Boolean = { false },
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    onGetFavoriteFlow: (Long) -> Flow<FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onEnsureFavoriteResourcesThrottled: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onImageClick: (CivitaiImage) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit,
    onRetryThumbnail: (String, () -> Unit) -> Unit = { _, _ -> },
    onToggleSelection: (Long) -> Unit = {},
    onLongClick: (Long) -> Unit = {},
    onUpdateGridColumns: (Int) -> Unit = {},
    autoplayEnabled: Boolean = false,
    isPreviewOpen: Boolean = false,
    isRefreshing: Boolean = false,
    showVideoIcon: Boolean = true,
    pressedId: Long?,
    animatedItems: MutableSet<Int>,
    visibleItemIds: Set<Int>,
    focusedItemIndex: Int?,
    pressedIdState: () -> Long?,
    pressedIdChange: (Long?) -> Unit,
) {
    val shimmerColors =
        listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        )

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnCount),
        state = state,
        modifier =
            Modifier.fillMaxSize().pointerInput(columnCount) {
                awaitEachGesture {
                    var totalZoom = 1f

                    awaitFirstDown(requireUnconsumed = false)

                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()

                        if (event.changes.size > 1) {
                            totalZoom *= zoom

                            if (totalZoom > 1.4f) {
                                if (columnCount > 1) {
                                    onUpdateGridColumns(columnCount - 1)
                                    event.changes.forEach { it.consume() }

                                    break
                                }
                            } else if (totalZoom < 0.7f) {
                                if (columnCount < 4) {
                                    onUpdateGridColumns(columnCount + 1)
                                    event.changes.forEach { it.consume() }

                                    break
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        itemsIndexed(
            items = images,
            key = { index, _ -> index },
            contentType = { _, _ -> "civitai_image" },
        ) { index, image ->
            val isAnimated =
                remember(index.toLong()) { mutableStateOf(animatedItems.contains(index)) }

            if (!isAnimated.value && visibleItemIds.contains(index)) {
                isAnimated.value = true

                animatedItems.add(index)
            }

            val itemScale by
                animateFloatAsState(
                    targetValue = if (focusedItemIndex == index) 1.02f else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 100f),
                    label = "ItemScale",
                )

            val isVisible = isAnimated.value || visibleItemIds.contains(index)

            ImageCard(
                image = image,
                imageLoader = imageLoader,
                isFavorite = favoriteIds.contains(image.id),
                favoriteIds = favoriteIds,
                downloadProgresses = downloadProgresses,
                showFavorite = showFavorite && !isSelectionMode,
                viewMode = viewMode,
                favoritesPath = favoritesPath,
                isSelected = selectedIds.contains(image.id),
                isSelectionMode = isSelectionMode,
                onGetFavoriteFlow = onGetFavoriteFlow,
                onEnsureFavoriteResources = onEnsureFavoriteResources,
                onEnsureFavoriteResourcesThrottled = onEnsureFavoriteResourcesThrottled,
                onClick = {
                    pressedIdState()
                    onImageClick(image)
                },
                onToggleFavorite = onToggleFavorite,
                onRetryThumbnail = onRetryThumbnail,
                onToggleSelection = { onToggleSelection(image.id) },
                onLongClick = { onLongClick(image.id) },
                autoplayEnabled = autoplayEnabled && !isPreviewOpen,
                isVisibleInViewport = visibleItemIds.contains(index),
                isScrolling = state.isScrollInProgress,
                isPreviewOpen = isPreviewOpen,
                animationIndex = if (isAnimated.value) -1 else index,
                isPressed = pressedIdState() == image.id,
                onPressChange = { isPressed -> pressedIdChange(if (isPressed) image.id else null) },
                showVideoIcon = showVideoIcon,
            )
        }

        if (isLoading && !isRefreshing)
            items(4) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(Brush.linearGradient(colors = shimmerColors))
                            .graphicsLayer { alpha = 0.5f }
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.Center),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    )
                }
            }
    }
}
