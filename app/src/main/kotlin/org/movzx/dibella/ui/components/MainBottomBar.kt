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
fun MainBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 0.dp,
        modifier =
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
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
                Icon(
                    if (feedSelected) Icons.Filled.Home else Icons.Default.Home,
                    contentDescription = stringResource(R.string.nav_feed),
                    modifier = Modifier.scale(feedScale),
                )
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
                Icon(
                    if (favSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.nav_favorites),
                    modifier = Modifier.scale(favScale),
                )
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
                Icon(
                    if (gallerySelected) Icons.Filled.Collections else Icons.Outlined.Collections,
                    contentDescription = stringResource(R.string.nav_gallery),
                    modifier = Modifier.scale(galleryScale),
                )
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
