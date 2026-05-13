package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterSidebar(
    type: String,
    sort: String,
    onDismiss: () -> Unit,
    onFilterChange: (String, String) -> Unit,
    onResetFilters: () -> Unit,
    amoledMode: Boolean = false,
) {
    val contentTypes = stringArrayResource(R.array.search_content_types)
    val sortOptions = stringArrayResource(R.array.search_sort_options)
    var currentType by remember(type) { mutableStateOf(type) }
    var currentSort by remember(sort) { mutableStateOf(sort) }

    BaseSidebar(
        title = stringResource(R.string.filters),
        onDismiss = onDismiss,
        amoledMode = amoledMode,
        footer = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        onFilterChange(currentType, currentSort)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_apply_settings))
                }

                OutlinedButton(
                    onClick = {
                        onResetFilters()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = androidx.compose.ui.res.colorResource(R.color.error)
                        ),
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))

                    Spacer(Modifier.width(8.dp))

                    Text(stringResource(R.string.btn_reset))
                }
            }
        },
    ) {
        SidebarSection(
            title = stringResource(R.string.content_type),
            icon = Icons.Default.FilterList,
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                contentTypes.forEach { option ->
                    FilterChip(
                        selected = currentType == option,
                        onClick = { currentType = option },
                        label = {
                            Text(
                                when (option) {
                                    "image" -> stringResource(R.string.opt_image)
                                    "video" -> stringResource(R.string.opt_video)
                                    else -> option
                                }
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorResource(R.color.primary),
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                            ),
                    )
                }
            }
        }

        SidebarSection(
            title = stringResource(R.string.sort),
            icon = Icons.AutoMirrored.Filled.Sort,
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sortOptions.forEach { option ->
                    FilterChip(
                        selected = currentSort == option,
                        onClick = { currentSort = option },
                        label = {
                            Text(
                                when (option) {
                                    "Relevancy" -> stringResource(R.string.opt_relevancy)
                                    "Most Reactions" -> stringResource(R.string.opt_most_reactions)
                                    "Most Comments" -> stringResource(R.string.opt_most_comments)
                                    "Most Collected" -> stringResource(R.string.opt_most_collected)
                                    "Most Buzz" -> stringResource(R.string.opt_most_buzz)
                                    "Newest" -> stringResource(R.string.opt_newest)
                                    else -> option
                                }
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorResource(R.color.primary),
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                            ),
                    )
                }
            }
        }
    }
}
