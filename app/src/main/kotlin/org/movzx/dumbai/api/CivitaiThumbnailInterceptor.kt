package org.movzx.dumbai.api

import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dumbai.util.getThumbnailUrl

class CivitaiThumbnailInterceptor : Interceptor {
    private val fallbackWidths = listOf(360, 450, 640, 720)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        var response = chain.proceed(request)

        if (!response.isSuccessful && response.code == 404 && url.contains("image.civitai.com")) {
            for (width in fallbackWidths) {
                val newUrl = getThumbnailUrl(url, width)

                if (newUrl == url) continue

                response.close()

                val newRequest = request.newBuilder().url(newUrl).build()

                response = chain.proceed(newRequest)

                if (response.isSuccessful) return response
            }
        }

        return response
    }
}
