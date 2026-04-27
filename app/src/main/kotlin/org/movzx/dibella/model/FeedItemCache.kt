package org.movzx.dibella.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "feed_cache")
data class FeedItemCache(
    @PrimaryKey val id: Long,
    val url: String,
    val width: Int?,
    val height: Int?,
    val nsfw: Boolean?,
    val type: String?,
    val feedType: String,
    val orderIndex: Int,
) {
    fun toCivitaiImage(): CivitaiImage {
        return CivitaiImage(
            id = id,
            url = url,
            width = width,
            height = height,
            nsfw = nsfw,
            type = type,
            meta = null,
        )
    }

    companion object {
        fun fromCivitaiImage(image: CivitaiImage, feedType: String, index: Int): FeedItemCache {
            return FeedItemCache(
                id = image.id,
                url = image.url,
                width = image.width,
                height = image.height,
                nsfw = image.nsfw,
                type = image.type,
                feedType = feedType,
                orderIndex = index,
            )
        }
    }
}
