package org.movzx.dibella.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import kotlinx.coroutines.flow.Flow
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

@OptIn(ExperimentalSharedTransitionApi::class)
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
    autoplayEnabled: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnCount),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        items(items = images, key = { it.id }, contentType = { "civitai_image" }) { image ->
            ImageCard(
                image = image,
                imageLoader = imageLoader,
                isFavorite = favoriteIds.contains(image.id),
                favoriteIds = favoriteIds,
                downloadProgresses = downloadProgresses,
                showFavorite = showFavorite && !isSelectionMode,
                viewMode = viewMode,
                isSelected = selectedIds.contains(image.id),
                isSelectionMode = isSelectionMode,
                onGetFavoriteFlow = onGetFavoriteFlow,
                onEnsureFavoriteResources = onEnsureFavoriteResources,
                onEnsureFavoriteResourcesThrottled = onEnsureFavoriteResourcesThrottled,
                onClick = onImageClick,
                onToggleFavorite = onToggleFavorite,
                onRetryThumbnail = onRetryThumbnail,
                onToggleSelection = { onToggleSelection(image.id) },
                onLongClick = { onLongClick(image.id) },
                autoplayEnabled = autoplayEnabled,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

        if (isLoading && images.isNotEmpty()) {
            items(count = columnCount * 2, contentType = { "skeleton" }) { index ->
                val aspectRatio = remember(index) { (8..14).random() / 10f }

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
