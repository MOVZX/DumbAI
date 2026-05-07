package org.movzx.dibella.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {
    @Test
    fun testGetExtensionFromBytes() {
        val jpgBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

        assertEquals("jpg", FileUtils.getExtensionFromBytes(jpgBytes))

        val webpBytes = "524946460000000057454250".decodeHex()

        assertEquals("webp", FileUtils.getExtensionFromBytes(webpBytes))

        val mp4Bytes = "0000002066747970".decodeHex()

        assertEquals("mp4", FileUtils.getExtensionFromBytes(mp4Bytes))
    }

    @Test
    fun testIsVideoFile() {
        val mp4Bytes = "0000002066747970".decodeHex()

        assertTrue(FileUtils.isVideoFile(mp4Bytes))

        val jpgBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

        assertFalse(FileUtils.isVideoFile(jpgBytes))
    }

    private fun String.decodeHex(): ByteArray {
        val result = ByteArray(length / 2)

        for (i in result.indices) {
            val index = i * 2
            result[i] = substring(index, index + 2).toInt(16).toByte()
        }

        return result
    }
}
