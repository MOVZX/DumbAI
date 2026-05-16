package org.movzx.dibella.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.source
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.util.CivitaiUrlBuilder
import org.movzx.dibella.util.DuplicateDetector
import org.movzx.dibella.util.FileUtils
import org.movzx.dibella.util.Logger

@Singleton
class GalleryRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val preferencesRepository: UserPreferencesRepository,
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val videoMetadataDispatcher = Dispatchers.IO.limitedParallelism(16)
    private val _downloadedIds = kotlinx.coroutines.flow.MutableStateFlow<Set<Long>>(emptySet())
    val downloadedIds: kotlinx.coroutines.flow.StateFlow<Set<Long>> = _downloadedIds.asStateFlow()
    private val refreshMutex = Mutex()
    private val downloadMutexes = java.util.concurrent.ConcurrentHashMap<Long, Mutex>()

    private fun getDownloadMutex(id: Long): Mutex {
        return downloadMutexes.getOrPut(id) { Mutex() }
    }

    suspend fun refreshDownloadedIds() {
        refreshMutex.withLock {
            val path = preferencesRepository.downloadPath.first()
            val rootDir = getDownloadDir(path)

            if (!rootDir.exists()) {
                _downloadedIds.value = emptySet()

                return@withLock
            }

            val files = rootDir.listFiles() ?: emptyArray()

            val ids =
                files
                    .filter { it.isFile && it.name.startsWith("Dibella_") }
                    .mapNotNull { file ->
                        file.nameWithoutExtension.removePrefix("Dibella_").toLongOrNull()
                    }
                    .toSet()

            Logger.d("Dibella_IO", "Refreshed Downloaded IDs: ${ids.size} found in ${rootDir.name}")

            _downloadedIds.value = ids
        }
    }

    suspend fun scanDirectory(path: String?): List<CivitaiImage> =
        withContext(videoMetadataDispatcher) {
            val rootDir = getDownloadDir(path)

            if (!rootDir.exists()) return@withContext emptyList()

            val files = rootDir.listFiles() ?: emptyArray()
            val list = mutableListOf<CivitaiImage>()
            val ids = mutableSetOf<Long>()

            for (file in files) {
                ensureActive()

                if (file.isFile) {
                    val ext = file.extension.lowercase()
                    val isVideo = ext in FileUtils.VIDEO_EXTENSIONS
                    val isImage = ext in FileUtils.IMAGE_EXTENSIONS

                    if (isImage || isVideo) {
                        val id =
                            if (file.name.startsWith("Dibella_")) {
                                file.nameWithoutExtension.removePrefix("Dibella_").toLongOrNull()
                                    ?: file.absolutePath.hashCode().toLong()
                            } else {
                                file.nameWithoutExtension.filter { it.isDigit() }.toLongOrNull()
                                    ?: file.absolutePath.hashCode().toLong()
                            }

                        if (file.name.startsWith("Dibella_")) {
                            file.nameWithoutExtension.removePrefix("Dibella_").toLongOrNull()?.let {
                                ids.add(it)
                            }
                        }

                        var w = 0
                        var h = 0

                        if (isImage) {
                            val options =
                                BitmapFactory.Options().apply { inJustDecodeBounds = true }

                            BitmapFactory.decodeFile(file.absolutePath, options)

                            w = options.outWidth
                            h = options.outHeight
                        } else if (isVideo) {
                            val retriever = MediaMetadataRetriever()
                            val metaStart = System.currentTimeMillis()

                            try {
                                retriever.setDataSource(file.absolutePath)

                                val rotation =
                                    retriever
                                        .extractMetadata(
                                            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                                        )
                                        ?.toIntOrNull() ?: 0

                                val vidW =
                                    retriever
                                        .extractMetadata(
                                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                                        )
                                        ?.toIntOrNull() ?: 0

                                val vidH =
                                    retriever
                                        .extractMetadata(
                                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                                        )
                                        ?.toIntOrNull() ?: 0

                                if (rotation == 90 || rotation == 270) {
                                    w = vidH
                                    h = vidW
                                } else {
                                    w = vidW
                                    h = vidH
                                }
                            } catch (e: Exception) {
                                Logger.e("Dibella_Codec", "Error reading video meta: ${e.message}")
                            } finally {
                                retriever.release()
                            }
                        }

                        list.add(
                            CivitaiImage(
                                id = id,
                                url = file.absolutePath,
                                width = w,
                                height = h,
                                nsfw = false,
                                type = if (isVideo) "video" else "image",
                                meta = null,
                            )
                        )
                    }
                }
            }

            refreshMutex.withLock { _downloadedIds.value = ids }

            list.sortedByDescending { File(it.url).lastModified() }
        }

    suspend fun deleteLocalFile(image: CivitaiImage): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val file = File(image.url)
                val result = file.exists() && file.delete()

                Logger.d(
                    "Dibella_IO",
                    "[${image.id}] Delete local file: $result | Path: ${image.url}",
                )

                if (result) {
                    MediaScannerConnection.scanFile(context, arrayOf(image.url), null, null)
                    _downloadedIds.update { it - image.id }
                }

                result
            } catch (e: Exception) {
                Logger.e("Dibella_IO", "[${image.id}] Delete Exception: ${e.message}")

                false
            }
        }

    suspend fun downloadImage(image: CivitaiImage, onProgress: (Float) -> Unit): Result<String> =
        withContext(Dispatchers.IO) {
            getDownloadMutex(image.id).withLock {
                val startTime = System.currentTimeMillis()
                val isVideo = image.type == "video"
                val urlsToTry = mutableListOf<String>()
                val expanded = CivitaiUrlBuilder.expandUrl(image.url, image.type)
                val uuid = CivitaiUrlBuilder.extractCivitaiUuid(expanded)

                if (isVideo) {
                    if (!CivitaiUrlBuilder.backendEnabled && uuid != null) {
                        urlsToTry.add(
                            "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/$uuid/original=true/$uuid.mp4"
                        )

                        urlsToTry.add(
                            "https://image-b2.civitai.com/file/civitai-media-cache/$uuid/original"
                        )

                        urlsToTry.add(
                            "https://image-b2.civitai.com/file/civitai-media-cache/$uuid/default"
                        )
                    } else {
                        urlsToTry.add(CivitaiUrlBuilder.getVideoOriginalUrl(expanded))
                    }
                } else {
                    urlsToTry.add(CivitaiUrlBuilder.getImageOriginalUrl(expanded))
                }

                if (!urlsToTry.contains(expanded)) urlsToTry.add(expanded)

                try {
                    val downloadDir = getDownloadDir(preferencesRepository.downloadPath.first())

                    if (!downloadDir.exists()) downloadDir.mkdirs()

                    var lastError: Exception? = null

                    for (url in urlsToTry) {
                        try {
                            val isBackendUrl =
                                CivitaiUrlBuilder.backendEnabled &&
                                    CivitaiUrlBuilder.backendUrl.isNotBlank() &&
                                    url.startsWith(CivitaiUrlBuilder.backendUrl.removeSuffix("/"))

                            val request = Request.Builder().url(url).build()

                            val callClient =
                                if (isBackendUrl) {
                                    okHttpClient
                                        .newBuilder()
                                        .callTimeout(
                                            CivitaiUrlBuilder.BACKEND_DOWNLOAD_TIMEOUT_SECONDS,
                                            java.util.concurrent.TimeUnit.SECONDS,
                                        )
                                        .build()
                                } else okHttpClient

                            if (isBackendUrl) {
                                Logger.d(
                                    "Dibella_Net",
                                    "[${image.id}] Backend download with ${CivitaiUrlBuilder.BACKEND_DOWNLOAD_TIMEOUT_SECONDS}s call timeout | URL: $url",
                                )
                            } else {
                                Logger.d(
                                    "Dibella_Net",
                                    "[${image.id}] Download attempt | URL: $url",
                                )
                            }

                            val result: String? =
                                callClient.newCall(request).execute().use { response ->
                                    if (!response.isSuccessful)
                                        throw Exception("HTTP ${response.code}")

                                    val body = response.body ?: throw Exception("Empty body")
                                    val source = body.source()

                                    val ext =
                                        FileUtils.detectExtension(
                                            contentType = response.header("Content-Type"),
                                            source = source,
                                            url = url,
                                        )

                                    val isActualVideo = FileUtils.VIDEO_EXTENSIONS.contains(ext)

                                    if (isVideo && !isActualVideo) {
                                        Logger.w(
                                            "Dibella_Net",
                                            "[${image.id}] Expected video but got $ext. Trying next fallback.",
                                        )

                                        return@use null
                                    }

                                    val file = File(downloadDir, "Dibella_${image.id}.$ext")
                                    val contentLength = body.contentLength()

                                    FileOutputStream(file).use { output ->
                                        val buffer = ByteArray(8192)
                                        var totalBytesRead = 0L
                                        var bytesRead: Int

                                        while (source.read(buffer).also { bytesRead = it } != -1) {
                                            output.write(buffer, 0, bytesRead)

                                            totalBytesRead += bytesRead

                                            if (contentLength > 0)
                                                onProgress(totalBytesRead.toFloat() / contentLength)
                                        }
                                    }

                                    MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(file.absolutePath),
                                        null,
                                        null,
                                    )
                                    _downloadedIds.update { it + image.id }

                                    Logger.d(
                                        "Dibella_IO",
                                        "[${image.id}] Download complete in ${System.currentTimeMillis() - startTime}ms | Path: $file",
                                    )

                                    file.absolutePath
                                }

                            if (result != null) return@withLock Result.success(result)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e

                            lastError = e

                            Logger.e("Dibella_Net", "[${image.id}] Fallback Failed: ${e.message}")
                        }
                    }

                    Result.failure(lastError ?: Exception("All download fallbacks failed"))
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Result.failure(e)
                }
            }
        }

    suspend fun findDuplicateGroups(): List<List<CivitaiImage>> =
        withContext(Dispatchers.IO) {
            val path = preferencesRepository.downloadPath.first()
            val rootDir = getDownloadDir(path)

            if (!rootDir.exists()) return@withContext emptyList()

            val files =
                rootDir.listFiles()?.filter {
                    it.isFile &&
                        (it.extension.lowercase() in
                            FileUtils.IMAGE_EXTENSIONS + FileUtils.VIDEO_EXTENSIONS)
                } ?: return@withContext emptyList()

            Logger.d("Dibella_IO", "Checking duplicates in ${rootDir.name} (${files.size} files)")

            DuplicateDetector.findDuplicateGroups(files, context)
        }

    suspend fun removeDuplicates(duplicateGroups: List<List<CivitaiImage>>): Int =
        withContext(Dispatchers.IO) {
            DuplicateDetector.removeDuplicateGroups(duplicateGroups, context) {
                refreshDownloadedIds()
            }
        }

    private fun getDownloadDir(path: String?): File {
        return path?.let { File(it) }
            ?: File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Dibella",
            )
    }
}
