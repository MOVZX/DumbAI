package org.movzx.dumbai.data

import android.content.Context
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.FavoriteImage
import org.movzx.dumbai.util.getThumbnailUrl
import org.movzx.dumbai.util.getVideoPreviewUrl
import org.movzx.dumbai.util.getVideoThumbnailUrl

class FavoritesRepository(
    private val context: Context,
    private val favoriteImageDao: FavoriteImageDao,
    private var okHttpClient: OkHttpClient,
) {
    var debugEnabled: Boolean = false

    private val favoritesDir = File(context.filesDir, "favorites").apply { mkdirs() }
    private val resourceChecksInProgress = ConcurrentHashMap.newKeySet<Long>()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateOkHttpClient(newClient: OkHttpClient) {
        this.okHttpClient = newClient
    }

    val allFavorites: Flow<List<CivitaiImage>> =
        favoriteImageDao.getAllFavorites().map { favorites ->
            favorites.map { it.toCivitaiImage() }
        }

    suspend fun getAllFavoritesSync(): List<FavoriteImage> =
        withContext(Dispatchers.IO) { favoriteImageDao.getAllFavoritesSync() }

    suspend fun importFavorites(favorites: List<FavoriteImage>) =
        withContext(Dispatchers.IO) { favoriteImageDao.insertFavorites(favorites) }

    val favoriteIds: Flow<Set<Long>> = favoriteImageDao.getAllFavoriteIds().map { it.toSet() }

    suspend fun toggleFavorite(image: CivitaiImage) {
        val isFav = favoriteImageDao.isFavoriteDirect(image.id)

        if (debugEnabled) android.util.Log.d("DumbAI", "toggleFavorite: ${image.id} isFav=$isFav")

        if (isFav) removeFavorite(image) else addFavorite(image)
    }

    private fun isValidImage(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false

        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }

        android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)

        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun isValidVideo(file: File): Boolean {
        if (!file.exists() || file.length() < 1000) return false

        val retriever = android.media.MediaMetadataRetriever()

        return try {
            retriever.setDataSource(file.absolutePath)

            val hasVideo =
                retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO
                )

            hasVideo == "yes"
        } catch (e: Exception) {
            false
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }

    suspend fun getLocalFile(id: Long): File? {
        val favorite = favoriteImageDao.getFavorite(id)
        val file = favorite?.localPath?.let { File(it) } ?: File(favoritesDir, "$id.jpg")
        val valid = isValidImage(file)

        if (debugEnabled)
            android.util.Log.d("DumbAI", "getLocalFile: $id path=${file.absolutePath} valid=$valid")

        return if (valid) file else null
    }

    suspend fun getLocalVideoFile(id: Long): File? {
        val favorite = favoriteImageDao.getFavorite(id)
        val file = favorite?.localVideoPath?.let { File(it) } ?: File(favoritesDir, "$id.mp4")
        val valid = isValidVideo(file)

        if (debugEnabled)
            android.util.Log.d(
                "DumbAI",
                "getLocalVideoFile: $id path=${file.absolutePath} valid=$valid",
            )

        return if (valid) file else null
    }

    fun getFavoriteFlow(id: Long): Flow<FavoriteImage?> = favoriteImageDao.getFavoriteFlow(id)

    suspend fun ensureFavoriteResources(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            if (!resourceChecksInProgress.add(image.id)) return@withContext

            try {
                if (debugEnabled)
                    android.util.Log.d("DumbAI", "Checking resources for favorite: ${image.id}")

                val favorite = favoriteImageDao.getFavorite(image.id) ?: return@withContext
                val thumbFile = File(favoritesDir, "${image.id}.jpg")

                val videoFile =
                    if (image.type == "video") File(favoritesDir, "${image.id}.mp4") else null

                var thumbPath = favorite.localPath
                var videoPath = favorite.localVideoPath
                var updated = false

                if (thumbPath == null || !isValidImage(File(thumbPath))) {
                    val thumbUrl =
                        if (image.type == "video") getVideoThumbnailUrl(image.url)
                        else getThumbnailUrl(image.url, 640)

                    if (debugEnabled)
                        android.util.Log.d(
                            "DumbAI",
                            "Downloading thumbnail for ${image.id}: $thumbUrl",
                        )

                    if (downloadFile(thumbUrl, thumbFile) && isValidImage(thumbFile)) {
                        thumbPath = thumbFile.absolutePath
                        updated = true
                    }
                }

                if (image.type == "video" && videoFile != null) {
                    if (videoPath == null || !isValidVideo(File(videoPath))) {
                        val videoUrl = getVideoPreviewUrl(image.url)

                        if (debugEnabled)
                            android.util.Log.d(
                                "DumbAI",
                                "Downloading video preview for ${image.id}: $videoUrl",
                            )

                        if (downloadFile(videoUrl, videoFile) && isValidVideo(videoFile)) {
                            videoPath = videoFile.absolutePath
                            updated = true
                        } else {
                            if (videoUrl != image.url) {
                                if (downloadFile(image.url, videoFile) && isValidVideo(videoFile)) {
                                    videoPath = videoFile.absolutePath
                                    updated = true
                                }
                            }
                        }
                    }
                }

                if (updated) {
                    val updatedFavorite =
                        favorite.copy(localPath = thumbPath, localVideoPath = videoPath)

                    favoriteImageDao.insertFavorite(updatedFavorite)

                    if (debugEnabled)
                        android.util.Log.d(
                            "DumbAI",
                            "Updated favorite storage in DB for ${image.id}",
                        )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                if (debugEnabled)
                    android.util.Log.e(
                        "DumbAI",
                        "Error in ensureFavoriteResources for ${image.id}: ${e.message}",
                    )
            } finally {
                resourceChecksInProgress.remove(image.id)
            }
        }

    private suspend fun addFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            if (debugEnabled) android.util.Log.d("DumbAI", "addFavorite: ${image.id}")

            val initialFavorite = FavoriteImage.fromCivitaiImage(image, null, null)

            favoriteImageDao.insertFavorite(initialFavorite)
            repositoryScope.launch { ensureFavoriteResources(image) }
        }

    private suspend fun downloadFile(url: String, destination: File): Boolean {
        return try {
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type")
                    val contentLength = response.body?.contentLength() ?: 0L

                    if (debugEnabled)
                        android.util.Log.d(
                            "DumbAI",
                            "Download: $url Type: $contentType Size: $contentLength",
                        )

                    if (contentLength < 100) return false

                    response.body?.byteStream()?.use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                    }

                    true
                } else false
            }
        } catch (e: Exception) {
            if (destination.exists()) destination.delete()

            false
        }
    }

    private suspend fun removeFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            if (debugEnabled) android.util.Log.d("DumbAI", "removeFavorite: ${image.id}")

            val favorite =
                favoriteImageDao.getFavorite(image.id) ?: FavoriteImage.fromCivitaiImage(image)

            favoriteImageDao.deleteFavorite(favorite)

            favorite.localPath?.let { File(it).let { f -> if (f.exists()) f.delete() } }
            favorite.localVideoPath?.let { File(it).let { f -> if (f.exists()) f.delete() } }

            File(favoritesDir, "${image.id}.jpg").let { if (it.exists()) it.delete() }
            File(favoritesDir, "${image.id}.mp4").let { if (it.exists()) it.delete() }
        }

    suspend fun clearUnusedResources(favoriteIds: Set<Long>) =
        withContext(Dispatchers.IO) {
            if (debugEnabled)
                android.util.Log.d(
                    "DumbAI",
                    "clearUnusedResources: keeping ${favoriteIds.size} ids",
                )

            val files = favoritesDir.listFiles() ?: return@withContext

            files.forEach { file ->
                val idStr = file.nameWithoutExtension
                val id = idStr.toLongOrNull()

                if (id != null && !favoriteIds.contains(id)) {
                    if (debugEnabled)
                        android.util.Log.d("DumbAI", "Deleting unused resource: ${file.name}")

                    file.delete()
                }
            }
        }

    fun isFavorite(id: Long): Flow<Boolean> = favoriteImageDao.isFavorite(id)
}
