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
            combine(
                    favoritesRepository.allFavorites,
                    favoritesRepository.favoriteIds,
                    repository.gridColumns,
                ) { favorites, ids, columns ->
                    Triple(favorites, ids, columns)
                }
                .collect { (favorites, ids, columns) ->
                    _uiState.update {
                        it.copy(images = favorites, favoriteIds = ids, gridColumns = columns)
                    }
                }
        }

        viewModelScope.launch {
            repository.feedScrollIndex("favorites").collect { index ->
                _uiState.update { it.copy(scrollIndex = index) }
            }
        }

        viewModelScope.launch {
            repository.feedScrollOffset("favorites").collect { offset ->
                _uiState.update { it.copy(scrollOffset = offset) }
            }
        }
    }

    fun getFavoriteFlow(id: Long): Flow<FavoriteImage?> = favoritesRepository.getFavoriteFlow(id)

    suspend fun ensureFavoriteResources(image: CivitaiImage) {
        favoritesRepository.ensureFavoriteResources(image)
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
}
