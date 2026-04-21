package org.movzx.dumbai.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppSettingsBackup(
    val nsfw: String,
    val sort: String,
    val period: String,
    val type: String,
    val tagId: Int?,
    val pageLimit: Int,
    val gridColumns: Int
)

@JsonClass(generateAdapter = true)
data class AppBackup(
    val version: Int = 1,
    val settings: AppSettingsBackup?,
    val favorites: List<FavoriteImage>
)
