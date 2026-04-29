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

    suspend fun manualFetch(url: String): Boolean =
        withContext(Dispatchers.IO) {
            val request = okhttp3.Request.Builder().url(url).build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    Logger.d("Dibella_Net", "Manual fetch result for $url: ${response.code}")

                    response.isSuccessful
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                Logger.e("Dibella_Net", "Manual fetch failed for $url: ${e.message}")

                false
            }
        }

    suspend fun toggleFavorite(image: CivitaiImage) {
        val isFav = favoriteImageDao.isFavoriteDirect(image.id)

        Logger.d("Dibella_Cache", "[${image.id}] toggleFavorite: currently isFav=$isFav")

        if (isFav) removeFavorite(image) else addFavorite(image)
    }

    private fun extractVideoFrame(videoFile: File, outputFile: File): Boolean {
        val retriever = MediaMetadataRetriever()
        val startTime = System.currentTimeMillis()

        return try {
            retriever.setDataSource(videoFile.absolutePath)

            val bitmap = retriever.getFrameAtTime(0)

            if (bitmap != null) {
                outputFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                Logger.d(
                    "Dibella_Codec",
                    "[${videoFile.name}] Frame extracted in ${System.currentTimeMillis() - startTime}ms",
                )

                true
            } else {
                Logger.e("Dibella_Codec", "[${videoFile.name}] Extraction failed: null bitmap")

                false
            }
        } catch (e: Exception) {
            Logger.e("Dibella_Codec", "[${videoFile.name}] Extraction Exception: ${e.message}")

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
            if (!resourceChecksInProgress.add(image.id)) {
                Logger.d(
                    "Dibella_Cache",
                    "[${image.id}] Resource check already active, skipping duplicate call",
                )

                return@withContext
            }

            try {
                Logger.d(
                    "Dibella_Cache",
                    "[${image.id}] Sync Start (Type: ${image.type}, Force: $force)",
                )

                val favorite =
                    favoriteImageDao.getFavorite(image.id)
                        ?: run {
                            Logger.w("Dibella_Cache", "[${image.id}] Aborted: Item not found in DB")

                            return@withContext
                        }

                if (force) {
                    Logger.d("Dibella_IO", "│ [${image.id}] Force cleaning local files")

                    val thumbFile = File(favoritesDir, "${image.id}.jpg")
                    val fullFile = File(favoritesDir, "${image.id}_full.jpg")
                    val videoFile = File(favoritesDir, "${image.id}.mp4")

                    thumbFile.delete()
                    fullFile.delete()
                    videoFile.delete()

                    listOf("png", "webp", "gif", "avif", "webm", "mkv").forEach { ext ->
                        File(favoritesDir, "${image.id}.$ext").delete()
                        File(favoritesDir, "${image.id}_full.$ext").delete()
                    }
                }

                var thumbPath: String? = if (force) null else favorite.localPath
                var fullPath: String? = if (force) null else favorite.localFullImagePath
                var videoPath: String? = if (force) null else favorite.localVideoPath
                var updated = false
                var thumbProgress = 0f
                var contentProgress = 0f

                fun updateTotalProgress() {
                    onProgress((thumbProgress + contentProgress) / 2f)
                }

                kotlinx.coroutines.coroutineScope {
                    launch {
                        val currentThumbFile =
                            thumbPath?.let { File(it) } ?: File(favoritesDir, "${image.id}.jpg")

                        if (!FileUtils.isRealMedia(currentThumbFile)) {
                            val thumbUrl =
                                if (image.type == "video") getVideoThumbnailUrl(image.url)
                                else getThumbnailUrl(image.url, 450)

                            Logger.d(
                                "Dibella_Cache",
                                "│ [${image.id}] Thumbnail missing -> Initiating download",
                            )

                            val tempFile =
                                File(
                                    context.cacheDir,
                                    "temp_thumb_${image.id}_${System.currentTimeMillis()}",
                                )

                            var downloadSuccess =
                                downloadFile(thumbUrl, tempFile) { p ->
                                    thumbProgress = p
                                    updateTotalProgress()
                                }

                            if (image.type == "video" && !downloadSuccess) {
                                Logger.w(
                                    "Dibella_Net",
                                    "│ [${image.id}] Static thumbnail fallback -> Video Preview",
                                )

                                downloadSuccess =
                                    downloadFile(getVideoPreviewUrl(image.url), tempFile) { p ->
                                        thumbProgress = p
                                        updateTotalProgress()
                                    }
                            }

                            if (downloadSuccess) {
                                val bytes = tempFile.inputStream().use { it.readNBytes(12) }
                                val ext = FileUtils.getExtensionFromBytes(bytes)

                                if (
                                    ext != null && ext in setOf("png", "jpg", "webp", "gif", "avif")
                                ) {
                                    val finalFile = File(favoritesDir, "${image.id}.$ext")

                                    if (finalFile.exists()) finalFile.delete()

                                    tempFile.renameTo(finalFile)

                                    thumbPath = finalFile.absolutePath
                                    updated = true
                                } else if (ext != null && ext in setOf("mp4", "webm", "mkv")) {
                                    val frameFile = File(favoritesDir, "${image.id}.jpg")
                                    if (extractVideoFrame(tempFile, frameFile)) {
                                        thumbPath = frameFile.absolutePath
                                        updated = true
                                    }

                                    tempFile.delete()
                                } else {
                                    tempFile.delete()
                                }
                            } else {
                                if (tempFile.exists()) tempFile.delete()
                            }
                        } else {
                            Logger.v("Dibella_Cache", "│ [${image.id}] Thumbnail verified locally")

                            thumbProgress = 1f

                            updateTotalProgress()
                        }
                    }

                    launch {
                        if (image.type != "video") {
                            val currentFullFile =
                                fullPath?.let { File(it) }
                                    ?: File(favoritesDir, "${image.id}_full.jpg")

                            if (!FileUtils.isRealMedia(currentFullFile)) {
                                Logger.d(
                                    "Dibella_Image",
                                    "│ [${image.id}] Full image missing -> Downloading",
                                )

                                val tempFile =
                                    File(
                                        context.cacheDir,
                                        "temp_full_${image.id}_${System.currentTimeMillis()}",
                                    )

                                if (
                                    downloadFile(image.url, tempFile) { p ->
                                        contentProgress = p
                                        updateTotalProgress()
                                    }
                                ) {
                                    val bytes = tempFile.inputStream().use { it.readNBytes(12) }
                                    val ext = FileUtils.getExtensionFromBytes(bytes)

                                    if (
                                        ext != null &&
                                            ext in setOf("png", "jpg", "webp", "gif", "avif")
                                    ) {
                                        val finalFile = File(favoritesDir, "${image.id}_full.$ext")

                                        if (finalFile.exists()) finalFile.delete()

                                        tempFile.renameTo(finalFile)

                                        fullPath = finalFile.absolutePath
                                        updated = true
                                    } else {
                                        tempFile.delete()
                                    }
                                } else {
                                    if (tempFile.exists()) tempFile.delete()
                                }
                            } else {
                                contentProgress = 1f

                                updateTotalProgress()
                            }
                        } else {
                            val currentVideoFile =
                                videoPath?.let { File(it) } ?: File(favoritesDir, "${image.id}.mp4")

                            if (!FileUtils.isRealMedia(currentVideoFile)) {
                                Logger.d(
                                    "Dibella_Video",
                                    "│ [${image.id}] Video missing -> Downloading preview",
                                )

                                val tempFile =
                                    File(
                                        context.cacheDir,
                                        "temp_video_${image.id}_${System.currentTimeMillis()}",
                                    )

                                var success =
                                    downloadFile(getVideoPreviewUrl(image.url), tempFile) { p ->
                                        contentProgress = p
                                        updateTotalProgress()
                                    }

                                if (!success) {
                                    success =
                                        downloadFile(image.url, tempFile) { p ->
                                            contentProgress = p
                                            updateTotalProgress()
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
                                    } else {
                                        tempFile.delete()
                                    }
                                } else {
                                    if (tempFile.exists()) tempFile.delete()
                                }
                            } else {
                                contentProgress = 1f

                                updateTotalProgress()
                            }
                        }
                    }
                }

                if (updated) {
                    val updatedFavorite =
                        favorite.copy(
                            localPath = thumbPath,
                            localFullImagePath = fullPath,
                            localVideoPath = videoPath,
                        )

                    favoriteImageDao.insertFavorite(updatedFavorite)

                    Logger.d("Dibella_DB", "[${image.id}] Sync Success: Database entries updated")
                } else {
                    Logger.d("Dibella_Cache", "[${image.id}] Sync Finish: No changes needed")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                Logger.e("Dibella_Cache", "[${image.id}] Sync Error: ${e.message}")
            } finally {
                resourceChecksInProgress.remove(image.id)
            }
        }

    private suspend fun downloadFile(
        url: String,
        destination: File,
        onProgress: (Float) -> Unit = {},
    ): Boolean {
        val startTime = System.currentTimeMillis()
        val tempFile =
            File(context.cacheDir, "temp_${System.currentTimeMillis()}_${destination.name}")

        return try {
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body ?: return false
                    val length = body.contentLength()

                    Logger.d(
                        "Dibella_Net",
                        "GET 200 | [${destination.name}] Size: ${length / 1024}KB",
                    )

                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var total = 0L

                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)

                                total += read

                                if (length > 0) onProgress(total.toFloat() / length)
                            }
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 10) {
                        if (!FileUtils.isRealMedia(tempFile)) {
                            Logger.e(
                                "Dibella_Codec",
                                "[${destination.name}] Validation Failed: Invalid media headers",
                            )

                            return false
                        }

                        if (destination.exists()) destination.delete()

                        val success = tempFile.renameTo(destination)

                        if (success) {
                            Logger.d(
                                "Dibella_IO",
                                "[${destination.name}] Download saved in ${System.currentTimeMillis() - startTime}ms",
                            )

                            true
                        } else {
                            Logger.e(
                                "Dibella_IO",
                                "[${destination.name}] Rename Failed: Temp at ${tempFile.absolutePath}",
                            )

                            false
                        }
                    } else false
                } else {
                    Logger.e("Dibella_Net", "GET ${response.code} | URL: $url")

                    false
                }
            }
        } catch (e: Exception) {
            Logger.e("Dibella_Net", "Download Exception: ${e.message}")

            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun addFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            Logger.d("Dibella_Cache", "[${image.id}] addFavorite: Initializing entry")

            val initialFavorite = FavoriteImage.fromCivitaiImage(image, null, null, null)

            favoriteImageDao.insertFavorite(initialFavorite)
            repositoryScope.launch { ensureFavoriteResources(image) }
        }

    private suspend fun removeFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            Logger.d("Dibella_Cache", "[${image.id}] removeFavorite: Purging data")

            val favorite =
                favoriteImageDao.getFavorite(image.id) ?: FavoriteImage.fromCivitaiImage(image)

            favoriteImageDao.deleteFavorite(favorite)

            listOfNotNull(favorite.localPath, favorite.localFullImagePath, favorite.localVideoPath)
                .forEach { path -> File(path).let { if (it.exists()) it.delete() } }

            listOf("${image.id}.jpg", "${image.id}_full.jpg", "${image.id}.mp4").forEach { name ->
                File(favoritesDir, name).let { if (it.exists()) it.delete() }
            }

            Logger.d("Dibella_IO", "[${image.id}] Purge complete")
        }

    suspend fun clearUnusedResources(favoriteIds: Set<Long>) =
        withContext(Dispatchers.IO) {
            val files = favoritesDir.listFiles() ?: return@withContext

            Logger.d("Dibella_IO", "Scanning for unused resources (${files.size} files found)")

            files.forEach { file ->
                val idStr = file.nameWithoutExtension.removeSuffix("_full")
                val id = idStr.toLongOrNull()

                if (id != null && !favoriteIds.contains(id)) {
                    Logger.d("Dibella_IO", "Deleting orphaned file: ${file.name}")

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

            val duplicateFiles = internalCalculateDuplicates(fileToFavorite.keys.toList())

            duplicateFiles.map { group -> group.mapNotNull { fileToFavorite[it] } }
        }

    suspend fun removeDuplicates(duplicateGroups: List<List<CivitaiImage>>): Int =
        withContext(Dispatchers.IO) {
            var removedCount = 0

            for (group in duplicateGroups) {
                val groupWithTimestamps = group.map { image ->
                    val fav = favoriteImageDao.getFavorite(image.id)
                    val path = fav?.localVideoPath ?: fav?.localFullImagePath ?: fav?.localPath
                    val lastModified = path?.let { File(it).lastModified() } ?: Long.MAX_VALUE

                    image to lastModified
                }

                val sortedGroup = groupWithTimestamps.sortedBy { it.second }.map { it.first }
                val toRemove = sortedGroup.drop(1)

                for (image in toRemove) {
                    removeFavorite(image)

                    removedCount++
                }
            }

            Logger.d("Dibella_Cache", "Duplicate cleanup: Removed $removedCount entries")

            removedCount
        }

    private fun internalCalculateDuplicates(files: List<File>): List<List<File>> {
        val sizeGroups = files.groupBy { it.length() }.filter { it.value.size > 1 }
        val duplicates = mutableListOf<List<File>>()

        sizeGroups.forEach { (_, group) ->
            val validHashes = group.mapNotNull { file ->
                FileUtils.calculateHash(file)?.let { file to it }
            }

            validHashes
                .groupBy { it.second }
                .filter { it.value.size > 1 }
                .forEach { entry -> duplicates.add(entry.value.map { it.first }) }
        }

        return duplicates
    }

    suspend fun getAllFavoritesSync(): List<FavoriteImage> = favoriteImageDao.getAllFavoritesSync()

    suspend fun importFavorites(favorites: List<FavoriteImage>) {
        Logger.d("Dibella_DB", "Importing ${favorites.size} records")

        favoriteImageDao.insertFavorites(favorites)
    }

    fun isFavorite(id: Long): Flow<Boolean> = favoriteImageDao.isFavorite(id)
}
