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
    val hidePlayerControls: Boolean = false,
    val alwaysEnableHD: Boolean = false,
    val alwaysMuteVideo: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class AppBackup(
    val version: Int = 1,
    val settings: AppSettingsBackup?,
    val favorites: List<FavoriteImage>,
)
