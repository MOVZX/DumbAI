package org.movzx.dibella.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.movzx.dibella.R
import org.movzx.dibella.data.FavoritesRepository
import org.movzx.dibella.data.GalleryRepository
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

@HiltViewModel
class FavoritesViewModel
@Inject
constructor(
    repository: UserPreferencesRepository,
    favoritesRepository: FavoritesRepository,
    galleryRepository: GalleryRepository,
) : BaseViewModel(repository, favoritesRepository, galleryRepository) {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initialIndex = repository.feedScrollIndex("favorites").first()
            val initialOffset = repository.feedScrollOffset("favorites").first()

            _uiState.update { it.copy(scrollIndex = initialIndex, scrollOffset = initialOffset) }

            combine(
                    favoritesRepository.allFavorites,
                    favoritesRepository.favoriteIds,
                    repository.gridColumns,
                    repository.favoritesType,
                ) { favorites, ids, columns, type ->
                    val filtered =
                        when (type) {
                            "image" -> favorites.filter { it.type == "image" }
                            "video" -> favorites.filter { it.type == "video" }
                            else -> favorites
                        }

                    _uiState.update {
                        it.copy(
                            images = filtered,
                            favoriteIds = ids,
                            gridColumns = columns,
                            type = type,
                        )
                    }
                }
                .collect {}
        }
    }

    fun updateType(type: String) {
        viewModelScope.launch { repository.updateFavoritesType(type) }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSelected =
                if (state.selectedIds.contains(id)) state.selectedIds - id
                else state.selectedIds + id

            state.copy(selectedIds = newSelected, isSelectionMode = newSelected.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.images.map { it.id }.toSet()
            val newSelected = if (state.selectedIds.size == allIds.size) emptySet() else allIds

            state.copy(selectedIds = newSelected, isSelectionMode = newSelected.isNotEmpty())
        }
    }

    fun batchUnfavorite() {
        viewModelScope.launch {
            val idsToUnfavorite = _uiState.value.selectedIds

            idsToUnfavorite.forEach { id ->
                val image = _uiState.value.images.find { it.id == id }

                if (image != null) favoritesRepository.toggleFavorite(image)
            }

            clearSelection()
        }
    }

    fun getFavoriteFlow(id: Long): Flow<FavoriteImage?> = favoritesRepository.getFavoriteFlow(id)

    fun forceRedownload(image: CivitaiImage) {
        viewModelScope.launch {
            val currentProgresses = _uiState.value.downloadProgresses

            _uiState.update { it.copy(downloadProgresses = currentProgresses + (image.id to 0f)) }

            favoritesRepository.ensureFavoriteResources(image, force = true) { progress ->
                _uiState.update {
                    it.copy(downloadProgresses = it.downloadProgresses + (image.id to progress))
                }
            }

            _uiState.update { it.copy(downloadProgresses = it.downloadProgresses - image.id) }
            sendMessage(R.string.msg_cache_cleared)
        }
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

    fun findDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val groups = favoritesRepository.findDuplicateGroups()

                if (groups.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            duplicateGroups = groups,
                            isShowingDuplicates = true,
                            isLoading = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    sendMessage(R.string.msg_no_duplicates_found)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                sendMessage(R.string.msg_load_failed)
            }
        }
    }

    fun clearDuplicatesMode() {
        _uiState.update { it.copy(isShowingDuplicates = false, duplicateGroups = emptyList()) }
    }

    fun removeDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val count = favoritesRepository.removeDuplicates(_uiState.value.duplicateGroups)

                _uiState.update {
                    it.copy(
                        isShowingDuplicates = false,
                        duplicateGroups = emptyList(),
                        isLoading = false,
                    )
                }

                if (count > 0) sendMessage(R.string.msg_duplicates_removed)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isShowingDuplicates = false,
                        duplicateGroups = emptyList(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun markRestored() {
        _uiState.update { it.copy(isRestored = true) }
    }
}
