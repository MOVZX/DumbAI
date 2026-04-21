package org.movzx.dumbai.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import androidx.compose.runtime.Immutable

@Immutable
@JsonClass(generateAdapter = true)
data class CivitaiImage(
    val id: Long,
    val url: String,
    val width: Int?,
    val height: Int?,
    val nsfw: Boolean?,
    val type: String? = "image",
    val meta: VideoMeta? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class VideoMeta(
    val size: Long? = null
)

@JsonClass(generateAdapter = true)
data class CivitaiApiResponse(
    val items: List<CivitaiImage>,
    val metadata: Metadata
)

@JsonClass(generateAdapter = true)
data class Metadata(
    val nextCursor: String?
)
