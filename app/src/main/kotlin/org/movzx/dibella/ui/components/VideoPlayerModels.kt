package org.movzx.dibella.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

enum class ScaleMode {
    NORMAL,
    CROP,
    FULL,
}

data class BackendConfig(val url: String = "", val apiKey: String = "")

val LocalBackendConfig = staticCompositionLocalOf { BackendConfig() }
