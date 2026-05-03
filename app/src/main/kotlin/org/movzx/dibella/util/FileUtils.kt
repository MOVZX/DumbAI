package org.movzx.dibella.util

import java.io.File
import java.io.FileInputStream
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex

object FileUtils {
    val IMAGE_EXTENSIONS = listOf("jpg", "png", "webp", "gif", "avif")
    val VIDEO_EXTENSIONS = listOf("mp4", "webm", "mkv")
    private val PNG_HEADER = "89504E47".decodeHex()
    private val JPEG_HEADER = "FFD8FF".decodeHex()
    private val WEBP_HEADER = "52494646".decodeHex()
    private val GIF_HEADER = "47494638".decodeHex()
    private val EBML_HEADER = "1A45DFA3".decodeHex()
    private val AVIF_HEADER = "61766966".decodeHex()

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
                "image/png" -> "png"
                "image/jpeg",
                "image/jpg" -> "jpg"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                "image/avif" -> "avif"
                "video/mp4" -> "mp4"
                "video/webm" -> "webm"
                "video/x-matroska" -> "mkv"
                else -> null
            }

        if (mappedExt != null) {
            Logger.v("Dibella_Codec", "Extension detected via Content-Type: $mappedExt")

            return mappedExt
        }

        val urlExt = url.substringAfterLast('.', "").lowercase()
        val validExts = setOf("png", "jpg", "jpeg", "webp", "gif", "avif", "mp4", "webm", "mkv")

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
            hex.startsWith("89504E47") -> "png"
            hex.startsWith("47494638") -> "gif"
            hex.startsWith("52494646") && hex.contains("57454250") -> "webp"
            hex.startsWith("1A45DFA3") -> {
                if (hex.contains("7765626D")) "webm" else "mkv"
            }
            hex.contains("66747970") && hex.contains("61766966") -> "avif"
            hex.contains("66747970") -> "mp4"
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

    fun findDuplicateGroups(files: List<File>): List<List<File>> {
        val sizeGroups = files.groupBy { it.length() }.filter { it.value.size > 1 }
        val duplicates = mutableListOf<List<File>>()

        sizeGroups.forEach { (_, group) ->
            val validHashes = group.mapNotNull { file -> calculateHash(file)?.let { file to it } }

            validHashes
                .groupBy { it.second }
                .filter { it.value.size > 1 }
                .forEach { entry -> duplicates.add(entry.value.map { it.first }) }
        }

        return duplicates
    }
}
