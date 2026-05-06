package org.movzx.dibella.util

object CivitaiUrlBuilder {
    private const val CDN_HOST = "image.civitai.com"
    private val FALLBACK_WIDTHS = 800
    private val WIDTH_REGEX = Regex("/width=\\d+/")
    private const val URL_PREFIX = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/"
    var backendEnabled: Boolean = false
    var backendUrl: String = ""
    var backendApiKey: String = ""

    fun compressUrl(url: String): String {
        if (!url.startsWith(URL_PREFIX)) return url

        val relative = url.substring(URL_PREFIX.length)
        val parts = relative.split("/")

        return if (parts.isNotEmpty()) parts[0] else relative
    }

    fun expandUrl(compressed: String, type: String?): String {
        if (compressed.startsWith("http")) return compressed

        val uuid = compressed
        val ext = if (type == "video") "mp4" else "jpg"
        val originalUrl = "$URL_PREFIX$uuid/original=true/$uuid.$ext"

        return mapToBackend(originalUrl) ?: originalUrl
    }

    fun isCivitaiMediaUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return (lowerUrl.contains("image.civitai.com") ||
            lowerUrl.contains("image-b2.civitai.com")) && !lowerUrl.contains("/api/")
    }

    fun mapToBackend(
        url: String,
        bEnabled: Boolean = backendEnabled,
        bUrl: String = backendUrl,
    ): String? {
        if (!bEnabled || bUrl.isBlank() || !isCivitaiMediaUrl(url)) return null

        val baseUrl = getBaseUrl(url)
        val uuid = baseUrl.substringAfterLast("/")

        if (uuid.isBlank() || uuid == baseUrl) return null

        val isVideo =
            url.contains("anim=false") ||
                url.contains("transcode=true") ||
                url.lowercase().contains(".mp4") ||
                url.lowercase().contains(".webm") ||
                url.lowercase().contains(".mov")

        val type = if (isVideo) "video" else "image"

        val quality =
            when {
                url.contains("original=true") -> "original"
                isVideo && !url.contains("transcode=true") && !url.contains("anim=false") ->
                    "original"
                url.contains("width=320") -> "thumbnail"
                url.contains("anim=false") -> "thumbnail"
                else -> "preview"
            }

        return "${bUrl.removeSuffix("/")}/api/v1/$type/$quality/$uuid"
    }

    fun getThumbnailUrl(url: String, width: Int): String {
        val modified = modifyUrl(url, "width=$width")

        return mapToBackend(modified) ?: modified
    }

    fun getVideoThumbnailUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        val baseUrl = getBaseUrl(url)
        val modified = "$baseUrl/anim=false,transcode=true,width=450,original=false,optimized=true"

        return mapToBackend(modified) ?: modified
    }

    fun getVideoPreviewUrl(url: String): String {
        if (!isCivitaiMediaUrl(url)) return url

        val baseUrl = getBaseUrl(url)
        val modified = "$baseUrl/transcode=true,width=450,optimized=true"

        return mapToBackend(modified) ?: modified
    }

    fun getVideoOriginalUrl(url: String): String {
        if (backendEnabled && backendUrl.isNotBlank() && isCivitaiMediaUrl(url)) {
            val baseUrl = getBaseUrl(url)
            val uuid = baseUrl.substringAfterLast("/")

            if (uuid.isNotBlank())
                return "${backendUrl.removeSuffix("/")}/api/v1/video/original/$uuid"
        }
        return getOriginalUrl(url)
    }

    fun getOriginalUrl(url: String): String {
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

        return mapToBackend(original) ?: original
    }

    fun getFallbackChain(url: String): List<String> {
        val chain = mutableListOf<String>()
        val isVideo = url.contains("anim=false") || url.contains("transcode=true")

        if (isVideo && url.contains("anim=false")) {
            val preview = url.replace("anim=false,", "").replace(",anim=false", "")

            if (preview != url) chain.add(preview)
        }

        val newUrl = getThumbnailUrl(url, FALLBACK_WIDTHS)

        if (newUrl != url && !chain.contains(newUrl)) chain.add(newUrl)

        if (isVideo && !url.contains("optimized=true")) {
            val transcode = url.replace(",optimized=true", "")

            if (transcode != url && !chain.contains(transcode)) chain.add(transcode)
        }

        val original = getOriginalUrl(url)

        if (original != url && !chain.contains(original)) chain.add(original)

        val lastResort = getOriginalUrl(url).replace("/original=true", "/width=450")

        if (lastResort != url && !chain.contains(lastResort)) chain.add(lastResort)

        return chain
    }

    private fun getBaseUrl(url: String): String {
        return when {
            url.contains("/original=true/") -> url.substringBefore("/original=true/")
            url.contains("/original=false/") -> url.substringBefore("/original=false/")
            url.contains(WIDTH_REGEX) ->
                url.substringBefore(url.split("/").find { it.startsWith("width=") } ?: "")
            else -> url.substringBeforeLast("/")
        }.removeSuffix("/")
    }

    private fun modifyUrl(url: String, variant: String): String {
        return when {
            url.contains("/original=true/") -> url.replace("/original=true/", "/$variant/")
            url.contains("/original=false/") -> url.replace("/original=false/", "/$variant/")
            url.contains(WIDTH_REGEX) -> url.replace(WIDTH_REGEX, "/$variant/")
            else -> {
                val lastSlashIndex = url.lastIndexOf('/')

                if (lastSlashIndex != -1) {
                    val prefix = url.substring(0, lastSlashIndex)
                    val fileName = url.substring(lastSlashIndex + 1)
                    if (fileName.contains(".")) "$prefix/$variant/$fileName" else "$url/$variant/"
                } else url
            }
        }
    }
}
