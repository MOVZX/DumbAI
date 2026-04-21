package org.movzx.dumbai.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "favorite_images")
data class FavoriteImage(
    @PrimaryKey val id: Long,
    val url: String,
    val width: Int?,
    val height: Int?,
    val nsfw: Boolean?,
    val type: String? = "image",
    val timestamp: Long = System.currentTimeMillis(),
    val localPath: String? = null
) {
    fun toCivitaiImage(): CivitaiImage {
        return CivitaiImage(
            id = id,
            url = url,
            width = width,
            height = height,
            nsfw = nsfw,
            type = type,
            meta = null
        )
    }

    companion object {
        fun fromCivitaiImage(image: CivitaiImage, localPath: String? = null): FavoriteImage {
            return FavoriteImage(
                id = image.id,
                url = image.url,
                width = image.width,
                height = image.height,
                nsfw = image.nsfw,
                type = image.type,
                localPath = localPath
            )
        }
    }
}

@Entity(tableName = "cached_feed_images")
data class CachedFeedImage(
    @PrimaryKey val id: Long,
    val url: String,
    val width: Int?,
    val height: Int?,
    val nsfw: Boolean?,
    val type: String?,
    val order: Int,
    val feedType: String
) {
    fun toCivitaiImage(): CivitaiImage {
        return CivitaiImage(
            id = id,
            url = url,
            width = width,
            height = height,
            nsfw = nsfw,
            type = type,
            meta = null
        )
    }

    companion object {
        fun fromCivitaiImage(image: CivitaiImage, order: Int, feedType: String): CachedFeedImage {
            return CachedFeedImage(
                id = image.id,
                url = image.url,
                width = image.width,
                height = image.height,
                nsfw = image.nsfw,
                type = image.type,
                order = order,
                feedType = feedType
            )
        }
    }
}
