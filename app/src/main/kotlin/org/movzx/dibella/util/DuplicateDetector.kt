package org.movzx.dibella.util

import android.content.Context
import android.media.MediaScannerConnection
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.movzx.dibella.model.CivitaiImage

object DuplicateDetector {
    suspend fun findDuplicateGroups(files: List<File>, context: Context): List<List<CivitaiImage>> =
        withContext(Dispatchers.IO) {
            val sizeGroups = files.groupBy { it.length() }.filter { it.value.size > 1 }
            val duplicates = mutableListOf<List<CivitaiImage>>()

            for ((_, group) in sizeGroups) {
                val fileToHash = group.mapNotNull { file ->
                    FileUtils.calculateHash(file)?.let { file to it }
                }

                for (entry in fileToHash.groupBy { it.second }) {
                    if (entry.value.size > 1) {
                        duplicates.add(
                            entry.value.map { (file, _) ->
                                CivitaiImage(
                                    id = extractId(file),
                                    url = file.absolutePath,
                                    width = 0,
                                    height = 0,
                                    nsfw = false,
                                    type =
                                        if (
                                            file.extension.lowercase() in FileUtils.VIDEO_EXTENSIONS
                                        )
                                            "video"
                                        else "image",
                                    meta = null,
                                )
                            }
                        )
                    }
                }
            }

            duplicates
        }

    suspend fun removeDuplicateGroups(
        duplicateGroups: List<List<CivitaiImage>>,
        context: Context,
        onRefreshDownloadedIds: suspend () -> Unit,
    ): Int =
        withContext(Dispatchers.IO) {
            var removedCount = 0

            for (group in duplicateGroups) {
                val sortedGroup = group.sortedBy { File(it.url).lastModified() }
                val toRemove = sortedGroup.drop(1)

                for (image in toRemove) {
                    val file = File(image.url)

                    if (file.delete()) {
                        MediaScannerConnection.scanFile(context, arrayOf(image.url), null, null)
                        removedCount++
                    }
                }
            }

            if (removedCount > 0) onRefreshDownloadedIds()

            removedCount
        }

    private fun extractId(file: File): Long {
        return if (file.name.startsWith("Dibella_")) {
            file.nameWithoutExtension.removePrefix("Dibella_").toLongOrNull()
                ?: file.absolutePath.hashCode().toLong()
        } else {
            file.nameWithoutExtension.filter { it.isDigit() }.toLongOrNull()
                ?: file.absolutePath.hashCode().toLong()
        }
    }
}
