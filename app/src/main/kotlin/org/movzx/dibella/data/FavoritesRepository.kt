package org.movzx.dibella.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
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

private const val TYPE_IMAGE = "image"
private const val TYPE_VIDEO = "video"
private const val TYPE_THUMBNAILS = "thumbnails"
private const val TYPE_PREVIEWS = "previews"

@Singleton
class FavoritesRepository(
    private val context: Context,
    private val favoriteImageDao: FavoriteImageDao,
    private val preferencesRepository: UserPreferencesRepository,
    okHttpClient: OkHttpClient,
    private val imageLoader: coil3.ImageLoader,
) {
    private val resourceChecksInProgress = ConcurrentHashMap.newKeySet<Long>()
    private val toggleMutexes = ConcurrentHashMap<Long, Mutex>()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var _okHttpClient: OkHttpClient = okHttpClient
        private set

    private fun getToggleMutex(id: Long) = toggleMutexes.getOrPut(id) { Mutex() }

    init {
        repositoryScope.launch { cleanupTempFiles() }
    }

    private fun cleanupTempFiles() {
        try {
            val cacheDir = context.cacheDir

            val tempFiles = cacheDir.listFiles { _, name ->
                name.startsWith("temp_thumb_") ||
                    name.startsWith("temp_full_") ||
                    name.startsWith("temp_video_")
            }

            tempFiles?.forEach {
                if (System.currentTimeMillis() - it.lastModified() > 3600000) {
                    it.delete()

                    Logger.d("Dibella_IO", "Cleaned up orphaned temp file: ${it.name}")
                }
            }
        } catch (e: Exception) {
            Logger.e("Dibella_IO", "Temp cleanup failed: ${e.message}")
        }
    }

    suspend fun repairSync(onProgress: (Int, Int) -> Unit = { _, _ -> }) =
        withContext(Dispatchers.IO) {
            val unsynced = favoriteImageDao.getAllFavoritesSync().filter { it.isSynced != true }

            Logger.d("Dibella_Cache", "Repair Sync: Found ${unsynced.size} items to check")

            val total = unsynced.size
            var completed = 0
            val mutex = Mutex()
            val semaphore = Semaphore(15)

            coroutineScope {
                unsynced.forEach { favorite ->
                    launch {
                        semaphore.withPermit {
                            ensureFavoriteResources(favorite.toCivitaiImage())
                            mutex.withLock {
                                completed++
                                onProgress(completed, total)
                            }
                        }
                    }
                }
            }
        }

    private suspend fun getFavoritesDir(): File {
        val path = preferencesRepository.effectiveFavoritesPath.first()
        val baseDir = File(path)

        if (!baseDir.exists()) {
            baseDir.mkdirs()

            try {
                File(baseDir, ".nomedia").createNewFile()
            } catch (e: Exception) {
                Logger.e("Dibella_IO", "Failed to create .nomedia: ${e.message}")
            }
        }

        return baseDir
    }

    private suspend fun getMediaDir(type: String, contentType: String): File {
        val base = getFavoritesDir()
        val mediaSub = if (type == TYPE_VIDEO) TYPE_VIDEO else TYPE_IMAGE
        val contentSub = if (contentType == "preview") TYPE_PREVIEWS else TYPE_THUMBNAILS
        val dir = File(File(base, mediaSub), contentSub)

        if (!dir.exists()) dir.mkdirs()

        return dir
    }

    fun updateOkHttpClient(newClient: OkHttpClient) {
        _okHttpClient = newClient
    }

    val allFavorites: Flow<List<CivitaiImage>> =
        favoriteImageDao.getAllFavorites().map { list -> list.map { it.toCivitaiImage() } }

    val favoriteIds: Flow<Set<Long>> = favoriteImageDao.getAllFavoriteIds().map { it.toSet() }

    fun getFavoriteFlow(id: Long): Flow<FavoriteImage?> = favoriteImageDao.getFavoriteFlow(id)

    suspend fun manualFetch(url: String): Boolean =
        withContext(Dispatchers.IO) {
            val request = okhttp3.Request.Builder().url(url).build()

            try {
                _okHttpClient.newCall(request).execute().use { response ->
                    Logger.d("Dibella_Net", "Manual fetch result for $url: ${response.code}")

                    response.isSuccessful
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                Logger.e("Dibella_Net", "Manual fetch failed for $url: ${e.message}")

                false
            }
        }

    suspend fun toggleFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            getToggleMutex(image.id).withLock {
                val isFav = favoriteImageDao.isFavoriteDirect(image.id)

                Logger.d("Dibella_Cache", "[${image.id}] toggleFavorite: currently isFav=$isFav")

                if (isFav) removeFavorite(image) else addFavorite(image)
            }

            toggleMutexes.remove(image.id)
        }

    private suspend fun evictFromCoilCache(url: String) {
        try {
            val diskCache = imageLoader.diskCache

            diskCache?.remove(url)

            Logger.d("Dibella_IO", "Evicted from Coil cache: $url")
        } catch (e: Exception) {
            Logger.w("Dibella_IO", "Failed to evict from Coil cache: ${e.message}")
        }
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
            } catch (e: Exception) {
                Logger.w(
                    "Dibella_Codec",
                    "[${videoFile.name}] Failed to release MediaMetadataRetriever: ${e.message}",
                )
            }
        }
    }

    private fun finalizeFile(tempFile: File, finalFile: File): String? {
        if (!tempFile.exists()) return null

        return if (tempFile.renameTo(finalFile)) {
            finalFile.absolutePath
        } else {
            if (copyFile(tempFile, finalFile)) {
                tempFile.delete()
                finalFile.absolutePath
            } else {
                null
            }
        }
    }

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

                val thumbDir = getMediaDir(image.type ?: TYPE_IMAGE, "thumbnail")
                val previewDir = getMediaDir(image.type ?: TYPE_IMAGE, "preview")

                val thumbBaseName =
                    if (image.type == TYPE_VIDEO) "${image.id}_thumb" else "${image.id}"

                if (force) {
                    Logger.d("Dibella_IO", "│ [${image.id}] Force cleaning local files")

                    (FileUtils.IMAGE_EXTENSIONS + FileUtils.VIDEO_EXTENSIONS).forEach { ext ->
                        File(thumbDir, "$thumbBaseName.$ext").delete()
                        File(previewDir, "${image.id}_full.$ext").delete()
                        File(previewDir, "${image.id}.$ext").delete()
                    }
                }

                var updated = false
                var thumbProgress = 0f
                var contentProgress = 0f

                fun updateTotalProgress() {
                    onProgress((thumbProgress + contentProgress) / 2f)
                }

                var videoFound = false
                var fullFound = false
                var thumbFound = false

                if (image.type == TYPE_VIDEO) {
                    val existingVideo =
                        FileUtils.VIDEO_EXTENSIONS.map { extension ->
                                File(previewDir, "${image.id}.$extension")
                            }
                            .firstOrNull { FileUtils.isRealMedia(it) }

                    if (existingVideo == null) {
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
                            val bytes = tempFile.inputStream().use { it.readNBytes(64) }
                            val ext = FileUtils.getExtensionFromBytes(bytes) ?: "mp4"
                            val finalFile = File(previewDir, "${image.id}.$ext")

                            if (finalizeFile(tempFile, finalFile) != null) {
                                videoFound = true
                                updated = true
                            }
                        } else {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    } else {
                        videoFound = true
                        contentProgress = 1f

                        updateTotalProgress()
                    }
                } else {
                    val existingFull =
                        FileUtils.IMAGE_EXTENSIONS.map { extension ->
                                File(previewDir, "${image.id}_full.$extension")
                            }
                            .firstOrNull { FileUtils.isRealMedia(it) }

                    if (existingFull == null) {
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
                            val bytes = tempFile.inputStream().use { it.readNBytes(64) }
                            val ext = FileUtils.getExtensionFromBytes(bytes) ?: "jpg"
                            val finalFile = File(previewDir, "${image.id}_full.$ext")

                            if (finalizeFile(tempFile, finalFile) != null) {
                                fullFound = true
                                updated = true
                            }
                        } else {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    } else {
                        fullFound = true
                        contentProgress = 1f

                        updateTotalProgress()
                    }
                }

                val existingThumb =
                    (FileUtils.IMAGE_EXTENSIONS.map { extension ->
                            File(thumbDir, "$thumbBaseName.$extension")
                        } +
                            FileUtils.IMAGE_EXTENSIONS.map { extension ->
                                File(thumbDir, "${image.id}.$extension")
                            })
                        .firstOrNull { FileUtils.isRealMedia(it) }

                if (existingThumb == null) {
                    var thumbResolved = false
                    val finalJpgFile = File(thumbDir, "$thumbBaseName.jpg")

                    if (image.type == TYPE_VIDEO && videoFound) {
                        val videoFile =
                            FileUtils.VIDEO_EXTENSIONS.map { extension ->
                                    File(previewDir, "${image.id}.$extension")
                                }
                                .firstOrNull { it.exists() }

                        if (videoFile != null) {
                            Logger.d(
                                "Dibella_Codec",
                                "│ [${image.id}] Extracting thumbnail from local video preview",
                            )

                            if (extractVideoFrame(videoFile, finalJpgFile)) {
                                updated = true
                                thumbResolved = true
                                thumbFound = true
                                thumbProgress = 1f

                                updateTotalProgress()
                            }
                        }
                    }

                    if (!thumbResolved) {
                        val thumbUrl =
                            if (image.type == TYPE_VIDEO) getVideoThumbnailUrl(image.url)
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
                            downloadFile(thumbUrl, tempFile, extractFrame = true) { p ->
                                thumbProgress = p

                                updateTotalProgress()
                            }

                        if (image.type == TYPE_VIDEO && !downloadSuccess) {
                            Logger.w(
                                "Dibella_Net",
                                "│ [${image.id}] Static thumbnail fallback -> Video Preview",
                            )

                            downloadSuccess =
                                downloadFile(
                                    getVideoPreviewUrl(image.url),
                                    tempFile,
                                    extractFrame = true,
                                ) { p ->
                                    thumbProgress = p

                                    updateTotalProgress()
                                }
                        }

                        if (downloadSuccess) {
                            val bytes = tempFile.inputStream().use { it.readNBytes(64) }
                            val ext = FileUtils.getExtensionFromBytes(bytes) ?: "jpg"
                            val actualFinalFile = File(thumbDir, "$thumbBaseName.$ext")

                            if (finalizeFile(tempFile, actualFinalFile) != null) {
                                thumbFound = true
                                updated = true
                            }
                        } else {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }
                } else {
                    Logger.v("Dibella_Cache", "│ [${image.id}] Thumbnail verified locally")

                    thumbFound = true
                    thumbProgress = 1f

                    updateTotalProgress()
                }

                if (updated || favorite.isSynced != true) {
                    val isFullySynced =
                        (image.type == TYPE_VIDEO && videoFound && thumbFound) ||
                            (image.type != TYPE_VIDEO && fullFound && thumbFound)

                    if (isFullySynced) {
                        val updatedFavorite = favorite.copy(isSynced = true)

                        favoriteImageDao.insertFavorite(updatedFavorite)

                        val thumbUrl =
                            if (image.type == TYPE_VIDEO) getVideoThumbnailUrl(image.url)
                            else getThumbnailUrl(image.url, 450)

                        evictFromCoilCache(thumbUrl)

                        if (image.type == TYPE_VIDEO)
                            evictFromCoilCache(getVideoPreviewUrl(image.url))

                        evictFromCoilCache(image.url)

                        Logger.d(
                            "Dibella_DB",
                            "[${image.id}] Sync Success: Database isSynced flag set",
                        )
                    }
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

    private fun copyFile(source: File, destination: File): Boolean {
        return try {
            destination.outputStream().use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            }

            Logger.d("Dibella_IO", "Copy succeeded: ${source.name} -> ${destination.name}")

            true
        } catch (e: Exception) {
            Logger.e("Dibella_IO", "Copy failed: ${e.message}")

            false
        }
    }

    private suspend fun downloadFile(
        url: String,
        destination: File,
        extractFrame: Boolean = false,
        onProgress: (Float) -> Unit = {},
    ): Boolean {
        val startTime = System.currentTimeMillis()
        val tempFile =
            File(context.cacheDir, "temp_${System.currentTimeMillis()}_${destination.name}")

        return try {
            val request = Request.Builder().url(url).build()

            _okHttpClient.newCall(request).execute().use { response ->
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
                        if (destination.exists()) destination.delete()

                        val finalSourceFile =
                            if (extractFrame) {
                                val bytes = tempFile.inputStream().use { it.readNBytes(64) }
                                val ext = FileUtils.getExtensionFromBytes(bytes)

                                if (ext == "mp4" || ext == "webm" || ext == "mkv") {
                                    Logger.d(
                                        "Dibella_Codec",
                                        "│ [${destination.name}] Video detected for thumbnail, extracting frame",
                                    )

                                    val frameFile =
                                        File(
                                            context.cacheDir,
                                            "frame_${System.currentTimeMillis()}_${destination.name}.jpg",
                                        )

                                    if (extractVideoFrame(tempFile, frameFile)) {
                                        tempFile.delete()
                                        frameFile
                                    } else {
                                        tempFile
                                    }
                                } else {
                                    tempFile
                                }
                            } else {
                                if (!FileUtils.isRealMedia(tempFile)) {
                                    Logger.e(
                                        "Dibella_Codec",
                                        "[${destination.name}] Validation Failed: Invalid media headers",
                                    )

                                    return false
                                }

                                tempFile
                            }

                        val success =
                            if (finalSourceFile.renameTo(destination)) {
                                true
                            } else {
                                Logger.d(
                                    "Dibella_IO",
                                    "[${destination.name}] Rename Failed (likely cross-mount), attempting copy",
                                )

                                copyFile(finalSourceFile, destination).also {
                                    if (it) finalSourceFile.delete()
                                }
                            }

                        if (success) {
                            Logger.d(
                                "Dibella_IO",
                                "[${destination.name}] Download saved in ${System.currentTimeMillis() - startTime}ms",
                            )
                            true
                        } else {
                            if (finalSourceFile.exists()) finalSourceFile.delete()

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

            val initialFavorite = FavoriteImage.fromCivitaiImage(image, isSynced = false)

            favoriteImageDao.insertFavorite(initialFavorite)
            repositoryScope.launch { ensureFavoriteResources(image) }
        }

    private suspend fun removeFavorite(image: CivitaiImage) =
        withContext(Dispatchers.IO) {
            Logger.d("Dibella_Cache", "[${image.id}] removeFavorite: Purging data")

            val favorite =
                favoriteImageDao.getFavorite(image.id) ?: FavoriteImage.fromCivitaiImage(image)

            favoriteImageDao.deleteFavorite(favorite)

            val thumbDir = getMediaDir(image.type ?: TYPE_IMAGE, "thumbnail")
            val previewDir = getMediaDir(image.type ?: TYPE_IMAGE, "preview")
            val thumbBaseName = if (image.type == TYPE_VIDEO) "${image.id}_thumb" else "${image.id}"

            FileUtils.IMAGE_EXTENSIONS.forEach { ext ->
                File(thumbDir, "$thumbBaseName.$ext").let { if (it.exists()) it.delete() }
                File(previewDir, "${image.id}_full.$ext").let { if (it.exists()) it.delete() }
            }

            FileUtils.VIDEO_EXTENSIONS.forEach { ext ->
                File(previewDir, "${image.id}.$ext").let { if (it.exists()) it.delete() }
            }

            Logger.d("Dibella_IO", "[${image.id}] Purge complete")
        }

    suspend fun clearUnusedResources(favoriteIds: Set<Long>) =
        withContext(Dispatchers.IO) {
            val base = getFavoritesDir()
            val dirsToScan = mutableListOf<File>()

            for (subDir in
                listOf(
                    "image/thumbnails",
                    "image/previews",
                    "video/thumbnails",
                    "video/previews",
                )) {
                dirsToScan.addAll(File(base, subDir).listFiles() ?: emptyArray())
            }

            val legacy = base.listFiles()?.filter { it.isFile } ?: emptyList()
            val files = dirsToScan + legacy

            Logger.d("Dibella_IO", "Scanning for unused resources (${files.size} files found)")

            files.forEach { file ->
                val idStr = file.nameWithoutExtension.removeSuffix("_full").removeSuffix("_thumb")
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
            val base = getFavoritesDir()

            favorites.forEach { fav ->
                val mediaSub = if (fav.type == TYPE_VIDEO) TYPE_VIDEO else TYPE_IMAGE
                val previewDir = File(File(base, mediaSub), TYPE_PREVIEWS)

                val extensions =
                    if (fav.type == TYPE_VIDEO) listOf("mp4", "webm", "mkv")
                    else listOf("jpg", "png", "webp", "gif", "avif")

                val baseName = if (fav.type == TYPE_VIDEO) "${fav.id}" else "${fav.id}_full"

                val mainFile =
                    extensions
                        .map { File(previewDir, "$baseName.$it") }
                        .firstOrNull { FileUtils.isRealMedia(it) }

                if (mainFile != null) fileToFavorite[mainFile] = fav.toCivitaiImage()
            }

            val duplicateFiles = FileUtils.findDuplicateGroups(fileToFavorite.keys.toList())

            duplicateFiles.map { group -> group.mapNotNull { fileToFavorite[it] } }
        }

    suspend fun removeDuplicates(duplicateGroups: List<List<CivitaiImage>>): Int =
        withContext(Dispatchers.IO) {
            var removedCount = 0
            val base = getFavoritesDir()

            for (group in duplicateGroups) {
                val groupWithTimestamps = group.map { image ->
                    val mediaSub = if (image.type == TYPE_VIDEO) TYPE_VIDEO else TYPE_IMAGE
                    val previewDir = File(File(base, mediaSub), TYPE_PREVIEWS)

                    val extensions =
                        if (image.type == TYPE_VIDEO) listOf("mp4", "webm", "mkv")
                        else listOf("jpg", "png", "webp", "gif", "avif")

                    val baseName =
                        if (image.type == TYPE_VIDEO) "${image.id}" else "${image.id}_full"

                    val path =
                        extensions
                            .map { File(previewDir, "$baseName.$it") }
                            .firstOrNull { it.exists() }
                            ?.absolutePath

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

    suspend fun getAllFavoritesSync(): List<FavoriteImage> = favoriteImageDao.getAllFavoritesSync()

    suspend fun importFavorites(favorites: List<FavoriteImage>) {
        Logger.d("Dibella_DB", "Importing ${favorites.size} records")

        favoriteImageDao.insertFavorites(favorites)
    }

    fun isFavorite(id: Long): Flow<Boolean> = favoriteImageDao.isFavorite(id)
}
