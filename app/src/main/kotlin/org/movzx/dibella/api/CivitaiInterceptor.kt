package org.movzx.dibella.api

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.util.Logger

@Singleton
class CivitaiInterceptor @Inject constructor(private val repository: UserPreferencesRepository) :
    Interceptor {
    private var apiKey: String = ""
    private var debugEnabled: Boolean = false
    private val scope =
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main
        )

    init {
        scope.launch {
            try {
                val settings = repository.getInterceptorSettings()
                apiKey = settings.first
                debugEnabled = settings.second
            } catch (e: Exception) {
                Logger.e("Dibella_Net", "Failed to load interceptor settings: ${e.message}")
            }
        }
    }

    fun updateSettings(key: String, enabled: Boolean) {
        this.apiKey = key
        this.debugEnabled = enabled
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        val host = original.url.host
        val path = original.url.encodedPath
        val isCivitai = host.contains("civitai.com")
        val isApi = isCivitai && path.contains("/api/")

        if (isCivitai) {
            requestBuilder.header(
                "User-Agent",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
            )

            if (isApi && apiKey.isNotBlank())
                requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        val request = requestBuilder.build()

        if (debugEnabled) Logger.d("Dibella_Net", "Request: ${request.method} ${request.url}")

        val response = chain.proceed(request)

        if (debugEnabled) {
            Logger.d(
                "Dibella_Net",
                "Response: ${response.code} ${response.message} for ${response.request.url}",
            )
        }

        return response
    }
}
