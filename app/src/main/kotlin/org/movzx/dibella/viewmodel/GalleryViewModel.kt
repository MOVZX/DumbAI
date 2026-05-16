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
import org.movzx.dibella.util.Logger

@HiltViewModel
class GalleryViewModel
@Inject
constructor(
    repository: UserPreferencesRepository,
    favoritesRepository: FavoritesRepository,
    galleryRepository: GalleryRepository,
) : BaseViewModel(repository, favoritesRepository, galleryRepository) {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initialIndex = repository.feedScrollIndex("gallery").first()
            val initialOffset = repository.feedScrollOffset("gallery").first()

            _uiState.update { it.copy(scrollIndex = initialIndex, scrollOffset = initialOffset) }

            combine(repository.downloadPath, repository.galleryType) { path, type ->
                    Pair(path, type)
                }
                .collectLatest { (path, type) ->
                    _uiState.update { it.copy(downloadPath = path, type = type) }
                    performRefresh()
                }
        }

        viewModelScope.launch {
            repository.gridColumns.collect { columns ->
                _uiState.update { it.copy(gridColumns = columns) }
            }
        }

        viewModelScope.launch {
            galleryRepository.downloadedIds.collect { ids ->
                _uiState.update { it.copy(downloadedIds = ids) }
            }
        }
    }

    override fun setActiveRoute(route: String?) {
        val wasInactive = activeRoute != "gallery"

        super.setActiveRoute(route)

        if (route == "gallery" && wasInactive) refresh()
    }

    fun refresh() {
        viewModelScope.launch { performRefresh() }
    }

    private suspend fun performRefresh() {
        if (activeRoute != null && activeRoute != "gallery") return

        _uiState.update { it.copy(isLoading = true, isRefreshing = true) }

        val images = galleryRepository.scanDirectory(_uiState.value.downloadPath)

        val filtered =
            when (_uiState.value.type) {
                "image" -> images.filter { it.type == "image" }
                "video" -> images.filter { it.type == "video" }
                else -> images
            }

        _uiState.update { it.copy(images = filtered, isLoading = false, isRefreshing = false) }
    }

    fun updateType(type: String) {
        viewModelScope.launch { repository.updateGalleryType(type) }
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

    fun batchDelete() {
        viewModelScope.launch {
            val idsToDelete = _uiState.value.selectedIds
            val imagesSnapshot = _uiState.value.images

            Logger.d("Dibella_IO", "Batch Delete: Initializing for ${idsToDelete.size} items")

            var successCount = 0
            var failCount = 0

            for (id in idsToDelete) {
                val image = imagesSnapshot.find { it.id == id }

                if (image != null)
                    if (galleryRepository.deleteLocalFile(image)) successCount++ else failCount++
            }

            Logger.d("Dibella_IO", "Batch Delete: $successCount succeeded, $failCount failed")

            clearSelection()
            refresh()
        }
    }

    fun deleteLocalFile(image: CivitaiImage) {
        viewModelScope.launch {
            Logger.d("Dibella_IO", "[${image.id}] UI requested file deletion")

            if (galleryRepository.deleteLocalFile(image)) refresh()
        }
    }

    override fun downloadImage(image: CivitaiImage) {
        Logger.d("Dibella_Cache", "[${image.id}] UI requested gallery download")

        performDownload(
            image = image,
            onUpdateProgress = { id, progress ->
                _uiState.update { state ->
                    val newProgresses = state.downloadProgresses.toMutableMap()

                    if (progress != null) newProgresses[id] = progress else newProgresses.remove(id)

                    state.copy(downloadProgresses = newProgresses)
                }
            },
            onSuccess = { refresh() },
        )
    }

    fun findDuplicates() {
        viewModelScope.launch {
            Logger.d("Dibella_IO", "Gallery duplicate search started")

            _uiState.update { it.copy(isLoading = true) }

            try {
                val groups = galleryRepository.findDuplicateGroups()

                if (groups.isNotEmpty()) {
                    Logger.d("Dibella_IO", "Gallery duplicates found: ${groups.size} groups")

                    _uiState.update {
                        it.copy(
                            duplicateGroups = groups,
                            isShowingDuplicates = true,
                            isLoading = false,
                        )
                    }
                } else {
                    Logger.d("Dibella_IO", "No gallery duplicates found")

                    _uiState.update { it.copy(isLoading = false) }
                    sendMessage(R.string.msg_no_duplicates_found)
                }
            } catch (e: Exception) {
                Logger.e("Dibella_IO", "Gallery duplicate scan failed: ${e.message}")

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
                val count = galleryRepository.removeDuplicates(_uiState.value.duplicateGroups)

                Logger.d("Dibella_IO", "Gallery duplicate removal complete: $count files purged")

                _uiState.update {
                    it.copy(
                        isShowingDuplicates = false,
                        duplicateGroups = emptyList(),
                        isLoading = false,
                    )
                }

                if (count > 0) {
                    refresh()
                    sendMessage(R.string.msg_duplicates_removed)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Logger.e("Dibella_IO", "Gallery duplicate removal error: ${e.message}")

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
