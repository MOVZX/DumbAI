package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DisplaySidebar(
    currentRoute: String,
    pageLimit: Int,
    gridColumns: Int,
    type: String,
    onDismiss: () -> Unit,
    onUpdatePageLimit: (Int) -> Unit,
    onUpdateGridColumns: (Int) -> Unit,
    onUpdateType: (String) -> Unit,
    onScanDuplicates: (() -> Unit)? = null,
    amoledMode: Boolean = false,
) {

    val pageLimits = integerArrayResource(R.array.page_limits)
    val gridColumnOptions = integerArrayResource(R.array.grid_columns)
    var currentPageLimit by remember(pageLimit) { mutableIntStateOf(pageLimit) }
    var currentGridColumns by remember(gridColumns) { mutableIntStateOf(gridColumns) }
    var currentType by remember(type) { mutableStateOf(type) }

    BaseSidebar(
        title = stringResource(R.string.display_options),
        onDismiss = onDismiss,
        amoledMode = amoledMode,
        footer = {
            Button(
                onClick = {
                    onUpdatePageLimit(currentPageLimit)
                    onUpdateGridColumns(currentGridColumns)

                    if (currentRoute != "search") onUpdateType(currentType)

                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.success),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_apply_settings))
            }
        },
    ) {
        SidebarSection(
            title = stringResource(R.string.section_app_config),
            icon = Icons.Outlined.Palette,
        ) {
            if (currentRoute == "feed" || currentRoute == "search") {
                Text(
                    stringResource(R.string.label_images_per_request),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pageLimits.forEach { limit ->
                        FilterChip(
                            selected = currentPageLimit == limit,
                            onClick = { currentPageLimit = limit },
                            label = { Text(limit.toString()) },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorResource(R.color.primary),
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (currentRoute != "feed" && currentRoute != "search") {
                Text(
                    stringResource(R.string.content_type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentType == "all",
                        onClick = { currentType = "all" },
                        label = { Text(stringResource(R.string.opt_all)) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorResource(R.color.primary),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    )

                    FilterChip(
                        selected = currentType == "image",
                        onClick = { currentType = "image" },
                        label = { Text(stringResource(R.string.opt_image)) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorResource(R.color.primary),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    )

                    FilterChip(
                        selected = currentType == "video",
                        onClick = { currentType = "video" },
                        label = { Text(stringResource(R.string.opt_video)) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorResource(R.color.primary),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                stringResource(R.string.label_grid_columns),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                gridColumnOptions.forEach { cols ->
                    FilterChip(
                        selected = currentGridColumns == cols,
                        onClick = { currentGridColumns = cols },
                        label = {
                            Text(
                                if (cols == 1) stringResource(R.string.opt_column)
                                else stringResource(R.string.opt_columns, cols)
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorResource(R.color.primary),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    )
                }
            }

            if (currentRoute == "favorites" || currentRoute == "gallery") {
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onScanDuplicates?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.error),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_scan_duplicates))
                }
            }
        }
    }
}
