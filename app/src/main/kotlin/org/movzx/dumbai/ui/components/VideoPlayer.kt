package org.movzx.dumbai.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun VideoPlayer(
    url: String,
    isPlaying: Boolean = true,
    scaleMode: ScaleMode = ScaleMode.NORMAL,
    modifier: Modifier = Modifier,
) {
    MpvVideoPlayer(url = url, isPlaying = isPlaying, scaleMode = scaleMode, modifier = modifier)
}
