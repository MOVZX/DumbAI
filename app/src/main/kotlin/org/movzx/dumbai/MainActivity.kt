package org.movzx.dumbai

import android.content.Intent
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import org.movzx.dumbai.util.getThumbnailUrl
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.movzx.dumbai.data.AppDatabase
import org.movzx.dumbai.data.FavoritesRepository
import org.movzx.dumbai.data.UserPreferencesRepository
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.ui.theme.DumbAITheme
import org.movzx.dumbai.viewmodel.MainViewModel
import org.movzx.dumbai.viewmodel.ViewMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = UserPreferencesRepository(applicationContext)
        val favoritesRepository = FavoritesRepository(applicationContext, database.favoriteImageDao())
        val viewModel = MainViewModel(applicationContext, repository, favoritesRepository, database.feedDao())

        setContent {
            DumbAITheme {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val feedImages by viewModel.feedImages.collectAsState()
    val favoriteImages by viewModel.favoriteImages.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val pageLimit by viewModel.pageLimit.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val downloadPath by viewModel.downloadPath.collectAsState()
    val downloadProgresses by viewModel.downloadProgresses.collectAsState()

    val nsfw by viewModel.nsfw.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val period by viewModel.period.collectAsState()
    val type by viewModel.type.collectAsState()
    val tagId by viewModel.tagId.collectAsState()
    val feedScrollIndex by viewModel.feedScrollIndex.collectAsState()
    val feedScrollOffset by viewModel.feedScrollOffset.collectAsState()

    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showDisplayOptions by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState()
    val settingsSheetState = rememberModalBottomSheetState()
    val displaySheetState = rememberModalBottomSheetState()

    val feedGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()
    val galleryGridState = rememberLazyStaggeredGridState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val path = resolveUriToPath(context, it)

            viewModel.updateDownloadPath(path)
        }
    }

    val manageFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager())
                viewModel.loadGallery()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    LaunchedEffect(viewMode) {
        if (viewMode == ViewMode.GALLERY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }

                    manageFilesLauncher.launch(intent)
                } else {
                    viewModel.loadGallery()
                }
            } else {
                viewModel.loadGallery()
            }
        }
    }

    LaunchedEffect(feedImages.isNotEmpty()) {
        if (feedImages.isNotEmpty() && viewMode == ViewMode.FEED)
            feedGridState.scrollToItem(feedScrollIndex, feedScrollOffset)
    }

    LaunchedEffect(feedGridState) {
        snapshotFlow {
            val firstItem = feedGridState.layoutInfo.visibleItemsInfo.firstOrNull()

            firstItem?.index to firstItem?.offset?.y
        }.collect { (index, offset) ->
            if (index != null && offset != null && viewMode == ViewMode.FEED)
                viewModel.saveScrollPosition(index, offset)
        }
    }

    LaunchedEffect(viewModel.downloadStatus) {
        viewModel.downloadStatus.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleFavoritesView() }) {
                        Icon(
                            if (viewMode == ViewMode.FAVORITES)
                                Icons.Filled.Favorite
                            else
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorites",
                                tint = Color.White
                        )
                    }

                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                if (viewMode != ViewMode.GALLERY)
                                    viewModel.setViewMode(ViewMode.GALLERY)
                                else
                                    viewModel.setViewMode(ViewMode.FEED)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Gallery",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    IconButton(onClick = { showDisplayOptions = true }) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Display Options",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }

            }
        },
        floatingActionButton = {
            val currentGridState = when(viewMode) {
                ViewMode.FEED -> feedGridState
                ViewMode.FAVORITES -> favoritesGridState
                ViewMode.GALLERY -> galleryGridState
            }
            val currentImages = when(viewMode) {
                ViewMode.FEED -> feedImages
                ViewMode.FAVORITES -> favoriteImages
                ViewMode.GALLERY -> galleryImages
            }

            val showScrollToTop by remember(currentGridState) {
                derivedStateOf {
                    currentGridState.canScrollBackward
                }
            }

            val showScrollToBottom by remember(currentGridState) {
                derivedStateOf {
                    currentGridState.canScrollForward
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (viewMode != ViewMode.FAVORITES && !isLoading) {
                    FloatingActionButton(
                        onClick = { viewModel.refresh() },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }

                AnimatedVisibility(visible = showScrollToTop, enter = fadeIn(), exit = fadeOut()) {
                    FloatingActionButton(
                        onClick = { scope.launch { currentGridState.animateScrollToItem(0) } },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Top")
                    }
                }

                if (showScrollToBottom) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                if (currentImages.isNotEmpty())
                                    currentGridState.animateScrollToItem(currentImages.size - 1)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Bottom")
                    }
                }

                if (hasMore && viewMode == ViewMode.FEED) {
                    FloatingActionButton(
                        onClick = { if (!isLoading) viewModel.loadMore() },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.KeyboardDoubleArrowDown, contentDescription = "Load More")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val currentImages = when(viewMode) {
                ViewMode.FEED -> feedImages
                ViewMode.FAVORITES -> favoriteImages
                ViewMode.GALLERY -> galleryImages
            }
            val currentGridState = when(viewMode) {
                ViewMode.FEED -> feedGridState
                ViewMode.FAVORITES -> favoritesGridState
                ViewMode.GALLERY -> galleryGridState
            }

            ImageGrid(
                images = currentImages,
                viewModel = viewModel,
                state = currentGridState,
                isLoading = if (viewMode == ViewMode.FEED) isLoading else false,
                favoriteIds = favoriteIds,
                columnCount = gridColumns,
                showFavorite = viewMode != ViewMode.GALLERY,
                onImageClick = { image ->
                    val index = currentImages.indexOf(image)

                    if (index != -1)
                        selectedImageIndex = index
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) }
            )

            if (isLoading && currentImages.isEmpty()) {
                SkeletonGrid(columnCount = gridColumns)
            }

            if (currentImages.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val (message, icon) = when(viewMode) {
                        ViewMode.FAVORITES -> "You haven't added any favorites yet." to Icons.Default.FavoriteBorder
                        ViewMode.GALLERY -> "Your local gallery is currently empty." to Icons.Default.Collections
                        ViewMode.FEED -> "No results found for current filters." to Icons.Default.Search
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
        }
    }

    if (showSettings) {
        SettingsBottomSheet(
            cacheSize = cacheSize,
            apiKey = apiKey,
            nsfw = nsfw,
            sort = sort,
            period = period,
            type = type,
            tagId = tagId,
            pageLimit = pageLimit,
            gridColumns = gridColumns,
            downloadPath = downloadPath,
            sheetState = settingsSheetState,
            onDismiss = { showSettings = false },
            onClearCache = {
                viewModel.clearImageCache()
                showSettings = false
            },
            onSaveApiKey = { viewModel.updateApiKey(it) },
            onUpdateFilters = { n, s, p, t, tg ->
                viewModel.updateFilters(n, s, p, t, tg)
            },
            onUpdatePageLimit = { viewModel.updatePageLimit(it) },
            onUpdateGridColumns = { viewModel.updateGridColumns(it) },
            onUpdateDownloadPath = { viewModel.updateDownloadPath(it) },
            onPickDirectory = { dirPickerLauncher.launch(null) },
            onExport = { exportLauncher.launch("dumbai_backup.json") },
            onImport = { importLauncher.launch(arrayOf("application/json")) }
        )
    }

    if (showFilters) {
        FilterBottomSheet(
            nsfw = nsfw,
            sort = sort,
            period = period,
            type = type,
            tagId = tagId,
            sheetState = filterSheetState,
            onDismiss = { showFilters = false },
            onFilterChange = { n, s, p, t, tg ->
                viewModel.updateFilters(n, s, p, t, tg)
            }
        )
    }

    if (showDisplayOptions) {
        DisplayBottomSheet(
            pageLimit = pageLimit,
            gridColumns = gridColumns,
            sheetState = displaySheetState,
            onDismiss = { showDisplayOptions = false },
            onUpdatePageLimit = { viewModel.updatePageLimit(it) },
            onUpdateGridColumns = { viewModel.updateGridColumns(it) }
        )
    }

    selectedImageIndex?.let { index ->
        val currentImages = when(viewMode) {
            ViewMode.FEED -> feedImages
            ViewMode.FAVORITES -> favoriteImages
            ViewMode.GALLERY -> galleryImages
        }
        if (index < currentImages.size) {
            FullScreenImage(
                images = currentImages,
                initialIndex = index,
                viewModel = viewModel,
                favoriteIds = favoriteIds,
                downloadProgresses = downloadProgresses,
                viewMode = viewMode,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onDismiss = { selectedImageIndex = null }
            )
        } else {
            selectedImageIndex = null
        }
    }
}

data class CivitaiTag(val id: Int?, val name: String)

val tagOptions = listOf(
    CivitaiTag(null, "All Tags"),
    CivitaiTag(4, "Anime"),
    CivitaiTag(111768, "Animal"),
    CivitaiTag(5169, "Armor"),
    CivitaiTag(5193, "Clothing"),
    CivitaiTag(2435, "Costume"),
    CivitaiTag(5499, "Dragon"),
    CivitaiTag(111767, "Astronomy"),
    CivitaiTag(5207, "Fantasy"),
    CivitaiTag(5211, "Game Character"),
    CivitaiTag(8363, "Landscape"),
    CivitaiTag(617, "Modern Art"),
    CivitaiTag(111763, "Outdoors"),
    CivitaiTag(5241, "Photography"),
    CivitaiTag(172, "Photorealistic"),
    CivitaiTag(213, "Post Apocalyptic"),
    CivitaiTag(6594, "Robot"),
    CivitaiTag(3060, "Sci-Fi"),
    CivitaiTag(5133, "Woman")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    nsfw: String,
    sort: String,
    period: String,
    type: String,
    tagId: Int?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onFilterChange: (String, String, String, String, Int?) -> Unit
) {
    val nsfwOptions = listOf("None", "Soft", "Mature", "X")
    val sortOptions = listOf("Most Reactions", "Most Comments", "Newest")
    val periodOptions = listOf("AllTime", "Year", "Month", "Week", "Day")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filters", style = MaterialTheme.typography.headlineSmall)

            Column {
                Text("Content Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("image", "video").forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { onFilterChange(nsfw, sort, period, option, tagId) },
                            label = { Text(option.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            Column {
                Text("Tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tagOptions.forEach { tag ->
                        FilterChip(
                            selected = tagId == tag.id,
                            onClick = { onFilterChange(nsfw, sort, period, type, tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            Column {
                Text("NSFW Level", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    nsfwOptions.forEach { option ->
                        FilterChip(
                            selected = nsfw == option,
                            onClick = { onFilterChange(option, sort, period, type, tagId) },
                            label = { Text(option) }
                        )
                    }
                }
            }

            Column {
                Text("Sort By", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortOptions.forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { onFilterChange(nsfw, option, period, type, tagId) },
                            label = { Text(option) }
                        )
                    }
                }
            }

            Column {
                Text("Period", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periodOptions.forEach { option ->
                        FilterChip(
                            selected = period == option,
                            onClick = { onFilterChange(nsfw, sort, option, type, tagId) },
                            label = { Text(if (option == "AllTime") "All Time" else option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonGrid(columnCount: Int) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnCount),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalItemSpacing = 4.dp,
        userScrollEnabled = false
    ) {
        items(12) { index ->
            val aspectRatio = remember(index) { (8..14).random() / 10f }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(MaterialTheme.shapes.small)
                    .shimmerBackground()
            )
        }
    }
}

@Composable
fun ImageGrid(
    images: List<CivitaiImage>,
    viewModel: MainViewModel,
    state: LazyStaggeredGridState,
    isLoading: Boolean,
    favoriteIds: Set<Long>,
    columnCount: Int,
    showFavorite: Boolean,
    onImageClick: (CivitaiImage) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnCount),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalItemSpacing = 4.dp
    ) {
        items(images, key = { it.id }) { image ->
            ImageCard(
                image = image,
                viewModel = viewModel,
                isFavorite = favoriteIds.contains(image.id),
                showFavorite = showFavorite,
                onClick = onImageClick,
                onToggleFavorite = onToggleFavorite
            )
        }

        if (isLoading && images.isNotEmpty()) {
            items(columnCount * 2) { index ->
                val aspectRatio = remember(index) { (8..14).random() / 10f }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerBackground()
                )
            }
        }
    }
}

@Composable
fun ImageCard(
    image: CivitaiImage,
    viewModel: MainViewModel,
    isFavorite: Boolean,
    showFavorite: Boolean,
    onClick: (CivitaiImage) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit
) {
    val imageData = if (image.url.startsWith("http")) getThumbnailUrl(image.url, 320) else image.url

    val aspectRatio = remember(image.id, image.width, image.height) {
        if (image.width != null && image.height != null && image.height > 0)
            image.width.toFloat() / image.height.toFloat()
        else
            1.0f
    }

    val isLocal = !image.url.startsWith("http")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable { onClick(image) },
        shape = MaterialTheme.shapes.small
    ) {
        Box(modifier = Modifier.fillMaxSize().shimmerBackground()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageData)
                    .crossfade(!isLocal)
                    .build(),
                imageLoader = viewModel.imageLoader,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (showFavorite) {
                IconButton(
                    onClick = { onToggleFavorite(image) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.extraLarge)
                        .size(32.dp)
                ) {
                    Icon(
                        if (isFavorite)
                            Icons.Filled.Favorite
                        else
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else Color.White,
                            modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    url: String,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE)
                exoPlayer.pause()
            else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME)
                exoPlayer.play()
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true

                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImage(
    images: List<CivitaiImage>,
    initialIndex: Int,
    viewModel: MainViewModel,
    favoriteIds: Set<Long>,
    downloadProgresses: Map<Long, Float>,
    viewMode: ViewMode,
    onToggleFavorite: (CivitaiImage) -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    val context = LocalContext.current
    var isZoomed by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed
        ) { page ->
            if (page < images.size) {
                val image = images[page]
                val localFile = if (image.url.startsWith("http")) {
                    remember(favoriteIds) { viewModel.getLocalFavoriteFile(image.id) }
                } else null

                val imageData = localFile ?: if (image.url.startsWith("http")) getThumbnailUrl(image.url, 720) else image.url

                if (image.type == "video") {
                    VideoPlayer(
                        url = image.url,
                        isPlaying = page == pagerState.currentPage
                    )
                } else {
                    ZoomableImage(
                        model = imageData,
                        imageLoader = viewModel.imageLoader,
                        onZoomChange = { isZoomed = it }
                    )
                }
            }
        }

        if (!isZoomed) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (viewMode != ViewMode.GALLERY && pagerState.currentPage < images.size) {
                    val currentImage = images[pagerState.currentPage]
                    val isFavorite = favoriteIds.contains(currentImage.id)

                    FloatingActionButton(
                        onClick = { onToggleFavorite(currentImage) },
                        containerColor = Color.White,
                        contentColor = if (isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (isFavorite)
                                Icons.Filled.Favorite
                            else
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite"
                        )
                    }
                }

                if (viewMode != ViewMode.GALLERY) {
                    val currentImage = if (pagerState.currentPage < images.size) images[pagerState.currentPage] else null
                    val progress = currentImage?.let { downloadProgresses[it.id] }

                    FloatingActionButton(
                        onClick = {
                            currentImage?.let { viewModel.downloadImage(it) }
                        },
                        containerColor = if (progress != null) Color.Gray else Color(0xFF4CAF50),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                }

                if (viewMode == ViewMode.GALLERY) {
                    FloatingActionButton(
                        onClick = {
                            if (pagerState.currentPage < images.size) {
                                val currentImage = images[pagerState.currentPage]

                                viewModel.deleteLocalFile(currentImage)
                                onDismiss()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }

                FloatingActionButton(
                    onClick = onDismiss,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    model: Any,
    imageLoader: coil.ImageLoader,
    onZoomChange: (Boolean) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val isLocal = model is String && !model.startsWith("http")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                        }

                        onZoomChange(scale > 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes

                        if (pointers.size > 1 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (zoom != 1f || pan != Offset.Zero) {
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale > 1f) offset + pan else Offset.Zero

                                onZoomChange(scale > 1f)
                            }

                            pointers.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (pointers.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .crossfade(!isLocal)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )
    }
}

fun Modifier.shimmerBackground(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = remember {
        listOf(
            Color.DarkGray.copy(alpha = 0.6f),
            Color.Gray.copy(alpha = 0.2f),
            Color.DarkGray.copy(alpha = 0.6f),
        )
    }

    this.drawBehind {
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim, y = translateAnim)
        )

        drawRect(brush = brush)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    cacheSize: String,
    apiKey: String,
    nsfw: String,
    sort: String,
    period: String,
    type: String,
    tagId: Int?,
    pageLimit: Int,
    gridColumns: Int,
    downloadPath: String?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onClearCache: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onUpdateFilters: (String, String, String, String, Int?) -> Unit,
    onUpdatePageLimit: (Int) -> Unit,
    onUpdateGridColumns: (Int) -> Unit,
    onUpdateDownloadPath: (String?) -> Unit,
    onPickDirectory: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    var key by remember(apiKey) { mutableStateOf(apiKey) }
    var path by remember(downloadPath) { mutableStateOf(downloadPath ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val nsfwOptions = listOf("None", "Soft", "Mature", "X")
                val sortOptions = listOf("Most Reactions", "Most Comments", "Newest")
                val periodOptions = listOf("AllTime", "Year", "Month", "Week", "Day")

                Text("Default Filters", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Content Type", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("image", "video").forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { onUpdateFilters(nsfw, sort, period, option, tagId) },
                            label = { Text(option.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Text("Tags", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tagOptions.forEach { tag ->
                        FilterChip(
                            selected = tagId == tag.id,
                            onClick = { onUpdateFilters(nsfw, sort, period, type, tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                }

                Text("NSFW Level", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    nsfwOptions.forEach { option ->
                        FilterChip(
                            selected = nsfw == option,
                            onClick = { onUpdateFilters(option, sort, period, type, tagId) },
                            label = { Text(option) }
                        )
                    }
                }

                Text("Sort By", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    sortOptions.forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { onUpdateFilters(nsfw, option, period, type, tagId) },
                            label = { Text(option) }
                        )
                    }
                }

                Text("Period", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    periodOptions.forEach { option ->
                        FilterChip(
                            selected = period == option,
                            onClick = { onUpdateFilters(nsfw, sort, option, type, tagId) },
                            label = { Text(if (option == "AllTime") "All Time" else option) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Images per Request", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(20, 40, 60, 80, 100).forEach { limit ->
                        FilterChip(
                            selected = pageLimit == limit,
                            onClick = { onUpdatePageLimit(limit) },
                            label = { Text(limit.toString()) }
                        )
                    }
                }

                Text("Grid Columns", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 2, 3).forEach { cols ->
                        FilterChip(
                            selected = gridColumns == cols,
                            onClick = { onUpdateGridColumns(cols) },
                            label = { Text("$cols ${if (cols == 1) "Column" else "Columns"}") }
                        )
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("API Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Civitai API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSaveApiKey(key) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save API Key")
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Storage Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Download & Gallery Location", style = MaterialTheme.typography.labelSmall)

                OutlinedTextField(
                    value = path,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Selected Location") },
                    placeholder = { Text("Default: Pictures/DumbAI") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = onPickDirectory) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Choose Folder")
                        }
                    }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPickDirectory,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose")
                    }
                    OutlinedButton(
                        onClick = {
                            path = ""
                            onUpdateDownloadPath(null)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Default")
                    }
                }

                Text(
                    "Note: If you change the path, you may need to grant 'All Files Access' again for the new location in the Gallery tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onExport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Backup")
                    }
                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import Backup")
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Storage & Cache", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Image Cache Size: $cacheSize", style = MaterialTheme.typography.bodyLarge)
                Text("Clearing the cache will remove temporary previews, but your Favorites will remain protected.",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Button(
                    onClick = { onClearCache() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Image Cache")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DisplayBottomSheet(
    pageLimit: Int,
    gridColumns: Int,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onUpdatePageLimit: (Int) -> Unit,
    onUpdateGridColumns: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Display Options", style = MaterialTheme.typography.headlineSmall)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Images per Request", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(20, 40, 60, 80, 100).forEach { limit ->
                        FilterChip(
                            selected = pageLimit == limit,
                            onClick = { onUpdatePageLimit(limit) },
                            label = { Text(limit.toString()) }
                        )
                    }
                }

                Text("Grid Columns", style = MaterialTheme.typography.labelSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 2, 3).forEach { cols ->
                        FilterChip(
                            selected = gridColumns == cols,
                            onClick = { onUpdateGridColumns(cols) },
                            label = { Text("$cols ${if (cols == 1) "Column" else "Columns"}") }
                        )
                    }
                }
            }
        }
    }
}

fun resolveUriToPath(context: android.content.Context, uri: android.net.Uri): String? {
    if ("com.android.externalstorage.documents" == uri.authority) {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]

        if ("primary".equals(type, ignoreCase = true))
            return "${android.os.Environment.getExternalStorageDirectory()}/${split[1]}"
        else
            return "/storage/$type/${split.getOrNull(1) ?: ""}"
    }

    return null
}
