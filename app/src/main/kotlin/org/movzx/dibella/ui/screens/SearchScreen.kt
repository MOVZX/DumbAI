package org.movzx.dibella.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.movzx.dibella.R
import org.movzx.dibella.RightSidebarType
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.CivitaiSearchResult
import org.movzx.dibella.ui.components.*
import org.movzx.dibella.util.CivitaiUrlBuilder
import org.movzx.dibella.viewmodel.FavoritesViewModel
import org.movzx.dibella.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    imageLoader: ImageLoader,
    gridState: LazyStaggeredGridState,
    amoledMode: Boolean,
    favoritesPath: String?,
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
    favoriteIds: Set<Long>,
    onToggleFavorite: (CivitaiImage) -> Unit,
    onGetFavoriteFlow:
        (Long) -> kotlinx.coroutines.flow.Flow<org.movzx.dibella.model.FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onEnsureFavoriteResourcesThrottled: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onUpdateGridColumns: (Int) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val viewModel: SearchViewModel = hiltViewModel(activity)
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val feedViewModel: org.movzx.dibella.viewmodel.FeedViewModel = hiltViewModel(activity)
    val feedState by feedViewModel.uiState.collectAsState()
    val favViewModel: FavoritesViewModel = hiltViewModel<FavoritesViewModel>(activity)
    val favState by favViewModel.uiState.collectAsState()
    val galleryViewModel: org.movzx.dibella.viewmodel.GalleryViewModel = hiltViewModel(activity)
    val galleryState by galleryViewModel.uiState.collectAsState()
    val bookmarkViewModel: org.movzx.dibella.viewmodel.BookmarkViewModel = hiltViewModel(activity)
    val bookmarkState by bookmarkViewModel.uiState.collectAsState()
    val searchCount by viewModel.searchResultCount.collectAsState(initial = 0)
    var showJumpDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedImageIndex) { if (selectedImageIndex != null) focusManager.clearFocus() }

    if (showJumpDialog) {
        JumpDialog(
            currentCursor = uiState.currentOffset.toString(),
            onApply = { targetOffset ->
                showJumpDialog = false
                viewModel.jumpToOffset(targetOffset.toIntOrNull() ?: 0)
            },
            onDismiss = { showJumpDialog = false },
        )
    }

    if (showBookmarkDialog) {
        BookmarkDialog(
            onApply = { title ->
                showBookmarkDialog = false
                viewModel.saveSearchBookmark(title)
            },
            onDismiss = { showBookmarkDialog = false },
        )
    }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        if (uiState.isRestored) {
            kotlinx.coroutines.delay(500)

            val absoluteIndex = uiState.currentPageStartOffset + gridState.firstVisibleItemIndex

            viewModel.saveScrollPosition(
                "search",
                absoluteIndex,
                gridState.firstVisibleItemScrollOffset,
            )
        }
    }

    LaunchedEffect(uiState.results.isNotEmpty(), uiState.isRestored) {
        if (!uiState.isRestored && uiState.results.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            gridState.scrollToItem(uiState.scrollIndex, uiState.scrollOffset)
            viewModel.markRestored()
        }
    }

    AppScaffold(
        topBar = {
            SearchTopBar(
                query = uiState.query,
                onQueryChange = { /* Do nothing - search on Enter only */ },
                onSearch = { query ->
                    focusManager.clearFocus()
                    viewModel.search(query, forceNew = true)
                },
                onShowFilters = { onOpenRightSidebar(RightSidebarType.SEARCH_FILTERS) },
                onShowSettings = { onOpenRightSidebar(RightSidebarType.SETTINGS) },
                gridColumns = uiState.gridColumns,
                onUpdateGridColumns = onUpdateGridColumns,
                scope = scope,
                leftDrawerState = leftDrawerState,
                onShowDisplayOptions = { scope.launch { leftDrawerState.open() } },
            )
        },
        bottomBar = {
            MainBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                feedCount = feedState.images.size,
                favoritesCount = favState.images.size,
                searchCount = searchCount,
                galleryCount = galleryState.images.size,
                bookmarkCount = bookmarkState.bookmarkCount,
            )
        },
        gridState = gridState,
        isLoading = uiState.isLoading,
        amoledMode = amoledMode,
        hasMore = uiState.hasMore,
        showRefresh = uiState.results.isNotEmpty(),
        showBookmarkJump = uiState.results.isNotEmpty(),
        onRefresh = { viewModel.search(uiState.query, forceNew = true) },
        onLoadMore = { viewModel.loadMore() },
        onJumpClicked = { showJumpDialog = true },
        onBookmarkClicked = { showBookmarkDialog = true },
    ) { padding ->
        val searchImages = uiState.results.map { it.toCivitaiImage() }

        SearchResultsGrid(
            results = uiState.results,
            imageLoader = imageLoader,
            state = gridState,
            isLoading = uiState.isLoading,
            favoriteIds = favoriteIds,
            downloadProgresses = emptyMap(),
            columnCount = uiState.gridColumns,
            favoritesPath = favoritesPath,
            contentPadding = padding,
            onGetFavoriteFlow = onGetFavoriteFlow,
            onEnsureFavoriteResources = onEnsureFavoriteResources,
            onEnsureFavoriteResourcesThrottled = onEnsureFavoriteResourcesThrottled,
            onImageClick = { result ->
                val index = uiState.results.indexOf(result)

                if (index != -1) onImageClick(searchImages, index, "search")
            },
            onToggleFavorite = { result -> onToggleFavorite(result.toCivitaiImage()) },
            onUpdateGridColumns = {},
            isPreviewOpen = selectedImageIndex != null,
            searchType = uiState.type,
        )

        if (uiState.isLoading && uiState.results.isEmpty())
            SkeletonGrid(columnCount = uiState.gridColumns, modifier = Modifier.padding(padding))

        if (uiState.results.isEmpty() && !uiState.isLoading)
            ModernEmptyState(type = EmptyStateType.SEARCH)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onShowFilters: () -> Unit,
    onShowSettings: () -> Unit,
    gridColumns: Int,
    onUpdateGridColumns: (Int) -> Unit,
    onShowDisplayOptions: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    leftDrawerState: DrawerState,
) {
    var searchQuery by remember { mutableStateOf(query) }

    LaunchedEffect(query) { searchQuery = query }

    val focusManager = LocalFocusManager.current

    CenterAlignedTopAppBar(
        title = {
            Box(modifier = Modifier.height(48.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    textStyle = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions =
                        KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onSearch(searchQuery)
                            }
                        ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty())
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    onSearch("")
                                }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.btn_close),
                                )
                            }
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        },
        navigationIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShowDisplayOptions) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = stringResource(R.string.display_options),
                    )
                }

                IconButton(
                    onClick = {
                        val nextCols = if (gridColumns >= 4) 1 else gridColumns + 1

                        onUpdateGridColumns(nextCols)
                    }
                ) {
                    Icon(
                        imageVector =
                            when (gridColumns) {
                                1 -> Icons.Default.ViewStream
                                2 -> Icons.Default.Dashboard
                                3 -> Icons.Default.ViewModule
                                else -> Icons.Filled.Grid4x4
                            },
                        contentDescription = stringResource(R.string.label_grid_columns),
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onShowFilters) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.filters),
                )
            }

            IconButton(onClick = onShowSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}

