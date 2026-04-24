package org.movzx.dumbai.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.movzx.dumbai.R
import org.movzx.dumbai.data.FavoritesRepository
import org.movzx.dumbai.data.GalleryRepository
import org.movzx.dumbai.data.UserPreferencesRepository
import org.movzx.dumbai.model.CivitaiImage

abstract class BaseViewModel(
    protected val repository: UserPreferencesRepository,
    protected val favoritesRepository: FavoritesRepository,
    protected val galleryRepository: GalleryRepository,
) : ViewModel() {
    private val _uiMessage = MutableSharedFlow<Int>()
    val uiMessage = _uiMessage.asSharedFlow()

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch { repository.updateGridColumns(columns) }
    }

    fun updatePageLimit(limit: Int) {
        viewModelScope.launch { repository.updatePageLimit(limit) }
    }

    fun saveScrollPosition(type: String, index: Int, offset: Int) {
        viewModelScope.launch { repository.updateScrollPosition(type, index, offset) }
    }

    protected fun sendMessage(@StringRes resId: Int) {
        viewModelScope.launch { _uiMessage.emit(resId) }
    }

    fun toggleFavorite(image: CivitaiImage) {
        viewModelScope.launch { favoritesRepository.toggleFavorite(image) }
    }

    protected fun performDownload(
        image: CivitaiImage,
        currentProgresses: Map<Long, Float>,
        onUpdateProgress: (Map<Long, Float>) -> Unit,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            onUpdateProgress(currentProgresses + (image.id to 0f))

            val result =
                galleryRepository.downloadImage(image) { progress ->
                    onUpdateProgress(currentProgresses + (image.id to progress))
                }

            if (result.isSuccess) {
                sendMessage(R.string.msg_saved_to)
                onSuccess()
            } else {
                sendMessage(R.string.msg_download_failed)
            }

            onUpdateProgress(currentProgresses - image.id)
        }
    }
}
