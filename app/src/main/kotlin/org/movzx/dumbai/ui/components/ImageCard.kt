package org.movzx.dumbai.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.FavoriteImage

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ImageCard(
    image: CivitaiImage,
    imageLoader: ImageLoader,
    isFavorite: Boolean,
    showFavorite: Boolean,
    viewMode: String,
    onGetFavoriteFlow: (Long) -> Flow<FavoriteImage?>,
    onEnsureFavoriteResources: suspend (CivitaiImage) -> Unit,
    onClick: (CivitaiImage) -> Unit,
    onToggleFavorite: (CivitaiImage) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val favoriteInfo by
        remember(image.id, isFavorite) {
                if (isFavorite) onGetFavoriteFlow(image.id) else flowOf(null)
            }
            .collectAsState(initial = null)

    val imageData by
        produceState<String>(
            initialValue = image.url,
            image.url,
            image.type,
            isFavorite,
            favoriteInfo,
        ) {
            value = org.movzx.dumbai.util.resolveImageData(image, favoriteInfo)

            if (isFavorite && image.url.startsWith("http")) onEnsureFavoriteResources(image)
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

    if (showUnfavoriteDialog) {
        ConfirmationDialog(
            title = stringResource(org.movzx.dumbai.R.string.dialog_unfavorite_title),
            message = stringResource(org.movzx.dumbai.R.string.dialog_unfavorite_msg),
            onConfirm = {
                onToggleFavorite(image)

                showUnfavoriteDialog = false
            },
            onDismiss = { showUnfavoriteDialog = false }
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
        modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio).clickable { onClick(image) },
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize().shimmerBackground()) {
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
                )
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
                                            .colorResource(org.movzx.dumbai.R.color.pure_black)
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
                                    .colorResource(org.movzx.dumbai.R.color.pure_black)
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
                                org.movzx.dumbai.R.color.pure_white
                            ),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (showFavorite) {
                IconButton(
                    onClick = {
                        if (isFavorite) showUnfavoriteDialog = true
                        else onToggleFavorite(image)
                    },
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .background(
                                androidx.compose.ui.res
                                    .colorResource(org.movzx.dumbai.R.color.pure_white)
                                    .copy(alpha = 0.2f),
                                androidx.compose.foundation.shape.CircleShape,
                            )
                            .clip(androidx.compose.foundation.shape.CircleShape),
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription =
                            stringResource(org.movzx.dumbai.R.string.nav_favorites),
                        tint =
                            if (isFavorite) androidx.compose.ui.graphics.Color.Red
                            else
                                androidx.compose.ui.res.colorResource(
                                    org.movzx.dumbai.R.color.pure_white
                                ),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (heartScale > 0f) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center).size(72.dp).scale(heartScale),
                )
            }
        }
    }
}
