package org.movzx.dibella.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ImageCard(
    image: CivitaiImage,
    imageLoader: ImageLoader,
    isFavorite: Boolean,
    favoriteIds: Set<Long>,
    downloadProgresses: Map<Long, Float>,
    showFavorite: Boolean,
    viewMode: String,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onGetFavoriteFlow: (Long) -> Flow<FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage, Boolean, (Float) -> Unit) -> Unit,
    onClick: (CivitaiImage) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit,
    onToggleSelection: () -> Unit = {},
    onLongClick: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val favoriteInfo by
        remember(image.id, isFavorite) {
                if (isFavorite) onGetFavoriteFlow(image.id) else flowOf(null)
            }
            .collectAsState(initial = null)

    val imageData =
        remember(image.url, favoriteInfo) {
            org.movzx.dibella.util.resolveImageData(context, image, favoriteInfo)
        }

    LaunchedEffect(isFavorite, image.url) {
        if (isFavorite && image.url.startsWith("http"))
            onEnsureFavoriteResources(image, false) { _ -> }
    }

    val aspectRatio =
        remember(image.id, image.width, image.height) {
            if (image.width != null && image.height != null && image.height > 0)
                image.width.toFloat() / image.height.toFloat()
            else 1.0f
        }

    var showHeartAnimation by remember { mutableStateOf(false) }
    var isFirstComposition by remember { mutableStateOf(true) }
    var showUnfavoriteDialog by remember { mutableStateOf(false) }
    var showRedownloadDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
        if (isFavorite && !isFirstComposition) showHeartAnimation = true

        isFirstComposition = false
    }

    Card(
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(aspectRatio)
                .combinedClickable(
                    onClick = { if (isSelectionMode) onToggleSelection() else onClick(image) },
                    onLongClick = onLongClick,
                ),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
    ) {
        var isError by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            with(sharedTransitionScope) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(imageData)
                            .crossfade(true)
                            .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier =
                        Modifier.fillMaxSize()
                            .sharedElement(
                                rememberSharedContentState(key = "image-${image.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        isError = state is coil3.compose.AsyncImagePainter.State.Error
                    },
                )
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

            if (showFavorite) {
                IconButton(
                    onClick = {
                        if (viewMode == "favorites") {
                            showRedownloadDialog = true
                        } else {
                            if (isFavorite) showUnfavoriteDialog = true else onToggleFavorite(image)
                        }
                    },
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .background(
                                androidx.compose.ui.res
                                    .colorResource(org.movzx.dibella.R.color.pure_white)
                                    .copy(alpha = 0.2f),
                                androidx.compose.foundation.shape.CircleShape,
                            )
                            .clip(androidx.compose.foundation.shape.CircleShape),
                ) {
                    if (viewMode == "favorites") {
                        val isThumbnailCached =
                            org.movzx.dibella.util.hasLocalCache(
                                context,
                                image.id,
                                image.type == "video",
                            )

                        val isPreviewCached =
                            org.movzx.dibella.util.hasFullCache(
                                context,
                                image.id,
                                image.type == "video",
                            )

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
