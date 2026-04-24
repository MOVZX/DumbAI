package org.movzx.dumbai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.ui.components.*
import org.movzx.dumbai.ui.theme.DumbAITheme
import org.movzx.dumbai.util.resolveUriToPath
import org.movzx.dumbai.viewmodel.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DumbAITheme { MainScreen(imageLoader) } }
    }
}

enum class RightSidebarType {
    FILTERS,
    SETTINGS,
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(imageLoader: ImageLoader) {
    val navController = rememberNavController()
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var fullScreenImages by remember { mutableStateOf<List<CivitaiImage>>(emptyList()) }
    var fullScreenViewMode by remember { mutableStateOf("") }
    val feedGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()
    val galleryGridState = rememberLazyStaggeredGridState()
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val exitConfirmMsg = stringResource(R.string.msg_exit_confirm)
    val leftDrawerState = rememberDrawerState(DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(DrawerValue.Closed)
    var rightSidebarType by remember { mutableStateOf(RightSidebarType.FILTERS) }
    val feedViewModel: FeedViewModel = hiltViewModel(activity)
    val favoritesViewModel: FavoritesViewModel = hiltViewModel(activity)
    val galleryViewModel: GalleryViewModel = hiltViewModel(activity)
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
    val feedUiState by feedViewModel.uiState.collectAsState()
    val favoritesUiState by favoritesViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    LaunchedEffect(settingsViewModel.exitEvent) {
        settingsViewModel.exitEvent.collect {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)

            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            (context as? Activity)?.finishAffinity()
            Runtime.getRuntime().exit(0)
        }
    }

    var favoritesRestored by remember { mutableStateOf(false) }
    var galleryRestored by remember { mutableStateOf(false) }

    LaunchedEffect(settingsViewModel.uiMessage) {
        settingsViewModel.uiMessage.collect { resId ->
            android.widget.Toast.makeText(
                    context,
                    context.getString(resId),
                    android.widget.Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    LaunchedEffect(feedViewModel.uiMessage) {
        feedViewModel.uiMessage.collect { resId ->
            android.widget.Toast.makeText(
                    context,
                    context.getString(resId),
                    android.widget.Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    LaunchedEffect(favoritesViewModel.uiMessage) {
        favoritesViewModel.uiMessage.collect { resId ->
            android.widget.Toast.makeText(
                    context,
                    context.getString(resId),
                    android.widget.Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    LaunchedEffect(galleryViewModel.uiMessage) {
        galleryViewModel.uiMessage.collect { resId ->
            android.widget.Toast.makeText(
                    context,
                    context.getString(resId),
                    android.widget.Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    val dirPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )

                val path = resolveUriToPath(context, it)

                settingsViewModel.updateDownloadPath(path)
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(context.getString(R.string.mime_json))
        ) { uri ->
            uri?.let { settingsViewModel.exportData(it) }
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { settingsViewModel.importData(it) }
        }

    BackHandler(enabled = selectedImageIndex == null) {
        if (leftDrawerState.isOpen) {
            scope.launch { leftDrawerState.close() }
        } else if (rightDrawerState.isOpen) {
            scope.launch { rightDrawerState.close() }
        } else {
            val currentTime = System.currentTimeMillis()

            if (currentTime - backPressedTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                backPressedTime = currentTime
                android.widget.Toast.makeText(
                        context,
                        exitConfirmMsg,
                        android.widget.Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = leftDrawerState,
        gesturesEnabled = selectedImageIndex == null,
        drawerContent = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            if (currentRoute == "feed") {
                DisplaySidebar(
                    pageLimit = feedUiState.pageLimit,
                    gridColumns = feedUiState.gridColumns,
                    onDismiss = { scope.launch { leftDrawerState.close() } },
                    onUpdatePageLimit = { feedViewModel.updatePageLimit(it) },
                    onUpdateGridColumns = { feedViewModel.updateGridColumns(it) },
                )
            } else if (currentRoute == "favorites") {
                DisplaySidebar(
                    pageLimit = 100,
                    gridColumns = favoritesUiState.gridColumns,
                    onDismiss = { scope.launch { leftDrawerState.close() } },
                    onUpdatePageLimit = { favoritesViewModel.updatePageLimit(it) },
                    onUpdateGridColumns = { favoritesViewModel.updateGridColumns(it) },
                )
            } else {
                ModalDrawerSheet(
                    modifier =
                        Modifier.width(320.dp)
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerShape = androidx.compose.ui.graphics.RectangleShape,
                    content = {},
                )
            }
        },
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            ModalNavigationDrawer(
                drawerState = rightDrawerState,
                gesturesEnabled = selectedImageIndex == null,
                drawerContent = {
                    CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides
                            androidx.compose.ui.unit.LayoutDirection.Ltr
                    ) {
                        if (rightSidebarType == RightSidebarType.FILTERS) {
                            FilterSidebar(
                                nsfw = feedUiState.nsfw,
                                sort = feedUiState.sort,
                                period = feedUiState.period,
                                type = feedUiState.type,
                                tagIds = feedUiState.tagIds,
                                onDismiss = { scope.launch { rightDrawerState.close() } },
                                onFilterChange = { n, s, p, t, tg ->
                                    feedViewModel.updateFilters(n, s, p, t, tg)
                                },
                                onResetFilters = { feedViewModel.resetFilters() },
                            )
                        } else {
                            SettingsSidebar(
                                cacheSize = settingsState.cacheSize,
                                apiKey = settingsState.apiKey,
                                downloadPath = settingsState.downloadPath,
                                debugEnabled = settingsState.debugEnabled,
                                onDismiss = { scope.launch { rightDrawerState.close() } },
                                onClearCache = { settingsViewModel.clearImageCache() },
                                onSaveApiKey = { settingsViewModel.updateApiKey(it) },
                                onUpdateDownloadPath = { settingsViewModel.updateDownloadPath(it) },
                                onPickDirectory = { dirPickerLauncher.launch(null) },
                                onExport = {
                                    exportLauncher.launch(
                                        context.getString(R.string.backup_filename)
                                    )
                                },
                                onImport = {
                                    importLauncher.launch(
                                        arrayOf(context.getString(R.string.mime_json))
                                    )
                                },
                                onToggleDebug = { settingsViewModel.updateDebugEnabled(it) },
                            )
                        }
                    }
                },
            ) {
                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    SharedTransitionLayout {
                        AnimatedContent(
                            targetState = selectedImageIndex,
                            label = "FullScreenTransition",
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                            },
                        ) { targetIndex ->
                            if (targetIndex == null) {
                                AppNavigation(
                                    navController = navController,
                                    imageLoader = imageLoader,
                                    feedGridState = feedGridState,
                                    favoritesGridState = favoritesGridState,
                                    galleryGridState = galleryGridState,
                                    favoritesRestored = favoritesRestored,
                                    onFavoritesRestored = { favoritesRestored = it },
                                    galleryRestored = galleryRestored,
                                    onGalleryRestored = { galleryRestored = it },
                                    onOpenLeftSidebar = { scope.launch { leftDrawerState.open() } },
                                    onOpenRightSidebar = { type ->
                                        rightSidebarType = type
                                        scope.launch { rightDrawerState.open() }
                                    },
                                    onImageClick = { images, index, viewMode ->
                                        fullScreenImages = images
                                        selectedImageIndex = index
                                        fullScreenViewMode = viewMode
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedContent,
                                )
                            } else {
                                val favoriteIds by favoritesViewModel.uiState.collectAsState()

                                val downloadProgresses =
                                    when (fullScreenViewMode) {
                                        "feed" ->
                                            feedViewModel.uiState
                                                .collectAsState()
                                                .value
                                                .downloadProgresses
                                        "favorites" ->
                                            favoritesViewModel.uiState
                                                .collectAsState()
                                                .value
                                                .downloadProgresses
                                        "gallery" ->
                                            galleryViewModel.uiState
                                                .collectAsState()
                                                .value
                                                .downloadProgresses
                                        else -> emptyMap()
                                    }

                                FullScreenImage(
                                    images = fullScreenImages,
                                    initialIndex = targetIndex,
                                    imageLoader = imageLoader,
                                    favoriteIds = favoriteIds.favoriteIds,
                                    downloadProgresses = downloadProgresses,
                                    viewMode = fullScreenViewMode,
                                    onGetFavoriteFlow = { favoritesViewModel.getFavoriteFlow(it) },
                                    onEnsureFavoriteResources = {
                                        favoritesViewModel.ensureFavoriteResources(it)
                                    },
                                    onToggleFavorite = {
                                        if (fullScreenViewMode == "feed")
                                            feedViewModel.toggleFavorite(it)
                                        else favoritesViewModel.toggleFavorite(it)
                                    },
                                    onDownloadImage = {
                                        if (fullScreenViewMode == "feed")
                                            feedViewModel.downloadImage(it)
                                        else if (fullScreenViewMode == "favorites")
                                            favoritesViewModel.downloadImage(it)
                                        else galleryViewModel.downloadImage(it)
                                    },
                                    onDeleteLocalFile = { galleryViewModel.deleteLocalFile(it) },
                                    onDismiss = { selectedImageIndex = null },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedContent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    imageLoader: ImageLoader,
    feedGridState: LazyStaggeredGridState,
    favoritesGridState: LazyStaggeredGridState,
    galleryGridState: LazyStaggeredGridState,
    favoritesRestored: Boolean,
    onFavoritesRestored: (Boolean) -> Unit,
    galleryRestored: Boolean,
    onGalleryRestored: (Boolean) -> Unit,
    onOpenLeftSidebar: () -> Unit,
    onOpenRightSidebar: (RightSidebarType) -> Unit,
    onImageClick: (List<CivitaiImage>, Int, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val onNavigate: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }

                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "feed") {
        composable("feed") {
            FeedScreen(
                imageLoader = imageLoader,
                gridState = feedGridState,
                onOpenLeftSidebar = onOpenLeftSidebar,
                onOpenRightSidebar = onOpenRightSidebar,
                onImageClick = onImageClick,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

        composable("favorites") {
            FavoritesScreen(
                imageLoader = imageLoader,
                gridState = favoritesGridState,
                onOpenLeftSidebar = onOpenLeftSidebar,
                onOpenRightSidebar = onOpenRightSidebar,
                onImageClick = onImageClick,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

        composable("gallery") {
            GalleryScreen(
                imageLoader = imageLoader,
                gridState = galleryGridState,
                onOpenRightSidebar = onOpenRightSidebar,
                onImageClick = onImageClick,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreen(
    imageLoader: ImageLoader,
    gridState: LazyStaggeredGridState,
    onOpenLeftSidebar: () -> Unit,
    onOpenRightSidebar: (RightSidebarType) -> Unit,
    onImageClick: (List<CivitaiImage>, Int, String) -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val viewModel: FeedViewModel = hiltViewModel(activity)
    val favViewModel: FavoritesViewModel = hiltViewModel(activity)
    val uiState by viewModel.uiState.collectAsState()

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
        bottomBar = { MainBottomBar(currentRoute, onNavigate) },
        gridState = gridState,
        isLoading = uiState.isLoading,
        hasMore = uiState.hasMore,
        showRefresh = true,
        onRefresh = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
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
                    columnCount = uiState.gridColumns,
                    showFavorite = true,
                    viewMode = "feed",
                    onGetFavoriteFlow = { favViewModel.getFavoriteFlow(it) },
                    onEnsureFavoriteResources = { favViewModel.ensureFavoriteResources(it) },
                    onImageClick = { image ->
                        val index = uiState.images.indexOf(image)

                        if (index != -1) onImageClick(uiState.images, index, "feed")
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }

            if (uiState.isLoading && uiState.images.isEmpty())
                SkeletonGrid(columnCount = uiState.gridColumns)

            if (uiState.images.isEmpty() && !uiState.isLoading) EmptyState("feed")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    imageLoader: ImageLoader,
    gridState: LazyStaggeredGridState,
    onOpenLeftSidebar: () -> Unit,
    onOpenRightSidebar: (RightSidebarType) -> Unit,
    onImageClick: (List<CivitaiImage>, Int, String) -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val viewModel: FavoritesViewModel = hiltViewModel(activity)
    val uiState by viewModel.uiState.collectAsState()

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
        bottomBar = { MainBottomBar(currentRoute, onNavigate) },
        gridState = gridState,
        isLoading = uiState.isLoading,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            ImageGrid(
                images = uiState.images,
                imageLoader = imageLoader,
                state = gridState,
                isLoading = uiState.isLoading,
                favoriteIds = uiState.favoriteIds,
                columnCount = uiState.gridColumns,
                showFavorite = true,
                viewMode = "favorites",
                onGetFavoriteFlow = { viewModel.getFavoriteFlow(it) },
                onEnsureFavoriteResources = { viewModel.ensureFavoriteResources(it) },
                onImageClick = { image ->
                    val index = uiState.images.indexOf(image)

                    if (index != -1) onImageClick(uiState.images, index, "favorites")
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )

            if (uiState.images.isEmpty() && !uiState.isLoading) EmptyState("favorites")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    imageLoader: ImageLoader,
    gridState: LazyStaggeredGridState,
    onOpenRightSidebar: (RightSidebarType) -> Unit,
    onImageClick: (List<CivitaiImage>, Int, String) -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val viewModel: GalleryViewModel = hiltViewModel(activity)
    val favViewModel: FavoritesViewModel = hiltViewModel(activity)
    val uiState by viewModel.uiState.collectAsState()
    val favoritesState by favViewModel.uiState.collectAsState()

    val permissions =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            results ->
            if (results.values.all { it }) viewModel.refresh()
        }

    val manageStorageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                if (android.os.Environment.isExternalStorageManager()) viewModel.refresh()
        }

    LaunchedEffect(uiState.downloadPath) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                val intent =
                    Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        .apply { data = android.net.Uri.parse("package:${context.packageName}") }

                try {
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intentAll =
                        Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)

                    manageStorageLauncher.launch(intentAll)
                }
            } else {
                viewModel.refresh()
            }
        } else {
            val allGranted = permissions.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (!allGranted) permissionLauncher.launch(permissions) else viewModel.refresh()
        }
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

    AppScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_gallery)) },
                actions = {
                    IconButton(onClick = { onOpenRightSidebar(RightSidebarType.SETTINGS) }) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier =
                    Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                    ),
            )
        },
        bottomBar = { MainBottomBar(currentRoute, onNavigate) },
        gridState = gridState,
        isLoading = uiState.isLoading,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            ImageGrid(
                images = uiState.images,
                imageLoader = imageLoader,
                state = gridState,
                isLoading = uiState.isLoading,
                favoriteIds = favoritesState.favoriteIds,
                columnCount = 3,
                showFavorite = false,
                viewMode = "gallery",
                onGetFavoriteFlow = { favViewModel.getFavoriteFlow(it) },
                onEnsureFavoriteResources = { favViewModel.ensureFavoriteResources(it) },
                onImageClick = { image ->
                    val index = uiState.images.indexOf(image)

                    if (index != -1) onImageClick(uiState.images, index, "gallery")
                },
                onToggleFavorite = {},
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )

            if (uiState.images.isEmpty() && !uiState.isLoading) EmptyState("gallery")
        }
    }
}
