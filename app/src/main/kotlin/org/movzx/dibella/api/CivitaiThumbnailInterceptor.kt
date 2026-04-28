package org.movzx.dibella.api

import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dibella.util.Logger
import org.movzx.dibella.util.getOriginalUrl
import org.movzx.dibella.util.getThumbnailUrl

class CivitaiThumbnailInterceptor : Interceptor {
    private val fallbackWidths = listOf(800, 1000, 1500)
    private val videoThumbnailTimeout = 8_000L
    private val imageThumbnailTimeout = 10_000L
    private val maxRetries = 3

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        var response = chain.proceed(request)

        if (!url.contains("image.civitai.com")) return response
        if (response.isSuccessful && isRealImage(response)) return response

        val isVideoThumbnail = url.contains("/original=false/")
        val isVideo = url.contains("anim=false") || url.contains("transcode=true")
        var retryCount = 0

        fun tryUrlWithRetry(testUrl: String, isFallback: Boolean = false): Response? {
            if (retryCount >= maxRetries) {
                Logger.w("Dibella_Net", "Max retries ($maxRetries) reached for: $testUrl")

                return null
            }

            if (testUrl == url) return null

            val previousResponse = response

            try {
                previousResponse?.close()

                val newRequest = request.newBuilder().url(testUrl).build()
                val attemptType = if (isFallback) "fallback" else "retry"

                Logger.d(
                    "Dibella_Net",
                    "[$attemptType] Attempt ${retryCount + 1}/$maxRetries: $testUrl",
                )

                val timeout = if (isVideo) videoThumbnailTimeout else imageThumbnailTimeout
                val timedResponse = withTimeout(chain, newRequest, timeout)

                timedResponse?.let {
                    if (it.isSuccessful && isRealImage(it)) {
                        Logger.d(
                            "Dibella_Net",
                            "Success after ${retryCount + 1} attempts: $testUrl",
                        )

                        response = it

                        return response
                    }
                }
            } catch (e: Exception) {
                Logger.e("Dibella_Net", "Error during retry: ${e.message}")
            }

            retryCount++

            return null
        }

        if (isVideoThumbnail && isVideo) {
            for (width in fallbackWidths) {
                val newUrl = getThumbnailUrl(url, width)

                if (tryUrlWithRetry(newUrl) != null) return response
            }

            if (retryCount >= maxRetries) {
                val transcodeUrl = url.replace(",optimized=true", "")

                if (tryUrlWithRetry(transcodeUrl, isFallback = true) != null) return response
            }

            if (!response.isSuccessful || !isRealImage(response)) {
                val previewUrl = url.replace("anim=false,", "").replace(",anim=false", "")

                if (previewUrl != url)
                    if (tryUrlWithRetry(previewUrl, isFallback = true) != null) return response
            }
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

                Logger.d(
                    "Dibella_Net",
                    "Video thumbnail failed, trying original video URL for frame extraction: $baseUrl",
                )

                response = chain.proceed(request.newBuilder().url(baseUrl).build())

                if (response.isSuccessful && isRealImage(response)) return response
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

                val fallbackType = if (isVideo) "video" else "image"

                Logger.d("Dibella_Net", "Final fallback [$fallbackType] trying: $originalUrl")

                response = chain.proceed(request.newBuilder().url(originalUrl).build())
            }
        }

        if (!response.isSuccessful || !isRealImage(response)) {
            val fallbackUrl = getOriginalUrl(url).replace("/original=true", "/width=450")

            if (fallbackUrl != url) {
                response.close()

                Logger.d("Dibella_Net", "Last resort fallback trying: $fallbackUrl")

                response = chain.proceed(request.newBuilder().url(fallbackUrl).build())
            }
        }

        return response
    }

    private fun withTimeout(
        chain: Interceptor.Chain,
        request: okhttp3.Request,
        timeoutMs: Long,
    ): Response? {
        val startTime = System.currentTimeMillis()

        return try {
            val response = chain.proceed(request)
            val elapsed = System.currentTimeMillis() - startTime

            if (elapsed > timeoutMs) {
                Logger.w("Dibella_Net", "Request timed out after ${elapsed}ms: ${request.url}")

                response.close()

                null
            } else {
                response
            }
        } catch (e: java.util.concurrent.TimeoutException) {
            Logger.w("Dibella_Net", "Request timeout: ${request.url}")

            null
        } catch (e: Exception) {
            Logger.e("Dibella_Net", "Request error: ${e.message}")

            null
        }
    }

    private fun isRealImage(response: Response): Boolean {
        val body = response.body ?: return false

        if (body.contentLength() == 0L) return false

        return try {
            val source = body.source()

            if (!source.request(32)) return false

            val peek = source.peek()
            val bytes = peek.readByteArray(32)

            if (bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()) return false

            org.movzx.dibella.util.FileUtils.getExtensionFromBytes(bytes) != null
        } catch (e: Exception) {
            Logger.e("Dibella_Net", "Error verifying image: ${e.message}")

            false
        }
    }
}
