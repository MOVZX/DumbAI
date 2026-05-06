package org.movzx.dibella.viewmodel

data class SettingsUiState(
    val lastRoute: String? = null,
    val cacheSize: String = "0 MB",
    val apiKey: String = "",
    val downloadPath: String? = null,
    val favoritesPath: String? = null,
    val effectiveFavoritesPath: String = "",
    val debugEnabled: Boolean = false,
    val backendEnabled: Boolean = false,
    val backendUrl: String = "",
    val backendApiKey: String = "",
    val hidePlayerControls: Boolean = false,
    val alwaysEnableHD: Boolean = false,
    val alwaysMuteVideo: Boolean = false,
    val feedVideoAutoplay: Boolean = false,
    val amoledMode: Boolean = false,
)
