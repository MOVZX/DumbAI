package org.movzx.dibella.util

import androidx.core.net.toUri

object CivitaiUrlBuilder {
    private const val URL_PREFIX = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/"
    private val WIDTH_REGEX = Regex("/width=\\d+/")

    private val UUID_REGEX =
        Regex(
            "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}",
            RegexOption.IGNORE_CASE,
        )

    private val uuidCache = java.util.Collections.synchronizedMap(mutableMapOf<String, String>())
    var backendEnabled: Boolean = false
    var backendUrl: String = ""
    var backendApiKey: String = ""
    const val BACKEND_DOWNLOAD_TIMEOUT_SECONDS = 60L

    fun compressUrl(url: String): String {
        if (!url.startsWith(URL_PREFIX)) return url

        return url.removePrefix(URL_PREFIX).substringBefore("/")
    }

    fun isCivitaiMediaUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()

        return (lowerUrl.contains("image.civitai.com") ||
            lowerUrl.contains("image-b2.civitai.com")) && !lowerUrl.contains("/api/")
    }

    fun expandUrl(compressed: String, type: String?): String {
        if (compressed.startsWith("http")) return compressed

        val uuid = compressed
        val base = "$URL_PREFIX$uuid"

        return if (type == "video") {
            "$base/anim=false,transcode=true,width=450,original=false,optimized=true/$uuid.jpg"
        } else {
            "$base/width=450/$uuid.jpg"
        }
    }

    fun extractCivitaiUuid(url: String): String? {
        if (!isCivitaiMediaUrl(url)) return null

        val cached = uuidCache.get(url)

        if (cached != null) return cached

        val uuid =
            UUID_REGEX.find(url)?.value
                ?: run {
                    val parsed = parseCivitaiUrl(url)

                    if (parsed != null) parsed.baseUrl.substringAfterLast("/")
                    else if (url.contains(URL_PREFIX))
                        url.substringAfter(URL_PREFIX).substringBefore("/")
                    else null
                }

        if (uuid != null) uuidCache.put(url, uuid)

        return uuid
    }

    private fun wrapBackendUrl(
        type: String,
        quality: String,
        url: String,
        fallback: () -> String,
    ): String {
        if (backendEnabled && backendUrl.isNotBlank()) {
            val uuid = extractCivitaiUuid(url)

            if (uuid != null) {
                val actualType =
                    if (
                        type == "image" &&
                            (url.contains(".mp4") ||
                                url.contains(".webm") ||
                                url.contains("transcode=true"))
                    )
                        "video"
                    else type
                return "${backendUrl.removeSuffix("/")}/api/v1/$actualType/$quality/$uuid"
            }
        }

        return fallback()
    }

    fun toBackendUrl(type: String, quality: String, uuid: String): String {
        return "${backendUrl.removeSuffix("/")}/api/v1/$type/$quality/$uuid"
    }

    fun buildBackendFeedUrl(originalUrl: String): String {
        if (!backendEnabled || backendUrl.isBlank()) return originalUrl

        val originalUri = originalUrl.toUri()
        val query = originalUri.query

        return "${backendUrl.removeSuffix("/")}/api/v1/feeds?$query"
    }

    fun getThumbnailUrl(url: String, width: Int): String {
        if (!isCivitaiMediaUrl(url)) return url

        return wrapBackendUrl("image", "thumbnail", url) { modifyUrl(url, "width=$width") }
    }

    fun getVideoThumbnailUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        return wrapBackendUrl("video", "thumbnail", url) {
            val baseUrl = getBaseUrl(url)

            "$baseUrl/anim=false,transcode=true,width=450,original=false,optimized=true"
        }
    }

    fun getVideoPreviewUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        return wrapBackendUrl("video", "preview", url) {
            val baseUrl = getBaseUrl(url)

            "$baseUrl/transcode=true,width=450,optimized=true"
        }
    }

    fun getVideoOriginalUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        return wrapBackendUrl("video", "original", url) {
            val uuid = extractCivitaiUuid(url)

            if (uuid != null) "${URL_PREFIX}$uuid/original=true/$uuid.mp4" else url
        }
    }

    fun getImageOriginalUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        return wrapBackendUrl("image", "original", url) {
            val uuid = extractCivitaiUuid(url)

            if (uuid != null) "${URL_PREFIX}$uuid/original=true/$uuid.jpg" else url
        }
    }

    fun getFallbackChain(url: String): List<String> {
        val chain = mutableListOf<String>()
        val isVideo = url.contains("anim=false") || url.contains("transcode=true")

        if (isVideo && url.contains("anim=false")) {
            val preview = url.replace("anim=false,", "").replace(",anim=false", "")

            if (preview != url) chain.add(preview)
        }

        val thumbnail = getThumbnailUrl(url, 800)

        if (thumbnail != url && !chain.contains(thumbnail)) chain.add(thumbnail)

        if (isVideo && !url.contains("optimized=true")) {
            val transcode = url.replace(",optimized=true", "")

            if (transcode != url && !chain.contains(transcode)) chain.add(transcode)
        }

        val original = getImageOriginalUrl(url)

        if (original != url && !chain.contains(original)) chain.add(original)

        return chain
    }

    fun getBaseUrl(url: String): String {
        val parsed = parseCivitaiUrl(url)
        return parsed?.baseUrl ?: url.substringBeforeLast("/")
    }

    fun modifyUrl(url: String, variant: String): String {
        val parsed = parseCivitaiUrl(url)

        return if (parsed != null) {
            val fileName = parsed.fileName

            "${parsed.baseUrl}/$variant/$fileName"
        } else url
    }

    private fun parseCivitaiUrl(url: String): ParseResult? {
        if (!isCivitaiMediaUrl(url)) return null

        val uri =
            try {
                url.toUri()
            } catch (e: Exception) {
                return null
            }

        val pathSegments = uri.pathSegments

        if (pathSegments.size < 2) return null

        val hash = pathSegments[0]
        val uuid = pathSegments[1]
        val baseUrl = "${uri.scheme}://${uri.host}/$hash/$uuid"
        val fileName = if (pathSegments.size >= 4) pathSegments.last() else "$uuid.jpg"
        val variantSegment = if (pathSegments.size >= 3) pathSegments[2] else ""

        val variant =
            when {
                variantSegment.contains("original=true") -> "original"
                variantSegment.contains("original=false") -> "thumbnail"
                variantSegment.contains("anim=false") -> "thumbnail"
                variantSegment.contains("width=") -> "thumbnail"
                variantSegment.contains("transcode=true") -> "preview"
                else -> null
            }

        return ParseResult(baseUrl, fileName, variant)
    }

    private data class ParseResult(val baseUrl: String, val fileName: String, val variant: String?)
}
