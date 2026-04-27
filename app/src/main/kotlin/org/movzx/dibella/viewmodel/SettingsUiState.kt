package org.movzx.dibella.viewmodel

data class SettingsUiState(
    val cacheSize: String = "0 MB",
    val apiKey: String = "",
    val downloadPath: String? = null,
    val debugEnabled: Boolean = false,
)
