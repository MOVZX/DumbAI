package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

data class CivitaiTag(val id: Int?, val name: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSidebar(
    nsfw: String,
    sort: String,
    period: String,
    type: String,
    tagIds: String?,
    onDismiss: () -> Unit,
    onFilterChange: (String, String, String, String, String?) -> Unit,
    onResetFilters: () -> Unit,
    amoledMode: Boolean = false,
) {
    val tagNames = stringArrayResource(R.array.tag_names)
    val tagIdsArray = integerArrayResource(R.array.tag_ids)

    val tagOptions =
        remember(tagNames, tagIdsArray) {
            tagNames.mapIndexed { index, name -> CivitaiTag(tagIdsArray[index], name) }
        }

    val contentTypes = stringArrayResource(R.array.content_types)
    val nsfwLevels = stringArrayResource(R.array.nsfw_levels)
    val sortOptions = stringArrayResource(R.array.sort_options)
    val periods = stringArrayResource(R.array.periods)
    var currentNsfw by remember(nsfw) { mutableStateOf(nsfw) }
    var currentSort by remember(sort) { mutableStateOf(sort) }
    var currentPeriod by remember(period) { mutableStateOf(period) }
    var currentType by remember(type) { mutableStateOf(type) }
    var currentTagIds by remember(tagIds) { mutableStateOf(tagIds) }

    val selectedTagIdsList =
        remember(currentTagIds) {
            currentTagIds?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
        }

    val nsfwTagNames = stringArrayResource(R.array.nsfw_tag_names)
    val nsfwTagIdsArray = integerArrayResource(R.array.nsfw_tag_ids)

    val nsfwTags =
        remember(nsfwTagNames, nsfwTagIdsArray) {
            nsfwTagNames.mapIndexed { index, name -> CivitaiTag(nsfwTagIdsArray[index], name) }
        }

    BaseSidebar(
        title = stringResource(R.string.filters),
        onDismiss = onDismiss,
        amoledMode = amoledMode,
        footer = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        onFilterChange(
                            currentNsfw,
                            currentSort,
                            currentPeriod,
                            currentType,
                            currentTagIds,
                        )
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
                                if (option == "video") stringResource(R.string.opt_video)
                                else stringResource(R.string.opt_image)
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
                                    "Most Reactions" -> stringResource(R.string.opt_most_reactions)
                                    "Most Comments" -> stringResource(R.string.opt_most_comments)
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

        SidebarSection(title = stringResource(R.string.period), icon = Icons.Default.Schedule) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEach { option ->
                    FilterChip(
                        selected = currentPeriod == option,
                        onClick = { currentPeriod = option },
                        label = {
                            Text(
                                when (option) {
                                    "AllTime" -> stringResource(R.string.opt_all_time)
                                    "Year" -> stringResource(R.string.opt_year)
                                    "Month" -> stringResource(R.string.opt_month)
                                    "Week" -> stringResource(R.string.opt_week)
                                    "Day" -> stringResource(R.string.opt_day)
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

        SidebarSection(title = stringResource(R.string.tags), icon = Icons.Default.Tag) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = currentTagIds == null,
                    onClick = { currentTagIds = null },
                    label = { Text(stringResource(R.string.opt_all)) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorResource(R.color.primary),
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        ),
                )

                tagOptions.forEach { tag ->
                    if (tag.id != null) {
                        val isSelected = selectedTagIdsList.contains(tag.id)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newList =
                                    if (isSelected) selectedTagIdsList - tag.id
                                    else selectedTagIdsList + tag.id

                                currentTagIds =
                                    if (newList.isEmpty()) null else newList.joinToString(",")
                            },
                            label = { Text(tag.name) },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorResource(R.color.primary),
                                    selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                ),
                        )
                    }
                }
            }

            if (currentNsfw != "None") {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = stringResource(R.string.nsfw_tags),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    nsfwTags.forEach { tag ->
                        if (tag.id != null) {
                            val isSelected = selectedTagIdsList.contains(tag.id)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newList =
                                        if (isSelected) selectedTagIdsList - tag.id
                                        else selectedTagIdsList + tag.id

                                    currentTagIds =
                                        if (newList.isEmpty()) null else newList.joinToString(",")
                                },
                                label = { Text(tag.name) },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colorResource(R.color.primary),
                                        selectedLabelColor =
                                            androidx.compose.ui.graphics.Color.White,
                                    ),
                            )
                        }
                    }
                }
            }
        }

        SidebarSection(title = stringResource(R.string.nsfw), icon = Icons.Default.Warning) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                nsfwLevels.forEach { option ->
                    FilterChip(
                        selected = currentNsfw == option,
                        onClick = { currentNsfw = option },
                        label = {
                            Text(
                                when (option) {
                                    "None" -> stringResource(R.string.opt_none)
                                    "Soft" -> stringResource(R.string.opt_soft)
                                    "Mature" -> stringResource(R.string.opt_mature)
                                    "X" -> stringResource(R.string.opt_x)
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
