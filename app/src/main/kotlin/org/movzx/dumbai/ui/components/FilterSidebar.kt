package org.movzx.dumbai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dumbai.R
import org.movzx.dumbai.util.scrollbar

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
) {
    val context = LocalContext.current

    val tagOptions =
        remember(context) {
            val names = context.resources.getStringArray(R.array.tag_names)
            val ids = context.resources.getIntArray(R.array.tag_ids)
            names.mapIndexed { index, name -> CivitaiTag(ids[index], name) }
        }

    val contentTypes = remember(context) { context.resources.getStringArray(R.array.content_types) }
    val nsfwLevels = remember(context) { context.resources.getStringArray(R.array.nsfw_levels) }
    val sortOptions = remember(context) { context.resources.getStringArray(R.array.sort_options) }
    val periods = remember(context) { context.resources.getStringArray(R.array.periods) }
    var currentNsfw by remember(nsfw) { mutableStateOf(nsfw) }
    var currentSort by remember(sort) { mutableStateOf(sort) }
    var currentPeriod by remember(period) { mutableStateOf(period) }
    var currentType by remember(type) { mutableStateOf(type) }
    var currentTagIds by remember(tagIds) { mutableStateOf(tagIds) }

    val selectedTagIdsList =
        remember(currentTagIds) {
            currentTagIds?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
        }

    val nsfwTags =
        remember(context) {
            val names = context.resources.getStringArray(R.array.nsfw_tag_names)
            val ids = context.resources.getIntArray(R.array.nsfw_tag_ids)

            names.mapIndexed { index, name -> CivitaiTag(ids[index], name) }
        }

    val scrollState = rememberScrollState()

    BaseSidebar(
        title = stringResource(R.string.filters),
        onDismiss = onDismiss,
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
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.btn_apply_settings))
                }

                OutlinedButton(
                    onClick = {
                        onResetFilters()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = androidx.compose.ui.res.colorResource(R.color.error)
                        ),
                ) {
                    Text(stringResource(R.string.btn_reset))
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().scrollbar(scrollState).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SidebarSection(title = stringResource(R.string.content_type)) {
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
                        )
                    }
                }
            }

            SidebarSection(title = stringResource(R.string.tags)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = currentTagIds == null,
                        onClick = { currentTagIds = null },
                        label = { Text(stringResource(R.string.opt_all)) },
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
                            )
                        }
                    }
                }

                if (currentNsfw != "None") {
                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "NSFW Tags",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                                            if (newList.isEmpty()) null
                                            else newList.joinToString(",")
                                    },
                                    label = { Text(tag.name) },
                                )
                            }
                        }
                    }
                }
            }

            SidebarSection(title = stringResource(R.string.nsfw)) {
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
                        )
                    }
                }
            }

            SidebarSection(title = stringResource(R.string.sort)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortOptions.forEach { option ->
                        FilterChip(
                            selected = currentSort == option,
                            onClick = { currentSort = option },
                            label = {
                                Text(
                                    when (option) {
                                        "Most Reactions" ->
                                            stringResource(R.string.opt_most_reactions)
                                        "Most Comments" ->
                                            stringResource(R.string.opt_most_comments)
                                        "Newest" -> stringResource(R.string.opt_newest)
                                        else -> option
                                    }
                                )
                            },
                        )
                    }
                }
            }

            SidebarSection(title = stringResource(R.string.period)) {
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
                        )
                    }
                }
            }
        }
    }
}
