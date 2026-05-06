package org.movzx.dibella.api

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.util.CivitaiUrlBuilder
import org.movzx.dibella.util.Logger

@Singleton
class CivitaiInterceptor @Inject constructor(private val repository: UserPreferencesRepository) :
    Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val settings = repository.interceptorSettings.value
        val apiKey = settings.apiKey
        val backendEnabled = settings.backendEnabled
        val backendUrl = settings.backendUrl
        val backendApiKey = settings.backendApiKey

        val original = chain.request()
        var request = original
        val host = request.url.host
        val path = request.url.encodedPath
        val isCivitai = host.contains("civitai.com")
        val isApi = isCivitai && path.contains("/api/")

        if (isCivitai && backendEnabled && !isApi) {
            val url = request.url.toString()
            val uuid = CivitaiUrlBuilder.extractCivitaiUuid(url)

            if (uuid != null) {
                val isVideo = url.contains("anim=false") || url.contains("transcode=true")
                val type = if (isVideo) "video" else "image"

                val quality = when {
                    url.contains("original=true") -> "original"
                    url.contains("anim=false") -> "thumbnail"
                    url.contains("width=320") -> "thumbnail"
                    else -> "preview"
                }

                val redirectedUrl = CivitaiUrlBuilder.toBackendUrl(type, quality, uuid)

                Logger.d("Dibella_Net", "Redirecting to backend: $redirectedUrl")

                request = request.newBuilder().url(redirectedUrl).build()
            }
        }

        val isBackend =
            backendEnabled &&
                backendUrl.isNotBlank() &&
                request.url.toString().startsWith(backendUrl.removeSuffix("/"))

        var activeChain = chain

        if (isBackend) {
            activeChain =
                chain
                    .withConnectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .withReadTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        }

        val requestBuilder = request.newBuilder()

        if (isBackend && backendApiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $backendApiKey")
        }

        if (isCivitai) {
            requestBuilder.header(
                "User-Agent",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
            )

            if (isApi && apiKey.isNotBlank())
                requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        request = requestBuilder.build()

        Logger.d("Dibella_Net", "Request: ${request.method} ${request.url}")

        val response = activeChain.proceed(request)

        Logger.d(
            "Dibella_Net",
            "Response: ${response.code} ${response.message} for ${response.request.url}",
        )

        return response
    }
}
