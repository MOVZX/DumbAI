package org.movzx.dumbai.api

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dumbai.data.UserPreferencesRepository

@Singleton
class CivitaiInterceptor @Inject constructor(private val repository: UserPreferencesRepository) :
    Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        val host = original.url.host
        val isCivitai = host.contains("civitai.com") || host.contains("civitai.red")
        val (nsfw, key, debugEnabled) = runBlocking { repository.getInterceptorSettings() }

        if (isCivitai) {
            val targetHost = if (nsfw != "None") "civitai.red" else "civitai.com"

            if (host != targetHost) {
                val newUrl = original.url.newBuilder().host(targetHost).build()

                requestBuilder.url(newUrl)
            }

            if (key.isNotBlank()) requestBuilder.header("Authorization", "Bearer $key")
        }

        val request = requestBuilder.build()

        if (debugEnabled) {
            android.util.Log.d("DumbAI_Net", "Request: ${request.method} ${request.url}")

            request.headers.forEach { (name, value) ->
                if (name != "Authorization")
                    android.util.Log.d("DumbAI_Net", "Header: $name: $value")
            }
        }

        val response = chain.proceed(request)

        if (debugEnabled) {
            android.util.Log.d(
                "DumbAI_Net",
                "Response: ${response.code} ${response.message} for ${response.request.url}",
            )
        }

        return response
    }
}
