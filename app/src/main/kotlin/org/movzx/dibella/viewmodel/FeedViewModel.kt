package org.movzx.dibella.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.movzx.dibella.R
import org.movzx.dibella.data.BookmarkRepository
import org.movzx.dibella.data.FavoritesRepository
import org.movzx.dibella.data.FeedRepository
import org.movzx.dibella.data.GalleryRepository
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.util.Logger

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel
@Inject
constructor(
    repository: UserPreferencesRepository,
    favoritesRepository: FavoritesRepository,
    galleryRepository: GalleryRepository,
    private val feedRepository: FeedRepository,
    private val bookmarkRepository: BookmarkRepository,
) : BaseViewModel(repository, favoritesRepository, galleryRepository) {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private var imageFeed = emptyList<CivitaiImage>()
    private var videoFeed = emptyList<CivitaiImage>()
    private var imageCursor: String? = null
    private var videoCursor: String? = null
    private var currentImageCursor: String? = null
    private var currentVideoCursor: String? = null
    private var loadingJob: Job? = null
    private var isFirstSettingsLoad = true
    private var isJumping = false

    init {
        viewModelScope.launch {
            combine(
                    repository.nsfw,
                    repository.sort,
                    repository.period,
                    repository.type,
                    repository.tagIds,
                    repository.pageLimit,
                    repository.gridColumns,
                    favoritesRepository.favoriteIds,
                    repository.effectiveFavoritesPath,
                ) { values ->
                    values
                }
                .collect { values ->
                    val nsfw = values[0] as String
                    val sort = values[1] as String
                    val period = values[2] as String
                    val type = values[3] as String
                    val tagIds = values[4] as? String
                    val pageLimit = values[5] as Int
                    val gridColumns = values[6] as Int
                    @Suppress("UNCHECKED_CAST") val favoriteIds = values[7] as Set<Long>
                    @Suppress("UNCHECKED_CAST") val favoritesPath = values[8] as String
                    val nsfwChanged = nsfw != _uiState.value.nsfw
                    val sortChanged = sort != _uiState.value.sort
                    val periodChanged = period != _uiState.value.period
                    val typeChanged = type != _uiState.value.type
                    val tagsChanged = tagIds != _uiState.value.tagIds

                    val shouldResetRestoration =
                        !BaseViewModel.isImporting &&
                            !isFirstSettingsLoad &&
                            (nsfwChanged ||
                                sortChanged ||
                                periodChanged ||
                                tagsChanged ||
                                typeChanged)

                    _uiState.update {
                        it.copy(
                            nsfw = nsfw,
                            sort = sort,
                            period = period,
                            type = type,
                            tagIds = tagIds,
                            pageLimit = pageLimit,
                            gridColumns = gridColumns,
                            favoriteIds = favoriteIds,
                            favoritesPath = favoritesPath,
                            isRestored = if (shouldResetRestoration) false else it.isRestored,
                        )
                    }

                    if (
                        !BaseViewModel.isImporting &&
                            !isFirstSettingsLoad &&
                            !isJumping &&
                            (nsfwChanged ||
                                sortChanged ||
                                periodChanged ||
                                tagsChanged ||
                                typeChanged)
                    ) {
                        refresh()
                    }

                    isFirstSettingsLoad = false
                }
        }

        viewModelScope.launch {
            val currentType = repository.type.first()
            val initialScrollIndex = repository.feedScrollIndex(currentType).first()
            val initialScrollOffset = repository.feedScrollOffset(currentType).first()

            imageCursor = repository.nextCursor("image").first()
            videoCursor = repository.nextCursor("video").first()
            imageFeed = feedRepository.getCachedFeed("image")
            videoFeed = feedRepository.getCachedFeed("video")
            val initialImages = if (currentType == "video") videoFeed else imageFeed

            _uiState.update {
                it.copy(
                    scrollIndex = initialScrollIndex,
                    scrollOffset = initialScrollOffset,
                    images = initialImages,
                    hasMore = (if (currentType == "video") videoCursor else imageCursor) != null,
                    nextCursor = if (currentType == "video") videoCursor else imageCursor,
                    isRestored = if (BaseViewModel.isImporting) false else it.isRestored,
                )
            }

            if (_uiState.value.images.isEmpty()) loadImages(isNew = true)
        }

        viewModelScope.launch {
            repository.type
                .flatMapLatest { type -> repository.feedScrollIndex(type) }
                .collect { index -> _uiState.update { it.copy(scrollIndex = index) } }
        }

        viewModelScope.launch {
            repository.type
                .flatMapLatest { type -> repository.feedScrollOffset(type) }
                .collect { offset -> _uiState.update { it.copy(scrollOffset = offset) } }
        }
    }

    override fun setActiveRoute(route: String?) {
        val wasInactive = activeRoute != "feed"

        super.setActiveRoute(route)

        if (route == "feed" && wasInactive) if (_uiState.value.images.isEmpty()) refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                images = emptyList(),
                hasMore = false,
                isLoading = true,
                isRefreshing = true,
                scrollIndex = 0,
                scrollOffset = 0,
                isRestored = false,
            )
        }

        val currentType = _uiState.value.type

        if (currentType == "video") {
            videoFeed = emptyList()
            videoCursor = null
        } else {
            imageFeed = emptyList()
            imageCursor = null
        }

        viewModelScope.launch {
            feedRepository.clearCache(currentType)
            repository.updateScrollPosition(currentType, 0, 0)
            repository.updateNextCursor(currentType, null)
            loadImages(isNew = true)
        }
    }

    fun loadMore() {
        val currentCursor = if (_uiState.value.type == "video") videoCursor else imageCursor

        if (currentCursor != null && !_uiState.value.isLoading) {
            Logger.d("Dibella_Cache", "Loading More Content | Cursor: $currentCursor")

            loadImages(isNew = false)
        }
    }

    fun jumpToCursor(cursor: String?) {
        if (_uiState.value.isLoading) return

        Logger.d("Dibella_Cache", "Jumping to Cursor: $cursor")

        val targetType = _uiState.value.type

        if (targetType == "video") videoCursor = cursor else imageCursor = cursor

        viewModelScope.launch { repository.updateNextCursor(targetType, cursor) }
        loadImages(isNew = true, cursorOverride = cursor)
    }

    private fun loadImages(isNew: Boolean, cursorOverride: String? = null) {
        if (activeRoute != null && activeRoute != "feed") return
        if (_uiState.value.isLoading && !isNew) return

        loadingJob?.cancel()

        loadingJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isRefreshing = isNew) }

            val targetType = _uiState.value.type

            val targetCursor =
                cursorOverride
                    ?: if (isNew) null
                    else (if (targetType == "video") videoCursor else imageCursor)

            try {
                val currentList = if (targetType == "video") videoFeed else imageFeed
                val startOrderIndex = if (isNew) 0 else currentList.size

                val (newItems, nextCursor) =
                    feedRepository.fetchImages(
                        type = targetType,
                        nsfw = _uiState.value.nsfw,
                        sort = _uiState.value.sort,
                        period = _uiState.value.period,
                        tagIds = _uiState.value.tagIds,
                        limit = _uiState.value.pageLimit,
                        cursor = targetCursor,
                        isNew = isNew,
                        startOrderIndex = startOrderIndex,
                    )

                if (_uiState.value.type != targetType) return@launch

                val finalItems =
                    if (isNew) newItems else (currentList + newItems).distinctBy { it.id }

                if (targetType == "video") {
                    videoFeed = finalItems
                    currentVideoCursor = targetCursor
                    videoCursor = nextCursor
                } else {
                    imageFeed = finalItems
                    currentImageCursor = targetCursor
                    imageCursor = nextCursor
                }

                if (_uiState.value.type == targetType)
                    _uiState.update {
                        it.copy(
                            images = finalItems,
                            hasMore = nextCursor != null,
                            nextCursor = nextCursor,
                        )
                    }

                repository.updateNextCursor(targetType, nextCursor)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException)
                    sendMessage(R.string.msg_load_failed)
            } finally {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    fun updateFilters(nsfw: String, sort: String, period: String, type: String, tagIds: String?) {
        viewModelScope.launch { repository.updateFilters(nsfw, sort, period, type, tagIds) }
    }

    fun resetFilters() {
        viewModelScope.launch {
            repository.updateFilters("None", "Most Reactions", "AllTime", "image", null)
        }
    }

    override fun downloadImage(image: CivitaiImage) {
        Logger.d("Dibella_Cache", "Download Initiated | ID: ${image.id}")

        performDownload(
            image = image,
            onUpdateProgress = { id, progress ->
                _uiState.update { state ->
                    val newProgresses = state.downloadProgresses.toMutableMap()

                    if (progress != null) newProgresses[id] = progress else newProgresses.remove(id)

                    state.copy(downloadProgresses = newProgresses)
                }
            },
        )
    }

    fun saveBookmark(title: String) {
        val currentState = _uiState.value

        val cursor =
            (if (currentState.type == "video") currentVideoCursor else currentImageCursor)
                ?: (if (currentState.type == "video") videoCursor else imageCursor)

        if (cursor == null) {
            sendMessage(R.string.msg_load_failed)

            return
        }

        viewModelScope.launch {
            bookmarkRepository.addBookmark(
                org.movzx.dibella.model.Bookmark(
                    title = title,
                    type = currentState.type,
                    sort = currentState.sort,
                    period = currentState.period,
                    nsfw = currentState.nsfw,
                    cursor = cursor,
                    tags = currentState.tagIds,
                )
            )
            sendMessage(R.string.msg_bookmark_saved)
        }
    }

    fun loadBookmark(bookmark: org.movzx.dibella.model.Bookmark) {
        viewModelScope.launch {
            isJumping = true

            repository.updateFilters(
                nsfw = bookmark.nsfw,
                sort = bookmark.sort,
                period = bookmark.period,
                type = bookmark.type,
                tagIds = bookmark.tags?.ifEmpty { null },
            )

            val cursor = bookmark.cursor.ifEmpty { null }

            repository.updateNextCursor(bookmark.type, cursor)
            jumpToCursor(cursor)
            sendMessage(R.string.msg_bookmark_loaded)
            kotlinx.coroutines.delay(500)

            isJumping = false
        }
    }

    fun markRestored() {
        _uiState.update { it.copy(isRestored = true) }
    }
}
