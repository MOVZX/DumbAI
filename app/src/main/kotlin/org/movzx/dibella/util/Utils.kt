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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

private const val TYPE_IMAGE = "image"
private const val TYPE_VIDEO = "video"
private const val TYPE_THUMBNAILS = "thumbnails"
private const val TYPE_PREVIEWS = "previews"

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

suspend fun hasLocalCache(
    context: Context,
    imageId: Long,
    isVideo: Boolean,
    favoritesDir: File? = null,
): Boolean =
    withContext(Dispatchers.IO) {
        val dir = getEffectiveFavoritesDir(favoritesDir)

        if (!dir.exists()) return@withContext false

        val mediaSub = if (isVideo) TYPE_VIDEO else TYPE_IMAGE
        val baseName = if (isVideo) "${imageId}_thumb" else "$imageId"

        val found =
            FileUtils.IMAGE_EXTENSIONS.any { ext ->
                val thumbFile = File(File(File(dir, mediaSub), TYPE_THUMBNAILS), "$baseName.$ext")

                thumbFile.exists() && thumbFile.length() > 100
            }

        if (found) Logger.v("Dibella_IO", "[$imageId] Local thumbnail cache found")

        return@withContext found
    }

suspend fun hasFullCache(
    context: Context,
    imageId: Long,
    isVideo: Boolean,
    favoritesDir: File? = null,
): Boolean =
    withContext(Dispatchers.IO) {
        val dir = getEffectiveFavoritesDir(favoritesDir)

        if (!dir.exists()) return@withContext false

        val found =
            if (isVideo) {
                FileUtils.VIDEO_EXTENSIONS.any { ext ->
                    val previewFile =
                        File(File(File(dir, TYPE_VIDEO), TYPE_PREVIEWS), "$imageId.$ext")

                    previewFile.exists() && previewFile.length() > 100
                }
            } else {
                FileUtils.IMAGE_EXTENSIONS.any { ext ->
                    val fullFile =
                        File(File(File(dir, TYPE_IMAGE), TYPE_PREVIEWS), "${imageId}_full.$ext")

                    fullFile.exists() && fullFile.length() > 100
                }
            }

        if (found) Logger.v("Dibella_IO", "[$imageId] Full resolution cache found")

        return@withContext found
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
    val isVideo = image.type == TYPE_VIDEO
    val mediaSub = if (isVideo) TYPE_VIDEO else TYPE_IMAGE
    val isFull = useVideoPath || thumbnailWidth > 400
    val contentSub = if (isFull) TYPE_PREVIEWS else TYPE_THUMBNAILS

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

fun getThumbnailUrl(url: String, width: Int): String {
    return CivitaiUrlBuilder.getThumbnailUrl(url, width)
}

fun getVideoThumbnailUrl(url: String): String {
    return CivitaiUrlBuilder.getVideoThumbnailUrl(url)
}

fun getVideoPreviewUrl(url: String): String {
    return CivitaiUrlBuilder.getVideoPreviewUrl(url)
}

fun getVideoOriginalUrl(url: String): String {
    return CivitaiUrlBuilder.getOriginalUrl(url)
}

fun getOriginalUrl(url: String): String {
    return CivitaiUrlBuilder.getOriginalUrl(url)
}

fun modifyCivitaiUrl(url: String, variant: String): String {
    return if (CivitaiUrlBuilder.isCivitaiUrl(url)) {
        if (variant.startsWith("width="))
            CivitaiUrlBuilder.getThumbnailUrl(url, variant.substringAfter("=").toIntOrNull() ?: 450)
        else CivitaiUrlBuilder.getOriginalUrl(url)
    } else url
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60
    val centiseconds = (ms % 1000) / 10
    val s = seconds.toString().padStart(2, '0')
    val cs = centiseconds.toString().padStart(2, '0')

    return when {
        hours > 0 -> {
            val m = minutes.toString().padStart(2, '0')
            "$hours:$m:$s:$cs"
        }
        minutes > 0 -> {
            val m = minutes.toString().padStart(2, '0')
            "$m:$s:$cs"
        }
        else -> "$s:$cs"
    }
}

fun playerPoolSizeForColumns(columns: Int): Int {
    return when (columns) {
        1 -> 4
        2 -> 10
        3 -> 18
        4 -> 24
        else -> 12
    }
}
