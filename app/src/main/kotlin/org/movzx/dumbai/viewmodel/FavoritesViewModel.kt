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
import org.movzx.dumbai.model.FavoriteImage

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

    fun getFavoriteFlow(id: Long): Flow<FavoriteImage?> = favoritesRepository.getFavoriteFlow(id)

    suspend fun ensureFavoriteResources(
        image: CivitaiImage,
        force: Boolean = false,
        onProgress: (Float) -> Unit = {},
    ) {
        favoritesRepository.ensureFavoriteResources(image, force, onProgress)
    }

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

    fun markRestored() {
        _uiState.update { it.copy(isRestored = true) }
    }
}
