package org.movzx.dumbai.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.movzx.dumbai.R
import org.movzx.dumbai.data.FavoritesRepository
import org.movzx.dumbai.data.GalleryRepository
import org.movzx.dumbai.data.UserPreferencesRepository
import org.movzx.dumbai.model.CivitaiImage

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

            combine(repository.downloadPath, repository.galleryType, repository.gridColumns) {
                    path,
                    type,
                    columns ->
                    Triple(path, type, columns)
                }
                .collect { (path, type, columns) ->
                    _uiState.update {
                        it.copy(downloadPath = path, type = type, gridColumns = columns)
                    }
                    refresh()
                }
        }

        viewModelScope.launch {
            galleryRepository.downloadedIds.collect { ids ->
                _uiState.update { it.copy(downloadedIds = ids) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val images = galleryRepository.scanDirectory(_uiState.value.downloadPath)

            val filtered =
                when (_uiState.value.type) {
                    "image" -> images.filter { it.type == "image" }
                    "video" -> images.filter { it.type == "video" }
                    else -> images
                }

            _uiState.update { it.copy(images = filtered, isLoading = false) }
        }
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

            idsToDelete.forEach { id ->
                val image = _uiState.value.images.find { it.id == id }

                if (image != null) galleryRepository.deleteLocalFile(image)
            }

            clearSelection()
            refresh()
        }
    }

    fun deleteLocalFile(image: CivitaiImage) {
        viewModelScope.launch { if (galleryRepository.deleteLocalFile(image)) refresh() }
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
            onSuccess = { refresh() },
        )
    }

    fun findDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val groups = galleryRepository.findDuplicateGroups()

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
                val count = galleryRepository.removeDuplicates(_uiState.value.duplicateGroups)

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
