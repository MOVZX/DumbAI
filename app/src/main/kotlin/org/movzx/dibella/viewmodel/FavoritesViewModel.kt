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
import org.movzx.dibella.util.Logger

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

            Logger.d(
                "Dibella_Cache",
                "Favorites: Restoring scroll pos ($initialIndex, $initialOffset)",
            )

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

                    Logger.d(
                        "Dibella_DB",
                        "Favorites updated: ${filtered.size} items (Type: $type)",
                    )

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
        viewModelScope.launch {
            Logger.d("Dibella_Cache", "Updating Favorites Filter Type: $type")

            repository.updateFavoritesType(type)
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSelected =
                if (state.selectedIds.contains(id)) state.selectedIds - id
                else state.selectedIds + id

            Logger.v(
                "Dibella_Cache",
                "Selection toggled: $id | Total selected: ${newSelected.size}",
            )

            state.copy(selectedIds = newSelected, isSelectionMode = newSelected.isNotEmpty())
        }
    }

    fun clearSelection() {
        Logger.d("Dibella_Cache", "Selection cleared")

        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.images.map { it.id }.toSet()
            val newSelected = if (state.selectedIds.size == allIds.size) emptySet() else allIds

            Logger.d("Dibella_Cache", "Select All: ${newSelected.size} items selected")

            state.copy(selectedIds = newSelected, isSelectionMode = newSelected.isNotEmpty())
        }
    }

    fun batchUnfavorite() {
        viewModelScope.launch {
            val idsToUnfavorite = _uiState.value.selectedIds

            Logger.d("Dibella_DB", "Batch Unfavorite Start: ${idsToUnfavorite.size} items")

            for (id in idsToUnfavorite) {
                val image = _uiState.value.images.find { it.id == id }

                if (image != null) favoritesRepository.toggleFavorite(image)
            }

            clearSelection()
        }
    }

    fun getFavoriteFlow(id: Long): Flow<FavoriteImage?> = favoritesRepository.getFavoriteFlow(id)

    fun forceRedownload(image: CivitaiImage) {
        viewModelScope.launch {
            Logger.d("Dibella_IO", "[${image.id}] Triggering Force Redownload")

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

    override fun downloadImage(image: CivitaiImage) {
        Logger.d("Dibella_Cache", "[${image.id}] UI initiated download")

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
            Logger.d("Dibella_IO", "Duplicate search initiated")

            _uiState.update { it.copy(isLoading = true) }

            try {
                val groups = favoritesRepository.findDuplicateGroups()

                if (groups.isNotEmpty()) {
                    Logger.d("Dibella_IO", "Duplicates found: ${groups.size} groups")

                    _uiState.update {
                        it.copy(
                            duplicateGroups = groups,
                            isShowingDuplicates = true,
                            isLoading = false,
                        )
                    }
                } else {
                    Logger.d("Dibella_IO", "No duplicates identified")

                    _uiState.update { it.copy(isLoading = false) }
                    sendMessage(R.string.msg_no_duplicates_found)
                }
            } catch (e: Exception) {
                Logger.e("Dibella_IO", "Duplicate scan failed: ${e.message}")

                _uiState.update { it.copy(isLoading = false) }
                sendMessage(R.string.msg_load_failed)
            }
        }
    }

    fun clearDuplicatesMode() {
        Logger.d("Dibella_Cache", "Exiting duplicate view mode")

        _uiState.update { it.copy(isShowingDuplicates = false, duplicateGroups = emptyList()) }
    }

    fun removeDuplicates() {
        viewModelScope.launch {
            Logger.d("Dibella_IO", "Removing identified duplicates...")

            _uiState.update { it.copy(isLoading = true) }

            try {
                val count = favoritesRepository.removeDuplicates(_uiState.value.duplicateGroups)

                Logger.d("Dibella_IO", "Duplicate removal complete: $count items purged")

                _uiState.update {
                    it.copy(
                        isShowingDuplicates = false,
                        duplicateGroups = emptyList(),
                        isLoading = false,
                    )
                }

                if (count > 0) sendMessage(R.string.msg_duplicates_removed)
            } catch (e: Exception) {
                Logger.e("Dibella_IO", "Error during duplicate removal: ${e.message}")

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
