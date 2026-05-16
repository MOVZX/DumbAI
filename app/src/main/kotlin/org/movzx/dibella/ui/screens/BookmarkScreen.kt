@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package org.movzx.dibella.ui.screens

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
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
import org.movzx.dibella.viewmodel.SearchViewModel

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

    LaunchedEffect(viewModel) {
        viewModel.uiMessage.collect { resId ->
            Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show()
        }
    }
    val feedState by feedViewModel.uiState.collectAsState()
    val favState by favViewModel.uiState.collectAsState()
    val galleryState by galleryViewModel.uiState.collectAsState()
    val gridState = rememberLazyStaggeredGridState()
    val searchViewModel: SearchViewModel = hiltViewModel(activity)
    val searchCount by searchViewModel.searchResultCount.collectAsState(initial = 0)
    var showLoadConfirmDialog by remember { mutableStateOf<Bookmark?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Bookmark?>(null) }
    var showEditDialog by remember { mutableStateOf<Bookmark?>(null) }
    val isSearchBookmark by derivedStateOf { showEditDialog?.query != null }
    var bookmarkSearch by remember { mutableStateOf("") }

    val filteredBookmarks =
        remember(uiState.bookmarks, bookmarkSearch) {
            if (bookmarkSearch.isBlank()) uiState.bookmarks
            else uiState.bookmarks.filter { it.title.contains(bookmarkSearch, ignoreCase = true) }
        }

    var editTitle by remember { mutableStateOf("") }
    var editCursor by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    var editSort by remember { mutableStateOf("") }
    var editPeriod by remember { mutableStateOf("") }
    var editNsfw by remember { mutableStateOf("") }
    var editQuery by remember { mutableStateOf("") }
    var editOffset by remember { mutableStateOf(0) }
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

    if (showLoadConfirmDialog != null) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_load_bookmark_title),
            message = stringResource(R.string.dialog_load_bookmark_msg),
            onConfirm = {
                val bookmark = showLoadConfirmDialog!!

                if (bookmark.query != null) {
                    searchViewModel.loadSearchBookmark(bookmark)
                    onNavigate("search")
                } else {
                    feedViewModel.loadBookmark(bookmark)
                    onNavigate("feed")
                }

                showLoadConfirmDialog = null
            },
            onDismiss = { showLoadConfirmDialog = null },
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
                    .mapIndexed { index, name -> TagOption(tagIds[index], name) }
                    .toMutableList()
            if (editNsfw != "None") {
                tags.addAll(
                    nsfwTagNames.mapIndexed { index, name -> TagOption(nsfwTagIds[index], name) }
                )
            }
            tags
        }

    if (showTagDialog) {
        TagSelectionDialog(
            allTags = allTags,
            initialTags = editTags,
            onConfirm = { newTags ->
                editTags = newTags
                showTagDialog = false
            },
            onDismiss = { showTagDialog = false },
        )
    }

    if (showEditDialog != null) {
        BookmarkEditDialog(
            bookmark = showEditDialog!!,
            onConfirm = { updatedBookmark ->
                if (isSearchBookmark) {
                    viewModel.updateBookmarkTitle(
                        updatedBookmark.copy(
                            title = editTitle,
                            query = editQuery,
                            offset = editOffset,
                            sort = editSort,
                        ),
                        editTitle,
                    )
                } else {
                    val filtersChanged =
                        showEditDialog!!.sort != editSort ||
                            showEditDialog!!.period != editPeriod ||
                            showEditDialog!!.nsfw != editNsfw ||
                            showEditDialog!!.tags != editTags

                    val finalCursor = if (filtersChanged) "" else editCursor
                    val finalOffset = if (filtersChanged) null else showEditDialog!!.offset

                    viewModel.updateBookmarkTitle(
                        updatedBookmark.copy(
                            title = editTitle,
                            cursor = finalCursor,
                            tags = editTags,
                            sort = editSort,
                            period = editPeriod,
                            nsfw = editNsfw,
                            offset = finalOffset,
                        ),
                        editTitle,
                    )
                }

                showEditDialog = null
            },
            onDismiss = { showEditDialog = null },
            showTagSelection = { showTagDialog = true },
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
                searchCount = searchCount,
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
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (uiState.bookmarks.isNotEmpty()) {
                    OutlinedTextField(
                        value = bookmarkSearch,
                        onValueChange = { bookmarkSearch = it },
                        placeholder = {
                            Text(stringResource(R.string.search_bookmarks_placeholder))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        shape = MaterialTheme.shapes.small,
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (filteredBookmarks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) { EmptyState("bookmarks") }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(1),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredBookmarks, key = { it.id }) { bookmark ->
                            BookmarkCard(
                                bookmark = bookmark,
                                onLoad = { showLoadConfirmDialog = bookmark },
                                onEdit = {
                                    editTitle = bookmark.title

                                    if (bookmark.query != null) {
                                        editQuery = bookmark.query
                                        editOffset = bookmark.offset ?: 0
                                        editSort = bookmark.sort
                                    } else {
                                        editCursor = bookmark.cursor
                                        editTags = bookmark.tags ?: ""
                                        editSort = bookmark.sort
                                        editPeriod = bookmark.period
                                        editNsfw = bookmark.nsfw
                                    }

                                    showEditDialog = bookmark
                                },
                                onDelete = { showDeleteConfirmDialog = bookmark },
                            )
                        }
                    }
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

    val resolvedTags =
        remember(bookmark.tags, tagMap) {
            bookmark.tags?.split(",")?.mapNotNull {
                val id = it.trim()

                if (id.isNotEmpty()) tagMap[id] ?: id else null
            } ?: emptyList()
        }

    val dateFormat = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    }

    val dateStr =
        remember(bookmark.timestamp) { dateFormat.format(java.util.Date(bookmark.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = bookmark.title,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = buildMetadataString(bookmark),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (resolvedTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onLoad,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.success),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
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

                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.error),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
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

private fun buildMetadataString(bookmark: Bookmark): String {
    val parts = mutableListOf<String>()

    parts.add("Type: ${bookmark.type.replaceFirstChar { it.uppercase() }}")
    parts.add("Sort: ${bookmark.sort}")

    if (bookmark.query != null) {
        parts.add("Query: ${bookmark.query}")
        parts.add("Offset: ${bookmark.offset ?: 0}")
    } else {
        parts.add("Period: ${bookmark.period}")
        parts.add("Cursor: ${bookmark.cursor}")
    }

    return parts.joinToString(" | ")
}
