package org.movzx.dibella.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.movzx.dibella.R
import org.movzx.dibella.api.CivitaiInterceptor
import org.movzx.dibella.data.BackupRepository
import org.movzx.dibella.data.FavoritesRepository
import org.movzx.dibella.data.GalleryRepository
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.util.Logger

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    repository: UserPreferencesRepository,
    favoritesRepository: FavoritesRepository,
    galleryRepository: GalleryRepository,
    private val backupRepository: BackupRepository,
    private val imageLoader: ImageLoader,
    private val civitaiInterceptor: CivitaiInterceptor,
) : BaseViewModel(repository, favoritesRepository, galleryRepository) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
    private val _exitEvent = MutableSharedFlow<Unit>()
    val exitEvent = _exitEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            combine<Any?, SettingsUiState>(
                    listOf(
                        repository.apiKey,
                        repository.searchApiKey,
                        repository.downloadPath,
                        repository.favoritesPath,
                        repository.effectiveFavoritesPath,
                        repository.debugEnabled,
                        repository.backendEnabled,
                        repository.backendUrl,
                        repository.backendApiKey,
                        repository.lastRoute,
                    )
                ) { values ->
                    @Suppress("UNCHECKED_CAST") val apiKey = values[0] as String
                    @Suppress("UNCHECKED_CAST") val searchApiKey = values[1] as String
                    @Suppress("UNCHECKED_CAST") val downloadPath = values[2] as? String
                    @Suppress("UNCHECKED_CAST") val favoritesPath = values[3] as? String
                    @Suppress("UNCHECKED_CAST") val effectiveFavoritesPath = values[4] as String
                    @Suppress("UNCHECKED_CAST") val debugEnabled = values[5] as Boolean
                    @Suppress("UNCHECKED_CAST") val backendEnabled = values[6] as Boolean
                    @Suppress("UNCHECKED_CAST") val backendUrl = values[7] as String
                    @Suppress("UNCHECKED_CAST") val backendApiKey = values[8] as String
                    @Suppress("UNCHECKED_CAST") val lastRoute = values[9] as? String

                    SettingsUiState(
                        apiKey = apiKey,
                        searchApiKey = searchApiKey,
                        downloadPath = downloadPath ?: "",
                        favoritesPath = favoritesPath,
                        effectiveFavoritesPath = effectiveFavoritesPath,
                        debugEnabled = debugEnabled,
                        backendEnabled = backendEnabled,
                        backendUrl = backendUrl,
                        backendApiKey = backendApiKey,
                        lastRoute = lastRoute,
                    )
                }
                .onEach { newState ->
                    _uiState.update { state ->
                        state.copy(
                            apiKey = newState.apiKey,
                            searchApiKey = newState.searchApiKey,
                            downloadPath = newState.downloadPath,
                            favoritesPath = newState.favoritesPath,
                            effectiveFavoritesPath = newState.effectiveFavoritesPath,
                            debugEnabled = newState.debugEnabled,
                            backendEnabled = newState.backendEnabled,
                            backendUrl = newState.backendUrl,
                            backendApiKey = newState.backendApiKey,
                            lastRoute = newState.lastRoute,
                        )
                    }

                    Logger.debugEnabled = newState.debugEnabled

                    updateCacheSize()
                }
                .collect()
        }

        viewModelScope.launch {
            repository.hidePlayerControls.collect { hidePlayerControls ->
                _uiState.update { it.copy(hidePlayerControls = hidePlayerControls) }
            }
        }

        viewModelScope.launch {
            repository.alwaysEnableHD.collect { alwaysEnableHD ->
                _uiState.update { it.copy(alwaysEnableHD = alwaysEnableHD) }
            }
        }

        viewModelScope.launch {
            repository.alwaysMuteVideo.collect { alwaysMuteVideo ->
                _uiState.update { it.copy(alwaysMuteVideo = alwaysMuteVideo) }
            }
        }

        viewModelScope.launch {
            repository.feedVideoAutoplay.collect { feedVideoAutoplay ->
                _uiState.update { it.copy(feedVideoAutoplay = feedVideoAutoplay) }
            }
        }

        viewModelScope.launch {
            repository.amoledMode.collect { amoledMode ->
                _uiState.update { it.copy(amoledMode = amoledMode) }
            }
        }
    }

    override fun downloadImage(image: org.movzx.dibella.model.CivitaiImage) {}

    fun updateLastRoute(route: String) {
        viewModelScope.launch { repository.updateLastRoute(route) }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            repository.updateApiKey(key)
            sendMessage(R.string.msg_api_key_saved)
        }
    }

    fun updateSearchApiKey(token: String) {
        viewModelScope.launch {
            repository.updateSearchApiKey(token)
            sendMessage(R.string.msg_bearer_token_saved)
        }
    }

    fun updateDownloadPath(path: String?) {
        viewModelScope.launch { repository.updateDownloadPath(path) }
    }

    fun updateFavoritesPath(path: String?) {
        viewModelScope.launch { repository.updateFavoritesPath(path) }
    }

    fun updateDebugEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateDebugEnabled(enabled) }
    }

    fun updateBackendEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateBackendEnabled(enabled) }
    }

    fun updateBackendUrl(url: String) {
        viewModelScope.launch { repository.updateBackendUrl(url) }
    }

    fun updateBackendApiKey(key: String) {
        viewModelScope.launch { repository.updateBackendApiKey(key) }
    }

    fun updateHidePlayerControls(enabled: Boolean) {
        viewModelScope.launch { repository.updateHidePlayerControls(enabled) }
    }

    fun updateAlwaysEnableHD(enabled: Boolean) {
        viewModelScope.launch { repository.updateAlwaysEnableHD(enabled) }
    }

    fun updateAlwaysMuteVideo(enabled: Boolean) {
        viewModelScope.launch { repository.updateAlwaysMuteVideo(enabled) }
    }

    fun updateFeedVideoAutoplay(enabled: Boolean) {
        viewModelScope.launch { repository.updateFeedVideoAutoplay(enabled) }
    }

    fun updateAmoledMode(enabled: Boolean) {
        viewModelScope.launch { repository.updateAmoledMode(enabled) }
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
            gb >= 1.0 -> String.format(java.util.Locale.ROOT, "%.2f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.ROOT, "%.2f MB", mb)
            else -> String.format(java.util.Locale.ROOT, "%.2f KB", kb)
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
            BaseViewModel.isImporting = true
            val success = backupRepository.importData(uri)

            if (success) {
                BaseViewModel.isImporting = false

                sendMessage(R.string.msg_import_success)
                _exitEvent.emit(Unit)
            } else {
                BaseViewModel.isImporting = false

                sendMessage(R.string.msg_import_failed)
            }
        }
    }
}
