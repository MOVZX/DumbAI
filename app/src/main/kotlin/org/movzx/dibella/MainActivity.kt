package org.movzx.dibella

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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
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
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.ui.components.*
import org.movzx.dibella.ui.theme.DibellaTheme
import org.movzx.dibella.util.resolveUriToPath
import org.movzx.dibella.viewmodel.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DibellaTheme { MainScreen(imageLoader) } }
    }
}

@Composable
fun UiMessageEffect(uiMessage: kotlinx.coroutines.flow.Flow<Int>) {
    val context = LocalContext.current

    LaunchedEffect(uiMessage) {
        uiMessage.collect { resId ->
            android.widget.Toast.makeText(
                    context,
                    context.getString(resId),
                    android.widget.Toast.LENGTH_SHORT,
                )
                .show()
        }
    }
}

@Composable
fun AppBackHandler(
    enabled: Boolean,
    isSelectionMode: Boolean = false,
    clearSelection: () -> Unit = {},
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    backPressedTime: Long,
    onUpdateBackPressedTime: (Long) -> Unit,
    exitConfirmMsg: String,
) {
    val context = LocalContext.current
    BackHandler(enabled = enabled) {
        if (isSelectionMode) {
            clearSelection()
        } else if (leftDrawerState.isOpen) {
            scope.launch { leftDrawerState.close() }
        } else if (rightDrawerState.isOpen) {
            scope.launch { rightDrawerState.close() }
        } else {
            val currentTime = System.currentTimeMillis()

            if (currentTime - backPressedTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                onUpdateBackPressedTime(currentTime)
                android.widget.Toast.makeText(
                        context,
                        exitConfirmMsg,
                        android.widget.Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
    }
}

@Composable
fun ScrollRestorationEffect(
    gridState: LazyStaggeredGridState,
    isRestored: Boolean,
    imagesNotEmpty: Boolean,
    scrollIndex: Int,
    scrollOffset: Int,
    scrollPositionType: String,
    saveScrollPosition: (String, Int, Int) -> Unit,
    markRestored: () -> Unit,
) {
    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        if (isRestored) {
            kotlinx.coroutines.delay(500)

            saveScrollPosition(
                scrollPositionType,
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset,
            )
        }
    }

    LaunchedEffect(imagesNotEmpty) {
        if (!isRestored && imagesNotEmpty) {
            gridState.scrollToItem(scrollIndex, scrollOffset)
            markRestored()
        }
    }
}

@Composable
fun InteractiveTopBar(
    isSelectionMode: Boolean,
    isShowingDuplicates: Boolean,
    selectedIdsSize: Int,
    duplicateGroupsSize: Int,
    gridColumns: Int,
    selectionActionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onShowBatchConfirmDialog: () -> Unit,
    onClearDuplicatesMode: () -> Unit,
    onRemoveDuplicates: () -> Unit,
    onOpenLeftSidebar: () -> Unit,
    onUpdateGridColumns: (Int) -> Unit,
    onShowFilters: () -> Unit,
    onShowSettings: () -> Unit,
) {
    if (isSelectionMode) {
        SelectionTopBar(
            selectedCount = selectedIdsSize,
            onClose = onClearSelection,
            onSelectAll = onSelectAll,
            onAction = onShowBatchConfirmDialog,
            actionIcon = selectionActionIcon,
        )
    } else if (isShowingDuplicates) {
        SelectionTopBar(
            selectedCount = duplicateGroupsSize,
            onClose = onClearDuplicatesMode,
            onSelectAll = {},
            onAction = onRemoveDuplicates,
            actionIcon = Icons.Default.DeleteSweep,
            title = stringResource(R.string.found_duplicates),
        )
    } else {
        MainTopBar(
            gridColumns = gridColumns,
            onShowDisplayOptions = onOpenLeftSidebar,
            onUpdateGridColumns = onUpdateGridColumns,
            onShowFilters = onShowFilters,
            onShowSettings = onShowSettings,
        )
    }
}

enum class RightSidebarType {
    FILTERS,
    SETTINGS,
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(imageLoader: ImageLoader) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    val permissionsToRequest =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            listOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    val launcher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
        }

    LaunchedEffect(Unit) {
        val needed = permissionsToRequest.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) launcher.launch(needed.toTypedArray())
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var fullScreenImages by remember { mutableStateOf<List<CivitaiImage>>(emptyList()) }
    var fullScreenViewMode by remember { mutableStateOf("") }
    val feedGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()
    val galleryGridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val exitConfirmMsg = stringResource(R.string.msg_exit_confirm)
    val leftDrawerState = rememberDrawerState(DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(DrawerValue.Closed)
    var rightSidebarType by remember { mutableStateOf(RightSidebarType.FILTERS) }
    var gestureSide by remember { mutableStateOf(LayoutDirection.Rtl) }

    LaunchedEffect(rightDrawerState.isClosed, currentRoute) {
        if (rightDrawerState.isClosed) {
            rightSidebarType =
                if (currentRoute == "feed") RightSidebarType.FILTERS else RightSidebarType.SETTINGS
        }
    }

    val leftGesturesEnabled =
        selectedImageIndex == null &&
            (leftDrawerState.isOpen ||
                (gestureSide == LayoutDirection.Ltr && rightDrawerState.isClosed))

    val rightGesturesEnabled =
        selectedImageIndex == null &&
            (rightDrawerState.isOpen ||
                (gestureSide == LayoutDirection.Rtl && leftDrawerState.isClosed))

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
    var showScanDuplicatesDialog by remember { mutableStateOf(false) }

    if (showScanDuplicatesDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_scan_duplicates_title),
            message = stringResource(R.string.dialog_scan_duplicates_msg),
            onConfirm = {
                if (currentRoute == "favorites") favoritesViewModel.findDuplicates()
                else if (currentRoute == "gallery") galleryViewModel.findDuplicates()

                showScanDuplicatesDialog = false
            },
            onDismiss = { showScanDuplicatesDialog = false },
        )
    }

    UiMessageEffect(settingsViewModel.uiMessage)
    UiMessageEffect(feedViewModel.uiMessage)
    UiMessageEffect(favoritesViewModel.uiMessage)
    UiMessageEffect(galleryViewModel.uiMessage)

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

    AppBackHandler(
        enabled = selectedImageIndex == null,
        leftDrawerState = leftDrawerState,
        rightDrawerState = rightDrawerState,
        scope = scope,
        backPressedTime = backPressedTime,
        onUpdateBackPressedTime = { backPressedTime = it },
        exitConfirmMsg = exitConfirmMsg,
    )

    ModalNavigationDrawer(
        drawerState = leftDrawerState,
        gesturesEnabled = leftGesturesEnabled,
        drawerContent = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: ""

            if (currentRoute == "feed") {
                DisplaySidebar(
                    currentRoute = currentRoute,
                    pageLimit = feedUiState.pageLimit,
                    gridColumns = feedUiState.gridColumns,
                    type = "all",
                    onDismiss = { scope.launch { leftDrawerState.close() } },
                    onUpdatePageLimit = { feedViewModel.updatePageLimit(it) },
                    onUpdateGridColumns = { feedViewModel.updateGridColumns(it) },
                    onUpdateType = {},
                )
            } else if (currentRoute == "favorites") {
                DisplaySidebar(
                    currentRoute = currentRoute,
                    pageLimit = 100,
                    gridColumns = favoritesUiState.gridColumns,
                    type = favoritesUiState.type,
                    onDismiss = { scope.launch { leftDrawerState.close() } },
                    onUpdatePageLimit = {},
                    onUpdateGridColumns = { favoritesViewModel.updateGridColumns(it) },
                    onUpdateType = { favoritesViewModel.updateType(it) },
                    onScanDuplicates = {
                        showScanDuplicatesDialog = true
                        scope.launch { leftDrawerState.close() }
                    },
                )
            } else if (currentRoute == "gallery") {
                val galleryUiState by galleryViewModel.uiState.collectAsState()

                DisplaySidebar(
                    currentRoute = currentRoute,
                    pageLimit = 100,
                    gridColumns = galleryUiState.gridColumns,
                    type = galleryUiState.type,
                    onDismiss = { scope.launch { leftDrawerState.close() } },
                    onUpdatePageLimit = {},
                    onUpdateGridColumns = { galleryViewModel.updateGridColumns(it) },
                    onUpdateType = { galleryViewModel.updateType(it) },
                    onScanDuplicates = {
                        showScanDuplicatesDialog = true
                        scope.launch { leftDrawerState.close() }
                    },
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = rightDrawerState,
                gesturesEnabled = rightGesturesEnabled,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        if (
                            rightSidebarType == RightSidebarType.FILTERS && currentRoute == "feed"
                        ) {
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
                                hidePlayerControls = settingsState.hidePlayerControls,
                                alwaysEnableHD = settingsState.alwaysEnableHD,
                                alwaysMuteVideo = settingsState.alwaysMuteVideo,
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
                                onHidePlayerControls = {
                                    settingsViewModel.updateHidePlayerControls(it)
                                },
                                onAlwaysEnableHD = { settingsViewModel.updateAlwaysEnableHD(it) },
                                onAlwaysMuteVideo = { settingsViewModel.updateAlwaysMuteVideo(it) },
                            )
                        }
                    }
                },
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val down = event.changes.find { it.changedToDown() }

                                        if (
                                            down != null &&
                                                leftDrawerState.isClosed &&
                                                rightDrawerState.isClosed
                                        ) {
                                            val x = down.position.x
                                            val threshold = 80.dp.toPx()

                                            if (x < threshold) gestureSide = LayoutDirection.Ltr
                                            else if (x > size.width - threshold)
                                                gestureSide = LayoutDirection.Rtl
                                        }
                                    }
                                }
                            }
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
                                    val startDestination =
                                        settingsState.lastRoute ?: return@AnimatedContent
                                    AppNavigation(
                                        navController = navController,
                                        startDestination = startDestination,
                                        imageLoader = imageLoader,
                                        feedGridState = feedGridState,
                                        favoritesGridState = favoritesGridState,
                                        galleryGridState = galleryGridState,
                                        favoritesRestored = favoritesRestored,
                                        onFavoritesRestored = { favoritesRestored = it },
                                        galleryRestored = galleryRestored,
                                        onGalleryRestored = { galleryRestored = it },
                                        onOpenLeftSidebar = {
                                            scope.launch { leftDrawerState.open() }
                                        },
                                        onOpenRightSidebar = { type ->
                                            rightSidebarType = type
                                            scope.launch { rightDrawerState.open() }
                                        },
                                        onImageClick = { images, index, viewMode ->
                                            fullScreenImages = images
                                            selectedImageIndex = index
                                            fullScreenViewMode = viewMode
                                        },
                                        onUpdateLastRoute = {
                                            settingsViewModel.updateLastRoute(it)
                                        },
                                        leftDrawerState = leftDrawerState,
                                        rightDrawerState = rightDrawerState,
                                        scope = scope,
                                        selectedImageIndex = selectedImageIndex,
                                        backPressedTime = backPressedTime,
                                        onUpdateBackPressedTime = { backPressedTime = it },
                                        exitConfirmMsg = exitConfirmMsg,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@AnimatedContent,
                                    )
                                } else {
                                    val favoriteIds by favoritesViewModel.uiState.collectAsState()
                                    val galleryState by galleryViewModel.uiState.collectAsState()

                                    val activeViewModel: org.movzx.dibella.viewmodel.BaseViewModel =
                                        when (fullScreenViewMode) {
                                            "feed" -> feedViewModel
                                            "favorites" -> favoritesViewModel
                                            else -> galleryViewModel
                                        }

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
                                        downloadedIds = galleryState.downloadedIds,
                                        downloadProgresses = downloadProgresses,
                                        viewMode = fullScreenViewMode,
                                        hidePlayerControls = settingsState.hidePlayerControls,
                                        alwaysEnableHD = settingsState.alwaysEnableHD,
                                        alwaysMuteVideo = settingsState.alwaysMuteVideo,
                                        onGetFavoriteFlow = {
                                            favoritesViewModel.getFavoriteFlow(it)
                                        },
                                        onEnsureFavoriteResources = { img, force, onProgress ->
                                            activeViewModel.ensureFavoriteResourcesThrottled(
                                                img,
                                                force,
                                                onProgress,
                                            )
                                        },
                                        onToggleFavorite = { activeViewModel.toggleFavorite(it) },
                                        onEnsureFavoriteResourcesThrottled = {
                                            img,
                                            force,
                                            onProgress ->
                                            activeViewModel.ensureFavoriteResourcesThrottled(
                                                img,
                                                force,
                                                onProgress,
                                            )
                                        },
                                        onDownloadImage = { activeViewModel.downloadImage(it) },
                                        onDeleteLocalFile = {
                                            galleryViewModel.deleteLocalFile(it)
                                        },
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
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
    onUpdateLastRoute: (String) -> Unit,
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    selectedImageIndex: Int?,
    backPressedTime: Long,
    onUpdateBackPressedTime: (Long) -> Unit,
    exitConfirmMsg: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) { currentRoute?.let { onUpdateLastRoute(it) } }

    val onNavigate: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }

                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("feed") {
            FeedScreen(
                imageLoader = imageLoader,
                gridState = feedGridState,
                onOpenLeftSidebar = onOpenLeftSidebar,
                onOpenRightSidebar = onOpenRightSidebar,
                onImageClick = onImageClick,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                leftDrawerState = leftDrawerState,
                rightDrawerState = rightDrawerState,
                scope = scope,
                selectedImageIndex = selectedImageIndex,
                backPressedTime = backPressedTime,
                onUpdateBackPressedTime = onUpdateBackPressedTime,
                exitConfirmMsg = exitConfirmMsg,
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
                favoritesRestored = favoritesRestored,
                onFavoritesRestored = onFavoritesRestored,
                leftDrawerState = leftDrawerState,
                rightDrawerState = rightDrawerState,
                scope = scope,
                selectedImageIndex = selectedImageIndex,
                backPressedTime = backPressedTime,
                onUpdateBackPressedTime = onUpdateBackPressedTime,
                exitConfirmMsg = exitConfirmMsg,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

        composable("gallery") {
            GalleryScreen(
                imageLoader = imageLoader,
                gridState = galleryGridState,
                onOpenLeftSidebar = onOpenLeftSidebar,
                onOpenRightSidebar = onOpenRightSidebar,
                onImageClick = onImageClick,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                galleryRestored = galleryRestored,
                onGalleryRestored = onGalleryRestored,
                leftDrawerState = leftDrawerState,
                rightDrawerState = rightDrawerState,
                scope = scope,
                selectedImageIndex = selectedImageIndex,
                backPressedTime = backPressedTime,
                onUpdateBackPressedTime = onUpdateBackPressedTime,
                exitConfirmMsg = exitConfirmMsg,
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
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    selectedImageIndex: Int?,
    backPressedTime: Long,
    onUpdateBackPressedTime: (Long) -> Unit,
    exitConfirmMsg: String,
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
                    downloadProgresses = uiState.downloadProgresses,
                    columnCount = uiState.gridColumns,
                    showFavorite = true,
                    viewMode = "feed",
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
    favoritesRestored: Boolean,
    onFavoritesRestored: (Boolean) -> Unit,
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    selectedImageIndex: Int?,
    backPressedTime: Long,
    onUpdateBackPressedTime: (Long) -> Unit,
    exitConfirmMsg: String,
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

    AppScaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedIds.size,
                    onClose = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onAction = { showBatchConfirmDialog = true },
                    actionIcon = Icons.Default.HeartBroken,
                )
            } else if (uiState.isShowingDuplicates) {
                SelectionTopBar(
                    selectedCount = uiState.duplicateGroups.flatten().size,
                    onClose = { viewModel.clearDuplicatesMode() },
                    onSelectAll = {},
                    onAction = { viewModel.removeDuplicates() },
                    actionIcon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.found_duplicates),
                )
            } else {
                MainTopBar(
                    gridColumns = uiState.gridColumns,
                    onShowDisplayOptions = onOpenLeftSidebar,
                    onUpdateGridColumns = { viewModel.updateGridColumns(it) },
                    onShowFilters = {},
                    onShowSettings = { onOpenRightSidebar(RightSidebarType.SETTINGS) },
                )
            }
        },
        bottomBar = { MainBottomBar(currentRoute, onNavigate) },
        gridState = gridState,
        isLoading = uiState.isLoading,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
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
                isSelectionMode = uiState.isSelectionMode,
                selectedIds = uiState.selectedIds,
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
                onToggleSelection = { viewModel.toggleSelection(it) },
                onLongClick = { viewModel.toggleSelection(it) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )

            if (!uiState.isShowingDuplicates && uiState.images.isEmpty() && !uiState.isLoading)
                EmptyState("favorites")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    imageLoader: ImageLoader,
    gridState: LazyStaggeredGridState,
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

    var showBatchConfirmDialog by remember { mutableStateOf(false) }

    ScrollRestorationEffect(
        gridState = gridState,
        isRestored = uiState.isRestored,
        imagesNotEmpty = uiState.images.isNotEmpty(),
        scrollIndex = uiState.scrollIndex,
        scrollOffset = uiState.scrollOffset,
        scrollPositionType = "gallery",
        saveScrollPosition = { type, idx, offset ->
            viewModel.saveScrollPosition(type, idx, offset)
        },
        markRestored = { viewModel.markRestored() },
    )

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
            title = stringResource(R.string.dialog_batch_delete_title),
            message = stringResource(R.string.dialog_batch_delete_msg, uiState.selectedIds.size),
            onConfirm = {
                viewModel.batchDelete()

                showBatchConfirmDialog = false
            },
            onDismiss = { showBatchConfirmDialog = false },
        )
    }

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
                onShowFilters = {},
                onShowSettings = { onOpenRightSidebar(RightSidebarType.SETTINGS) },
            )
        },
        bottomBar = { MainBottomBar(currentRoute, onNavigate) },
        gridState = gridState,
        isLoading = uiState.isLoading,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            ImageGrid(
                images =
                    if (uiState.isShowingDuplicates) uiState.duplicateGroups.flatten()
                    else uiState.images,
                imageLoader = imageLoader,
                state = gridState,
                isLoading = uiState.isLoading,
                favoriteIds = favoritesState.favoriteIds,
                downloadProgresses = favoritesState.downloadProgresses,
                columnCount = uiState.gridColumns,
                showFavorite = false,
                viewMode = "gallery",
                isSelectionMode = uiState.isSelectionMode,
                selectedIds = uiState.selectedIds,
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
                onToggleSelection = { viewModel.toggleSelection(it) },
                onLongClick = { viewModel.toggleSelection(it) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )

            if (!uiState.isShowingDuplicates && uiState.images.isEmpty() && !uiState.isLoading)
                EmptyState("gallery")
        }
    }
}
