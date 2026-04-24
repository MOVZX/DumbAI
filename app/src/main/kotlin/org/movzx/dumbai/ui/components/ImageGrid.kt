package org.movzx.dumbai.ui.components

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
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.FavoriteImage

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageGrid(
    images: List<CivitaiImage>,
    imageLoader: ImageLoader,
    state: LazyStaggeredGridState,
    isLoading: Boolean,
    favoriteIds: Set<Long>,
    columnCount: Int,
    showFavorite: Boolean,
    viewMode: String,
    onGetFavoriteFlow: (Long) -> Flow<FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage) -> Unit,
    onImageClick: (CivitaiImage) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit,
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
                showFavorite = showFavorite,
                viewMode = viewMode,
                onGetFavoriteFlow = onGetFavoriteFlow,
                onEnsureFavoriteResources = onEnsureFavoriteResources,
                onClick = onImageClick,
                onToggleFavorite = onToggleFavorite,
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
