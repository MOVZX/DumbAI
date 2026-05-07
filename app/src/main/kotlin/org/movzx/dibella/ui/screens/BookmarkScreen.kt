@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package org.movzx.dibella.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.movzx.dibella.R
import org.movzx.dibella.RightSidebarType
import org.movzx.dibella.model.Bookmark
import org.movzx.dibella.ui.components.*
import org.movzx.dibella.viewmodel.BookmarkViewModel
import org.movzx.dibella.viewmodel.FavoritesViewModel
import org.movzx.dibella.viewmodel.FeedViewModel
import org.movzx.dibella.viewmodel.GalleryViewModel

private data class CivitaiTag(val id: Int?, val name: String)

@Composable
fun BookmarkScreen(
    currentRoute: String?,
    amoledMode: Boolean,
    onNavigate: (String) -> Unit,
    onOpenRightSidebar: (RightSidebarType) -> Unit,
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    selectedImageIndex: Int?,
    backPressedTime: Long,
    onUpdateBackPressedTime: (Long) -> Unit,
    exitConfirmMsg: String,
    galleryRestored: Boolean,
    onGalleryRestored: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val viewModel: BookmarkViewModel = hiltViewModel(activity)
    val feedViewModel: FeedViewModel = hiltViewModel(activity)
    val favViewModel: FavoritesViewModel = hiltViewModel(activity)
    val galleryViewModel: GalleryViewModel = hiltViewModel(activity)
    val uiState by viewModel.uiState.collectAsState()
    val feedState by feedViewModel.uiState.collectAsState()
    val favState by favViewModel.uiState.collectAsState()
    val galleryState by galleryViewModel.uiState.collectAsState()
    val gridState = rememberLazyStaggeredGridState()
    var showDeleteConfirmDialog by remember { mutableStateOf<Bookmark?>(null) }
    var showEditDialog by remember { mutableStateOf<Bookmark?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editCursor by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    var editSort by remember { mutableStateOf("") }
    var editPeriod by remember { mutableStateOf("") }
    var editNsfw by remember { mutableStateOf("") }
    var showTagDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog != null) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_bookmark_title),
            message = stringResource(R.string.dialog_delete_bookmark_msg),
            onConfirm = {
                viewModel.deleteBookmark(showDeleteConfirmDialog!!)
                showDeleteConfirmDialog = null
            },
            onDismiss = { showDeleteConfirmDialog = null },
        )
    }

    val nsfwTagNames = stringArrayResource(R.array.nsfw_tag_names)
    val nsfwTagIds = integerArrayResource(R.array.nsfw_tag_ids)

    val allTags =
        remember(context, editNsfw, nsfwTagNames, nsfwTagIds) {
            val tagNames = context.resources.getStringArray(R.array.tag_names)
            val tagIds = context.resources.getIntArray(R.array.tag_ids)
            val tags =
                tagNames
                    .mapIndexed { index, name -> CivitaiTag(tagIds[index], name) }
                    .toMutableList()
            if (editNsfw != "None") {
                tags.addAll(
                    nsfwTagNames.mapIndexed { index, name -> CivitaiTag(nsfwTagIds[index], name) }
                )
            }
            tags
        }

    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Select Tags") },
            text = {
                val selectedIds = editTags.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                ) {
                    allTags.forEach { tag ->
                        val isSelected = selectedIds.contains(tag.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val currentIds =
                                    editTags
                                        .split(",")
                                        .mapNotNull { it.trim().toIntOrNull() }
                                        .toMutableSet()
                                if (isSelected) currentIds.remove(tag.id)
                                else currentIds.add(tag.id!!)
                                editTags = currentIds.joinToString(",")
                            },
                            label = { Text(tag.name) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTagDialog = false }) { Text("OK") } },
        )
    }

    if (showEditDialog != null) {
        val sortOptions = stringArrayResource(R.array.sort_options)
        val periodOptions = stringArrayResource(R.array.periods)
        val nsfwLevels = stringArrayResource(R.array.nsfw_levels)

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(text = "Edit Bookmark") },
            text = {
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
                    OutlinedTextField(
                        value = editCursor,
                        onValueChange = { editCursor = it },
                        label = { Text("Cursor") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { showTagDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (editTags.isBlank()) "Select Tags" else "Tags: $editTags")
                    }
                    var sortExpanded by remember { mutableStateOf(false) }
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
                    var periodExpanded by remember { mutableStateOf(false) }
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
                    var nsfwExpanded by remember { mutableStateOf(false) }
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBookmarkTitle(
                            showEditDialog!!.copy(
                                title = editTitle,
                                cursor = editCursor,
                                tags = editTags,
                                sort = editSort,
                                period = editPeriod,
                                nsfw = editNsfw,
                            ),
                            editTitle,
                        )
                        showEditDialog = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = null }) { Text("Cancel") } },
        )
    }

    AppScaffold(
        topBar = {
            MainTopBar(
                gridColumns = 1,
                onShowDisplayOptions = null,
                onUpdateGridColumns = null,
                onShowFilters = null,
                onShowSettings = { onOpenRightSidebar(RightSidebarType.SETTINGS) },
            )
        },
        bottomBar = {
            MainBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                feedCount = feedState.images.size,
                favoritesCount = favState.images.size,
                galleryCount = galleryState.images.size,
                bookmarkCount = uiState.bookmarkCount,
            )
        },
        gridState = gridState,
        isLoading = uiState.isLoading,
        amoledMode = amoledMode,
        showBookmarkJump = false,
    ) { padding ->
        if (uiState.isLoading && uiState.bookmarks.isEmpty()) {
            SkeletonGrid(columnCount = 1)
        } else if (uiState.bookmarks.isEmpty() && !uiState.isLoading) {
            EmptyState("bookmarks")
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(1),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.bookmarks, key = { it.id }) { bookmark ->
                    BookmarkCard(
                        bookmark = bookmark,
                        onLoad = {
                            feedViewModel.loadBookmark(bookmark)
                            onNavigate("feed")
                        },
                        onEdit = {
                            editTitle = bookmark.title
                            editCursor = bookmark.cursor ?: ""
                            editTags = bookmark.tags ?: ""
                            editSort = bookmark.sort
                            editPeriod = bookmark.period
                            editNsfw = bookmark.nsfw
                            showEditDialog = bookmark
                        },
                        onDelete = { showDeleteConfirmDialog = bookmark },
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkCard(bookmark: Bookmark, onLoad: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current

    val tagMap =
        remember(context) {
            try {
                val map = mutableMapOf<String, String>()
                val tagNames = context.resources.getStringArray(R.array.tag_names)
                val tagIds = context.resources.getIntArray(R.array.tag_ids)

                tagIds.forEachIndexed { index, id -> map[id.toString()] = tagNames[index] }

                val nsfwTagNames = context.resources.getStringArray(R.array.nsfw_tag_names)
                val nsfwTagIds = context.resources.getIntArray(R.array.nsfw_tag_ids)

                nsfwTagIds.forEachIndexed { index, id -> map[id.toString()] = nsfwTagNames[index] }

                map
            } catch (e: Exception) {
                emptyMap()
            }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            val dateFormat = remember {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            }

            val dateStr =
                remember(bookmark.timestamp) {
                    dateFormat.format(java.util.Date(bookmark.timestamp))
                }

            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = bookmark.title,
                modifier = Modifier.padding(vertical = 4.dp),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text =
                    "Type: ${bookmark.type.replaceFirstChar { it.uppercase() }} | Cursor: ${bookmark.cursor ?: ""}",
                style = MaterialTheme.typography.labelSmall,
            )

            Text(
                text = "Sort: ${bookmark.sort} | Period: ${bookmark.period}",
                style = MaterialTheme.typography.labelSmall,
            )

            if (!bookmark.tags.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                val resolvedTags =
                    remember(bookmark.tags, tagMap) {
                        bookmark.tags.split(",").mapNotNull {
                            val id = it.trim()

                            if (id.isNotEmpty()) tagMap[id] ?: id else null
                        }
                    }

                if (resolvedTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        resolvedTags.forEach { tagName ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                border =
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                color = androidx.compose.ui.graphics.Color.Transparent,
                            ) {
                                Text(
                                    text = tagName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(onClick = onLoad, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(text = "Load", style = MaterialTheme.typography.labelSmall)
                }

                FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(text = "Edit", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(text = "Delete", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
