package org.movzx.dibella.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

enum class EmptyStateType(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val messageResId: Int,
    val actionLabelResId: Int?,
) {
    FEED(
        icon = Icons.Default.GridView,
        messageResId = R.string.empty_feed,
        actionLabelResId = R.string.btn_refresh_feed,
    ),
    FAVORITES(
        icon = Icons.Outlined.FavoriteBorder,
        messageResId = R.string.empty_favorites,
        actionLabelResId = R.string.btn_browse_feed,
    ),
    GALLERY(
        icon = Icons.Outlined.PhotoLibrary,
        messageResId = R.string.empty_gallery,
        actionLabelResId = R.string.btn_browse_feed,
    ),
    SEARCH(
        icon = Icons.Default.Search,
        messageResId = R.string.empty_search,
        actionLabelResId = null,
    ),
    DUPLICATES(
        icon = Icons.Default.DeleteSweep,
        messageResId = R.string.msg_no_duplicates_found,
        actionLabelResId = null,
    ),
    BOOKMARKS(
        icon = Icons.Outlined.BookmarkBorder,
        messageResId = R.string.empty_bookmarks,
        actionLabelResId = null,
    ),
}

@Composable
fun ModernEmptyState(
    type: EmptyStateType,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyState")

    val floatOffset by
        infiniteTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2400, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "float",
        )

    val glowAlpha by
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.35f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glow",
        )

    val entryProgress by
        animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "entry",
        )

    val iconScale = 0.6f + 0.4f * entryProgress

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
            border =
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ),
            modifier =
                Modifier.size(120.dp).graphicsLayer {
                    translationY = floatOffset
                    scaleX = iconScale
                    scaleY = iconScale
                },
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                ) {}

                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(type.messageResId),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.padding(horizontal = 48.dp).graphicsLayer { alpha = 0.85f * entryProgress },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text =
                when (type) {
                    EmptyStateType.FEED -> stringResource(R.string.empty_feed_hint)
                    EmptyStateType.FAVORITES -> stringResource(R.string.empty_favorites_hint)
                    EmptyStateType.GALLERY -> stringResource(R.string.empty_gallery_hint)
                    EmptyStateType.SEARCH -> stringResource(R.string.empty_search_hint)
                    EmptyStateType.DUPLICATES -> ""
                    EmptyStateType.BOOKMARKS -> stringResource(R.string.empty_bookmarks_hint)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.padding(horizontal = 48.dp).graphicsLayer { alpha = 0.6f * entryProgress },
        )

        if (type.actionLabelResId != null) {
            Spacer(modifier = Modifier.height(28.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val pressScale by
                animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "pressScale",
                )

            ElevatedButton(
                onClick = onAction,
                modifier =
                    Modifier.graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .graphicsLayer { alpha = entryProgress },
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                elevation =
                    ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp,
                    ),
                interactionSource = interactionSource,
            ) {
                Icon(
                    imageVector =
                        when (type) {
                            EmptyStateType.FEED -> Icons.Default.Refresh
                            EmptyStateType.FAVORITES -> Icons.Default.Home
                            EmptyStateType.GALLERY -> Icons.Default.Home
                            else -> Icons.AutoMirrored.Default.ArrowForward
                        },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(type.actionLabelResId),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
