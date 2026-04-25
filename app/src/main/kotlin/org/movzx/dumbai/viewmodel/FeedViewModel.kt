package org.movzx.dumbai.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.movzx.dumbai.R
import org.movzx.dumbai.api.CivitaiApi
import org.movzx.dumbai.data.FavoritesRepository
import org.movzx.dumbai.data.FeedCacheDao
import org.movzx.dumbai.data.GalleryRepository
import org.movzx.dumbai.data.UserPreferencesRepository
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.FeedItemCache

@HiltViewModel
class FeedViewModel
@Inject
constructor(
    repository: UserPreferencesRepository,
    favoritesRepository: FavoritesRepository,
    galleryRepository: GalleryRepository,
    private val civitaiApi: CivitaiApi,
    private val feedCacheDao: FeedCacheDao,
) : BaseViewModel(repository, favoritesRepository, galleryRepository) {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private var imageFeed = emptyList<CivitaiImage>()
    private var videoFeed = emptyList<CivitaiImage>()
    private var imageCursor: String? = null
    private var videoCursor: String? = null
    private var loadingJob: Job? = null
    private var isFirstSettingsLoad = true

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
                    val favoriteIds = values[7] as Set<Long>
                    val nsfwChanged = nsfw != _uiState.value.nsfw
                    val sortChanged = sort != _uiState.value.sort
                    val periodChanged = period != _uiState.value.period
                    val typeChanged = type != _uiState.value.type
                    val tagsChanged = tagIds != _uiState.value.tagIds

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
                            isRestored =
                                if (
                                    !isFirstSettingsLoad &&
                                        (nsfwChanged ||
                                            sortChanged ||
                                            periodChanged ||
                                            tagsChanged ||
                                            typeChanged)
                                )
                                    false
                                else it.isRestored,
                        )
                    }

                    if (
                        !isFirstSettingsLoad &&
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
            repository.type
                .flatMapLatest { type -> repository.feedScrollIndex(type) }
                .collect { index -> _uiState.update { it.copy(scrollIndex = index) } }
        }

        viewModelScope.launch {
            repository.type
                .flatMapLatest { type -> repository.feedScrollOffset(type) }
                .collect { offset -> _uiState.update { it.copy(scrollOffset = offset) } }
        }

        viewModelScope.launch {
            imageCursor = repository.nextCursor("image").first()
            videoCursor = repository.nextCursor("video").first()
            val cachedImages = feedCacheDao.getFeed("image").map { it.toCivitaiImage() }
            val cachedVideos = feedCacheDao.getFeed("video").map { it.toCivitaiImage() }
            imageFeed = cachedImages
            videoFeed = cachedVideos
            val currentType = repository.type.first()
            val initialImages = if (currentType == "video") videoFeed else imageFeed

            _uiState.update {
                it.copy(
                    images = initialImages,
                    hasMore = (if (currentType == "video") videoCursor else imageCursor) != null,
                )
            }

            if (_uiState.value.images.isEmpty()) loadImages(isNew = true)
        }
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                images = emptyList(),
                hasMore = false,
                isLoading = true,
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
            feedCacheDao.clearFeed(currentType)
            repository.updateScrollPosition(currentType, 0, 0)
            repository.updateNextCursor(currentType, null)
            loadImages(isNew = true)
        }
    }

    fun loadMore() {
        val currentCursor = if (_uiState.value.type == "video") videoCursor else imageCursor

        if (currentCursor != null && !_uiState.value.isLoading) loadImages(isNew = false)
    }

    private fun loadImages(isNew: Boolean) {
        if (_uiState.value.isLoading && !isNew) return

        loadingJob?.cancel()

        loadingJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            var success = false
            var attempt = 0
            val targetType = _uiState.value.type

            val targetCursor =
                if (isNew) null else (if (targetType == "video") videoCursor else imageCursor)

            while (!success) {
                try {
                    val response =
                        civitaiApi.getImages(
                            limit = _uiState.value.pageLimit,
                            nsfw = _uiState.value.nsfw,
                            sort = _uiState.value.sort,
                            period = _uiState.value.period,
                            type = targetType,
                            tags = _uiState.value.tagIds,
                            cursor = targetCursor,
                        )

                    if (_uiState.value.type != targetType) break

                    val items =
                        if (isNew) {
                            response.items.distinctBy { it.id }
                        } else {
                            val currentList = if (targetType == "video") videoFeed else imageFeed

                            (currentList + response.items).distinctBy { it.id }
                        }

                    val cursor = response.metadata.nextCursor

                    if (targetType == "video") {
                        videoFeed = items
                        videoCursor = cursor
                    } else {
                        imageFeed = items
                        imageCursor = cursor
                    }

                    if (_uiState.value.type == targetType)
                        _uiState.update { it.copy(images = items, hasMore = cursor != null) }

                    repository.updateNextCursor(targetType, cursor)

                    val cacheItems = items.mapIndexed { index, image ->
                        FeedItemCache.fromCivitaiImage(image, targetType, index)
                    }

                    feedCacheDao.replaceFeed(targetType, cacheItems)

                    success = true
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e

                    attempt++

                    if (attempt >= 3) {
                        sendMessage(R.string.msg_load_failed)

                        break
                    }

                    delay((2000L * attempt).coerceAtMost(10000L))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
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

    suspend fun ensureFavoriteResources(
        image: CivitaiImage,
        force: Boolean = false,
        onProgress: (Float) -> Unit = {},
    ) {
        favoritesRepository.ensureFavoriteResources(image, force, onProgress)
    }

    fun downloadImage(image: CivitaiImage) {
        performDownload(
            image = image,
            currentProgresses = _uiState.value.downloadProgresses,
            onUpdateProgress = { progresses ->
                _uiState.update { it.copy(downloadProgresses = progresses) }
            },
        )
    }

    fun markRestored() {
        _uiState.update { it.copy(isRestored = true) }
    }
}
