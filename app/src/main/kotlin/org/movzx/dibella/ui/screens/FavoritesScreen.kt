package org.movzx.dibella.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.ImageLoader
import org.movzx.dibella.R
import org.movzx.dibella.RightSidebarType
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.ui.components.*
import org.movzx.dibella.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    imageLoader: ImageLoader,
    gridState: LazyStaggeredGridState,
    feedVideoAutoplay: Boolean,
    favoritesPath: String?,
    onOpenLeftSidebar: () -> Unit,
    onOpenRightSidebar: (RightSidebarType) -> Unit,
    onImageClick: (List<CivitaiImage>, Int, String) -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    favoritesRestored: Boolean,
    onFavoritesRestored: (Boolean) -> Unit,
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
    val viewModel: FavoritesViewModel = hiltViewModel(activity)
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
                "favorites",
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

    AppBackHandler(
        enabled = selectedImageIndex == null,
        isSelectionMode = uiState.isSelectionMode,
        clearSelection = { viewModel.clearSelection() },
        leftDrawerState = leftDrawerState,
        rightDrawerState = rightDrawerState,
        scope = scope,
        backPressedTime = backPressedTime,
        onUpdateBackPressedTime = onUpdateBackPressedTime,
        exitConfirmMsg = exitConfirmMsg,
    )

    if (showBatchConfirmDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_batch_unfavorite_title),
            message =
                stringResource(R.string.dialog_batch_unfavorite_msg, uiState.selectedIds.size),
            onConfirm = {
                viewModel.batchUnfavorite()
                showBatchConfirmDialog = false
            },
            onDismiss = { showBatchConfirmDialog = false },
        )
    }

    val feedViewModel: org.movzx.dibella.viewmodel.FeedViewModel = hiltViewModel(activity)
    val feedState by feedViewModel.uiState.collectAsState()
    val galleryViewModel: org.movzx.dibella.viewmodel.GalleryViewModel = hiltViewModel(activity)
    val galleryState by galleryViewModel.uiState.collectAsState()
    val feedCount = if (feedState.isLoading) 0 else feedState.images.size

    AppScaffold(
        topBar = {
            InteractiveTopBar(
                isSelectionMode = uiState.isSelectionMode,
                isShowingDuplicates = uiState.isShowingDuplicates,
                selectedIdsSize = uiState.selectedIds.size,
                duplicateGroupsSize = uiState.duplicateGroups.flatten().size,
                gridColumns = uiState.gridColumns,
                selectionActionIcon = Icons.Default.HeartBroken,
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll() },
                onShowBatchConfirmDialog = { showBatchConfirmDialog = true },
                onClearDuplicatesMode = { viewModel.clearDuplicatesMode() },
                onRemoveDuplicates = { viewModel.removeDuplicates() },
                onOpenLeftSidebar = onOpenLeftSidebar,
                onUpdateGridColumns = { viewModel.updateGridColumns(it) },
                onShowFilters = {},
                onShowSettings = { onOpenRightSidebar(RightSidebarType.SETTINGS) },
            )
        },
        bottomBar = {
            MainBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                feedCount = feedCount,
                favoritesCount = uiState.images.size,
                galleryCount = galleryState.images.size,
            )
        },
        gridState = gridState,
        isLoading = uiState.isLoading,
    ) { padding ->
        ImageGrid(
            images =
                if (uiState.isShowingDuplicates) uiState.duplicateGroups.flatten()
                else uiState.images,
            imageLoader = imageLoader,
            state = gridState,
            isLoading = uiState.isLoading,
            favoriteIds = uiState.favoriteIds,
            downloadProgresses = uiState.downloadProgresses,
            columnCount = uiState.gridColumns,
            showFavorite = true,
            viewMode = "favorites",
            favoritesPath = favoritesPath,
            isSelectionMode = uiState.isSelectionMode,
            selectedIds = uiState.selectedIds,
            contentPadding = padding,
            onGetFavoriteFlow = { viewModel.getFavoriteFlow(it) },
            onEnsureFavoriteResources = { img, force, onProgress ->
                viewModel.ensureFavoriteResources(img, force, onProgress)
            },
            onEnsureFavoriteResourcesThrottled = { img, force, onProgress ->
                viewModel.ensureFavoriteResourcesThrottled(img, force, onProgress)
            },
            onImageClick = { image ->
                val images =
                    if (uiState.isShowingDuplicates) uiState.duplicateGroups.flatten()
                    else uiState.images

                val index = images.indexOf(image)

                if (index != -1) onImageClick(images, index, "favorites")
            },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onRetryThumbnail = { url, onComplete -> viewModel.retryThumbnail(url, onComplete) },
            onUpdateGridColumns = { viewModel.updateGridColumns(it) },
            onToggleSelection = { viewModel.toggleSelection(it) },
            onLongClick = { viewModel.toggleSelection(it) },
            autoplayEnabled = feedVideoAutoplay,
            isPreviewOpen = selectedImageIndex != null,
        )

        if (!uiState.isShowingDuplicates && uiState.images.isEmpty() && !uiState.isLoading)
            EmptyState("favorites")
    }
}
