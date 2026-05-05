package org.movzx.dibella.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.*
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex

object FileUtils {
    val IMAGE_EXTENSIONS = listOf("jpeg", "jpg", "webp")
    val VIDEO_EXTENSIONS = listOf("mp4", "webm")
    private val JPEG_HEADER = "FFD8FF".decodeHex()
    private val WEBP_HEADER = "52494646".decodeHex()

    fun saveBitmapAsWebP(bitmap: Bitmap, outputFile: File, quality: Int): Boolean {
        return try {
            outputFile.outputStream().use { out ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out)
                }
            }

            true
        } catch (e: Exception) {
            Logger.e("Dibella_Codec", "Failed to save bitmap as WebP: ${e.message}")

            false
        }
    }

    fun convertFileToWebP(inputFile: File, outputFile: File, quality: Int): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)

            if (bitmap != null) {
                val success = saveBitmapAsWebP(bitmap, outputFile, quality)

                bitmap.recycle()
                success
            } else {
                Logger.e(
                    "Dibella_Codec",
                    "Failed to decode bitmap for WebP conversion: ${inputFile.name}",
                )

                false
            }
        } catch (e: Exception) {
            Logger.e("Dibella_Codec", "WebP conversion failed for ${inputFile.name}: ${e.message}")

            false
        }
    }

    fun detectExtension(contentType: String?, source: BufferedSource, url: String): String {
        try {
            val peek = source.peek()

            if (peek.request(64)) {
                val bytes = peek.readByteArray(64)
                val sniffed = getExtensionFromBytes(bytes)

                if (sniffed != null) {
                    Logger.v("Dibella_Codec", "Extension sniffed via magic bytes: $sniffed")

                    return sniffed
                }
            }
        } catch (e: Exception) {
            Logger.e("Dibella_Codec", "Error sniffing headers: ${e.message}")
        }

        val mappedExt =
            when (contentType?.lowercase()) {
                "image/jpeg",
                "image/jpg" -> "jpg"
                "image/webp" -> "webp"
                "video/mp4" -> "mp4"
                "video/webm" -> "webm"
                else -> null
            }

        if (mappedExt != null) {
            Logger.v("Dibella_Codec", "Extension detected via Content-Type: $mappedExt")

            return mappedExt
        }

        val urlExt = url.substringAfterLast('.', "").lowercase()
        val validExts = setOf("jpeg", "jpg", "webp", "mp4", "webm")

        if (urlExt in validExts) {
            val result = if (urlExt == "jpeg") "jpg" else urlExt

            Logger.v("Dibella_Codec", "Extension detected via URL: $result")

            return result
        }

        Logger.w("Dibella_Codec", "Could not detect extension, defaulting to jpg")

        return "jpg"
    }

    fun getExtensionFromBytes(bytes: ByteArray): String? {
        if (bytes.size < 4) return null

        val hex = bytes.joinToString("") { "%02X".format(it) }.uppercase()

        return when {
            hex.startsWith("FFD8FF") -> "jpg"
            hex.startsWith("52494646") && hex.contains("57454250") -> "webp"
            hex.contains("66747970") -> "mp4"
            hex.startsWith("1A45DFA3") && hex.contains("7765626D") -> "webm"
            hex.startsWith("1A45DFA3") -> "mkv"
            else -> null
        }
    }

    fun isRealMedia(file: File): Boolean {
        if (!file.exists() || file.length() < 12) {
            Logger.w("Dibella_IO", "Media Check Failed: File too small or missing | ${file}")

            return false
        }

        return try {
            val bytes =
                FileInputStream(file).use { input ->
                    val buffer = ByteArray(64)

                    input.read(buffer)
                    buffer
                }

            if (bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()) {
                Logger.e(
                    "Dibella_Codec",
                    "Media Validation Error: File is actually JSON/API error | ${file}",
                )

                return false
            }

            val hasExtension = getExtensionFromBytes(bytes) != null

            if (!hasExtension) {
                Logger.e(
                    "Dibella_Codec",
                    "Media Validation Error: Unknown or invalid file headers | ${file}",
                )
            }

            hasExtension
        } catch (e: Exception) {
            Logger.e("Dibella_Codec", "Exception during media check: ${e.message}")

            false
        }
    }

    fun calculateHash(file: File): String? {
        if (!file.exists()) return null

        val startTime = System.currentTimeMillis()

        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)

            FileInputStream(file).use { input ->
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val hash = digest.digest().joinToString("") { "%02x".format(it) }

            Logger.v(
                "Dibella_IO",
                "Hash Calculated: ${file} | Duration: ${System.currentTimeMillis() - startTime}ms",
            )

            hash
        } catch (e: Exception) {
            Logger.e("Dibella_IO", "Hash Calculation Failed: ${e.message}")

            null
        }
    }

    suspend fun findDuplicateGroups(files: List<File>): List<List<File>> =
        withContext(Dispatchers.IO) {
            val sizeGroups = files.groupBy { it.length() }.filter { it.value.size > 1 }
            val duplicates = mutableListOf<List<File>>()

            sizeGroups.forEach { (_, group) ->
                coroutineScope {
                    val validHashes =
                        group
                            .map { file -> async { calculateHash(file)?.let { file to it } } }
                            .awaitAll()
                            .filterNotNull()

                    validHashes
                        .groupBy { it.second }
                        .filter { it.value.size > 1 }
                        .forEach { entry -> duplicates.add(entry.value.map { it.first }) }
                }
            }

            duplicates
        }
}
