package org.movzx.dumbai.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    isPlaying: Boolean = true,
    isMuted: Boolean = true,
    scaleMode: ScaleMode = ScaleMode.NORMAL,
    onProgressUpdate: (Long, Long) -> Unit = { _, _ -> },
    onAudioStateChange: (Boolean) -> Unit = {},
    onPlaybackError: (String?) -> Unit = {},
    seekPosition: Long? = null,
    onSeekConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }

    DisposableEffect(exoPlayer) {
        val listener =
            object : Player.Listener {
                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    val hasAudio =
                        tracks.groups.any {
                            it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && it.isSupported
                        }

                    onAudioStateChange(hasAudio)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    onPlaybackError(error.message)
                }
            }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) { exoPlayer.playWhenReady = isPlaying }

    LaunchedEffect(isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }

    LaunchedEffect(seekPosition) {
        seekPosition?.let {
            exoPlayer.seekTo(it)
            onSeekConsumed()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)
                kotlinx.coroutines.delay(33)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
            else if (event == Lifecycle.Event.ON_RESUME) if (isPlaying) exoPlayer.play()
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                layoutParams =
                    android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        },
        update = { playerView ->
            playerView.resizeMode =
                when (scaleMode) {
                    ScaleMode.NORMAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    ScaleMode.FULL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
        },
        modifier = modifier.fillMaxSize(),
    )
}
