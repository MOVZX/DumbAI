package org.movzx.dibella

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DibellaApplication : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return imageLoader
    }
}
