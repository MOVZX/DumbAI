package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier.shadow(
                    8.dp,
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                    ),
                )
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            )
                    ),
                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                        ),
                ),
    ) {
        BottomNavItem(
            selected = currentRoute == "feed",
            onClick = { onNavigate("feed") },
            icon = Icons.Default.Home,
            selectedIcon = Icons.Filled.Home,
            label = stringResource(R.string.nav_feed),
            count = feedCount,
            selectedColor = MaterialTheme.colorScheme.primary,
        )

        BottomNavItem(
            selected = currentRoute == "favorites",
            onClick = { onNavigate("favorites") },
            icon = Icons.Outlined.FavoriteBorder,
            selectedIcon = Icons.Filled.Favorite,
            label = stringResource(R.string.nav_favorites),
            count = favoritesCount,
            selectedColor = colorResource(R.color.tertiary),
        )

        BottomNavItem(
            selected = currentRoute == "gallery",
            onClick = { onNavigate("gallery") },
            icon = Icons.Outlined.Collections,
            selectedIcon = Icons.Filled.Collections,
            label = stringResource(R.string.nav_gallery),
            count = galleryCount,
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
    count: Int,
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

    val rotation by
        animateFloatAsState(
            targetValue = if (selected) 5f else 0f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            label = "rotation",
        )

    NavigationBarItem(
        selected = selected,
        onClick = { if (!selected) onClick() },
        icon = {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(selectedColor.copy(alpha = 0.2f), Color.Transparent)
                                ),
                            radius = size.minDimension / 1.2f,
                        )
                    }
                }

                Icon(
                    imageVector = if (selected) selectedIcon else icon,
                    contentDescription = label,
                    modifier = Modifier.scale(scale).graphicsLayer { rotationZ = rotation },
                    tint =
                        if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (count > 0) {
                    Text(
                        text = formatCount(count),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color =
                                    if (selected) selectedColor.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.5f
                                        ),
                            ),
                    )
                }
            }
        },
        alwaysShowLabel = true,
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

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fm", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}
