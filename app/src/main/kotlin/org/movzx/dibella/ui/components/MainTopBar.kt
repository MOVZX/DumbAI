package org.movzx.dibella.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onAction: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text =
                    title
                        ?: stringResource(
                            org.movzx.dibella.R.string.label_selected_count,
                            selectedCount,
                        ),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(org.movzx.dibella.R.string.btn_close),
                )
            }
        },
        actions = {
            if (title == null) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        Icons.Default.SelectAll,
                        contentDescription =
                            stringResource(org.movzx.dibella.R.string.btn_select_all),
                    )
                }
            }
            IconButton(onClick = onAction) {
                Icon(
                    actionIcon,
                    contentDescription = "Action",
                    tint = colorResource(org.movzx.dibella.R.color.error),
                )
            }
        },
        colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier =
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    gridColumns: Int,
    onShowDisplayOptions: () -> Unit,
    onUpdateGridColumns: (Int) -> Unit,
    onShowFilters: () -> Unit,
    onShowSettings: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(42.dp),
            )
        },
        navigationIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShowDisplayOptions) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = stringResource(R.string.display_options),
                    )
                }
                IconButton(
                    onClick = {
                        val nextCols = if (gridColumns >= 3) 1 else gridColumns + 1
                        onUpdateGridColumns(nextCols)
                    }
                ) {
                    Icon(
                        imageVector =
                            when (gridColumns) {
                                1 -> Icons.Default.ViewStream
                                2 -> Icons.Default.Dashboard
                                else -> Icons.Default.ViewWeek
                            },
                        contentDescription = stringResource(R.string.label_grid_columns),
                    )
                }
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShowFilters) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.filters),
                    )
                }
                IconButton(onClick = onShowSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier =
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    )
}
