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
        val requestBuilder = original.newBuilder()
        val host = original.url.host
        val isCivitai = host.contains("civitai.com")
        val (key, debugEnabled) = runBlocking { repository.getInterceptorSettings() }

        if (isCivitai) if (key.isNotBlank()) requestBuilder.header("Authorization", "Bearer $key")

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
