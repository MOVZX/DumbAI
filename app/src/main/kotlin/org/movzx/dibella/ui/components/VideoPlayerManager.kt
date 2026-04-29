package org.movzx.dibella.ui.components

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

@OptIn(UnstableApi::class)
class VideoPlayerManager(private val context: Context) {
    private val pool = mutableListOf<ExoPlayer>()
    private val activePlayers = mutableSetOf<ExoPlayer>()

    var maxPoolSize by androidx.compose.runtime.mutableIntStateOf(12)
        private set

    var activeCount by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    fun updateLimit(newLimit: Int) {
        if (newLimit == maxPoolSize) return

        org.movzx.dibella.util.Logger.d(
            "VideoPlayerManager",
            "Updating limit: $maxPoolSize -> $newLimit",
        )

        maxPoolSize = newLimit

        while (pool.size > maxPoolSize) {
            pool.removeAt(0).release()
        }
    }

    fun acquirePlayer(): ExoPlayer? {
        if (activePlayers.size >= maxPoolSize) {
            org.movzx.dibella.util.Logger.d(
                "VideoPlayerManager",
                "Pool exhausted (${activePlayers.size}/$maxPoolSize)",
            )

            return null
        }

        val player = if (pool.isNotEmpty()) pool.removeAt(0) else createPlayer()

        activePlayers.add(player)

        activeCount = activePlayers.size

        org.movzx.dibella.util.Logger.d(
            "VideoPlayerManager",
            "Acquired player. Active: $activeCount",
        )

        return player
    }

    fun releasePlayer(player: ExoPlayer) {
        if (activePlayers.remove(player)) {
            player.pause()
            player.stop()
            player.clearMediaItems()

            if (pool.size < maxPoolSize) pool.add(player) else player.release()

            activeCount = activePlayers.size

            org.movzx.dibella.util.Logger.d(
                "VideoPlayerManager",
                "Released player. Active: $activeCount",
            )
        }
    }

    private fun createPlayer(): ExoPlayer {
        val renderersFactory =
            DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
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

        return ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    fun releaseAll() {
        activePlayers.forEach { it.release() }
        activePlayers.clear()
        pool.forEach { it.release() }
        pool.clear()
    }
}

val LocalVideoPlayerManager = staticCompositionLocalOf<VideoPlayerManager?> { null }
