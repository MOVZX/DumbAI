package org.movzx.dibella.ui.components

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import org.movzx.dibella.util.Logger

@Composable
fun MpvPlayer(
    url: String,
    isPlaying: Boolean = true,
    isMuted: Boolean = true,
    playbackSpeed: Float = 1.0f,
    onProgressUpdate: (Long, Long) -> Unit = { _, _ -> },
    onFpsUpdate: (Int) -> Unit = {},
    onAudioStateChange: (Boolean) -> Unit = {},
    seekPosition: Long? = null,
    onSeekConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val instanceId = remember { (Math.random() * Int.MAX_VALUE).toInt() }
    var isSurfaceReady by remember { mutableStateOf(false) }
    var currentSurface by remember { mutableStateOf<android.view.Surface?>(null) }
    var lastPos by remember { mutableDoubleStateOf(0.0) }

    val mpv = remember {
        MPVLib.initialize(context)
        MPVLib.safeSetOptionString("vo", "gpu")
        MPVLib.safeSetOptionString("gpu-context", "android")
        MPVLib.safeSetOptionString("hwdec", "auto")
        MPVLib.safeSetOptionString("force-window", "yes")
        MPVLib.safeSetOptionString("framedrop", "no")
        MPVLib.safeSetOptionString("hr-seek", "yes")
        MPVLib
    }

    SideEffect {
        if (isPlaying && MPVLib.lastOwnerId != instanceId) {
            Logger.d("MpvPlayer", "[$instanceId] SideEffect: Taking ownership")

            MPVLib.lastOwnerId = instanceId
        }
    }

    LaunchedEffect(isPlaying, isSurfaceReady) {
        if (isPlaying && isSurfaceReady) {
            while (true) {
                if (MPVLib.lastOwnerId == instanceId) {
                    val pos = MPVLib.safeGetPropertyDouble("time-pos") ?: lastPos
                    val dur = MPVLib.safeGetPropertyDouble("duration") ?: 0.0
                    val fps = MPVLib.safeGetPropertyDouble("estimated-vf-fps") ?: 0.0
                    lastPos = pos

                    onProgressUpdate((pos * 1000).toLong(), (dur * 1000).toLong())
                    onFpsUpdate(fps.toInt())

                    val aid = MPVLib.safeGetPropertyInt("aid") ?: 0

                    onAudioStateChange(aid > 0)
                }

                delay(50)
            }
        }
    }

    LaunchedEffect(url, isSurfaceReady, isPlaying) {
        if (isPlaying) {
            if (isSurfaceReady) {
                delay(50)

                if (MPVLib.lastOwnerId != instanceId) {
                    Logger.d("MpvPlayer", "[$instanceId] Taking ownership and loading $url")

                    MPVLib.lastOwnerId = instanceId

                    currentSurface?.let { mpv.safeAttachSurface(it) }

                    val currentFile = MPVLib.getPropertyString("path")

                    if (currentFile != url) mpv.safeCommand(arrayOf("loadfile", url, "replace"))

                    mpv.safeSetPropertyBoolean("pause", false)
                    mpv.safeSetPropertyBoolean("mute", isMuted)
                    mpv.safeSetPropertyDouble("speed", playbackSpeed.toDouble())
                }
            }
        } else {
            if (MPVLib.lastOwnerId == instanceId) {
                Logger.d("MpvPlayer", "[$instanceId] Releasing ownership (pausing)")

                mpv.safeSetPropertyBoolean("pause", true)
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (MPVLib.lastOwnerId == instanceId) mpv.safeSetPropertyBoolean("pause", !isPlaying)
    }

    LaunchedEffect(isMuted) {
        if (MPVLib.lastOwnerId == instanceId) mpv.safeSetPropertyBoolean("mute", isMuted)
    }

    LaunchedEffect(playbackSpeed) {
        if (MPVLib.lastOwnerId == instanceId)
            mpv.safeSetPropertyDouble("speed", playbackSpeed.toDouble())
    }

    LaunchedEffect(seekPosition) {
        if (seekPosition != null && MPVLib.lastOwnerId == instanceId) {
            mpv.safeCommand(arrayOf("seek", (seekPosition / 1000.0).toString(), "absolute"))
            onSeekConsumed()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (MPVLib.lastOwnerId == instanceId) {
                mpv.safeCommand(arrayOf("stop"))
                mpv.safeDetachSurface()

                MPVLib.lastOwnerId = -1
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(
                    object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            currentSurface = holder.surface
                            isSurfaceReady = true

                            if (isPlaying) mpv.safeAttachSurface(holder.surface)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int,
                        ) {
                            currentSurface = holder.surface

                            if (isPlaying && MPVLib.lastOwnerId == instanceId)
                                mpv.safeAttachSurface(holder.surface)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            currentSurface = null
                            isSurfaceReady = false
                        }
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}
