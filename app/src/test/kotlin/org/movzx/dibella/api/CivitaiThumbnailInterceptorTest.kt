package org.movzx.dibella.api

import android.net.Uri
import android.util.Log
import io.mockk.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.movzx.dibella.util.CivitaiUrlBuilder

class CivitaiThumbnailInterceptorTest {
    private lateinit var interceptor: CivitaiThumbnailInterceptor
    private lateinit var chain: Interceptor.Chain

    @Before
    fun setup() {
        interceptor = CivitaiThumbnailInterceptor()
        chain = mockk<Interceptor.Chain>()

        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        mockkStatic(Uri::class)

        every { Uri.parse(any<String>()) } answers
            {
                val url = it.invocation.args[0] as String
                val mockUri = mockk<Uri>()
                val scheme = if (url.contains("://")) url.substringBefore("://") else "https"
                val hostAndPath = if (url.contains("://")) url.substringAfter("://") else url
                val host = hostAndPath.substringBefore("/")
                val path = hostAndPath.substringAfter("/", "")
                val segments = path.split("/").filter { it.isNotEmpty() }

                every { mockUri.scheme } returns scheme
                every { mockUri.host } returns host
                every { mockUri.pathSegments } returns segments
                every { mockUri.toString() } returns url
                mockUri
            }

        mockkObject(CivitaiUrlBuilder)

        every { CivitaiUrlBuilder.isCivitaiMediaUrl(any<String>()) } answers
            {
                val url = it.invocation.args[0] as String

                url.contains("civitai.com")
            }
        every { CivitaiUrlBuilder.getFallbackChain(any<String>()) } returns
            listOf(
                "https://image.civitai.com/fallback-1.jpg",
                "https://image.civitai.com/fallback-2.jpg",
            )

        every { chain.withReadTimeout(any<Int>(), any()) } returns chain
        every { chain.withConnectTimeout(any<Int>(), any()) } returns chain
        every { chain.request() } returns mockk<Request>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `intercept proceeds normally for non-Civitai URLs`() {
        val request = Request.Builder().url("https://example.com/image.jpg").build()
        val response = mockk<Response>()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        val result = interceptor.intercept(chain)

        assertEquals(response, result)
        verify(exactly = 1) { chain.proceed(request) }
    }

    @Test
    fun `intercept returns original response if successful and valid`() {
        try {
            val url = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid/width=450/image.jpg"
            val request = Request.Builder().url(url).build()
            val body = "fake-image-data".toResponseBody("image/jpeg".toMediaType())

            val response =
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body)
                    .build()

            every { chain.request() } returns request
            every { chain.proceed(any<Request>()) } returns response

            val result = interceptor.intercept(chain)

            assertEquals(200, result.code)
        } catch (e: Exception) {
            e.printStackTrace()

            throw e
        }
    }

    @Test
    fun `intercept triggers fallback if first response is 404`() {
        val url = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid/width=450/image.jpg"
        val request = Request.Builder().url(url).build()

        val errorResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .body("".toResponseBody(null))
                .build()

        val successBody = "fallback-image-data".toResponseBody("image/jpeg".toMediaType())

        val successResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(successBody)
                .build()

        every { chain.request() } returns request
        every { chain.proceed(any<Request>()) } returns errorResponse andThen successResponse

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        verify(atLeast = 2) { chain.proceed(any()) }
    }
}
