package org.movzx.dibella.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "bookmarks")
@JsonClass(generateAdapter = true)
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val sort: String,
    val period: String,
    val nsfw: String,
    val cursor: String,
    val tags: String?,
    val query: String? = null,
    val offset: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
