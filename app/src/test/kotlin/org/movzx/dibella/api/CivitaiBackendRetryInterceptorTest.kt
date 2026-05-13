package org.movzx.dibella.api

import android.util.Log
import io.mockk.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.util.CivitaiUrlBuilder
import org.movzx.dibella.util.Logger

class CivitaiBackendRetryInterceptorTest {
    private lateinit var interceptor: CivitaiBackendRetryInterceptor
    private lateinit var chain: Interceptor.Chain
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        interceptor = CivitaiBackendRetryInterceptor(repository)
        chain = mockk<Interceptor.Chain>()

        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        mockkObject(CivitaiUrlBuilder)
        mockkObject(Logger)
        every { Logger.d(any<String>(), any<String>()) } returns Unit
        every { Logger.w(any<String>(), any<String>()) } returns Unit
        every { Logger.e(any<String>(), any<String>()) } returns Unit
        every { CivitaiUrlBuilder.backendEnabled } returns true
        every { CivitaiUrlBuilder.backendUrl } returns "https://backend.example.com"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `intercept proceeds normally for non-backend URLs`() {
        val request =
            Request.Builder().url("https://image.civitai.com/uuid/width=450/image.jpg").build()

        val response = mockk<Response>()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        val result = interceptor.intercept(chain)

        assertEquals(response, result)
        verify(exactly = 1) { chain.proceed(request) }
    }

    @Test
    fun `intercept returns immediately on success`() {
        val url = "https://backend.example.com/api/v1/image/thumbnail/uuid"
        val request = Request.Builder().url(url).build()
        val body = "image-data".toResponseBody("image/jpeg".toMediaType())

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
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun `intercept retries on 404 and succeeds on retry`() {
        val url = "https://backend.example.com/api/v1/image/thumbnail/uuid"
        val request = Request.Builder().url(url).build()

        val notFoundResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .body("".toResponseBody(null))
                .build()

        val successResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("image-data".toResponseBody("image/jpeg".toMediaType()))
                .build()

        every { chain.request() } returns request
        every { chain.proceed(any<Request>()) } returns notFoundResponse andThen successResponse

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        verify(atLeast = 2) { chain.proceed(any()) }
    }

    @Test
    fun `intercept retries on 500 and succeeds on retry`() {
        val url = "https://backend.example.com/api/v1/video/original/uuid"
        val request = Request.Builder().url(url).build()

        val serverErrorResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body("".toResponseBody(null))
                .build()

        val successResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("video-data".toResponseBody("video/mp4".toMediaType()))
                .build()

        every { chain.request() } returns request
        every { chain.proceed(any<Request>()) } returns serverErrorResponse andThen successResponse

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        verify(atLeast = 2) { chain.proceed(any()) }
    }

    @Test
    fun `intercept retries on 502 and exhausts all retries`() {
        val url = "https://backend.example.com/api/v1/image/original/uuid"
        val request = Request.Builder().url(url).build()

        val badGatewayResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(502)
                .message("Bad Gateway")
                .body("".toResponseBody(null))
                .build()

        every { chain.request() } returns request
        every { chain.proceed(any<Request>()) } returns badGatewayResponse

        val result = interceptor.intercept(chain)

        assertEquals(502, result.code)
        verify(exactly = 4) { chain.proceed(any()) }
    }

    @Test
    fun `intercept does not retry on 403 forbidden`() {
        val url = "https://backend.example.com/api/v1/image/thumbnail/uuid"
        val request = Request.Builder().url(url).build()

        val forbiddenResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(403)
                .message("Forbidden")
                .body("".toResponseBody(null))
                .build()

        every { chain.request() } returns request
        every { chain.proceed(any<Request>()) } returns forbiddenResponse

        val result = interceptor.intercept(chain)

        assertEquals(403, result.code)
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun `intercept does not retry when backend is disabled`() {
        every { CivitaiUrlBuilder.backendEnabled } returns false

        val url = "https://backend.example.com/api/v1/image/thumbnail/uuid"
        val request = Request.Builder().url(url).build()
        val errorResponse =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body("".toResponseBody(null))
                .build()

        every { chain.request() } returns request
        every { chain.proceed(any<Request>()) } returns errorResponse

        val result = interceptor.intercept(chain)

        assertEquals(500, result.code)
        verify(exactly = 1) { chain.proceed(any()) }
    }
}
