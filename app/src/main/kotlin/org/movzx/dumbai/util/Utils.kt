package org.movzx.dumbai.util

fun getThumbnailUrl(url: String, width: Int): String {
    if (!url.contains("image.civitai.com")) return url

    val baseUrl = when {
        url.contains("/original=true/") -> url.replace("/original=true/", "/width=$width/")
        url.contains(Regex("/width=\\d+/")) -> url.replace(Regex("/width=\\d+/"), "/width=$width/")
        else -> {
            val lastSlashIndex = url.lastIndexOf('/')

            if (lastSlashIndex != -1) {
                val prefix = url.substring(0, lastSlashIndex)
                val fileName = url.substring(lastSlashIndex + 1)
                "$prefix/width=$width/$fileName"
            }
            else
                url
        }
    }

    return if (baseUrl.contains(".mp4", ignoreCase = true)) {
        baseUrl.replace(".mp4", ".jpeg", ignoreCase = true)
    } else baseUrl
}
