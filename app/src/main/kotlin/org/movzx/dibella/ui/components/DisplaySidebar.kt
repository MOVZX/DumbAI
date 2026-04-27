package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R
import org.movzx.dibella.util.scrollbar

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
    onScanDuplicates: () -> Unit = {},
) {
    val context = LocalContext.current
    val pageLimits = remember(context) { context.resources.getIntArray(R.array.page_limits) }

    val gridColumnOptions =
        remember(context) { context.resources.getIntArray(R.array.grid_columns) }

    var currentPageLimit by remember(pageLimit) { mutableStateOf(pageLimit) }
    var currentGridColumns by remember(gridColumns) { mutableStateOf(gridColumns) }
    var currentType by remember(type) { mutableStateOf(type) }
    val scrollState = rememberScrollState()

    BaseSidebar(
        title = stringResource(R.string.display_options),
        onDismiss = onDismiss,
        footer = {
            Button(
                onClick = {
                    onUpdatePageLimit(currentPageLimit)
                    onUpdateGridColumns(currentGridColumns)
                    onUpdateType(currentType)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.btn_apply_settings))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().scrollbar(scrollState).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SidebarSection(title = stringResource(R.string.section_app_config)) {
                if (currentRoute == "feed") {
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
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (currentRoute != "feed") {
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
                        )

                        FilterChip(
                            selected = currentType == "image",
                            onClick = { currentType = "image" },
                            label = { Text(stringResource(R.string.opt_image)) },
                        )

                        FilterChip(
                            selected = currentType == "video",
                            onClick = { currentType = "video" },
                            label = { Text(stringResource(R.string.opt_video)) },
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
                        )
                    }
                }

                if (currentRoute == "favorites" || currentRoute == "gallery") {
                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onScanDuplicates,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    ) {
                        Text(stringResource(R.string.btn_scan_duplicates))
                    }
                }
            }
        }
    }
}
