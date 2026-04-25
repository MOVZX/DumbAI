package org.movzx.dumbai.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

            repository.downloadPath.collect { path ->
                _uiState.update { it.copy(downloadPath = path) }
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

            _uiState.update { it.copy(images = images, isLoading = false) }
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

    fun markRestored() {
        _uiState.update { it.copy(isRestored = true) }
    }
}
