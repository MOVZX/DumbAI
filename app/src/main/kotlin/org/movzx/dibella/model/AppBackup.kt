package org.movzx.dibella.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppSettingsBackup(
    val nsfw: String,
    val sort: String,
    val period: String,
    val type: String,
    val tagIds: String?,
    val pageLimit: Int,
    val gridColumns: Int,
    val apiKey: String?,
    val favoritesPath: String? = null,
    val hidePlayerControls: Boolean = false,
    val alwaysEnableHD: Boolean = false,
    val alwaysMuteVideo: Boolean = false,
    val feedVideoAutoplay: Boolean = false,
    val feedScrollIndexImage: Int = 0,
    val feedScrollOffsetImage: Int = 0,
    val feedScrollIndexVideo: Int = 0,
    val feedScrollOffsetVideo: Int = 0,
    val favoritesScrollIndex: Int = 0,
    val favoritesScrollOffset: Int = 0,
    val galleryScrollIndex: Int = 0,
    val galleryScrollOffset: Int = 0,
    val nextCursorImage: String? = null,
    val nextCursorVideo: String? = null,
)

@JsonClass(generateAdapter = true)
data class FavoriteImageBackup(
    val id: Long,
    val url: String,
    val nsfw: Boolean?,
    val type: String?,
    val timestamp: Long,
)

@JsonClass(generateAdapter = true)
data class FeedItemBackup(
    val id: Long,
    val url: String,
    val width: Int?,
    val height: Int?,
    val nsfw: Boolean?,
    val type: String?,
    val feedType: String,
    val orderIndex: Int,
)

@JsonClass(generateAdapter = true)
data class AppBackup(
    val version: Int = 1,
    val settings: AppSettingsBackup?,
    val favorites: List<FavoriteImageBackup>,
    val feedItems: List<FeedItemBackup> = emptyList(),
)
