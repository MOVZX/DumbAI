package org.movzx.dibella.api

import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dibella.util.Logger
import org.movzx.dibella.util.getOriginalUrl
import org.movzx.dibella.util.getThumbnailUrl

class CivitaiThumbnailInterceptor : Interceptor {
    private val fallbackWidths = listOf(450, 720, 1280)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        var response = chain.proceed(request)

        if (url.contains("image.civitai.com")) {
            if (response.isSuccessful && isRealImage(response)) return response

            val isVideoThumbnail = url.contains("/original=false/")
            val isVideo = url.contains("anim=false") || url.contains("transcode=true")

            for (width in fallbackWidths) {
                val newUrl = getThumbnailUrl(url, width)

                if (newUrl == url) continue

                response.close()

                val newRequest = request.newBuilder().url(newUrl).build()

                Logger.d("Dibella_Net", "Retrying with width $width: $newUrl")

                response = chain.proceed(newRequest)

                if (response.isSuccessful && isRealImage(response)) return response
            }

            if (
                (!response.isSuccessful || !isRealImage(response)) &&
                    isVideo &&
                    !url.contains("original=true")
            ) {
                val baseUrl =
                    if (url.contains("/original=true/")) url.substringBefore("/original=true/")
                    else url.substringBeforeLast("/")

                if (baseUrl != url) {
                    response.close()

                    val originalRequest = request.newBuilder().url(baseUrl).build()

                    Logger.d(
                        "Dibella_Net",
                        "Video thumbnail failed, trying original video URL for frame extraction: $baseUrl",
                    )

                    response = chain.proceed(originalRequest)
                }
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

                    Logger.d("Dibella_Net", "Final retry with original: $originalUrl")

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

            org.movzx.dibella.util.FileUtils.getExtensionFromBytes(bytes) != null
        } catch (e: Exception) {
            false
        }
    }
}
