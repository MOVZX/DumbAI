package org.movzx.dumbai.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun VideoPlayer(url: String, isPlaying: Boolean = true, modifier: Modifier = Modifier) {
    MpvVideoPlayer(url = url, isPlaying = isPlaying, modifier = modifier)
}
