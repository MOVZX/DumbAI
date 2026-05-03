package org.movzx.dibella.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCard(
    image: CivitaiImage,
    imageLoader: ImageLoader,
    isFavorite: Boolean,
    favoriteIds: Set<Long>,
    downloadProgresses: Map<Long, Float>,
    showFavorite: Boolean,
    viewMode: String,
    favoritesPath: String? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onGetFavoriteFlow: (Long) -> Flow<FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onEnsureFavoriteResourcesThrottled: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onClick: (CivitaiImage) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit,
    onRetryThumbnail: (String, () -> Unit) -> Unit = { _, _ -> },
    onToggleSelection: () -> Unit = {},
    onLongClick: () -> Unit = {},
    autoplayEnabled: Boolean = false,
    isVisibleInViewport: Boolean = false,
    isScrolling: Boolean = false,
    animationIndex: Int = -1,
    isPressed: Boolean = false,
    onPressChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val favoriteInfo by
        remember(image.id, isFavorite) {
                if (isFavorite) onGetFavoriteFlow(image.id) else flowOf(null)
            }
            .collectAsState(initial = null)

    var retryCount by remember { mutableIntStateOf(0) }
    var isRetrying by remember { mutableStateOf(false) }
    val favDir = remember(favoritesPath) { favoritesPath?.let { java.io.File(it) } }

    val imageData =
        remember(image.url, favoriteInfo?.isSynced, retryCount, favDir) {
            org.movzx.dibella.util.resolveImageData(
                context,
                image,
                favoriteInfo,
                favoritesDir = favDir,
            )
        }

    val imageRequest =
        remember(imageData) {
            val isLocal = imageData.startsWith("/") || imageData.startsWith("file://")

            ImageRequest.Builder(context)
                .data(imageData)
                .crossfade(true)
                .apply {
                    if (isLocal) {
                        diskCachePolicy(coil3.request.CachePolicy.DISABLED)
                    }
                }
                .build()
        }

    var isAutoplayDebounced by remember { mutableStateOf(false) }
    var videoError by remember { mutableStateOf(false) }
    var isLongPressPreviewActive by remember { mutableStateOf(false) }

    LaunchedEffect(isVisibleInViewport) {
        if (isVisibleInViewport) {
            kotlinx.coroutines.delay(150)

            isAutoplayDebounced = true
        } else {
            isAutoplayDebounced = false
            isLongPressPreviewActive = false
        }
    }

    val videoData =
        remember(image.url, favoriteInfo, autoplayEnabled, isLongPressPreviewActive, favDir) {
            if ((autoplayEnabled || isLongPressPreviewActive) && image.type == "video") {
                org.movzx.dibella.util.resolveImageData(
                    context,
                    image,
                    favoriteInfo,
                    useVideoPath = true,
                    favoritesDir = favDir,
                )
            } else null
        }

    var hasEnsuredResources by remember(image.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isThumbnailCached by
        produceState(
            initialValue = false,
            context,
            image.id,
            image.type,
            favoriteInfo?.isSynced,
            downloadProgresses[image.id],
            retryCount,
            favDir,
        ) {
            value =
                org.movzx.dibella.util.hasLocalCache(
                    context = context,
                    imageId = image.id,
                    isVideo = image.type == "video",
                    favoritesDir = favDir,
                )
        }

    val isPreviewCached by
        produceState(
            initialValue = false,
            context,
            image.id,
            image.type,
            favoriteInfo?.isSynced,
            downloadProgresses[image.id],
            retryCount,
            favDir,
        ) {
            value =
                org.movzx.dibella.util.hasFullCache(
                    context = context,
                    imageId = image.id,
                    isVideo = image.type == "video",
                    favoritesDir = favDir,
                )
        }

    LaunchedEffect(image.id, isFavorite) {
        if (isFavorite && image.url.startsWith("http") && !hasEnsuredResources) {
            hasEnsuredResources = true

            onEnsureFavoriteResourcesThrottled(image, false) { _ -> }
        }
    }

    val aspectRatio =
        remember(image.id, image.width, image.height) {
            if (image.width != null && image.height != null && image.height > 0)
                image.width.toFloat() / image.height.toFloat()
            else 1.0f
        }

    var showHeartAnimation by remember { mutableStateOf(false) }
    var showHeartParticles by remember { mutableStateOf(false) }
    var isFirstComposition by remember { mutableStateOf(true) }
    var showUnfavoriteDialog by remember { mutableStateOf(false) }
    var showRedownloadDialog by remember { mutableStateOf(false) }
    var manualProgress by remember { mutableStateOf<Float?>(null) }

    if (showUnfavoriteDialog) {
        ConfirmationDialog(
            title = stringResource(org.movzx.dibella.R.string.dialog_unfavorite_title),
            message = stringResource(org.movzx.dibella.R.string.dialog_unfavorite_msg),
            onConfirm = {
                onToggleFavorite(image)

                showUnfavoriteDialog = false
            },
            onDismiss = { showUnfavoriteDialog = false },
        )
    }

    if (showRedownloadDialog) {
        ConfirmationDialog(
            title = stringResource(org.movzx.dibella.R.string.dialog_redownload_title),
            message = stringResource(org.movzx.dibella.R.string.dialog_redownload_msg),
            onConfirm = {
                scope.launch {
                    manualProgress = 0f
                    onEnsureFavoriteResources(image, true) { progress -> manualProgress = progress }

                    manualProgress = null
                }

                showRedownloadDialog = false
            },
            onDismiss = { showRedownloadDialog = false },
        )
    }

    val heartScale by
        animateFloatAsState(
            targetValue = if (showHeartAnimation) 1.2f else 0f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "HeartScale",
            finishedListener = { if (it > 0f) showHeartAnimation = false },
        )

    LaunchedEffect(isFavorite) {
        if (isFavorite && !isFirstComposition) {
            showHeartAnimation = true
            showHeartParticles = true
        }

        isFirstComposition = false
    }

    val interactionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }

    val isPressedLocal by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressedLocal) { onPressChange(isPressedLocal) }

    val scale by
        animateFloatAsState(
            targetValue = if (isPressed) 0.96f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "CardScale",
        )

    var isVisible by remember { mutableStateOf(animationIndex == -1) }

    LaunchedEffect(Unit) {
        if (animationIndex != -1) {
            delay((animationIndex % 12) * 80L)

            isVisible = true
        }
    }

    val entryProgress by
        animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 100f),
            label = "EntryProgress",
        )

    Card(
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(aspectRatio)
                .graphicsLayer {
                    alpha = entryProgress
                    translationY = (1f - entryProgress) * 100f
                    scaleX = scale
                    scaleY = scale
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)

                        if (isSelectionMode) onToggleSelection() else onClick(image)
                    },
                    onLongClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

                        if (image.type == "video") isLongPressPreviewActive = true

                        onLongClick()
                    },
                ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
    ) {
        var isError by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }
        var videoProgress by remember { mutableFloatStateOf(0f) }
        var isVideoReady by remember(image.id, videoData == null) { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (videoData != null && isVisibleInViewport && isAutoplayDebounced && !videoError) {
                VideoPlayer(
                    url = videoData,
                    isPlaying = isVisibleInViewport && !isScrolling,
                    isMuted = true,
                    scaleMode = ScaleMode.CROP,
                    onProgressUpdate = { pos, dur ->
                        if (dur > 0) videoProgress = pos.toFloat() / dur.toFloat()
                    },
                    onReady = { isVideoReady = true },
                    onPlaybackError = { videoError = true },
                    onTap = { if (isSelectionMode) onToggleSelection() else onClick(image) },
                    onLongPress = onLongClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val thumbnailAlpha by
                remember(isVideoReady, isScrolling, isVisibleInViewport) {
                    derivedStateOf {
                        if (isVideoReady && !isScrolling && isVisibleInViewport) 0f else 1f
                    }
                }

            if (thumbnailAlpha > 0.01f) {
                androidx.compose.animation.AnimatedContent(
                    targetState = imageRequest,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                    },
                    label = "ImageContent",
                    modifier = Modifier.graphicsLayer { alpha = thumbnailAlpha },
                ) { targetRequest ->
                    AsyncImage(
                        model = targetRequest,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            isLoading = state is coil3.compose.AsyncImagePainter.State.Loading
                            isError = state is coil3.compose.AsyncImagePainter.State.Error
                        },
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = colorResource(org.movzx.dibella.R.color.success),
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            if (isError) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Failed to load",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.res
                                            .colorResource(org.movzx.dibella.R.color.pure_black)
                                            .copy(alpha = 0.6f),
                                    )
                            )
                        )
            )

            if (videoProgress > 0f && isVisibleInViewport && !videoError) {
                LinearProgressIndicator(
                    progress = { videoProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }

            if (image.type == "video" && viewMode != "feed") {
                Box(
                    modifier =
                        Modifier.padding(8.dp)
                            .align(Alignment.TopEnd)
                            .background(
                                androidx.compose.ui.res
                                    .colorResource(org.movzx.dibella.R.color.pure_black)
                                    .copy(alpha = 0.4f),
                                MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint =
                            androidx.compose.ui.res.colorResource(
                                org.movzx.dibella.R.color.pure_white
                            ),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (showFavorite && !isLoading) {
                IconButton(
                    onClick = {
                        if (isRetrying) return@IconButton

                        if (isError) {
                            isRetrying = true

                            onRetryThumbnail(imageData) {
                                retryCount++

                                isError = false
                                isLoading = true
                                isRetrying = false
                            }
                        } else if (viewMode == "favorites") {
                            showRedownloadDialog = true
                        } else {
                            if (isFavorite) {
                                view.performHapticFeedback(
                                    android.view.HapticFeedbackConstants.LONG_PRESS
                                )

                                showUnfavoriteDialog = true
                            } else {
                                view.performHapticFeedback(
                                    android.view.HapticFeedbackConstants.VIRTUAL_KEY
                                )

                                onToggleFavorite(image)
                            }
                        }
                    },
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .background(
                                androidx.compose.ui.res
                                    .colorResource(org.movzx.dibella.R.color.pure_white)
                                    .copy(alpha = 0.4f),
                                androidx.compose.foundation.shape.CircleShape,
                            )
                            .clip(androidx.compose.foundation.shape.CircleShape),
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else if (isError) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint =
                                androidx.compose.ui.res.colorResource(
                                    org.movzx.dibella.R.color.pure_white
                                ),
                            modifier = Modifier.size(16.dp),
                        )
                    } else if (viewMode == "favorites") {
                        val cloudColor =
                            if (isThumbnailCached && isPreviewCached) Color.Green else Color.Yellow

                        val progress = manualProgress ?: downloadProgresses[image.id]

                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = cloudColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = "Cache Status",
                                tint = cloudColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription =
                                stringResource(org.movzx.dibella.R.string.nav_favorites),
                            tint =
                                if (isFavorite)
                                    androidx.compose.ui.res.colorResource(
                                        org.movzx.dibella.R.color.error
                                    )
                                else
                                    androidx.compose.ui.res.colorResource(
                                        org.movzx.dibella.R.color.pure_white
                                    ),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (showHeartParticles) {
                HeartParticles(
                    modifier = Modifier.align(Alignment.Center),
                    onFinished = { showHeartParticles = false },
                )
            }

            if (heartScale > 0f) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = colorResource(org.movzx.dibella.R.color.error).copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center).size(72.dp).scale(heartScale),
                )
            }
        }
    }
}

