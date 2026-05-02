package org.movzx.dibella.ui.components

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
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
) {
    var pressedId by remember { mutableStateOf<Long?>(null) }
    val animatedItems = remember { mutableSetOf<Long>() }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val visibleItemIds by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

            if (viewportHeight <= 0) return@derivedStateOf emptySet<Long>()

            layoutInfo.visibleItemsInfo
                .filter { item ->
                    val itemHeight = item.size.height

                    if (itemHeight <= 0) return@filter false

                    val top = item.offset.y
                    val bottom = top + itemHeight
                    val visibleTop = maxOf(0, top)
                    val visibleBottom = minOf(viewportHeight, bottom)
                    val visibleHeight = maxOf(0, visibleBottom - visibleTop)

                    visibleHeight >= itemHeight * 0.3f
                }
                .mapNotNull { it.key as? Long }
                .toSet()
        }
    }

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
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        itemsIndexed(
            items = images,
            key = { _, it -> it.id },
            contentType = { _, _ -> "civitai_image" },
        ) { index, image ->
            var isAnimated by
                remember(image.id) { mutableStateOf(animatedItems.contains(image.id)) }

            if (!isAnimated && visibleItemIds.contains(image.id)) {
                isAnimated = true

                animatedItems.add(image.id)
            }

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
                    pressedId = null
                    onImageClick(it)
                },
                onToggleFavorite = onToggleFavorite,
                onRetryThumbnail = onRetryThumbnail,
                onToggleSelection = { onToggleSelection(image.id) },
                onLongClick = { onLongClick(image.id) },
                autoplayEnabled = autoplayEnabled && !isPreviewOpen,
                isVisibleInViewport = visibleItemIds.contains(image.id),
                isScrolling = state.isScrollInProgress,
                animationIndex = if (isAnimated) -1 else index,
                isPressed = pressedId == image.id,
                onPressChange = { isPressed -> pressedId = if (isPressed) image.id else null },
            )
        }

        if (isLoading && images.isNotEmpty()) {
            items(count = columnCount * 2, contentType = { "skeleton" }) { index ->
                val aspectRatios = listOf(0.8f, 1.0f, 1.2f, 1.4f, 0.75f, 1.33f, 0.67f, 1.5f)
                val aspectRatio = aspectRatios[index % aspectRatios.size]

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .aspectRatio(aspectRatio)
                            .clip(MaterialTheme.shapes.large)
                            .shimmerBackground()
                )
            }
        }
    }
}
