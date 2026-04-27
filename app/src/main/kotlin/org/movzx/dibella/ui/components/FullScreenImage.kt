package org.movzx.dibella.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.movzx.dibella.R
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FullScreenImage(
    images: List<CivitaiImage>,
    initialIndex: Int,
    imageLoader: ImageLoader,
    favoriteIds: Set<Long>,
    downloadedIds: Set<Long> = emptySet(),
    downloadProgresses: Map<Long, Float>,
    viewMode: String,
    onGetFavoriteFlow: (Long) -> Flow<FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit,
    onDownloadImage: (CivitaiImage) -> Unit,
    onDeleteLocalFile: (CivitaiImage) -> Unit,
    onDismiss: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    var isZoomed by remember { mutableStateOf(false) }
    var showUI by remember { mutableStateOf(true) }
    var offsetY by remember { mutableStateOf(0f) }
    var showUnfavoriteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userIsPlaying by remember { mutableStateOf(true) }
    var userIsMuted by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var scaleMode by remember { mutableStateOf(ScaleMode.NORMAL) }
    var videoProgress by remember { mutableLongStateOf(0L) }
    var videoDuration by remember { mutableLongStateOf(0L) }
    var seekToPosition by remember { mutableStateOf<Long?>(null) }
    var isDraggingSeekBar by remember { mutableStateOf(false) }
    var hasAudio by remember { mutableStateOf(false) }
    var videoPlaybackError by remember { mutableStateOf<String?>(null) }
    var currentFps by remember { mutableIntStateOf(0) }
    var currentPlayerType by remember { mutableStateOf("ExoPlayer") }

    val density = LocalDensity.current

    if (showUnfavoriteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_unfavorite_title),
            message = stringResource(R.string.dialog_unfavorite_msg),
            onConfirm = {
                if (pagerState.currentPage < images.size) {
                    onToggleFavorite(images[pagerState.currentPage])

                    if (viewMode == "favorites") onDismiss()
                }

                showUnfavoriteDialog = false
            },
            onDismiss = { showUnfavoriteDialog = false },
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_title),
            message = stringResource(R.string.dialog_delete_msg),
            onConfirm = {
                if (pagerState.currentPage < images.size) {
                    onDeleteLocalFile(images[pagerState.currentPage])
                    onDismiss()
                }

                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    val dismissThreshold = with(density) { 150.dp.toPx() }

    BackHandler(onBack = onDismiss)

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    androidx.compose.ui.res
                        .colorResource(R.color.pure_black)
                        .copy(
                            alpha = (1f - (abs(offsetY) / (dismissThreshold * 2f))).coerceIn(0f, 1f)
                        )
                )
                .pointerInput(isZoomed) {
                    if (!isZoomed) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y
                            },
                            onDragEnd = {
                                if (abs(offsetY) > dismissThreshold) onDismiss() else offsetY = 0f
                            },
                            onDragCancel = { offsetY = 0f },
                        )
                    }
                }
                .graphicsLayer { translationY = offsetY }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed && offsetY == 0f,
            key = { images.getOrNull(it)?.id ?: it },
        ) { page ->
            if (page < images.size) {
                val image = images[page]
                val isFavorite = remember(favoriteIds) { favoriteIds.contains(image.id) }
                val favoriteInfo by
                    remember(image.id, isFavorite) {
                            if (isFavorite) onGetFavoriteFlow(image.id) else flowOf(null)
                        }
                        .collectAsState(initial = null)

                val context = androidx.compose.ui.platform.LocalContext.current
                val previewData =
                    remember(image.url, favoriteInfo) {
                        org.movzx.dibella.util.resolveImageData(
                            context = context,
                            image = image,
                            favoriteInfo = favoriteInfo,
                            thumbnailWidth = 450,
                            useVideoPath = true,
                        )
                    }

                LaunchedEffect(isFavorite, image.url) {
                    if (isFavorite && image.url.startsWith("http"))
                        onEnsureFavoriteResources(image, false) { _ -> }
                }

                if (image.type == "video") {
                    LaunchedEffect(pagerState.currentPage) {
                        videoProgress = 0L
                        videoDuration = 0L
                        hasAudio = false
                        videoPlaybackError = null
                        isZoomed = false
                        currentFps = 0
                    }

                    with(sharedTransitionScope) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .sharedElement(
                                        rememberSharedContentState(key = "image-${image.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    )
                        ) {
                            VideoPlayer(
                                url = previewData,
                                isPlaying = page == pagerState.currentPage && userIsPlaying,
                                isMuted = userIsMuted,
                                playbackSpeed = playbackSpeed,
                                scaleMode = scaleMode,
                                onProgressUpdate = { pos, dur ->
                                    if (!isDraggingSeekBar) {
                                        videoProgress = pos
                                        videoDuration = if (dur > 0) dur else 0L
                                    }
                                },
                                onFpsUpdate = { currentFps = it },
                                onPlayerTypeUpdate = { currentPlayerType = it },
                                onAudioStateChange = { hasAudio = it },
                                onPlaybackError = { videoPlaybackError = it },
                                onZoomChange = { isZoomed = it },
                                onTap = { showUI = !showUI },
                                seekPosition = seekToPosition,
                                onSeekConsumed = { seekToPosition = null },
                            )

                            if (videoPlaybackError != null) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.res.colorResource(R.color.error),
                                        modifier = Modifier.size(48.dp),
                                    )

                                    Text(
                                        text = stringResource(R.string.msg_playback_error),
                                        color =
                                            androidx.compose.ui.res.colorResource(
                                                R.color.pure_white
                                            ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    with(sharedTransitionScope) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .sharedElement(
                                        rememberSharedContentState(key = "image-${image.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    )
                        ) {
                            ZoomableImage(
                                model = previewData,
                                imageLoader = imageLoader,
                                onZoomChange = { isZoomed = it },
                                onTap = { showUI = !showUI },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showUI && offsetY == 0f,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
        ) {
            val currentImage =
                if (pagerState.currentPage < images.size) images[pagerState.currentPage] else null

            Box(modifier = Modifier.fillMaxWidth()) {
                if (currentImage?.type == "video" && currentFps > 0) {
                    Surface(
                        color =
                            androidx.compose.ui.res
                                .colorResource(R.color.pure_black)
                                .copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Text(
                            text = "$currentPlayerType: $currentFps FPS",
                            color = androidx.compose.ui.res.colorResource(R.color.pure_white),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (currentImage != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current

                    FilledTonalIconButton(
                        onClick = {
                            val url = "https://civitai.com/images/${currentImage.id}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

                            context.startActivity(intent)
                        },
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor =
                                    androidx.compose.ui.res
                                        .colorResource(R.color.pure_black)
                                        .copy(alpha = 0.5f),
                                contentColor =
                                    androidx.compose.ui.res.colorResource(R.color.pure_white),
                            ),
                        modifier = Modifier.size(48.dp).align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Browser",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showUI && offsetY == 0f,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .background(
                            androidx.compose.ui.res
                                .colorResource(R.color.pure_black)
                                .copy(alpha = 0.5f),
                            MaterialTheme.shapes.large,
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val currentImage =
                    if (pagerState.currentPage < images.size) images[pagerState.currentPage]
                    else null

                if (currentImage?.type == "video" && videoDuration > 0) {
                    Slider(
                        value = videoProgress.toFloat(),
                        onValueChange = {
                            isDraggingSeekBar = true
                            userIsPlaying = false
                            videoProgress = it.toLong()
                            seekToPosition = it.toLong()
                        },
                        onValueChangeFinished = { isDraggingSeekBar = false },
                        valueRange = 0f..videoDuration.toFloat(),
                        colors =
                            SliderDefaults.colors(
                                thumbColor =
                                    androidx.compose.ui.res.colorResource(R.color.pure_white),
                                activeTrackColor =
                                    androidx.compose.ui.res.colorResource(R.color.pure_white),
                                inactiveTrackColor =
                                    androidx.compose.ui.res
                                        .colorResource(R.color.pure_white)
                                        .copy(alpha = 0.3f),
                            ),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentImage?.type == "video") {
                        IconButton(
                            onClick = { userIsPlaying = !userIsPlaying },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                if (userIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = androidx.compose.ui.res.colorResource(R.color.pure_white),
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        IconButton(
                            onClick = { userIsMuted = !userIsMuted },
                            enabled = hasAudio,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                if (userIsMuted) Icons.AutoMirrored.Filled.VolumeOff
                                else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute/Unmute",
                                tint =
                                    androidx.compose.ui.res
                                        .colorResource(R.color.pure_white)
                                        .copy(alpha = if (hasAudio) 1f else 0.3f),
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        TextButton(
                            onClick = {
                                playbackSpeed =
                                    when (playbackSpeed) {
                                        0.5f -> 0.75f
                                        0.75f -> 1.0f
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 0.5f
                                    }
                            },
                            modifier = Modifier.width(64.dp),
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = colorResource(R.color.pure_white),
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            )
                        }

                        IconButton(
                            onClick = {
                                scaleMode =
                                    when (scaleMode) {
                                        ScaleMode.NORMAL -> ScaleMode.CROP
                                        ScaleMode.CROP -> ScaleMode.FULL
                                        ScaleMode.FULL -> ScaleMode.NORMAL
                                    }
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                when (scaleMode) {
                                    ScaleMode.NORMAL -> Icons.Default.Fullscreen
                                    ScaleMode.CROP -> Icons.Default.Crop
                                    ScaleMode.FULL -> Icons.Default.AspectRatio
                                },
                                contentDescription = "Scale Mode",
                                tint = androidx.compose.ui.res.colorResource(R.color.pure_white),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    if (viewMode != "gallery" && currentImage != null) {
                        val isFavorite = favoriteIds.contains(currentImage.id)

                        IconButton(
                            onClick = {
                                if (isFavorite) showUnfavoriteDialog = true
                                else onToggleFavorite(currentImage)
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite
                                else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.nav_favorites),
                                tint =
                                    if (isFavorite)
                                        androidx.compose.ui.res.colorResource(R.color.error)
                                    else androidx.compose.ui.res.colorResource(R.color.pure_white),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    if (viewMode != "gallery" && currentImage != null) {
                        val progress = downloadProgresses[currentImage.id]
                        val isDownloaded = downloadedIds.contains(currentImage.id)

                        IconButton(
                            onClick = { onDownloadImage(currentImage) },
                            enabled = progress == null && !isDownloaded,
                            modifier = Modifier.size(48.dp),
                        ) {
                            if (progress != null) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    color =
                                        androidx.compose.ui.res.colorResource(R.color.pure_white),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                Icon(
                                    imageVector =
                                        if (isDownloaded) Icons.Default.Done
                                        else Icons.Default.Download,
                                    contentDescription = stringResource(R.string.btn_download),
                                    tint =
                                        androidx.compose.ui.res
                                            .colorResource(R.color.pure_white)
                                            .copy(alpha = if (isDownloaded) 0.5f else 1f),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }

                    if (viewMode == "gallery") {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < images.size) showDeleteDialog = true
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = androidx.compose.ui.res.colorResource(R.color.error),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
