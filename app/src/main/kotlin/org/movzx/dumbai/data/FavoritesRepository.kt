package org.movzx.dumbai.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
import org.movzx.dumbai.util.Logger
import org.movzx.dumbai.util.getThumbnailUrl
import org.movzx.dumbai.util.getVideoPreviewUrl
import org.movzx.dumbai.util.getVideoThumbnailUrl

class FavoritesRepository(
    private val context: Context,
    private val favoriteImageDao: FavoriteImageDao,
    private var okHttpClient: OkHttpClient,
) {
    private val favoritesDir = File(context.filesDir, "favorites").apply { mkdirs() }
    private val resourceChecksInProgress = ConcurrentHashMap.newKeySet<Long>()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateOkHttpClient(newClient: OkHttpClient) {
        okHttpClient = newClient
    }

    val allFavorites: Flow<List<CivitaiImage>> =
        favoriteImageDao.getAllFavorites().map { list -> list.map { it.toCivitaiImage() } }

    val favoriteIds: Flow<Set<Long>> = favoriteImageDao.getAllFavoriteIds().map { it.toSet() }

    suspend fun toggleFavorite(image: CivitaiImage) {
        val isFav = favoriteImageDao.isFavoriteDirect(image.id)

        Logger.d("DumbAI", "toggleFavorite: ${image.id} isFav=$isFav")

        if (isFav) removeFavorite(image) else addFavorite(image)
    }

    private fun isValidImage(file: File): Boolean = isValidImageWithExt(file) != null

    private fun isValidImageWithExt(file: File): String? {
        if (!file.exists() || file.length() < 100) return null

        return try {
            val bytes =
                file.inputStream().use { input ->
                    val buffer = ByteArray(24)

                    input.read(buffer)
                    buffer
                }

            val ext = org.movzx.dumbai.util.FileUtils.getExtensionFromBytes(bytes)

            if (ext in setOf("png", "jpg", "webp", "gif", "avif")) ext else null
        } catch (e: Exception) {
            if (file.length() > 1024) "jpg" else null
        }
    }

    private fun isValidVideo(file: File): Boolean = isValidVideoWithExt(file) != null

    private fun isValidVideoWithExt(file: File): String? {
        if (!file.exists() || file.length() < 1000) return null

        return try {
            val bytes =
                file.inputStream().use { input ->
                    val buffer = ByteArray(12)

                    input.read(buffer)
                    buffer
                }

            val ext = org.movzx.dumbai.util.FileUtils.getExtensionFromBytes(bytes)

            if (ext in setOf("mp4", "webm", "mkv")) ext
            else if (file.length() > 50 * 1024) "mp4" else null
        } catch (e: Exception) {
            if (file.length() > 1024) "mp4" else null
        }
    }

    private fun extractVideoFrame(videoFile: File, outputFile: File): Boolean {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(videoFile.absolutePath)

            val bitmap = retriever.getFrameAtTime(0)

            if (bitmap != null) {
                outputFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                true
            } else false
        } catch (e: Exception) {
            Logger.e("DumbAI_Res", "Frame Extraction Failed: ${e.message}")

            false
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }

    fun getFavoriteFlow(id: Long): Flow<FavoriteImage?> = favoriteImageDao.getFavoriteFlow(id)

    suspend fun ensureFavoriteResources(
        image: CivitaiImage,
        force: Boolean = false,
        onProgress: (Float) -> Unit = {},
    ) =
        withContext(Dispatchers.IO) {
            if (!resourceChecksInProgress.add(image.id)) return@withContext

            try {
                Logger.d("DumbAI_Res", "ID: ${image.id} | Cache Check | Started (force=$force)")

                val favorite = favoriteImageDao.getFavorite(image.id) ?: return@withContext
                val thumbFile = File(favoritesDir, "${image.id}.jpg")
                val fullFile = File(favoritesDir, "${image.id}_full.jpg")

                val videoFile =
                    if (image.type == "video") File(favoritesDir, "${image.id}.mp4") else null

                if (force) {
                    thumbFile.delete()
                    fullFile.delete()
                    videoFile?.delete()

                    listOf("png", "webp", "gif", "avif").forEach { ext ->
                        File(favoritesDir, "${image.id}.$ext").delete()
                        File(favoritesDir, "${image.id}_full.$ext").delete()
                    }
                }

                var thumbPath = if (force) null else favorite.localPath
                var fullPath = if (force) null else favorite.localFullImagePath
                var videoPath = if (force) null else favorite.localVideoPath
                var updated = false
                val totalSteps = 2f
                var currentStep = 0f
                val currentThumbFile = thumbPath?.let { File(it) } ?: thumbFile

                if (!isValidImage(currentThumbFile)) {
                    val thumbUrl =
                        if (image.type == "video") getVideoThumbnailUrl(image.url)
                        else getThumbnailUrl(image.url, 640)

                    Logger.d("DumbAI_Res", "ID: ${image.id} | Cache | Downloading Thumbnail")

                    val tempFile = File(context.cacheDir, "temp_thumb_${image.id}")

                    if (
                        downloadFile(thumbUrl, tempFile) { p ->
                            onProgress((currentStep + p) / totalSteps)
                        }
                    ) {
                        val ext = isValidImageWithExt(tempFile)

                        if (ext != null) {
                            val finalFile = File(favoritesDir, "${image.id}.$ext")

                            if (finalFile.exists()) finalFile.delete()

                            tempFile.renameTo(finalFile)

                            thumbPath = finalFile.absolutePath
                            updated = true
                        } else {
                            val videoExt = isValidVideoWithExt(tempFile)

                            if (videoExt != null) {
                                val frameFile = File(favoritesDir, "${image.id}.jpg")

                                if (extractVideoFrame(tempFile, frameFile)) {
                                    thumbPath = frameFile.absolutePath
                                    updated = true
                                }
                            }
                        }
                    }
                }

                currentStep++
                onProgress(currentStep / totalSteps)

                if (image.type != "video") {
                    val currentFullFile = fullPath?.let { File(it) } ?: fullFile

                    if (!isValidImage(currentFullFile)) {
                        Logger.d("DumbAI_Res", "ID: ${image.id} | Cache | Downloading Full Image")

                        val tempFile = File(context.cacheDir, "temp_full_${image.id}")

                        if (
                            downloadFile(image.url, tempFile) { p ->
                                onProgress((currentStep + p) / totalSteps)
                            }
                        ) {
                            val ext = isValidImageWithExt(tempFile)

                            if (ext != null) {
                                val finalFile = File(favoritesDir, "${image.id}_full.$ext")

                                if (finalFile.exists()) finalFile.delete()

                                tempFile.renameTo(finalFile)

                                fullPath = finalFile.absolutePath
                                updated = true
                            }
                        }
                    }
                } else {
                    val currentVideoFile = videoPath?.let { File(it) } ?: videoFile!!

                    if (!isValidVideo(currentVideoFile)) {
                        val videoUrl = getVideoPreviewUrl(image.url)

                        Logger.d("DumbAI_Res", "ID: ${image.id} | Cache | Downloading Video")

                        val tempFile = File(context.cacheDir, "temp_video_${image.id}")

                        var success =
                            downloadFile(videoUrl, tempFile) { p ->
                                onProgress((currentStep + p) / totalSteps)
                            }

                        if (!success && videoUrl != image.url) {
                            success =
                                downloadFile(image.url, tempFile) { p ->
                                    onProgress((currentStep + p) / totalSteps)
                                }
                        }

                        if (success) {
                            val ext = isValidVideoWithExt(tempFile)

                            if (ext != null) {
                                val finalFile = File(favoritesDir, "${image.id}.$ext")

                                if (finalFile.exists()) finalFile.delete()

                                tempFile.renameTo(finalFile)

                                videoPath = finalFile.absolutePath
                                updated = true
                            }
                        }
                    }
                }

                currentStep++
                onProgress(1.0f)

                if (updated) {
                    val updatedFavorite =
                        favorite.copy(
                            localPath = thumbPath,
                            localFullImagePath = fullPath,
                            localVideoPath = videoPath,
                        )

                    favoriteImageDao.insertFavorite(updatedFavorite)

                    Logger.d("DumbAI_Res", "ID: ${image.id} | Cache | Database Updated")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Logger.e("DumbAI_Res", "ID: ${image.id} | Cache | Error: ${e.message}")
            } finally {
                resourceChecksInProgress.remove(image.id)
            }
        }

    private suspend fun downloadFile(
        url: String,
        destination: File,
        onProgress: (Float) -> Unit = {},
    ): Boolean {
        val tempFile =
            File(context.cacheDir, "temp_${System.currentTimeMillis()}_${destination.name}")

        return try {
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type")
                    val contentLength = response.body?.contentLength() ?: 0L

                    Logger.d("DumbAI_Res", "Download: $url Type: $contentType Size: $contentLength")

                    if (contentLength < 100) {
                        Logger.e(
                            "DumbAI_Res",
                            "Download Failed: File too small ($contentLength bytes)",
                        )

                        return false
                    }

                    response.body?.byteStream()?.use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)

                                totalRead += bytesRead

                                if (contentLength > 0)
                                    onProgress(totalRead.toFloat() / contentLength)
                            }
                        }
                    }

                    // Atomic move with fallback
                    if (tempFile.exists() && tempFile.length() > 100) {
                        if (destination.exists()) destination.delete()

                        val success = tempFile.renameTo(destination)

                        if (!success) {
                            try {
                                tempFile.copyTo(destination, overwrite = true)

                                true
                            } catch (e: Exception) {
                                Logger.e("DumbAI_Res", "File Save Failed: ${e.message}")

                                false
                            }
                        } else true
                    } else {
                        Logger.e("DumbAI_Res", "Download Failed: Temp file invalid")

                        false
                    }
                } else {
                    Logger.e("DumbAI_Res", "Download Failed: HTTP ${response.code}")

                    false
                }
            }
        } catch (e: Exception) {
            Logger.e("DumbAI_Res", "Download Exception: ${e.message}")

            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun addFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            Logger.d("DumbAI", "addFavorite: ${image.id}")

            val initialFavorite = FavoriteImage.fromCivitaiImage(image, null, null, null)

            favoriteImageDao.insertFavorite(initialFavorite)
            repositoryScope.launch { ensureFavoriteResources(image) }
        }

    private suspend fun removeFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            Logger.d("DumbAI", "removeFavorite: ${image.id}")

            val favorite =
                favoriteImageDao.getFavorite(image.id) ?: FavoriteImage.fromCivitaiImage(image)

            favoriteImageDao.deleteFavorite(favorite)
            favorite.localPath?.let { File(it).let { f -> if (f.exists()) f.delete() } }
            favorite.localFullImagePath?.let { File(it).let { f -> if (f.exists()) f.delete() } }
            favorite.localVideoPath?.let { File(it).let { f -> if (f.exists()) f.delete() } }
            File(favoritesDir, "${image.id}.jpg").let { if (it.exists()) it.delete() }
            File(favoritesDir, "${image.id}_full.jpg").let { if (it.exists()) it.delete() }
            File(favoritesDir, "${image.id}.mp4").let { if (it.exists()) it.delete() }
        }

    suspend fun clearUnusedResources(favoriteIds: Set<Long>) =
        withContext(Dispatchers.IO) {
            Logger.d("DumbAI", "clearUnusedResources: keeping ${favoriteIds.size} ids")

            val files = favoritesDir.listFiles() ?: return@withContext

            files.forEach { file ->
                val idStr = file.nameWithoutExtension.removeSuffix("_full")
                val id = idStr.toLongOrNull()

                if (id != null && !favoriteIds.contains(id)) {
                    Logger.d("DumbAI", "Deleting unused resource: ${file.name}")

                    file.delete()
                }
            }
        }

    suspend fun getAllFavoritesSync(): List<FavoriteImage> = favoriteImageDao.getAllFavoritesSync()

    suspend fun importFavorites(favorites: List<FavoriteImage>) {
        favoriteImageDao.insertFavorites(favorites)
    }

    fun isFavorite(id: Long): Flow<Boolean> = favoriteImageDao.isFavorite(id)
}
