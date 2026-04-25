package org.movzx.dumbai.api

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dumbai.data.UserPreferencesRepository
import org.movzx.dumbai.util.Logger

@Singleton
class CivitaiInterceptor @Inject constructor(private val repository: UserPreferencesRepository) :
    Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url
        val (key, _, nsfwSetting) = runBlocking { repository.getInterceptorSettings() }
        val nsfwParam = originalUrl.queryParameter("nsfw")
        val useRed = (nsfwParam != null && nsfwParam != "None") || (nsfwSetting != "None")

        val newUrl =
            if (originalUrl.host.endsWith("civitai.com") && useRed) {
                val newHost = originalUrl.host.replace("civitai.com", "civitai.red")

                originalUrl.newBuilder().host(newHost).build()
            } else {
                originalUrl
            }

        val requestBuilder = original.newBuilder().url(newUrl)
        val currentHost = newUrl.host
        val isCivitai = currentHost.endsWith("civitai.com") || currentHost.endsWith("civitai.red")

        if (isCivitai && key.isNotBlank()) requestBuilder.header("Authorization", "Bearer $key")

        val request = requestBuilder.build()

        Logger.d("DumbAI_Net", "Request: ${request.method} ${request.url}")

        request.headers.forEach { (name, value) ->
            if (name != "Authorization") Logger.d("DumbAI_Net", "Header: $name: $value")
        }

        val response = chain.proceed(request)

        Logger.d(
            "DumbAI_Net",
            "Response: ${response.code} ${response.message} for ${response.request.url}",
        )

        return response
    }
}
