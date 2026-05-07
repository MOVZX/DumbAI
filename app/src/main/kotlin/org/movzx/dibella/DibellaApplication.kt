package org.movzx.dibella

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.movzx.dibella.data.FavoritesRepository
import org.movzx.dibella.util.Logger

@HiltAndroidApp
class DibellaApplication : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var favoritesRepository: FavoritesRepository

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return imageLoader
    }

    override fun onTerminate() {
        super.onTerminate()

        try {
            favoritesRepository.close()

            Logger.d("Dibella_App", "FavoritesRepository closed")
        } catch (e: Exception) {
            Logger.e("Dibella_App", "Failed to close FavoritesRepository: ${e.message}")
        }
    }
}
