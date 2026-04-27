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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.source
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.util.FileUtils
import org.movzx.dibella.util.Logger

@Singleton
class GalleryRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val preferencesRepository: UserPreferencesRepository,
) {
    private val videoMetadataDispatcher = Dispatchers.IO.limitedParallelism(16)
    private val _downloadedIds = kotlinx.coroutines.flow.MutableStateFlow<Set<Long>>(emptySet())
    val downloadedIds: kotlinx.coroutines.flow.StateFlow<Set<Long>> = _downloadedIds.asStateFlow()

    suspend fun refreshDownloadedIds() {
        val path = preferencesRepository.downloadPath.first()
        val rootDir = getDownloadDir(path)

        if (!rootDir.exists()) {
            _downloadedIds.value = emptySet()

            return
        }

        val files = rootDir.listFiles() ?: emptyArray()

        val ids =
            files
                .filter { it.isFile && it.name.startsWith("Dibella_") }
                .mapNotNull { file ->
                    file.nameWithoutExtension.removePrefix("Dibella_").toLongOrNull()
                }
                .toSet()

        _downloadedIds.value = ids
    }

    suspend fun scanDirectory(path: String?): List<CivitaiImage> =
        withContext(videoMetadataDispatcher) {
            val rootDir = getDownloadDir(path)

            if (!rootDir.exists()) return@withContext emptyList()

            val files = rootDir.listFiles() ?: emptyArray()
            val list = mutableListOf<CivitaiImage>()
            val ids = mutableSetOf<Long>()

            for (file in files) {
                if (file.isFile) {
                    val ext = file.extension.lowercase()
                    val isVideo = ext == "mp4"
                    val isImage = ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp"

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
                                Logger.e("GalleryRepo", "Error reading video meta: ${e.message}")
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

            _downloadedIds.value = ids
            list.sortedByDescending { File(it.url).lastModified() }
        }

    suspend fun deleteLocalFile(image: CivitaiImage): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val file = File(image.url)
                val result = file.exists() && file.delete()

                if (result) {
                    MediaScannerConnection.scanFile(context, arrayOf(image.url), null, null)
                    refreshDownloadedIds()
                }
                result
            } catch (e: Exception) {
                false
            }
        }

    suspend fun downloadImage(image: CivitaiImage, onProgress: (Float) -> Unit): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val downloadDir = getDownloadDir(preferencesRepository.downloadPath.first())

                if (!downloadDir.exists()) downloadDir.mkdirs()

                val request = Request.Builder().url(image.url).build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(Exception("HTTP ${response.code}"))

                    val body =
                        response.body ?: return@withContext Result.failure(Exception("Empty body"))

                    val contentLength = body.contentLength()
                    val source = body.source()

                    val ext =
                        FileUtils.detectExtension(
                            contentType = response.header("Content-Type"),
                            source = source,
                            url = image.url,
                        )

                    val file = File(downloadDir, "Dibella_${image.id}.$ext")

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

                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                    refreshDownloadedIds()
                    Result.success(file.absolutePath)
                }
            } catch (e: Exception) {
                Result.failure(e)
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
                        (it.extension.lowercase() in setOf("mp4", "jpg", "jpeg", "png", "webp"))
                } ?: return@withContext emptyList()

            val duplicateFiles = internalCalculateDuplicates(files)

            duplicateFiles.map { group ->
                group.map { file ->
                    val id =
                        if (file.name.startsWith("Dibella_")) {
                            file.nameWithoutExtension.removePrefix("Dibella_").toLongOrNull()
                                ?: file.absolutePath.hashCode().toLong()
                        } else {
                            file.nameWithoutExtension.filter { it.isDigit() }.toLongOrNull()
                                ?: file.absolutePath.hashCode().toLong()
                        }
                    CivitaiImage(
                        id = id,
                        url = file.absolutePath,
                        width = 0,
                        height = 0,
                        nsfw = false,
                        type = if (file.extension.lowercase() == "mp4") "video" else "image",
                        meta = null,
                    )
                }
            }
        }

    suspend fun removeDuplicates(duplicateGroups: List<List<CivitaiImage>>): Int =
        withContext(Dispatchers.IO) {
            var removedCount = 0

            duplicateGroups.forEach { group ->
                val sortedGroup = group.sortedBy { File(it.url).lastModified() }
                val toRemove = sortedGroup.drop(1)

                toRemove.forEach { image ->
                    if (File(image.url).delete()) {
                        MediaScannerConnection.scanFile(context, arrayOf(image.url), null, null)
                        removedCount++
                    }
                }
            }

            if (removedCount > 0) refreshDownloadedIds()

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

    private fun getDownloadDir(path: String?): File {
        return path?.let { File(it) }
            ?: File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Dibella",
            )
    }
}
