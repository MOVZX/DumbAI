package org.movzx.dibella.util

import android.net.Uri
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CivitaiUrlBuilderTest {
    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers
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
                every { mockUri.lastPathSegment } returns segments.lastOrNull()
                every { mockUri.toString() } returns url
                mockUri
            }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testCompressUrl() {
        val original =
            "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid-123/width=450/uuid-123.jpg"

        val compressed = CivitaiUrlBuilder.compressUrl(original)

        assertEquals("uuid-123", compressed)
    }

    @Test
    fun testIsCivitaiMediaUrl() {
        assertTrue(
            CivitaiUrlBuilder.isCivitaiMediaUrl(
                "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid-123/width=450/uuid-123.jpg"
            )
        )

        assertTrue(
            CivitaiUrlBuilder.isCivitaiMediaUrl(
                "https://image-b2.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid-123/width=450/uuid-123.jpg"
            )
        )
    }

    @Test
    fun testGetThumbnailUrl() {
        val url = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid-123/width=450/uuid-123.jpg"
        val thumbnail = CivitaiUrlBuilder.getThumbnailUrl(url, 800)

        assertTrue(
            "Thumbnail URL should contain width=800, but was $thumbnail",
            thumbnail.contains("width=800"),
        )

        assertEquals(
            "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid-123/width=800/uuid-123.jpg",
            thumbnail,
        )
    }

    @Test
    fun testGetVideoThumbnailUrl() {
        val url =
            "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid-123/original=true/uuid-123.mp4"

        val thumbnail = CivitaiUrlBuilder.getVideoThumbnailUrl(url)

        assertTrue(thumbnail.contains("transcode=true"))
        assertTrue(thumbnail.contains("anim=false"))
    }

    @Test
    fun testGetFallbackChain() {
        val url =
            "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid-123/anim=false,transcode=true,width=450,original=false,optimized=true/uuid-123.jpg"

        val chain = CivitaiUrlBuilder.getFallbackChain(url)

        // Should contain preview (without anim=false)
        assertTrue(chain.any { it.contains("transcode=true") && !it.contains("anim=false") })
        // Should contain 800px thumbnail
        assertTrue(chain.any { it.contains("width=800") })
        // Should contain original
        assertTrue(chain.any { it.contains("original=true") })
    }
}
