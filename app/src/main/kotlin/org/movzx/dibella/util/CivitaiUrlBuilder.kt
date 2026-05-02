package org.movzx.dibella.util

object CivitaiUrlBuilder {
    private const val CDN_HOST = "image.civitai.com"
    private val FALLBACK_WIDTHS = listOf(600, 800, 1200)
    private val WIDTH_REGEX = Regex("/width=\\d+/")
    private const val URL_PREFIX = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/"

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

        return "$URL_PREFIX$uuid/original=true/$uuid.$ext"
    }

    fun isCivitaiUrl(url: String): Boolean = url.contains(CDN_HOST)

    fun getThumbnailUrl(url: String, width: Int): String {
        return modifyUrl(url, "width=$width")
    }

    fun getVideoThumbnailUrl(url: String): String {
        if (!isCivitaiUrl(url)) return url

        val baseUrl = getBaseUrl(url)

        return "$baseUrl/anim=false,transcode=true,width=450,original=false,optimized=true"
    }

    fun getVideoPreviewUrl(url: String): String {
        if (!isCivitaiUrl(url)) return url

        val baseUrl = getBaseUrl(url)

        return "$baseUrl/transcode=true,width=450,optimized=true"
    }

    fun getOriginalUrl(url: String): String {
        if (!isCivitaiUrl(url)) return url

        return when {
            url.contains(WIDTH_REGEX) -> url.replace(WIDTH_REGEX, "/original=true/")
            url.contains("/original=false/") -> url.replace("/original=false/", "/original=true/")
            else -> url
        }
    }

    fun getFallbackChain(url: String): List<String> {
        if (!isCivitaiUrl(url)) return emptyList()

        val chain = mutableListOf<String>()
        val isVideo = url.contains("anim=false") || url.contains("transcode=true")

        if (isVideo && url.contains("anim=false")) {
            val preview = url.replace("anim=false,", "").replace(",anim=false", "")

            if (preview != url) chain.add(preview)
        }

        for (width in FALLBACK_WIDTHS) {
            val newUrl = getThumbnailUrl(url, width)

            if (newUrl != url && !chain.contains(newUrl)) chain.add(newUrl)
        }

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
        if (!isCivitaiUrl(url)) return url

        return when {
            url.contains("/original=true/") -> url.replace("/original=true/", "/$variant/")
            url.contains("/original=false/") -> url.replace("/original=false/", "/$variant/")
            url.contains(WIDTH_REGEX) -> url.replace(WIDTH_REGEX, "/$variant/")
            else -> {
                val lastSlashIndex = url.lastIndexOf('/')

                if (lastSlashIndex != -1) {
                    val prefix = url.substring(0, lastSlashIndex)
                    val fileName = url.substring(lastSlashIndex + 1)
                    "$prefix/$variant/$fileName"
                } else url
            }
        }
    }
}
