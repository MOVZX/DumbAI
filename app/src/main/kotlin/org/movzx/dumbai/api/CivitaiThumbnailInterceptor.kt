package org.movzx.dumbai.api

import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dumbai.util.Logger

class CivitaiThumbnailInterceptor : Interceptor {
    private val fallbackWidths = listOf(450, 720, 1280)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        var response = chain.proceed(request)

        if (url.contains("image.civitai.com")) {
            if (response.isSuccessful && isRealImage(response)) return response

            val isDebug = true
            val isVideoThumbnail = url.contains("/original=false/")

            for (width in fallbackWidths) {
                val newUrl = getThumbnailUrl(url, width)

                if (newUrl == url) continue

                response.close()

                val newRequest = request.newBuilder().url(newUrl).build()

                if (isDebug) Logger.d("DumbAI_Net", "Retrying with width $width: $newUrl")

                response = chain.proceed(newRequest)

                if (response.isSuccessful && isRealImage(response)) return response
            }

            if (
                (!response.isSuccessful || !isRealImage(response)) &&
                    !url.contains("original=true") &&
                    !isVideoThumbnail
            ) {
                val originalUrl = getOriginalUrl(url)

                if (originalUrl != url) {
                    response.close()

                    val originalRequest = request.newBuilder().url(originalUrl).build()

                    if (isDebug) Logger.d("DumbAI_Net", "Final retry with original: $originalUrl")

                    response = chain.proceed(originalRequest)
                }
            }
        }

        return response
    }

    private fun isRealImage(response: Response): Boolean {
        val body = response.body ?: return false

        if (body.contentLength() == 0L) return false

        return try {
            val source = body.source()

            if (!source.request(12)) return false

            val peek = source.peek()
            val bytes = peek.readByteArray(12)

            if (bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()) return false

            val hex = bytes.joinToString("") { "%02X".format(it) }

            val hasHeader =
                hex.startsWith("89504E47") ||
                    hex.startsWith("FFD8FF") ||
                    hex.startsWith("52494646") ||
                    hex.startsWith("47494638") ||
                    hex.contains("61766966") ||
                    hex.contains("66747970") ||
                    hex.startsWith("1A45DFA3")

            hasHeader
        } catch (e: Exception) {
            false
        }
    }

    private fun getThumbnailUrl(url: String, width: Int): String {
        return when {
            url.contains("/original=true/") -> url.replace("/original=true/", "/width=$width/")
            url.contains("/original=false/") -> url.replace("/original=false/", "/width=$width/")
            url.contains(Regex("/width=\\d+/")) ->
                url.replace(Regex("/width=\\d+/"), "/width=$width/")
            else -> {
                val lastSlashIndex = url.lastIndexOf('/')

                if (lastSlashIndex != -1) {
                    val prefix = url.substring(0, lastSlashIndex)
                    val fileName = url.substring(lastSlashIndex + 1)

                    "$prefix/width=$width/$fileName"
                } else url
            }
        }
    }

    private fun getOriginalUrl(url: String): String {
        return when {
            url.contains(Regex("/width=\\d+/")) ->
                url.replace(Regex("/width=\\d+/"), "/original=true/")
            url.contains("/original=false/") -> url.replace("/original=false/", "/original=true/")
            else -> url
        }
    }
}
