package org.movzx.dumbai.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.FavoriteImage

fun Modifier.scrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f),
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by
        animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = duration),
            label = "scrollbar_alpha",
        )

    drawWithContent {
        drawContent()

        val viewHeight = size.height
        val totalHeight = state.maxValue.toFloat() + viewHeight
        val barHeight = (viewHeight / totalHeight) * viewHeight
        val barOffset = (state.value.toFloat() / totalHeight) * viewHeight

        drawRect(
            color = color.copy(alpha = alpha * color.alpha),
            topLeft = Offset(size.width - width.toPx(), barOffset),
            size = Size(width.toPx(), barHeight),
        )
    }
}

fun resolveImageData(
    image: CivitaiImage,
    favoriteInfo: FavoriteImage?,
    thumbnailWidth: Int = 320,
    useVideoPath: Boolean = false,
): String {
    val localPath =
        if (useVideoPath && image.type == "video") favoriteInfo?.localVideoPath
        else favoriteInfo?.localPath

    val localFile = localPath?.let { File(it) }

    if (localFile != null && localFile.exists()) return localFile.absolutePath

    if (image.url.startsWith("http")) {
        return if (image.type == "video") {
            if (useVideoPath) getVideoPreviewUrl(image.url) else getVideoThumbnailUrl(image.url)
        } else getThumbnailUrl(image.url, thumbnailWidth)
    }

    return image.url
}

fun getThumbnailUrl(url: String, width: Int): String {
    if (!url.contains("image.civitai.com")) return url

    val baseUrl =
        when {
            url.contains("/original=true/") -> url.replace("/original=true/", "/width=$width/")
            url.contains(Regex("/width=\\d+/")) ->
                url.replace(Regex("/width=\\d+/"), "/width=$width/")
            else -> {
                val lastSlashIndex = url.lastIndexOf('/')

                if (lastSlashIndex != -1) {
                    val prefix = url.substring(0, lastSlashIndex)
                    val fileName = url.substring(lastSlashIndex + 1)

                    "$prefix/width=$width/$fileName"
                } else url
            }
        }

    return baseUrl
}

fun getVideoThumbnailUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    val result =
        when {
            url.contains("/original=false/") -> url
            url.contains("/original=true/") -> url.replace("/original=true/", "/original=false/")
            url.contains(Regex("/width=\\d+/")) ->
                url.replace(Regex("/width=\\d+/"), "/original=false/")
            else -> {
                val lastSlashIndex = url.lastIndexOf('/')

                if (lastSlashIndex != -1) {
                    val prefix = url.substring(0, lastSlashIndex)
                    val fileName = url.substring(lastSlashIndex + 1)

                    "$prefix/original=false/$fileName"
                } else url
            }
        }

    return result
}

fun getVideoPreviewUrl(url: String): String {
    return url
}
