package org.movzx.dumbai.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.movzx.dumbai.R
import org.movzx.dumbai.data.BackupRepository
import org.movzx.dumbai.data.FavoritesRepository
import org.movzx.dumbai.data.GalleryRepository
import org.movzx.dumbai.data.UserPreferencesRepository

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    repository: UserPreferencesRepository,
    favoritesRepository: FavoritesRepository,
    galleryRepository: GalleryRepository,
    private val backupRepository: BackupRepository,
) : BaseViewModel(repository, favoritesRepository, galleryRepository) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val _exitEvent = MutableSharedFlow<Unit>()
    val exitEvent = _exitEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(repository.apiKey, repository.downloadPath, repository.debugEnabled) {
                    apiKey,
                    downloadPath,
                    debugEnabled ->
                    _uiState.update {
                        it.copy(
                            apiKey = apiKey,
                            downloadPath = downloadPath,
                            debugEnabled = debugEnabled,
                        )
                    }

                    favoritesRepository.debugEnabled = debugEnabled

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
        viewModelScope.launch {
            repository.updateDownloadPath(path)
            sendMessage(R.string.msg_download_path_updated)
        }
    }

    fun updateDebugEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateDebugEnabled(enabled) }
    }

    fun clearImageCache() {
        viewModelScope.launch {
            val coilCache = coil3.SingletonImageLoader.get(context).diskCache

            coilCache?.clear()
            updateCacheSize()
            sendMessage(R.string.msg_cache_cleared)
        }
    }

    private fun updateCacheSize() {
        val coilCache = coil3.SingletonImageLoader.get(context).diskCache
        val size = coilCache?.size ?: 0L

        val sizeStr =
            if (size > 1024 * 1024 * 1024)
                String.format("%.2f GB", size.toDouble() / (1024 * 1024 * 1024))
            else String.format("%.2f MB", size.toDouble() / (1024 * 1024))

        _uiState.update { it.copy(cacheSize = sizeStr) }
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
