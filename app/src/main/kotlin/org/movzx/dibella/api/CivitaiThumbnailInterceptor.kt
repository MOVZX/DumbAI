package org.movzx.dibella.api

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.movzx.dibella.util.CivitaiUrlBuilder
import org.movzx.dibella.util.FileUtils
import org.movzx.dibella.util.Logger

class CivitaiThumbnailInterceptor : Interceptor {
    companion object {
        const val VIDEO_TIMEOUT_MS = 10_000L
        const val IMAGE_TIMEOUT_MS = 15_000L
        const val MAX_RETRIES = 3
    }

    private val videoThumbnailTimeout = VIDEO_TIMEOUT_MS
    private val imageThumbnailTimeout = IMAGE_TIMEOUT_MS
    private val maxRetries = MAX_RETRIES

    private fun cacheable(response: Response): Response {
        if (!response.isSuccessful) return response

        val requestUrl = response.request.url.toString()

        val maxAge =
            when {
                requestUrl.contains("anim=false") ||
                    (!requestUrl.contains("transcode=true") &&
                        requestUrl.contains("/original=false/")) -> 7 * 86400
                requestUrl.contains("/original=true/") ||
                    (requestUrl.contains("original=true") &&
                        !requestUrl.contains("transcode=true")) -> 1 * 86400
                else -> 3 * 86400
            }

        return response.newBuilder().header("Cache-Control", "public, max-age=$maxAge").build()
    }

    private fun Response?.closeQuietly() {
        try {
            this?.close()
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun safeProceed(
        chain: Interceptor.Chain,
        request: Request,
        url: String,
        timeoutMs: Long,
    ): Response? {
        return try {
            val newRequest = request.newBuilder().url(url).build()

            chain
                .withReadTimeout(timeoutMs.toInt(), TimeUnit.MILLISECONDS)
                .withConnectTimeout(timeoutMs.toInt(), TimeUnit.MILLISECONDS)
                .proceed(newRequest)
        } catch (e: Exception) {
            Logger.w("Dibella_Net", "Request failed for $url: ${e.message}")

            null
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        if (!CivitaiUrlBuilder.isCivitaiUrl(url)) return chain.proceed(request)

        var response = chain.proceed(request)

        if (response.isSuccessful && isValidMedia(response)) return cacheable(response)

        val isVideo = url.contains("anim=false") || url.contains("transcode=true")
        val timeout = if (isVideo) videoThumbnailTimeout else imageThumbnailTimeout
        val fallbacks = CivitaiUrlBuilder.getFallbackChain(url)
        var retryCount = 0

        for (fallbackUrl in fallbacks) {
            if (retryCount >= maxRetries) break

            response.closeQuietly()

            Logger.d("Dibella_Net", "[Fallback] Attempt ${retryCount + 1}: $fallbackUrl")

            val fallbackResponse = safeProceed(chain, request, fallbackUrl, timeout)

            if (fallbackResponse != null) {
                response = fallbackResponse

                if (response.isSuccessful && isValidMedia(response)) {
                    Logger.d("Dibella_Net", "[Fallback] Success: $fallbackUrl")

                    return cacheable(response)
                }
            }

            retryCount++
        }

        return cacheable(response)
    }

    private fun isValidMedia(response: Response): Boolean {
        val body = response.body ?: return false

        if (body.contentLength() == 0L) return false

        val contentType = body.contentType()?.toString()?.lowercase() ?: ""

        if (contentType.contains("application/json") || contentType.contains("text/html"))
            return false

        return try {
            val source = body.source()

            if (!source.request(64)) return false

            val bytes = source.peek().readByteArray(64)

            if (bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()) return false

            FileUtils.getExtensionFromBytes(bytes) != null ||
                contentType.startsWith("image/") ||
                contentType.startsWith("video/")
        } catch (e: Exception) {
            Logger.e("Dibella_Net", "Error verifying media: ${e.message}")

            false
        }
    }
}
