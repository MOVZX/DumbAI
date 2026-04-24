package org.movzx.dumbai.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.movzx.dumbai.R
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.FavoriteImage

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FullScreenImage(
    images: List<CivitaiImage>,
    initialIndex: Int,
    imageLoader: ImageLoader,
    favoriteIds: Set<Long>,
    downloadProgresses: Map<Long, Float>,
    viewMode: String,
    onGetFavoriteFlow: (Long) -> Flow<FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage) -> Unit,
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
    val density = LocalDensity.current
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

                val previewData by
                    produceState<String>(
                        initialValue = image.url,
                        image.url,
                        image.type,
                        isFavorite,
                        favoriteInfo,
                    ) {
                        value =
                            org.movzx.dumbai.util.resolveImageData(
                                image = image,
                                favoriteInfo = favoriteInfo,
                                thumbnailWidth = 640,
                                useVideoPath = true,
                            )

                        if (isFavorite && image.url.startsWith("http"))
                            onEnsureFavoriteResources(image)
                    }

                if (image.type == "video") {
                    with(sharedTransitionScope) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .sharedElement(
                                        rememberSharedContentState(key = "image-${image.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        showUI = !showUI
                                    }
                        ) {
                            VideoPlayer(
                                url = previewData,
                                isPlaying = page == pagerState.currentPage,
                            )
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
            visible = showUI && !isZoomed && offsetY == 0f,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier.padding(16.dp)
                        .statusBarsPadding()
                        .background(
                            androidx.compose.ui.res
                                .colorResource(R.color.pure_black)
                                .copy(alpha = 0.5f),
                            androidx.compose.foundation.shape.CircleShape,
                        ),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.btn_close),
                    tint = androidx.compose.ui.res.colorResource(R.color.pure_white),
                )
            }
        }

        AnimatedVisibility(
            visible = showUI && !isZoomed && offsetY == 0f,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier =
                    Modifier.padding(16.dp)
                        .navigationBarsPadding()
                        .background(
                            androidx.compose.ui.res
                                .colorResource(R.color.pure_black)
                                .copy(alpha = 0.5f),
                            MaterialTheme.shapes.large,
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (viewMode != "gallery" && pagerState.currentPage < images.size) {
                    val currentImage = images[pagerState.currentPage]
                    val isFavorite = favoriteIds.contains(currentImage.id)

                    IconButton(onClick = { onToggleFavorite(currentImage) }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.nav_favorites),
                            tint =
                                if (isFavorite) androidx.compose.ui.graphics.Color.Red
                                else androidx.compose.ui.res.colorResource(R.color.pure_white),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                if (viewMode != "gallery") {
                    val currentImage =
                        if (pagerState.currentPage < images.size) images[pagerState.currentPage]
                        else null

                    val progress = currentImage?.let { downloadProgresses[it.id] }

                    IconButton(onClick = { currentImage?.let { onDownloadImage(it) } }) {
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = androidx.compose.ui.res.colorResource(R.color.pure_white),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = stringResource(R.string.btn_download),
                                tint = androidx.compose.ui.res.colorResource(R.color.pure_white),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }

                if (viewMode == "gallery") {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < images.size) {
                                val currentImage = images[pagerState.currentPage]

                                onDeleteLocalFile(currentImage)
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.btn_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}
