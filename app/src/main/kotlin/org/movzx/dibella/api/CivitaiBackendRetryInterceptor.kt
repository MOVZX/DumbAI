package org.movzx.dibella.api

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.util.CivitaiUrlBuilder
import org.movzx.dibella.util.Logger

@Singleton
class CivitaiBackendRetryInterceptor
@Inject
constructor(private val repository: UserPreferencesRepository) : Interceptor {
    companion object {
        const val RETRY_DELAY_SECONDS = 15L
        const val MAX_RETRIES = 5
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalUrl = request.url.toString()

        if (!isBackendRequest(originalUrl)) return chain.proceed(request)

        var response = chain.proceed(request)

        if (response.isSuccessful) return response

        val errorCode = response.code
        val isServerError = errorCode >= 500 || (errorCode >= 400 && errorCode < 500)

        if (!isServerError) return response

        Logger.w(
            "Dibella_Net",
            "[BackendRetry] Error ${response.code} for $originalUrl, starting retry",
        )

        response.closeQuietly()

        var retryCount = 0

        while (retryCount < MAX_RETRIES) {
            retryCount++

            Logger.d(
                "Dibella_Net",
                "[BackendRetry] Attempt $retryCount/$MAX_RETRIES for $originalUrl",
            )

            try {
                Thread.sleep(RETRY_DELAY_SECONDS * 1000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()

                Logger.w("Dibella_Net", "[BackendRetry] Interrupted during wait")

                return response
            }

            val newRequest = request.newBuilder().url(originalUrl).build()
            response = chain.proceed(newRequest)

            if (response.isSuccessful) {
                Logger.d("Dibella_Net", "[BackendRetry] Success on attempt $retryCount")

                return response
            }

            if (retryCount < MAX_RETRIES) response.closeQuietly()

            Logger.w(
                "Dibella_Net",
                "[BackendRetry] Attempt $retryCount failed with ${response.code}",
            )
        }

        Logger.e(
            "Dibella_Net",
            "[BackendRetry] All $MAX_RETRIES retries exhausted for $originalUrl",
        )

        return response
    }

    private fun isBackendRequest(url: String): Boolean {
        if (!CivitaiUrlBuilder.backendEnabled || CivitaiUrlBuilder.backendUrl.isBlank())
            return false

        return url.startsWith(CivitaiUrlBuilder.backendUrl.removeSuffix("/"))
    }

    private fun Response?.closeQuietly() {
        try {
            this?.close()
        } catch (e: Exception) {
            // Ignored
        }
    }
}
