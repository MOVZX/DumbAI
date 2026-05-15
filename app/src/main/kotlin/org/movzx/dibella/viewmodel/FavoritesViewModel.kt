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
                "Favorites: Pre-loading scroll pos ($initialIndex, $initialOffset)",
            )

            combine(
                    favoritesRepository.allFavorites,
                    favoritesRepository.favoriteIds,
                    repository.gridColumns,
                    repository.favoritesType,
                    repository.effectiveFavoritesPath,
                    repository.showNsfwFavorites,
                ) { values ->
                    @Suppress("UNCHECKED_CAST") val favorites = values[0] as List<CivitaiImage>
                    @Suppress("UNCHECKED_CAST") val ids = values[1] as Set<Long>
                    val columns = values[2] as Int
                    val type = values[3] as String
                    val path = values[4] as String?
                    val showNsfw = values[5] as Boolean

                    val filtered = favorites.filter { image ->
                        val isNsfw = image.nsfw == true || image.nsfw == null

                        (type == "image" && image.type == "image" ||
                            type == "video" && image.type == "video" ||
                            type == "all") && (showNsfw || !isNsfw)
                    }

                    Logger.d(
                        "Dibella_DB",
                        "Favorites updated: ${filtered.size} items (Type: $type, NSFW: $showNsfw)",
                    )

                    _uiState.update {
                        it.copy(
                            images = filtered,
                            favoriteIds = ids,
                            gridColumns = columns,
                            type = type,
                            favoritesPath = path,
                            scrollIndex = if (!it.isRestored) initialIndex else it.scrollIndex,
                            scrollOffset = if (!it.isRestored) initialOffset else it.scrollOffset,
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
            val imagesSnapshot = _uiState.value.images

            Logger.d("Dibella_DB", "Batch Unfavorite Start: ${idsToUnfavorite.size} items")

            var successCount = 0
            var failCount = 0

            for (id in idsToUnfavorite) {
                val image = imagesSnapshot.find { it.id == id }

                if (image != null) {
                    try {
                        favoritesRepository.toggleFavorite(image)
                        successCount++
                    } catch (e: Exception) {
                        failCount++

                        Logger.e("Dibella_DB", "Batch Unfavorite failed for $id: ${e.message}")
                    }
                }
            }

            Logger.d("Dibella_DB", "Batch Unfavorite: $successCount succeeded, $failCount failed")

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
            onUpdateProgress = { id, progress ->
                _uiState.update { state ->
                    val newProgresses = state.downloadProgresses.toMutableMap()

                    if (progress != null) newProgresses[id] = progress else newProgresses.remove(id)

                    state.copy(downloadProgresses = newProgresses)
                }
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

    fun refresh() {
        viewModelScope.launch {
            Logger.d("Dibella_DB", "Favorites refresh triggered")

            _uiState.update { it.copy(isRefreshing = true) }
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun markRestored() {
        _uiState.update { it.copy(isRestored = true) }
    }
}
