package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.movzx.dibella.R

@Composable
fun MainBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    feedCount: Int = 0,
    favoritesCount: Int = 0,
    galleryCount: Int = 0,
) {
    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        BottomNavItem(
            selected = currentRoute == "feed",
            onClick = { onNavigate("feed") },
            icon = Icons.Default.Home,
            selectedIcon = Icons.Filled.Home,
            label = stringResource(R.string.nav_feed),
            badgeCount = feedCount,
            selectedColor = MaterialTheme.colorScheme.primary,
        )

        BottomNavItem(
            selected = currentRoute == "favorites",
            onClick = { onNavigate("favorites") },
            icon = Icons.Outlined.FavoriteBorder,
            selectedIcon = Icons.Filled.Favorite,
            label = stringResource(R.string.nav_favorites),
            badgeCount = favoritesCount,
            selectedColor = colorResource(R.color.error),
        )

        BottomNavItem(
            selected = currentRoute == "gallery",
            onClick = { onNavigate("gallery") },
            icon = Icons.Outlined.Collections,
            selectedIcon = Icons.Filled.Collections,
            label = stringResource(R.string.nav_gallery),
            badgeCount = galleryCount,
            selectedColor = colorResource(R.color.success),
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badgeCount: Int,
    selectedColor: androidx.compose.ui.graphics.Color,
) {
    val scale by
        animateFloatAsState(
            targetValue = if (selected) 1.15f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            label = "scale",
        )

    val transition = rememberInfiniteTransition(label = "badgePulse")
    val pulseScale by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulse",
        )

    NavigationBarItem(
        selected = selected,
        onClick = { if (!selected) onClick() },
        icon = {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                if (selected) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush =
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors =
                                        listOf(
                                            selectedColor.copy(alpha = 0.15f),
                                            androidx.compose.ui.graphics.Color.Transparent,
                                        )
                                ),
                            radius = size.minDimension / 1.2f,
                        )
                    }
                }

                BadgedBox(
                    badge = {
                        if (badgeCount > 0) {
                            Surface(
                                color = selectedColor,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                modifier =
                                    Modifier.scale(if (selected) pulseScale else 1f)
                                        .padding(horizontal = 2.dp)
                                        .widthIn(min = 40.dp),
                            ) {
                                Text(
                                    text = badgeCount.toString(),
                                    style =
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            textAlign =
                                                androidx.compose.ui.text.style.TextAlign.Center,
                                        ),
                                    modifier = Modifier.padding(horizontal = 1.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selected) selectedIcon else icon,
                        contentDescription = label,
                        modifier = Modifier.scale(scale),
                        tint =
                            if (selected) selectedColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        alwaysShowLabel = false,
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = selectedColor,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = selectedColor.copy(alpha = 0.1f),
            ),
    )
}
