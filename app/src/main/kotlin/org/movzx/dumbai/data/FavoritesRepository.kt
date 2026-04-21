package org.movzx.dumbai.data

import android.content.Context
import org.movzx.dumbai.util.getThumbnailUrl
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.FavoriteImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class FavoritesRepository(
    private val context: Context,
    private val favoriteImageDao: FavoriteImageDao
) {
    private val favoritesDir = File(context.filesDir, "favorites").apply { mkdirs() }
    private val okHttpClient = OkHttpClient()

    val allFavorites: Flow<List<CivitaiImage>> = favoriteImageDao.getAllFavorites().map { favorites ->
        favorites.map { it.toCivitaiImage() }
    }

    suspend fun getAllFavoritesSync(): List<FavoriteImage> = withContext(Dispatchers.IO) {
        favoriteImageDao.getAllFavoritesSync()
    }

    suspend fun importFavorites(favorites: List<FavoriteImage>) = withContext(Dispatchers.IO) {
        favorites.forEach {
            favoriteImageDao.insertFavorite(it)
        }
    }

    val favoriteIds: Flow<Set<Long>> = favoriteImageDao.getAllFavoriteIds().map { it.toSet() }

    suspend fun toggleFavorite(image: CivitaiImage) {
        if (favoriteImageDao.isFavoriteDirect(image.id))
            removeFavorite(image)
        else
            addFavorite(image)
    }

    private suspend fun addFavorite(image: CivitaiImage) = withContext(Dispatchers.IO) {
        val file = File(favoritesDir, "${image.id}.jpg")
        val favorite = FavoriteImage.fromCivitaiImage(image, file.absolutePath)

        favoriteImageDao.insertFavorite(favorite)

        try {
            val thumbUrl = getThumbnailUrl(image.url, 720)
            val request = Request.Builder().url(thumbUrl).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun removeFavorite(image: CivitaiImage) = withContext(Dispatchers.IO) {
        val favorite = FavoriteImage.fromCivitaiImage(image)

        favoriteImageDao.deleteFavorite(favorite)

        val file = File(favoritesDir, "${image.id}.jpg")

        if (file.exists())
            file.delete()
    }

    fun isFavorite(id: Long): Flow<Boolean> = favoriteImageDao.isFavorite(id)

    fun getLocalFile(id: Long): File? {
        val file = File(favoritesDir, "$id.jpg")

        return if (file.exists()) file else null
    }
}
