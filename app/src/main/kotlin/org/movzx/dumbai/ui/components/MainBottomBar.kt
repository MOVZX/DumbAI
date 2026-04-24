package org.movzx.dumbai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dumbai.R

@Composable
fun MainBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 0.dp,
        modifier =
            Modifier.height(72.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        NavigationBarItem(
            selected = currentRoute == "feed",
            onClick = { if (currentRoute != "feed") onNavigate("feed") },
            icon = {
                Icon(
                    if (currentRoute == "feed") Icons.Filled.Home else Icons.Default.Home,
                    contentDescription = stringResource(R.string.nav_feed),
                )
            },
            label = null,
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ),
        )

        NavigationBarItem(
            selected = currentRoute == "favorites",
            onClick = { if (currentRoute != "favorites") onNavigate("favorites") },
            icon = {
                Icon(
                    if (currentRoute == "favorites") Icons.Filled.Favorite
                    else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.nav_favorites),
                )
            },
            label = null,
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.tertiary,
                    indicatorColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                ),
        )

        NavigationBarItem(
            selected = currentRoute == "gallery",
            onClick = { if (currentRoute != "gallery") onNavigate("gallery") },
            icon = {
                Icon(
                    if (currentRoute == "gallery") Icons.Filled.Collections
                    else Icons.Outlined.Collections,
                    contentDescription = stringResource(R.string.nav_gallery),
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
