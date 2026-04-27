package org.movzx.dibella.data

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
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage
import org.movzx.dibella.util.FileUtils
import org.movzx.dibella.util.Logger
import org.movzx.dibella.util.getThumbnailUrl
import org.movzx.dibella.util.getVideoPreviewUrl
import org.movzx.dibella.util.getVideoThumbnailUrl

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

        Logger.d("Dibella", "toggleFavorite: ${image.id} isFav=$isFav")

        if (isFav) removeFavorite(image) else addFavorite(image)
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
            Logger.e("Dibella_Res", "Frame Extraction Failed: ${e.message}")

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
                Logger.d("Dibella_Res", "ID: ${image.id} | Cache Check | Started (force=$force)")

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

                if (!FileUtils.isRealMedia(currentThumbFile)) {
                    val thumbUrl =
                        if (image.type == "video") getVideoThumbnailUrl(image.url)
                        else getThumbnailUrl(image.url, 640)

                    Logger.d("Dibella_Res", "ID: ${image.id} | Cache | Downloading Thumbnail")

                    val tempFile = File(context.cacheDir, "temp_thumb_${image.id}")

                    if (
                        downloadFile(thumbUrl, tempFile) { p ->
                            onProgress((currentStep + p) / totalSteps)
                        }
                    ) {
                        val bytes = tempFile.inputStream().use { it.readNBytes(12) }
                        val ext = FileUtils.getExtensionFromBytes(bytes)

                        if (ext != null && ext in setOf("png", "jpg", "webp", "gif", "avif")) {
                            val finalFile = File(favoritesDir, "${image.id}.$ext")

                            if (finalFile.exists()) finalFile.delete()

                            tempFile.renameTo(finalFile)

                            thumbPath = finalFile.absolutePath
                            updated = true
                        } else {
                            if (ext != null && ext in setOf("mp4", "webm", "mkv")) {
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

                    if (!FileUtils.isRealMedia(currentFullFile)) {
                        Logger.d("Dibella_Res", "ID: ${image.id} | Cache | Downloading Full Image")

                        val tempFile = File(context.cacheDir, "temp_full_${image.id}")

                        if (
                            downloadFile(image.url, tempFile) { p ->
                                onProgress((currentStep + p) / totalSteps)
                            }
                        ) {
                            val bytes = tempFile.inputStream().use { it.readNBytes(12) }
                            val ext = FileUtils.getExtensionFromBytes(bytes)

                            if (ext != null && ext in setOf("png", "jpg", "webp", "gif", "avif")) {
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

                    if (!FileUtils.isRealMedia(currentVideoFile)) {
                        val videoUrl = getVideoPreviewUrl(image.url)

                        Logger.d("Dibella_Res", "ID: ${image.id} | Cache | Downloading Video")

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
                            val bytes = tempFile.inputStream().use { it.readNBytes(12) }
                            val ext = FileUtils.getExtensionFromBytes(bytes)

                            if (ext != null && ext in setOf("mp4", "webm", "mkv")) {
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

                    Logger.d("Dibella_Res", "ID: ${image.id} | Cache | Database Updated")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Logger.e("Dibella_Res", "ID: ${image.id} | Cache | Error: ${e.message}")
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
                    val body = response.body ?: return false
                    val contentLength = body.contentLength()

                    Logger.d(
                        "Dibella_Res",
                        "Download: $url Type: $contentType Size: $contentLength",
                    )

                    body.byteStream().use { input ->
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

                    if (tempFile.exists() && tempFile.length() > 10) {
                        if (!FileUtils.isRealMedia(tempFile)) {
                            Logger.e(
                                "Dibella_Res",
                                "Download Failed: Invalid media content for $url",
                            )

                            return false
                        }

                        if (destination.exists()) destination.delete()

                        val success = tempFile.renameTo(destination)

                        if (!success) {
                            try {
                                tempFile.copyTo(destination, overwrite = true)

                                true
                            } catch (e: Exception) {
                                Logger.e("Dibella_Res", "File Save Failed: ${e.message}")

                                false
                            }
                        } else true
                    } else {
                        Logger.e("Dibella_Res", "Download Failed: Temp file invalid or empty")

                        false
                    }
                } else {
                    Logger.e("Dibella_Res", "Download Failed: HTTP ${response.code}")

                    false
                }
            }
        } catch (e: Exception) {
            Logger.e("Dibella_Res", "Download Exception: ${e.message}")

            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun addFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            Logger.d("Dibella", "addFavorite: ${image.id}")

            val initialFavorite = FavoriteImage.fromCivitaiImage(image, null, null, null)

            favoriteImageDao.insertFavorite(initialFavorite)
            repositoryScope.launch { ensureFavoriteResources(image) }
        }

    private suspend fun removeFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            Logger.d("Dibella", "removeFavorite: ${image.id}")

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
            Logger.d("Dibella", "clearUnusedResources: keeping ${favoriteIds.size} ids")

            val files = favoritesDir.listFiles() ?: return@withContext

            files.forEach { file ->
                val idStr = file.nameWithoutExtension.removeSuffix("_full")
                val id = idStr.toLongOrNull()

                if (id != null && !favoriteIds.contains(id)) {
                    Logger.d("Dibella", "Deleting unused resource: ${file.name}")

                    file.delete()
                }
            }
        }

    suspend fun findDuplicateGroups(): List<List<CivitaiImage>> =
        withContext(Dispatchers.IO) {
            val favorites = favoriteImageDao.getAllFavoritesSync()
            val fileToFavorite = mutableMapOf<File, CivitaiImage>()

            favorites.forEach { fav ->
                val mainFile =
                    fav.localVideoPath?.let { File(it) }
                        ?: fav.localFullImagePath?.let { File(it) }
                        ?: fav.localPath?.let { File(it) }

                if (mainFile != null && mainFile.exists())
                    fileToFavorite[mainFile] = fav.toCivitaiImage()
            }

            val files = fileToFavorite.keys.toList()
            val duplicateFiles = internalCalculateDuplicates(files)

            duplicateFiles.map { group -> group.mapNotNull { fileToFavorite[it] } }
        }

    suspend fun removeDuplicates(duplicateGroups: List<List<CivitaiImage>>): Int =
        withContext(Dispatchers.IO) {
            var removedCount = 0

            for (group in duplicateGroups) {
                val imageWithTime = mutableListOf<Pair<CivitaiImage, Long>>()

                for (image in group) {
                    val fav = favoriteImageDao.getFavorite(image.id)

                    val mainFile =
                        fav?.localVideoPath?.let { File(it) }
                            ?: fav?.localFullImagePath?.let { File(it) }
                            ?: fav?.localPath?.let { File(it) }

                    imageWithTime.add(image to (mainFile?.lastModified() ?: Long.MAX_VALUE))
                }

                val sortedGroup = imageWithTime.sortedBy { it.second }.map { it.first }
                val toRemove = sortedGroup.drop(1)

                for (image in toRemove) {
                    removeFavorite(image)
                    removedCount++
                }
            }

            removedCount
        }

    private fun internalCalculateDuplicates(files: List<File>): List<List<File>> {
        val sizeGroups = files.groupBy { it.length() }.filter { it.value.size > 1 }
        val duplicates = mutableListOf<List<File>>()

        sizeGroups.forEach { (_, group) ->
            val validHashes = group.mapNotNull { file ->
                val hash = FileUtils.calculateHash(file)

                if (hash != null) file to hash else null
            }

            val hashGroups = validHashes.groupBy { it.second }.filter { it.value.size > 1 }

            hashGroups.forEach { entry -> duplicates.add(entry.value.map { it.first }) }
        }

        return duplicates
    }

    suspend fun getAllFavoritesSync(): List<FavoriteImage> = favoriteImageDao.getAllFavoritesSync()

    suspend fun importFavorites(favorites: List<FavoriteImage>) {
        favoriteImageDao.insertFavorites(favorites)
    }

    fun isFavorite(id: Long): Flow<Boolean> = favoriteImageDao.isFavorite(id)
}
