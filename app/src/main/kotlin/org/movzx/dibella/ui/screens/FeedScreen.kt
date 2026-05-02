package org.movzx.dibella.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.ImageLoader
import org.movzx.dibella.RightSidebarType
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.ui.components.*
import org.movzx.dibella.viewmodel.FavoritesViewModel
import org.movzx.dibella.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    imageLoader: ImageLoader,
    gridState: LazyStaggeredGridState,
    feedVideoAutoplay: Boolean,
    favoritesPath: String?,
    onOpenLeftSidebar: () -> Unit,
    onOpenRightSidebar: (RightSidebarType) -> Unit,
    onImageClick: (List<CivitaiImage>, Int, String) -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    selectedImageIndex: Int?,
    backPressedTime: Long,
    onUpdateBackPressedTime: (Long) -> Unit,
    exitConfirmMsg: String,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val viewModel: FeedViewModel = hiltViewModel(activity)
    val favViewModel: FavoritesViewModel = hiltViewModel(activity)
    val uiState by viewModel.uiState.collectAsState()
    val videoPlayerManager = LocalVideoPlayerManager.current

    LaunchedEffect(uiState.gridColumns) {
        videoPlayerManager?.updateLimit(
            org.movzx.dibella.util.playerPoolSizeForColumns(uiState.gridColumns)
        )
    }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        if (uiState.isRestored) {
            kotlinx.coroutines.delay(500)

            viewModel.saveScrollPosition(
                uiState.type,
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset,
            )
        }
    }

    LaunchedEffect(uiState.images.isNotEmpty()) {
        if (!uiState.isRestored && uiState.images.isNotEmpty()) {
            gridState.scrollToItem(uiState.scrollIndex, uiState.scrollOffset)
            viewModel.markRestored()
        }
    }

    val favState by favViewModel.uiState.collectAsState()
    val galleryViewModel: org.movzx.dibella.viewmodel.GalleryViewModel = hiltViewModel(activity)
    val galleryState by galleryViewModel.uiState.collectAsState()
    val feedCount = uiState.images.size

    AppScaffold(
        topBar = {
            MainTopBar(
                gridColumns = uiState.gridColumns,
                onShowDisplayOptions = onOpenLeftSidebar,
                onUpdateGridColumns = { viewModel.updateGridColumns(it) },
                onShowFilters = { onOpenRightSidebar(RightSidebarType.FILTERS) },
                onShowSettings = { onOpenRightSidebar(RightSidebarType.SETTINGS) },
            )
        },
        bottomBar = {
            MainBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                feedCount = feedCount,
                favoritesCount = favState.images.size,
                galleryCount = galleryState.images.size,
            )
        },
        gridState = gridState,
        isLoading = uiState.isLoading,
        hasMore = uiState.hasMore,
        showRefresh = true,
        onRefresh = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            ImageGrid(
                images = uiState.images,
                imageLoader = imageLoader,
                state = gridState,
                isLoading = uiState.isLoading,
                favoriteIds = uiState.favoriteIds,
                downloadProgresses = uiState.downloadProgresses,
                columnCount = uiState.gridColumns,
                showFavorite = true,
                viewMode = "feed",
                favoritesPath = favoritesPath,
                contentPadding = padding,
                onGetFavoriteFlow = { favViewModel.getFavoriteFlow(it) },
                onEnsureFavoriteResources = { img, force, onProgress ->
                    favViewModel.ensureFavoriteResources(img, force, onProgress)
                },
                onEnsureFavoriteResourcesThrottled = { img, force, onProgress ->
                    favViewModel.ensureFavoriteResourcesThrottled(img, force, onProgress)
                },
                onImageClick = { image ->
                    val index = uiState.images.indexOf(image)

                    if (index != -1) onImageClick(uiState.images, index, "feed")
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onRetryThumbnail = { url, onComplete -> viewModel.retryThumbnail(url, onComplete) },
                onUpdateGridColumns = { viewModel.updateGridColumns(it) },
                autoplayEnabled = feedVideoAutoplay,
                isPreviewOpen = selectedImageIndex != null,
            )
        }

        if (uiState.isLoading && uiState.images.isEmpty())
            SkeletonGrid(columnCount = uiState.gridColumns)

        if (uiState.images.isEmpty() && !uiState.isLoading) EmptyState("feed")
    }
}
