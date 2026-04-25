package org.movzx.dumbai.util

import okio.BufferedSource
import okio.ByteString.Companion.decodeHex

object FileUtils {
    private val PNG_HEADER = "89504E47".decodeHex()
    private val JPEG_HEADER = "FFD8FF".decodeHex()
    private val WEBP_HEADER = "52494646".decodeHex()
    private val GIF_HEADER = "47494638".decodeHex()
    private val EBML_HEADER = "1A45DFA3".decodeHex()

    fun detectExtension(contentType: String?, source: BufferedSource, url: String): String {
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
        if (mappedExt != null) return mappedExt

        try {
            val peek = source.peek()
            if (peek.rangeEquals(0, PNG_HEADER)) return "png"
            if (peek.rangeEquals(0, JPEG_HEADER)) return "jpg"
            if (peek.rangeEquals(0, WEBP_HEADER)) return "webp"
            if (peek.rangeEquals(0, GIF_HEADER)) return "gif"

            if (peek.rangeEquals(0, EBML_HEADER)) {
                if (peek.request(64)) {
                    val data = peek.readByteString(64).toString()

                    return if (data.contains("webm")) "webm" else "mkv"
                }

                return "webm"
            }

            if (peek.request(12)) {
                val ftyp = peek.readByteString(12).toString()

                if (ftyp.contains("avif")) return "avif"
                if (ftyp.contains("ftyp")) return "mp4"
            }
        } catch (e: Exception) {}

        val urlExt = url.substringAfterLast('.', "").lowercase()
        val validExts = setOf("png", "jpg", "jpeg", "webp", "gif", "avif", "mp4", "webm", "mkv")

        if (urlExt in validExts) return if (urlExt == "jpeg") "jpg" else urlExt

        return "jpg"
    }
}
