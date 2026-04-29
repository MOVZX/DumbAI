package org.movzx.dibella.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    isPlaying: Boolean = true,
    isMuted: Boolean = true,
    playbackSpeed: Float = 1.0f,
    scaleMode: ScaleMode = ScaleMode.NORMAL,
    onProgressUpdate: (Long, Long) -> Unit = { _, _ -> },
    onFpsUpdate: (Int) -> Unit = {},
    onPlayerTypeUpdate: (String) -> Unit = {},
    onAudioStateChange: (Boolean) -> Unit = {},
    onPlaybackError: (String?) -> Unit = {},
    onZoomChange: (Boolean) -> Unit = {},
    onTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    seekPosition: Long? = null,
    onSeekConsumed: () -> Unit = {},
    usePool: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var useMpv by remember(url) { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(useMpv) { onPlayerTypeUpdate(if (useMpv) "MPV" else "ExoPlayer") }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress() },
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 3f
                            }

                            onZoomChange(scale > 1f)
                        },
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pointers = event.changes

                            if (pointers.size > 1 || scale > 1f) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                if (zoom != 1f || pan != Offset.Zero) {
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    offset = if (scale > 1f) offset + pan else Offset.Zero

                                    onZoomChange(scale > 1f)
                                }

                                pointers.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (pointers.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
    ) {
        if (useMpv) {
            MpvPlayer(
                url = url,
                isPlaying = isPlaying,
                isMuted = isMuted,
                playbackSpeed = playbackSpeed,
                onProgressUpdate = onProgressUpdate,
                onFpsUpdate = onFpsUpdate,
                onAudioStateChange = onAudioStateChange,
                seekPosition = seekPosition,
                onSeekConsumed = onSeekConsumed,
            )
        } else {
            ExoVideoPlayer(
                url = url,
                isPlaying = isPlaying,
                isMuted = isMuted,
                playbackSpeed = playbackSpeed,
                scaleMode = scaleMode,
                onProgressUpdate = onProgressUpdate,
                onFpsUpdate = onFpsUpdate,
                onAudioStateChange = onAudioStateChange,
                onPlaybackError = onPlaybackError,
                onSwitchToMpv = { useMpv = true },
                seekPosition = seekPosition,
                onSeekConsumed = onSeekConsumed,
                usePool = usePool,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoVideoPlayer(
    url: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    scaleMode: ScaleMode,
    onProgressUpdate: (Long, Long) -> Unit,
    onFpsUpdate: (Int) -> Unit,
    onAudioStateChange: (Boolean) -> Unit,
    onPlaybackError: (String?) -> Unit,
    onSwitchToMpv: () -> Unit,
    seekPosition: Long?,
    onSeekConsumed: () -> Unit,
    usePool: Boolean,
) {
    val context = LocalContext.current
    val manager = LocalVideoPlayerManager.current
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    val dedicatedPlayer =
        remember(usePool) {
            if (!usePool) {
                val renderersFactory =
                    DefaultRenderersFactory(context).apply {
                        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        setMediaCodecSelector {
                            mimeType,
                            requiresSecureDecoder,
                            requiresTunnelingDecoder ->
                            val decoders =
                                MediaCodecSelector.DEFAULT.getDecoderInfos(
                                    mimeType,
                                    requiresSecureDecoder,
                                    requiresTunnelingDecoder,
                                )
                            if (mimeType.contains("video/avc")) {
                                decoders.filter {
                                    it.name != "c2.qti.avc.decoder" &&
                                        it.name != "c2.qti.avc.decoder.low_latency"
                                }
                            } else {
                                decoders
                            }
                        }
                    }

                ExoPlayer.Builder(context, renderersFactory).build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                }
            } else null
        }

    DisposableEffect(dedicatedPlayer) { onDispose { dedicatedPlayer?.release() } }

    var pooledPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    val exoPlayer = if (usePool) pooledPlayer else dedicatedPlayer
    var isReady by remember(url, exoPlayer) { mutableStateOf(false) }
    val activeCount = manager?.activeCount ?: 0

    LaunchedEffect(usePool, url, activeCount) {
        if (usePool && pooledPlayer == null) {
            val player = manager?.acquirePlayer()

            if (player != null) {
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()

                pooledPlayer = player
            }
        }
    }

    DisposableEffect(usePool, url) {
        onDispose {
            pooledPlayer?.let {
                manager?.releasePlayer(it)

                pooledPlayer = null
            }
        }
    }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer, url) {
            val listener =
                object : Player.Listener {
                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                        val hasAudio =
                            tracks.groups.any {
                                it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO &&
                                    it.isSupported
                            }

                        onAudioStateChange(hasAudio)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (
                            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                                error.errorCode ==
                                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                                error.errorCode ==
                                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                        ) {
                            onSwitchToMpv()
                        } else {
                            onPlaybackError(error.message)
                        }
                    }

                    override fun onRenderedFirstFrame() {
                        isReady = true
                    }
                }
            exoPlayer.addListener(listener)
            onDispose { exoPlayer.removeListener(listener) }
        }

        LaunchedEffect(url, dedicatedPlayer) {
            if (!usePool && dedicatedPlayer != null) {
                isReady = false
                val currentPos = dedicatedPlayer.currentPosition

                dedicatedPlayer.setMediaItem(MediaItem.fromUri(url))

                if (currentPos > 0) dedicatedPlayer.seekTo(currentPos)

                dedicatedPlayer.prepare()
            }
        }

        LaunchedEffect(exoPlayer, isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }

        LaunchedEffect(exoPlayer, playbackSpeed) {
            exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
        }

        LaunchedEffect(exoPlayer, seekPosition) {
            seekPosition?.let {
                exoPlayer.seekTo(it)
                onSeekConsumed()
            }
        }

        LaunchedEffect(exoPlayer, isPlaying) {
            exoPlayer.playWhenReady = isPlaying

            if (!usePool) {
                var lastRenderedCount = 0L
                var lastTime = System.currentTimeMillis()

                while (isActive) {
                    if (exoPlayer.duration > 0)
                        onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)

                    if (isPlaying) {
                        val currentRenderedCount =
                            exoPlayer.videoDecoderCounters?.renderedOutputBufferCount?.toLong()
                                ?: 0L

                        val currentTime = System.currentTimeMillis()
                        val elapsed = currentTime - lastTime

                        if (elapsed >= 1000) {
                            val fps =
                                ((currentRenderedCount - lastRenderedCount) * 1000f / elapsed)
                                    .toInt()

                            onFpsUpdate(fps)

                            lastRenderedCount = currentRenderedCount
                            lastTime = currentTime
                        }
                    } else {
                        exoPlayer.stop()

                        break
                    }

                    delay(if (isPlaying) 33 else 500)
                }
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
                else if (event == Lifecycle.Event.ON_RESUME) {
                    if (currentIsPlaying) exoPlayer.play()
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = exoPlayer
                    useController = false

                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)

                    findViewById<android.view.View>(androidx.media3.ui.R.id.exo_shutter)
                        ?.visibility = android.view.View.GONE

                    layoutParams =
                        android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode =
                    when (scaleMode) {
                        ScaleMode.NORMAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        ScaleMode.FULL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
            },
            onRelease = { playerView -> playerView.player = null },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
