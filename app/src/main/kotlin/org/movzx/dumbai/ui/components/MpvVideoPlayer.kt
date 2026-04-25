package org.movzx.dumbai.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.MPVLib

class MpvVideoView(context: Context) : BaseMPVView(context) {
    override fun initOptions() {
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("hwdec", "mediacodec")
        MPVLib.setOptionString("video-sync", "audio")
        MPVLib.setOptionString("loop-file", "inf")
    }

    override fun postInitOptions() {}

    override fun observeProperties() {}
}

@Composable
fun MpvVideoPlayer(
    url: String,
    isPlaying: Boolean = true,
    scaleMode: ScaleMode = ScaleMode.NORMAL,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mpvView = remember {
        MpvVideoView(context).apply {
            val configDir = context.filesDir.absolutePath
            val cacheDir = context.cacheDir.absolutePath

            initialize(configDir, cacheDir)
        }
    }

    LaunchedEffect(url) { mpvView.playFile(url) }

    LaunchedEffect(isPlaying) { MPVLib.setPropertyBoolean("pause", !isPlaying) }

    LaunchedEffect(scaleMode) {
        when (scaleMode) {
            ScaleMode.NORMAL -> {
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyString("panscan", "0.0")
            }

            ScaleMode.CROP -> {
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyString("panscan", "1.0")
            }

            ScaleMode.FULL -> {
                MPVLib.setPropertyString("keepaspect", "no")
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> MPVLib.setPropertyBoolean("pause", true)
                Lifecycle.Event.ON_RESUME ->
                    if (isPlaying) MPVLib.setPropertyBoolean("pause", false)
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mpvView.destroy()
        }
    }

    AndroidView(factory = { mpvView }, modifier = modifier.fillMaxSize())
}
