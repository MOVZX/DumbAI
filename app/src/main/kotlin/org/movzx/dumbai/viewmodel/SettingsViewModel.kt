package org.movzx.dumbai.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.movzx.dumbai.R
import org.movzx.dumbai.data.BackupRepository
import org.movzx.dumbai.data.FavoritesRepository
import org.movzx.dumbai.data.GalleryRepository
import org.movzx.dumbai.data.UserPreferencesRepository
import org.movzx.dumbai.util.Logger

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    repository: UserPreferencesRepository,
    favoritesRepository: FavoritesRepository,
    galleryRepository: GalleryRepository,
    private val backupRepository: BackupRepository,
    private val imageLoader: ImageLoader,
) : BaseViewModel(repository, favoritesRepository, galleryRepository) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
    private val _exitEvent = MutableSharedFlow<Unit>()
    val exitEvent = _exitEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(repository.apiKey, repository.downloadPath, repository.debugEnabled) {
                    apiKey,
                    downloadPath,
                    debugEnabled ->
                    Triple(apiKey, downloadPath, debugEnabled)
                }
                .onEach { (apiKey, downloadPath, debugEnabled) ->
                    _uiState.update { state ->
                        state.copy(
                            apiKey = apiKey,
                            downloadPath = downloadPath ?: "",
                            debugEnabled = debugEnabled,
                        )
                    }
                    Logger.debugEnabled = debugEnabled

                    updateCacheSize()
                }
                .collect()
        }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            repository.updateApiKey(key)
            sendMessage(R.string.msg_api_key_saved)
        }
    }

    fun updateDownloadPath(path: String?) {
        viewModelScope.launch { repository.updateDownloadPath(path) }
    }

    fun updateDebugEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateDebugEnabled(enabled) }
    }

    fun clearImageCache() {
        viewModelScope.launch {
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            updateCacheSize()
            sendMessage(R.string.msg_cache_cleared)
        }
    }

    fun updateCacheSize() {
        val size = imageLoader.diskCache?.size ?: 0L

        _uiState.update { it.copy(cacheSize = formatSize(size)) }
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            else -> String.format("%.2f KB", kb)
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val success = backupRepository.exportData(uri)

            if (success) sendMessage(R.string.msg_export_success)
            else sendMessage(R.string.msg_export_failed)
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            val success = backupRepository.importData(uri)

            if (success) {
                sendMessage(R.string.msg_import_success)
                _exitEvent.emit(Unit)
            } else {
                sendMessage(R.string.msg_import_failed)
            }
        }
    }
}
