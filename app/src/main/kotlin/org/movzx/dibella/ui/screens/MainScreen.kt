package org.movzx.dibella.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import kotlinx.coroutines.launch
import org.movzx.dibella.MainActivity
import org.movzx.dibella.R
import org.movzx.dibella.RightSidebarType
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.ui.components.*
import org.movzx.dibella.viewmodel.*

@Composable
fun UiMessageEffect(uiMessage: kotlinx.coroutines.flow.Flow<Int>) {
    val context = LocalContext.current

    LaunchedEffect(uiMessage) {
        uiMessage.collect { resId ->
            android.widget.Toast.makeText(context, resId, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onShowFilters: (() -> Unit)?,
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

@Composable
fun MainScreen(imageLoader: ImageLoader) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val videoPlayerManager = remember { VideoPlayerManager(context) }
    val feedViewModel: FeedViewModel = hiltViewModel(activity)
    val favoritesViewModel: FavoritesViewModel = hiltViewModel(activity)
    val galleryViewModel: GalleryViewModel = hiltViewModel(activity)
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
    val feedUiState by feedViewModel.uiState.collectAsState()
    val favoritesUiState by favoritesViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val feedGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()
    val galleryGridState = rememberLazyStaggeredGridState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var favoritesRestored by remember { mutableStateOf(false) }
    var galleryRestored by remember { mutableStateOf(false) }
    var rightSidebarType by remember { mutableStateOf(RightSidebarType.FILTERS) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var fullScreenImages by remember { mutableStateOf<List<CivitaiImage>>(emptyList()) }
    var fullScreenViewMode by remember { mutableStateOf("feed") }
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val exitConfirmMsg = stringResource(R.string.msg_exit_confirm)

    LaunchedEffect(settingsViewModel.exitEvent) {
        settingsViewModel.exitEvent.collect {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)

            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            (context as? Activity)?.finishAffinity()
        }
    }

    val dirPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )

                settingsViewModel.updateDownloadPath(it.toString())
            }
        }

    val favDirPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )

                settingsViewModel.updateFavoritesPath(it.toString())
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let { settingsViewModel.exportData(it) }
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { settingsViewModel.importData(it) }
        }

    UiMessageEffect(settingsViewModel.uiMessage)
    UiMessageEffect(feedViewModel.uiMessage)
    UiMessageEffect(favoritesViewModel.uiMessage)
    UiMessageEffect(galleryViewModel.uiMessage)

    val leftParallax by
        animateFloatAsState(
            targetValue = if (leftDrawerState.targetValue == DrawerValue.Open) 40f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "leftParallax",
        )

    val rightParallax by
        animateFloatAsState(
            targetValue = if (rightDrawerState.targetValue == DrawerValue.Open) -40f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "rightParallax",
        )

    CompositionLocalProvider(LocalVideoPlayerManager provides videoPlayerManager) {
        val sidebarColor =
            if (settingsState.amoledMode) androidx.compose.ui.graphics.Color.Black
            else MaterialTheme.colorScheme.surface

        ModalNavigationDrawer(
            drawerState = leftDrawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp).fillMaxHeight(),
                    drawerContainerColor = sidebarColor,
                    drawerShape = androidx.compose.ui.graphics.RectangleShape,
                ) {
                    if (currentRoute == "feed") {
                        DisplaySidebar(
                            currentRoute = currentRoute,
                            pageLimit = feedUiState.pageLimit,
                            gridColumns = feedUiState.gridColumns,
                            type = "all",
                            amoledMode = settingsState.amoledMode,
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
                            amoledMode = settingsState.amoledMode,
                            onDismiss = { scope.launch { leftDrawerState.close() } },
                            onUpdatePageLimit = {},
                            onUpdateGridColumns = { favoritesViewModel.updateGridColumns(it) },
                            onUpdateType = { favoritesViewModel.updateType(it) },
                            onScanDuplicates = {
                                scope.launch {
                                    favoritesViewModel.findDuplicates()
                                    leftDrawerState.close()
                                }
                            },
                        )
                    } else if (currentRoute == "gallery") {
                        val galleryUiState by galleryViewModel.uiState.collectAsState()
                        DisplaySidebar(
                            currentRoute = currentRoute,
                            pageLimit = 100,
                            gridColumns = galleryUiState.gridColumns,
                            type = galleryUiState.type,
                            amoledMode = settingsState.amoledMode,
                            onDismiss = { scope.launch { leftDrawerState.close() } },
                            onUpdatePageLimit = {},
                            onUpdateGridColumns = { galleryViewModel.updateGridColumns(it) },
                            onUpdateType = { galleryViewModel.updateType(it) },
                            onScanDuplicates = {
                                scope.launch {
                                    galleryViewModel.findDuplicates()
                                    leftDrawerState.close()
                                }
                            },
                        )
                    }
                }
            },
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ModalNavigationDrawer(
                    drawerState = rightDrawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Ltr
                        ) {
                            if (
                                rightSidebarType == RightSidebarType.FILTERS &&
                                    currentRoute == "feed"
                            ) {
                                FilterSidebar(
                                    nsfw = feedUiState.nsfw,
                                    sort = feedUiState.sort,
                                    period = feedUiState.period,
                                    type = feedUiState.type,
                                    tagIds = feedUiState.tagIds,
                                    amoledMode = settingsState.amoledMode,
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
                                    favoritesPath = settingsState.favoritesPath,
                                    debugEnabled = settingsState.debugEnabled,
                                    hidePlayerControls = settingsState.hidePlayerControls,
                                    alwaysEnableHD = settingsState.alwaysEnableHD,
                                    alwaysMuteVideo = settingsState.alwaysMuteVideo,
                                    feedVideoAutoplay = settingsState.feedVideoAutoplay,
                                    amoledMode = settingsState.amoledMode,
                                    onDismiss = { scope.launch { rightDrawerState.close() } },
                                    onClearCache = { settingsViewModel.clearImageCache() },
                                    onSaveApiKey = { settingsViewModel.updateApiKey(it) },
                                    onUpdateDownloadPath = {
                                        settingsViewModel.updateDownloadPath(it)
                                    },
                                    onUpdateFavoritesPath = {
                                        settingsViewModel.updateFavoritesPath(it)
                                    },
                                    onPickDirectory = { dirPickerLauncher.launch(null) },
                                    onPickFavoritesDirectory = {
                                        favDirPickerLauncher.launch(null)
                                    },
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
                                    onAlwaysEnableHD = {
                                        settingsViewModel.updateAlwaysEnableHD(it)
                                    },
                                    onAlwaysMuteVideo = {
                                        settingsViewModel.updateAlwaysMuteVideo(it)
                                    },
                                    onFeedVideoAutoplay = {
                                        settingsViewModel.updateFeedVideoAutoplay(it)
                                    },
                                    onToggleAmoled = { settingsViewModel.updateAmoledMode(it) },
                                )
                            }
                        }
                    },
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .graphicsLayer { translationX = leftParallax + rightParallax }
                                    .background(
                                        if (settingsState.amoledMode)
                                            androidx.compose.ui.graphics.SolidColor(
                                                androidx.compose.ui.graphics.Color.Black
                                            )
                                        else
                                            Brush.radialGradient(
                                                colors =
                                                    listOf(
                                                        colorResource(R.color.background_center),
                                                        colorResource(R.color.background),
                                                    )
                                            )
                                    )
                        ) {
                            if (!settingsState.amoledMode) {
                                val glowTransition =
                                    rememberInfiniteTransition(label = "ambientGlow")

                                val glowOffset by
                                    glowTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec =
                                            infiniteRepeatable(
                                                animation = tween(8000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart,
                                            ),
                                        label = "glowOffset",
                                    )

                                val glowAlpha by
                                    glowTransition.animateFloat(
                                        initialValue = 0.03f,
                                        targetValue = 0.08f,
                                        animationSpec =
                                            infiniteRepeatable(
                                                animation = tween(6000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse,
                                            ),
                                        label = "glowAlpha",
                                    )

                                val glowX by
                                    glowTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 0.7f,
                                        animationSpec =
                                            infiniteRepeatable(
                                                animation = tween(10000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse,
                                            ),
                                        label = "glowX",
                                    )

                                val glowY by
                                    glowTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 0.7f,
                                        animationSpec =
                                            infiniteRepeatable(
                                                animation = tween(12000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse,
                                            ),
                                        label = "glowY",
                                    )

                                val glowColors =
                                    listOf(
                                        colorResource(R.color.primary).copy(alpha = glowAlpha),
                                        colorResource(R.color.secondary)
                                            .copy(alpha = glowAlpha * 0.5f),
                                        androidx.compose.ui.graphics.Color.Transparent,
                                    )

                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val size = size.minDimension
                                    val x = glowX * size
                                    val y = glowY * size

                                    drawCircle(
                                        brush = Brush.radialGradient(colors = glowColors),
                                        radius = size * 0.4f,
                                        center = androidx.compose.ui.geometry.Offset(x, y),
                                    )
                                }
                            }
                            val startDestination = settingsState.lastRoute ?: return@Box

                            AppNavigation(
                                navController = navController,
                                startDestination = startDestination,
                                imageLoader = imageLoader,
                                feedVideoAutoplay = settingsState.feedVideoAutoplay,
                                amoledMode = settingsState.amoledMode,
                                favoritesPath = settingsState.effectiveFavoritesPath,
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
                                onUpdateLastRoute = { settingsViewModel.updateLastRoute(it) },
                                leftDrawerState = leftDrawerState,
                                rightDrawerState = rightDrawerState,
                                scope = scope,
                                selectedImageIndex = selectedImageIndex,
                                backPressedTime = backPressedTime,
                                onUpdateBackPressedTime = { backPressedTime = it },
                                exitConfirmMsg = exitConfirmMsg,
                            )

                            AppBackHandler(
                                enabled = true,
                                isSelectionMode = false,
                                clearSelection = {},
                                leftDrawerState = leftDrawerState,
                                rightDrawerState = rightDrawerState,
                                scope = scope,
                                backPressedTime = backPressedTime,
                                onUpdateBackPressedTime = { backPressedTime = it },
                                exitConfirmMsg = exitConfirmMsg,
                            )

                            AnimatedVisibility(
                                visible = selectedImageIndex != null,
                                enter =
                                    fadeIn(animationSpec = tween(400)) +
                                        scaleIn(
                                            initialScale = 0.9f,
                                            animationSpec =
                                                spring(dampingRatio = 0.8f, stiffness = 300f),
                                        ),
                                exit =
                                    fadeOut(animationSpec = tween(300)) +
                                        scaleOut(
                                            targetScale = 0.9f,
                                            animationSpec =
                                                spring(dampingRatio = 0.8f, stiffness = 300f),
                                        ),
                            ) {
                                val targetIndex = selectedImageIndex ?: 0

                                val activeViewModel: org.movzx.dibella.viewmodel.BaseViewModel =
                                    when (fullScreenViewMode) {
                                        "feed" -> feedViewModel
                                        "favorites" -> favoritesViewModel
                                        else -> galleryViewModel
                                    }

                                val currentDownloadProgresses =
                                    when (fullScreenViewMode) {
                                        "feed" -> feedUiState.downloadProgresses
                                        "favorites" -> favoritesUiState.downloadProgresses
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
                                    favoriteIds = favoritesUiState.favoriteIds,
                                    downloadedIds =
                                        galleryViewModel.uiState
                                            .collectAsState()
                                            .value
                                            .downloadedIds,
                                    downloadProgresses = currentDownloadProgresses,
                                    viewMode = fullScreenViewMode,
                                    favoritesPath = settingsState.effectiveFavoritesPath,
                                    hidePlayerControls = settingsState.hidePlayerControls,
                                    alwaysEnableHD = settingsState.alwaysEnableHD,
                                    alwaysMuteVideo = settingsState.alwaysMuteVideo,
                                    autoplayEnabled = settingsState.feedVideoAutoplay,
                                    onGetFavoriteFlow = { favoritesViewModel.getFavoriteFlow(it) },
                                    onEnsureFavoriteResources = { img, force, onProgress ->
                                        activeViewModel.ensureFavoriteResourcesThrottled(
                                            img,
                                            force,
                                            onProgress,
                                        )
                                    },
                                    onToggleFavorite = { activeViewModel.toggleFavorite(it) },
                                    onEnsureFavoriteResourcesThrottled = { img, force, onProgress ->
                                        activeViewModel.ensureFavoriteResourcesThrottled(
                                            img,
                                            force,
                                            onProgress,
                                        )
                                    },
                                    onDownloadImage = { activeViewModel.downloadImage(it) },
                                    onDeleteLocalFile = { galleryViewModel.deleteLocalFile(it) },
                                    onDismiss = { selectedImageIndex = null },
                                    onIndexChange = { selectedImageIndex = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    imageLoader: ImageLoader,
    feedVideoAutoplay: Boolean,
    amoledMode: Boolean,
    favoritesPath: String?,
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
                feedVideoAutoplay = feedVideoAutoplay,
                amoledMode = amoledMode,
                favoritesPath = favoritesPath,
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
            )
        }

        composable("favorites") {
            FavoritesScreen(
                imageLoader = imageLoader,
                gridState = favoritesGridState,
                feedVideoAutoplay = feedVideoAutoplay,
                amoledMode = amoledMode,
                favoritesPath = favoritesPath,
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
            )
        }

        composable("gallery") {
            GalleryScreen(
                imageLoader = imageLoader,
                gridState = galleryGridState,
                feedVideoAutoplay = feedVideoAutoplay,
                amoledMode = amoledMode,
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
            )
        }
    }
}
