package org.movzx.dibella.model

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
    val isSynced: Boolean = false,
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
        fun fromCivitaiImage(image: CivitaiImage, isSynced: Boolean = false): FavoriteImage {
            return FavoriteImage(
                id = image.id,
                url = image.url,
                width = image.width,
                height = image.height,
                nsfw = image.nsfw,
                type = image.type,
                isSynced = isSynced,
            )
        }
    }
}
