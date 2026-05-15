package org.movzx.dibella.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.ImageLoader
import org.movzx.dibella.R
import org.movzx.dibella.RightSidebarType
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.ui.components.*
import org.movzx.dibella.viewmodel.FavoritesViewModel
import org.movzx.dibella.viewmodel.GalleryViewModel
import org.movzx.dibella.viewmodel.SearchViewModel

@Composable
fun GalleryScreen(
    imageLoader: ImageLoader,
    gridState: LazyStaggeredGridState,
    feedVideoAutoplay: Boolean,
    amoledMode: Boolean,
    onOpenLeftSidebar: () -> Unit,
    onOpenRightSidebar: (RightSidebarType) -> Unit,
    onImageClick: (List<CivitaiImage>, Int, String) -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    galleryRestored: Boolean,
    onGalleryRestored: (Boolean) -> Unit,
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
    val viewModel: GalleryViewModel = hiltViewModel(activity)
    val favViewModel: FavoritesViewModel = hiltViewModel(activity)
    val bookmarkViewModel: org.movzx.dibella.viewmodel.BookmarkViewModel = hiltViewModel(activity)
    val bookmarkState by bookmarkViewModel.uiState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val favoritesState by favViewModel.uiState.collectAsState()
    val videoPlayerManager = LocalVideoPlayerManager.current
    val searchViewModel: SearchViewModel = hiltViewModel(activity)
    val searchCount by searchViewModel.searchResultCount.collectAsState(initial = 0)

    LaunchedEffect(uiState.gridColumns) {
        videoPlayerManager?.updateLimit(
            org.movzx.dibella.util.playerPoolSizeForColumns(uiState.gridColumns)
        )
    }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        if (uiState.isRestored) {
            kotlinx.coroutines.delay(500)

            viewModel.saveScrollPosition(
                "gallery",
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

    var showBatchConfirmDialog by remember { mutableStateOf(false) }

    if (showBatchConfirmDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_batch_delete_title),
            message = stringResource(R.string.dialog_batch_delete_msg, uiState.selectedIds.size),
            onConfirm = {
                viewModel.batchDelete()
                showBatchConfirmDialog = false
            },
            onDismiss = { showBatchConfirmDialog = false },
        )
    }

    val feedViewModel: org.movzx.dibella.viewmodel.FeedViewModel = hiltViewModel(activity)
    val feedState by feedViewModel.uiState.collectAsState()
    val feedCount = if (feedState.isLoading) 0 else feedState.images.size

    AppScaffold(
        topBar = {
            InteractiveTopBar(
                isSelectionMode = uiState.isSelectionMode,
                isShowingDuplicates = uiState.isShowingDuplicates,
                selectedIdsSize = uiState.selectedIds.size,
                duplicateGroupsSize = uiState.duplicateGroups.flatten().size,
                gridColumns = uiState.gridColumns,
                selectionActionIcon = Icons.Default.Delete,
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll() },
                onShowBatchConfirmDialog = { showBatchConfirmDialog = true },
                onClearDuplicatesMode = { viewModel.clearDuplicatesMode() },
                onRemoveDuplicates = { viewModel.removeDuplicates() },
                onOpenLeftSidebar = onOpenLeftSidebar,
                onUpdateGridColumns = { viewModel.updateGridColumns(it) },
                onShowFilters = null,
                onShowSettings = { onOpenRightSidebar(RightSidebarType.SETTINGS) },
            )
        },
        bottomBar = {
            MainBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                feedCount = feedCount,
                favoritesCount = favoritesState.images.size,
                searchCount = searchCount,
                galleryCount = uiState.images.size,
                bookmarkCount = bookmarkState.bookmarkCount,
            )
        },
        gridState = gridState,
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        amoledMode = amoledMode,
        showRefresh = true,
        showBookmarkJump = false,
        onRefresh = { viewModel.refresh() },
    ) { padding ->
        ImageGrid(
            images =
                if (uiState.isShowingDuplicates) uiState.duplicateGroups.flatten()
                else uiState.images,
            imageLoader = imageLoader,
            state = gridState,
            isLoading = uiState.isLoading,
            favoriteIds = favoritesState.favoriteIds,
            downloadProgresses = uiState.downloadProgresses,
            columnCount = uiState.gridColumns,
            showFavorite = false,
            viewMode = "gallery",
            isSelectionMode = uiState.isSelectionMode,
            selectedIds = uiState.selectedIds,
            contentPadding = padding,
            onGetFavoriteFlow = { favViewModel.getFavoriteFlow(it) },
            onEnsureFavoriteResources = { img, force, onProgress ->
                favViewModel.ensureFavoriteResources(img, force, onProgress)
            },
            onEnsureFavoriteResourcesThrottled = { img, force, onProgress ->
                favViewModel.ensureFavoriteResourcesThrottled(img, force, onProgress)
            },
            onImageClick = { image ->
                val images =
                    if (uiState.isShowingDuplicates) uiState.duplicateGroups.flatten()
                    else uiState.images

                val index = images.indexOf(image)

                if (index != -1) onImageClick(images, index, "gallery")
            },
            onToggleFavorite = { favViewModel.toggleFavorite(it) },
            onRetryThumbnail = { url, onComplete -> favViewModel.retryThumbnail(url, onComplete) },
            onUpdateGridColumns = { viewModel.updateGridColumns(it) },
            onToggleSelection = { viewModel.toggleSelection(it) },
            onLongClick = { viewModel.toggleSelection(it) },
            autoplayEnabled = feedVideoAutoplay,
            isPreviewOpen = selectedImageIndex != null,
            isRefreshing = uiState.isRefreshing,
        )

        if (uiState.isShowingDuplicates && uiState.duplicateGroups.isEmpty() && !uiState.isLoading)
            ModernEmptyState(type = EmptyStateType.DUPLICATES)
        else if (!uiState.isShowingDuplicates && uiState.images.isEmpty() && !uiState.isLoading)
            ModernEmptyState(type = EmptyStateType.GALLERY, onAction = { onNavigate("feed") })
    }
}
