package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkEditDialog(
    bookmark: org.movzx.dibella.model.Bookmark,
    onConfirm: (org.movzx.dibella.model.Bookmark) -> Unit,
    onDismiss: () -> Unit,
    showTagSelection: () -> Unit,
) {
    val view = LocalView.current
    val isSearchBookmark = bookmark.query != null
    var editTitle by remember(bookmark.title) { mutableStateOf(bookmark.title) }
    var editQuery by remember(bookmark.query ?: "") { mutableStateOf(bookmark.query ?: "") }
    var editCursor by remember(bookmark.cursor ?: "") { mutableStateOf(bookmark.cursor ?: "") }
    var editOffset by remember(bookmark.offset ?: 0) { mutableIntStateOf(bookmark.offset ?: 0) }
    var editTags by remember(bookmark.tags ?: "") { mutableStateOf(bookmark.tags ?: "") }
    var editSort by remember(bookmark.sort) { mutableStateOf(bookmark.sort) }
    var editPeriod by remember(bookmark.period) { mutableStateOf(bookmark.period) }
    var editNsfw by remember(bookmark.nsfw) { mutableStateOf(bookmark.nsfw) }

    val sortOptions =
        if (isSearchBookmark) stringArrayResource(R.array.search_sort_options)
        else stringArrayResource(R.array.sort_options)

    val periodOptions = stringArrayResource(R.array.periods)
    val nsfwLevels = stringArrayResource(R.array.nsfw_levels)
    var sortExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }
    var nsfwExpanded by remember { mutableStateOf(false) }

    val scale by
        animateFloatAsState(
            targetValue = 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "dialogScale",
        )

    ModernDialog(
        title = "Edit Bookmark",
        icon = Icons.Default.Edit,
        onConfirm = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)

            val original = bookmark

            val filtersChanged =
                !isSearchBookmark &&
                    (original.sort != editSort ||
                        original.period != editPeriod ||
                        original.nsfw != editNsfw ||
                        original.tags != editTags)

            val finalCursor = if (filtersChanged) "" else editCursor
            val finalOffset = if (filtersChanged) null else original.offset

            if (isSearchBookmark)
                onConfirm(
                    original.copy(
                        title = editTitle,
                        query = editQuery,
                        offset = editOffset,
                        sort = editSort,
                    )
                )
            else
                onConfirm(
                    original.copy(
                        title = editTitle,
                        cursor = finalCursor,
                        tags = editTags,
                        sort = editSort,
                        period = editPeriod,
                        nsfw = editNsfw,
                        offset = finalOffset,
                    )
                )
        },
        confirmText = "Save",
        confirmIcon = Icons.Default.Check,
        onDismiss = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onDismiss()
        },
        dismissText = "Cancel",
        dismissIcon = Icons.Default.Close,
        customContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isSearchBookmark) {
                    OutlinedTextField(
                        value = editQuery,
                        onValueChange = { editQuery = it },
                        label = { Text("Query") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = editOffset.toString(),
                        onValueChange = { editOffset = it.toIntOrNull() ?: 0 },
                        label = { Text("Offset") },
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = editCursor,
                        onValueChange = { editCursor = it },
                        label = { Text("Cursor") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Button(
                        onClick = showTagSelection,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    ) {
                        Text(if (editTags.isBlank()) "Select Tags" else "Tags: $editTags")
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = !sortExpanded },
                ) {
                    OutlinedTextField(
                        value = editSort,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sort") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded)
                        },
                        modifier =
                            Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    true,
                                )
                                .fillMaxWidth(),
                    )

                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    editSort = option
                                    sortExpanded = false
                                },
                            )
                        }
                    }
                }

                if (!isSearchBookmark) {
                    ExposedDropdownMenuBox(
                        expanded = periodExpanded,
                        onExpandedChange = { periodExpanded = !periodExpanded },
                    ) {
                        OutlinedTextField(
                            value = editPeriod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Period") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded)
                            },
                            modifier =
                                Modifier.menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        true,
                                    )
                                    .fillMaxWidth(),
                        )

                        ExposedDropdownMenu(
                            expanded = periodExpanded,
                            onDismissRequest = { periodExpanded = false },
                        ) {
                            periodOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        editPeriod = option
                                        periodExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = nsfwExpanded,
                        onExpandedChange = { nsfwExpanded = !nsfwExpanded },
                    ) {
                        OutlinedTextField(
                            value = editNsfw,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("NSFW Level") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = nsfwExpanded)
                            },
                            modifier =
                                Modifier.menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        true,
                                    )
                                    .fillMaxWidth(),
                        )

                        ExposedDropdownMenu(
                            expanded = nsfwExpanded,
                            onDismissRequest = { nsfwExpanded = false },
                        ) {
                            nsfwLevels.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        editNsfw = option
                                        nsfwExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}