@Composable
fun SearchResultsGrid(
    results: List<org.movzx.dibella.model.CivitaiSearchResult>,
    imageLoader: ImageLoader,
    state: LazyStaggeredGridState,
    isLoading: Boolean,
    favoriteIds: Set<Long>,
    downloadProgresses: Map<Long, Float>,
    columnCount: Int,
    favoritesPath: String?,
    contentPadding: PaddingValues,
    onGetFavoriteFlow:
        (Long) -> kotlinx.coroutines.flow.Flow<org.movzx.dibella.model.FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onEnsureFavoriteResourcesThrottled: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onImageClick: (org.movzx.dibella.model.CivitaiSearchResult) -> Unit,
    onToggleFavorite: (org.movzx.dibella.model.CivitaiSearchResult) -> Unit,
    onUpdateGridColumns: (Int) -> Unit,
    isPreviewOpen: Boolean,
    searchType: String,
) {
    val images = results.map { it.toCivitaiImage() }

    ImageGrid(
        images = images,
        imageLoader = imageLoader,
        state = state,
        isLoading = isLoading,
        favoriteIds = favoriteIds,
        downloadProgresses = downloadProgresses,
        columnCount = columnCount,
        showFavorite = true,
        viewMode = "search",
        favoritesPath = favoritesPath,
        contentPadding = contentPadding,
        onGetFavoriteFlow = onGetFavoriteFlow,
        onEnsureFavoriteResources = onEnsureFavoriteResources,
        onEnsureFavoriteResourcesThrottled = onEnsureFavoriteResourcesThrottled,
        onImageClick = { image ->
            val result = results.find { it.id == image.id }

            result?.let { onImageClick(it) }
        },
        onToggleFavorite = { image ->
            val result = results.find { it.id == image.id }

            result?.let { onToggleFavorite(it) }
        },
        onUpdateGridColumns = onUpdateGridColumns,
        isPreviewOpen = isPreviewOpen,
        showVideoIcon = searchType != "video",
    )
}

fun org.movzx.dibella.model.CivitaiSearchResult.toCivitaiImage(): CivitaiImage {
    return CivitaiImage(
        id = this.id,
        url = CivitaiUrlBuilder.expandUrl(this.url, this.type),
        nsfw = this.nsfwLevel > 4,
        width = this.width,
        height = this.height,
        type = this.type,
    )
}
