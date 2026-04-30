package org.movzx.dibella.util

import android.content.Context
import android.os.Environment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
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
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

fun Modifier.scrollbar(state: ScrollState, width: Dp = 4.dp, color: Color? = null): Modifier =
    composed {
        val barColor = color ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                color = barColor.copy(alpha = alpha * barColor.alpha),
                topLeft = Offset(size.width - width.toPx(), barOffset),
                size = Size(width.toPx(), barHeight),
            )
        }
    }

private fun getEffectiveFavoritesDir(favoritesDir: File?): File {
    return favoritesDir
        ?: File(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Dibella",
            ),
            ".favorite",
        )
}

fun hasLocalCache(
    context: Context,
    imageId: Long,
    isVideo: Boolean,
    favoritesDir: File? = null,
): Boolean {
    val dir = getEffectiveFavoritesDir(favoritesDir)

    if (!dir.exists()) return false

    val mediaSub = if (isVideo) "video" else "image"
    val baseName = if (isVideo) "${imageId}_thumb" else "$imageId"

    val found =
        FileUtils.IMAGE_EXTENSIONS.any { ext ->
            val thumbFile = File(File(File(dir, mediaSub), "thumbnails"), "$baseName.$ext")

            thumbFile.exists() && thumbFile.length() > 100
        }

    if (found) Logger.v("Dibella_IO", "[$imageId] Local thumbnail cache found")

    return found
}

fun hasFullCache(
    context: Context,
    imageId: Long,
    isVideo: Boolean,
    favoritesDir: File? = null,
): Boolean {
    val dir = getEffectiveFavoritesDir(favoritesDir)

    if (!dir.exists()) return false

    val found =
        if (isVideo) {
            FileUtils.VIDEO_EXTENSIONS.any { ext ->
                val previewFile = File(File(File(dir, "video"), "previews"), "$imageId.$ext")

                previewFile.exists() && previewFile.length() > 100
            }
        } else {
            FileUtils.IMAGE_EXTENSIONS.any { ext ->
                val fullFile = File(File(File(dir, "image"), "previews"), "${imageId}_full.$ext")

                fullFile.exists() && fullFile.length() > 100
            }
        }

    if (found) Logger.v("Dibella_IO", "[$imageId] Full resolution cache found")

    return found
}

fun resolveImageData(
    context: Context,
    image: CivitaiImage,
    favoriteInfo: FavoriteImage?,
    thumbnailWidth: Int = 320,
    useVideoPath: Boolean = false,
    favoritesDir: File? = null,
): String {
    val dir = getEffectiveFavoritesDir(favoritesDir)
    val isVideo = image.type == "video"
    val mediaSub = if (isVideo) "video" else "image"
    val isFull = useVideoPath || thumbnailWidth > 400
    val contentSub = if (isFull) "previews" else "thumbnails"

    val baseName =
        when {
            isVideo && useVideoPath -> "${image.id}"
            isVideo && !useVideoPath -> "${image.id}_thumb"
            !isVideo && isFull -> "${image.id}_full"
            else -> "${image.id}"
        }

    val extensions =
        if (isVideo && useVideoPath) FileUtils.VIDEO_EXTENSIONS else FileUtils.IMAGE_EXTENSIONS

    if (dir.exists()) {
        for (ext in extensions) {
            val file = File(File(File(dir, mediaSub), contentSub), "$baseName.$ext")

            if (file.exists() && file.length() > 100) return file.absolutePath
        }
    }

    if (image.url.startsWith("http")) {
        val remoteUrl =
            if (isVideo)
                if (useVideoPath) getVideoPreviewUrl(image.url) else getVideoThumbnailUrl(image.url)
            else getThumbnailUrl(image.url, thumbnailWidth)

        Logger.d("Dibella_Res", "ID: ${image.id} | Remote | URL: $remoteUrl")

        return remoteUrl
    }

    return image.url
}

fun modifyCivitaiUrl(url: String, variant: String): String {
    if (!url.contains("image.civitai.com")) return url

    return when {
        url.contains("/original=true/") -> url.replace("/original=true/", "/$variant/")
        url.contains("/original=false/") -> url.replace("/original=false/", "/$variant/")
        url.contains(Regex("/width=\\d+/")) -> url.replace(Regex("/width=\\d+/"), "/$variant/")
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

fun getThumbnailUrl(url: String, width: Int): String {
    return modifyCivitaiUrl(url, "width=$width")
}

fun getVideoThumbnailUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    val baseUrl =
        if (url.contains("/original=true/")) url.substringBefore("/original=true/")
        else url.substringBeforeLast("/")

    return "$baseUrl/anim=false,transcode=true,width=450,original=false,optimized=true"
}

fun getVideoPreviewUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    val baseUrl =
        if (url.contains("/original=true/")) url.substringBefore("/original=true/")
        else url.substringBeforeLast("/")

    return "$baseUrl/transcode=true,width=450,optimized=true"
}

fun getVideoOriginalUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    val baseUrl =
        if (url.contains("/original=true/")) url.substringBefore("/original=true/")
        else url.substringBeforeLast("/")

    return "$baseUrl/original=true"
}

fun getOriginalUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    return when {
        url.contains(Regex("/width=\\d+/")) -> url.replace(Regex("/width=\\d+/"), "/original=true/")
        url.contains("/original=false/") -> url.replace("/original=false/", "/original=true/")
        else -> url
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60
    val centiseconds = (ms % 1000) / 10

    return when {
        hours > 0 -> String.format("%d:%02d:%02d:%02d", hours, minutes, seconds, centiseconds)
        minutes > 0 -> String.format("%02d:%02d:%02d", minutes, seconds, centiseconds)
        else -> String.format("%02d:%02d", seconds, centiseconds)
    }
}

fun playerPoolSizeForColumns(columns: Int): Int {
    return when (columns) {
        1 -> 4
        2 -> 10
        3 -> 18
        else -> 12
    }
}
