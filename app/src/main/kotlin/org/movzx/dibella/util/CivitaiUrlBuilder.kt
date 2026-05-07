package org.movzx.dibella.util

object CivitaiUrlBuilder {
    private const val URL_PREFIX = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/"
    private val WIDTH_REGEX = Regex("/width=\\d+/")
    private val uuidCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    var backendEnabled: Boolean = false
    var backendUrl: String = ""
    var backendApiKey: String = ""

    fun compressUrl(url: String): String {
        if (!url.startsWith(URL_PREFIX)) return url

        val relative = url.substring(URL_PREFIX.length)
        val parts = relative.split("/")

        return if (parts.isNotEmpty()) parts[0] else relative
    }

    fun extractVariant(url: String): String? {
        if (!url.startsWith(URL_PREFIX)) return null

        val relative = url.substring(URL_PREFIX.length)
        val segments = relative.split("/")

        if (segments.size < 2) return null

        return when {
            segments[1].startsWith("original=true") -> "original"
            segments[1].startsWith("original=false") -> "thumbnail"
            segments[1].startsWith("width=") -> "thumbnail"
            else -> null
        }
    }

    fun isCivitaiMediaUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()

        return (lowerUrl.contains("image.civitai.com") ||
            lowerUrl.contains("image-b2.civitai.com")) && !lowerUrl.contains("/api/")
    }

    fun expandUrl(compressed: String, type: String?, variant: String? = null): String {
        if (compressed.startsWith("http")) return compressed

        val uuid = compressed
        val ext = if (type == "video") "mp4" else "jpg"
        val base = "$URL_PREFIX$uuid"

        val url =
            when (variant) {
                "original" -> "$base/original=true/$uuid.$ext"
                "thumbnail" -> "$base/original=false/$uuid.jpg"
                else -> "$base/original=true/$uuid.$ext"
            }

        return url
    }

    fun extractCivitaiUuid(url: String): String? {
        if (!isCivitaiMediaUrl(url)) return null

        val cached = uuidCache[url]

        if (cached != null) return cached

        val baseUrl = getBaseUrl(url)
        val uuid = if (baseUrl != url) baseUrl.substringAfterLast("/") else null

        if (uuid != null) uuidCache[url] = uuid

        return uuid
    }

    fun toBackendUrl(type: String, quality: String, uuid: String): String {
        return "${backendUrl.removeSuffix("/")}/api/v1/$type/$quality/$uuid"
    }

    fun getThumbnailUrl(url: String, width: Int): String {
        if (!isCivitaiMediaUrl(url)) return url

        if (backendEnabled && backendUrl.isNotBlank()) {
            val uuid = extractCivitaiUuid(url) ?: return url

            return toBackendUrl("image", "thumbnail", uuid)
        }

        return modifyUrl(url, "width=$width")
    }

    fun getVideoThumbnailUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        if (backendEnabled && backendUrl.isNotBlank()) {
            val uuid = extractCivitaiUuid(url) ?: return url

            return toBackendUrl("video", "thumbnail", uuid)
        }

        val baseUrl = getBaseUrl(url)

        return "$baseUrl/anim=false,transcode=true,width=450,original=false,optimized=true"
    }

    fun getVideoPreviewUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        if (backendEnabled && backendUrl.isNotBlank()) {
            val uuid = extractCivitaiUuid(url) ?: return url

            return toBackendUrl("video", "preview", uuid)
        }

        val baseUrl = getBaseUrl(url)

        return "$baseUrl/transcode=true,width=450,optimized=true"
    }

    fun getVideoOriginalUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        if (backendEnabled && backendUrl.isNotBlank()) {
            val uuid = extractCivitaiUuid(url) ?: return url

            return toBackendUrl("video", "original", uuid)
        }

        return getOriginalUrl(url)
    }

    fun getImageOriginalUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        if (backendEnabled && backendUrl.isNotBlank()) {
            val uuid = extractCivitaiUuid(url) ?: return url

            return toBackendUrl("image", "original", uuid)
        }

        val original =
            when {
                url.contains(WIDTH_REGEX) -> url.replace(WIDTH_REGEX, "/original=true/")
                url.contains("/original=false/") ->
                    url.replace("/original=false/", "/original=true/")
                url.contains("original=true") -> url
                else -> {
                    if (isCivitaiMediaUrl(url)) {
                        val baseUrl = getBaseUrl(url)
                        "$baseUrl/original=true/${baseUrl.substringAfterLast("/")}.jpg"
                    } else url
                }
            }

        return original
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
            val fileName = parsed.fileName ?: ""

            if (fileName.isNotEmpty()) "$parsed.baseUrl/$variant/$fileName"
            else "$parsed.baseUrl/$variant/"
        } else url
    }

    private fun parseCivitaiUrl(url: String): ParseResult? {
        if (!isCivitaiMediaUrl(url)) return null

        val originalVariant =
            when {
                url.contains("/original=true/") -> "original"
                url.contains("/original=false/") -> "thumbnail"
                url.contains(WIDTH_REGEX) -> "thumbnail"
                else -> null
            }

        val baseUrl =
            when {
                url.contains("/original=true/") -> url.substringBefore("/original=true/")
                url.contains("/original=false/") -> url.substringBefore("/original=false/")
                url.contains(WIDTH_REGEX) -> {
                    val widthPart = url.split("/").find { it.startsWith("width=") } ?: ""
                    url.substringBefore(widthPart)
                }
                else -> url.substringBeforeLast("/")
            }

        val fileName = url.substringAfterLast("/")

        return ParseResult(baseUrl.removeSuffix("/"), fileName, originalVariant)
    }

    private data class ParseResult(
        val baseUrl: String,
        val fileName: String,
        val originalVariant: String?,
    )
}
