package org.movzx.dibella.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.movzx.dibella.R
import org.movzx.dibella.data.BookmarkRepository
import org.movzx.dibella.data.SearchRepository
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.model.Bookmark

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val searchRepository: SearchRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private val _uiMessage = MutableSharedFlow<Int>()
    val uiMessage = _uiMessage.asSharedFlow()

    val searchResultCount: StateFlow<Int> =
        _uiState.map { it.results.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private var searchJob: Job? = null
    private var currentOffset = 0
    private var currentPageStartOffset = 0
    private var pageSize = 200
    private var isJumping = false

    init {
        viewModelScope.launch {
            preferencesRepository.gridColumns.collect { columns ->
                _uiState.update { it.copy(gridColumns = columns) }
            }
        }

        viewModelScope.launch {
            preferencesRepository.pageLimit.collect { limit -> pageSize = limit }
        }

        viewModelScope.launch {
            data class SearchState(
                val query: String,
                val type: String,
                val sort: String,
                val gridColumns: Int,
                val pageLimit: Int,
            )

            val queryFlow = preferencesRepository.searchQuery
            val typeFlow = preferencesRepository.searchType
            val sortFlow = preferencesRepository.searchSort
            val gridColumnsFlow = preferencesRepository.gridColumns
            val pageLimitFlow = preferencesRepository.pageLimit

            queryFlow
                .combine(typeFlow) { q, t -> SearchState(q, t, "", 0, 0) }
                .combine(sortFlow) { qt, s ->
                    SearchState(qt.query, qt.type, s, qt.gridColumns, qt.pageLimit)
                }
                .combine(gridColumnsFlow) { qts, g ->
                    SearchState(qts.query, qts.type, qts.sort, g, qts.pageLimit)
                }
                .combine(pageLimitFlow) { qtsG, p ->
                    SearchState(qtsG.query, qtsG.type, qtsG.sort, qtsG.gridColumns, p)
                }
                .collect { state ->
                    if (state.query.isBlank()) return@collect

                    val prevQuery = _uiState.value.query

                    if (prevQuery.isBlank()) return@collect

                    val queryChanged = state.query != prevQuery
                    val typeChanged = state.type != _uiState.value.type
                    val sortChanged = state.sort != _uiState.value.sort

                    _uiState.update {
                        it.copy(
                            query = state.query,
                            type = state.type,
                            sort = state.sort,
                            gridColumns = state.gridColumns,
                            isLoading = false,
                        )
                    }

                    pageSize = state.pageLimit

                    if (!isJumping && (queryChanged || typeChanged || sortChanged)) {
                        currentOffset = 0
                        currentPageStartOffset = 0

                        search(state.query, forceNew = true)
                    }
                }
        }

        viewModelScope.launch {
            preferencesRepository.searchQuery
                .flatMapLatest { query -> preferencesRepository.searchScrollIndex() }
                .collect { index -> _uiState.update { it.copy(scrollIndex = index) } }
        }

        viewModelScope.launch {
            preferencesRepository.searchQuery
                .flatMapLatest { query -> preferencesRepository.searchScrollOffset() }
                .collect { offset -> _uiState.update { it.copy(scrollOffset = offset) } }
        }

        viewModelScope.launch {
            val initialQuery = preferencesRepository.searchQuery.first()
            val initialType = preferencesRepository.searchType.first()
            val initialSort = preferencesRepository.searchSort.first()
            val initialScrollIndex = preferencesRepository.searchScrollIndex().first()
            val initialScrollOffset = preferencesRepository.searchScrollOffset().first()
            val initialOffset = preferencesRepository.searchOffset.first()
            val initialPageSize = preferencesRepository.pageLimit.first()

            _uiState.update {
                it.copy(
                    query = initialQuery,
                    type = initialType,
                    sort = initialSort,
                    scrollIndex = initialScrollIndex,
                    scrollOffset = initialScrollOffset,
                    currentOffset = initialOffset,
                )
            }

            if (initialQuery.isNotBlank()) {
                currentOffset = initialOffset

                val restoreOffset =
                    maxOf(0, initialScrollIndex - (initialScrollIndex % initialPageSize))

                restoreSearch(initialQuery, restoreOffset)
            }
        }
    }

    private fun restoreSearch(query: String, offset: Int) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            var attempt = 0
            var currentSearchOffset = offset

            while (attempt < 3) {
                try {
                    val bearerToken = preferencesRepository.searchApiKey.first()
                    val type = _uiState.value.type
                    val sort = _uiState.value.sort

                    val (results, totalHits) =
                        searchRepository.search(
                            query = _uiState.value.query,
                            type = type,
                            sort = sort,
                            limit = pageSize,
                            offset = currentSearchOffset,
                            bearerToken = bearerToken,
                        )

                    currentPageStartOffset = currentSearchOffset

                    val absoluteScrollIndex = _uiState.value.scrollIndex

                    val relativeScrollIndex =
                        (absoluteScrollIndex - currentSearchOffset).coerceIn(
                            0,
                            maxOf(0, results.size - 1),
                        )

                    val relativeScrollOffset =
                        if (relativeScrollIndex != absoluteScrollIndex) 0
                        else _uiState.value.scrollOffset

                    _uiState.update {
                        it.copy(
                            results = results,
                            totalHits = totalHits,
                            hasMore = results.size == pageSize && currentOffset < totalHits,
                            isLoading = false,
                            currentOffset = currentOffset,
                            scrollIndex = relativeScrollIndex,
                            scrollOffset = relativeScrollOffset,
                        )
                    }

                    return@launch
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e

                    attempt++

                    if (attempt < 3) {
                        currentSearchOffset++
                        kotlinx.coroutines.delay(2000L * attempt)
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message ?: "Search failed")
                        }
                    }
                }
            }
        }
    }

    fun search(query: String, forceNew: Boolean = false, startOffset: Int? = null) {
        if (query.isBlank()) {
            searchJob?.cancel()

            _uiState.update {
                it.copy(
                    results = emptyList(),
                    totalHits = 0,
                    query = "",
                    hasMore = false,
                    isRestored = false,
                    isLoading = false,
                    error = null,
                )
            }

            viewModelScope.launch {
                preferencesRepository.updateSearchQuery("")
                preferencesRepository.updateSearchOffset(0)
            }

            return
        }

        if (forceNew) {
            currentOffset = startOffset ?: 0

            _uiState.update { it.copy(results = emptyList(), hasMore = false) }
        }

        val currentQuery = _uiState.value.query
        val shouldStartNew = forceNew || currentQuery != query

        if (shouldStartNew) {
            currentOffset = startOffset ?: 0

            _uiState.update { it.copy(query = query, results = emptyList(), hasMore = false) }
            viewModelScope.launch {
                preferencesRepository.updateSearchQuery(query)

                if (startOffset == null) preferencesRepository.updateSearchOffset(0)
            }
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            var attempt = 0
            var currentSearchOffset = currentOffset

            while (attempt < 3) {
                try {
                    val bearerToken = preferencesRepository.searchApiKey.first()
                    val type = _uiState.value.type
                    val sort = _uiState.value.sort

                    val (results, totalHits) =
                        searchRepository.search(
                            query = query,
                            type = type,
                            sort = sort,
                            limit = pageSize,
                            offset = currentSearchOffset,
                            bearerToken = bearerToken,
                        )

                    if (_uiState.value.query.isBlank() || _uiState.value.query != query)
                        return@launch

                    val newResults =
                        if (currentSearchOffset == 0) results else _uiState.value.results + results

                    currentPageStartOffset = currentSearchOffset
                    currentOffset = currentSearchOffset + results.size

                    viewModelScope.launch {
                        preferencesRepository.updateSearchOffset(currentOffset)
                    }

                    _uiState.update {
                        it.copy(
                            results = newResults,
                            totalHits = totalHits,
                            hasMore = results.size == pageSize && currentOffset < totalHits,
                            isLoading = false,
                            currentOffset = currentOffset,
                        )
                    }
                    return@launch
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e

                    attempt++

                    if (attempt < 3) {
                        currentSearchOffset++
                        kotlinx.coroutines.delay(2000L * attempt)
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message ?: "Search failed")
                        }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value

        if (state.isLoading || !state.hasMore || state.query.isBlank()) return

        search(state.query, forceNew = false)
    }

    fun saveScrollPosition(type: String, index: Int, offset: Int) {
        viewModelScope.launch { preferencesRepository.updateSearchScrollPosition(index, offset) }
    }

    fun updateSearchFilters(type: String, sort: String) {
        _uiState.update { it.copy(type = type, sort = sort) }

        viewModelScope.launch { preferencesRepository.updateSearchFilters(type, sort) }

        if (_uiState.value.query.isNotBlank()) {
            currentOffset = 0
            currentPageStartOffset = 0

            search(_uiState.value.query, forceNew = true)
        }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch { preferencesRepository.updateGridColumns(columns) }
    }

    fun clearSearch() {
        searchJob?.cancel()

        currentOffset = 0

        viewModelScope.launch { preferencesRepository.resetSearch() }

        _uiState.update { SearchUiState(gridColumns = it.gridColumns) }
    }

    fun markRestored() {
        _uiState.update { it.copy(isRestored = true) }
    }

    private fun sendMessage(@androidx.annotation.StringRes resId: Int) {
        viewModelScope.launch { _uiMessage.emit(resId) }
    }

    fun saveSearchBookmark(title: String) {
        val state = _uiState.value

        viewModelScope.launch {
            bookmarkRepository.addBookmark(
                Bookmark(
                    title = title,
                    type = state.type,
                    sort = state.sort,
                    period = "",
                    nsfw = "None",
                    cursor = "",
                    tags = null,
                    query = state.query,
                    offset = currentPageStartOffset,
                )
            )

            sendMessage(R.string.msg_bookmark_saved)
        }
    }

    fun loadSearchBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            val q = bookmark.query ?: return@launch

            preferencesRepository.updateSearchQuery(q)
            preferencesRepository.updateSearchFilters(bookmark.type, bookmark.sort)
            _uiState.update { it.copy(query = q, type = bookmark.type, sort = bookmark.sort) }
            search(q, forceNew = true, startOffset = bookmark.offset ?: 0)
            sendMessage(R.string.msg_bookmark_loaded)
        }
    }

    fun jumpToOffset(targetOffset: Int) {
        if (targetOffset < 0 || targetOffset >= _uiState.value.totalHits) return

        isJumping = true
        currentOffset = targetOffset

        viewModelScope.launch { preferencesRepository.updateSearchOffset(targetOffset) }

        _uiState.update {
            it.copy(results = emptyList(), hasMore = false, currentOffset = targetOffset)
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            var attempt = 0
            var currentSearchOffset = targetOffset

            while (attempt < 3) {
                try {
                    val bearerToken = preferencesRepository.searchApiKey.first()
                    val type = _uiState.value.type
                    val sort = _uiState.value.sort

                    val (results, totalHits) =
                        searchRepository.search(
                            query = _uiState.value.query,
                            type = type,
                            sort = sort,
                            limit = pageSize,
                            offset = currentSearchOffset,
                            bearerToken = bearerToken,
                        )

                    currentPageStartOffset = currentSearchOffset
                    currentOffset = currentSearchOffset + results.size

                    _uiState.update {
                        it.copy(
                            results = results,
                            totalHits = totalHits,
                            hasMore = results.size == pageSize && currentOffset < totalHits,
                            isLoading = false,
                            currentOffset = currentOffset,
                        )
                    }

                    return@launch
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e

                    attempt++

                    if (attempt < 3) {
                        currentSearchOffset++
                        kotlinx.coroutines.delay(2000L * attempt)
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message ?: "Search failed")
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            delay(1000)

            isJumping = false
        }
    }
}