@Composable
fun HeartParticles(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val particles = remember { List(12) { it } }
    val sparkles = remember { List(3) { it } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing),
        )

        onFinished()
    }

    val colorTertiary = colorResource(org.movzx.dibella.R.color.tertiary)
    val colorTertiaryLight = colorResource(org.movzx.dibella.R.color.tertiary_light)

    Box(modifier = modifier) {
        particles.forEach { index ->
            val angle = index * (360f / particles.size)
            val distance = 50.dp * progress.value
            val alpha = 1f - progress.value
            val scale = 0.4f + (1f - progress.value) * 0.6f
            val color = if (index % 2 == 0) colorTertiary else colorTertiaryLight

            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = color.copy(alpha = alpha),
                modifier =
                    Modifier.size(12.dp).graphicsLayer {
                        val rad = Math.toRadians(angle.toDouble())

                        translationX = (Math.cos(rad) * distance.toPx()).toFloat()
                        translationY = (Math.sin(rad) * distance.toPx()).toFloat()
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }

        sparkles.forEach { index ->
            val angle = (index * 120f) + 60f
            val distance = 30.dp * progress.value
            val alpha = (1f - progress.value) * 0.8f

            Box(
                modifier =
                    Modifier.size(4.dp)
                        .graphicsLayer {
                            val rad = Math.toRadians(angle.toDouble())
                            translationX = (Math.cos(rad) * distance.toPx()).toFloat()
                            translationY = (Math.sin(rad) * distance.toPx()).toFloat()
                            this.alpha = alpha
                        }
                        .background(Color.White, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
