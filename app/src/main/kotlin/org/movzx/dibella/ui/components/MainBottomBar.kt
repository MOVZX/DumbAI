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
        val feedSelected = currentRoute == "feed"

        val feedScale by
            animateFloatAsState(
                targetValue = if (feedSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "feedScale",
            )

        NavigationBarItem(
            selected = feedSelected,
            onClick = { if (feedSelected.not()) onNavigate("feed") },
            icon = {
                BadgedBox(
                    badge = {
                        if (feedCount > 0)
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Text(feedCount.toString())
                            }
                    }
                ) {
                    Icon(
                        if (feedSelected) Icons.Filled.Home else Icons.Default.Home,
                        contentDescription = stringResource(R.string.nav_feed),
                        modifier = Modifier.scale(feedScale),
                    )
                }
            },
            label = null,
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
        )

        val favSelected = currentRoute == "favorites"

        val favScale by
            animateFloatAsState(
                targetValue = if (favSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "favScale",
            )

        NavigationBarItem(
            selected = favSelected,
            onClick = { if (favSelected.not()) onNavigate("favorites") },
            icon = {
                BadgedBox(
                    badge = {
                        if (favoritesCount > 0)
                            Badge(
                                containerColor = colorResource(R.color.error),
                                contentColor = androidx.compose.ui.graphics.Color.White,
                            ) {
                                Text(favoritesCount.toString())
                            }
                    }
                ) {
                    Icon(
                        if (favSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.nav_favorites),
                        modifier = Modifier.scale(favScale),
                    )
                }
            },
            label = null,
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = colorResource(R.color.error),
                    indicatorColor = colorResource(R.color.error).copy(alpha = 0.2f),
                ),
        )

        val gallerySelected = currentRoute == "gallery"

        val galleryScale by
            animateFloatAsState(
                targetValue = if (gallerySelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "galleryScale",
            )

        NavigationBarItem(
            selected = gallerySelected,
            onClick = { if (gallerySelected.not()) onNavigate("gallery") },
            icon = {
                BadgedBox(
                    badge = {
                        if (galleryCount > 0)
                            Badge(
                                containerColor = colorResource(R.color.success),
                                contentColor = androidx.compose.ui.graphics.Color.White,
                            ) {
                                Text(galleryCount.toString())
                            }
                    }
                ) {
                    Icon(
                        if (gallerySelected) Icons.Filled.Collections
                        else Icons.Outlined.Collections,
                        contentDescription = stringResource(R.string.nav_gallery),
                        modifier = Modifier.scale(galleryScale),
                    )
                }
            },
            label = null,
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = colorResource(R.color.success),
                    indicatorColor = colorResource(R.color.success).copy(alpha = 0.2f),
                ),
        )
    }
}
