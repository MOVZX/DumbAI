package org.movzx.dibella.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class CivitaiSearchResult(
    val id: Long,
    val url: String,
    val width: Int?,
    val height: Int?,
    val nsfwLevel: Any?,
    val type: String?,
    val name: String?,
    val createdAt: String?,
    val user: SearchUser?,
    val stats: SearchStats?,
    val hash: String?,
    val metadata: SearchMetadata?,
    val postId: Long?,
) {
    fun getNsfwLevelInt(): Int? {
        return when (nsfwLevel) {
            is Int -> nsfwLevel
            is Double -> nsfwLevel.toInt()
            is List<*> -> {
                (nsfwLevel as? List<Number>)?.firstOrNull()?.toInt()
            }
            else -> null
        }
    }
}

@Immutable
@JsonClass(generateAdapter = true)
data class SearchUser(val id: Long, val username: String?)

@Immutable
@JsonClass(generateAdapter = true)
data class SearchStats(
    val reactionCountAllTime: Int = 0,
    val commentCountAllTime: Int = 0,
    val collectedCountAllTime: Int = 0,
    val tippedAmountCountAllTime: Int = 0,
)

@Immutable
@JsonClass(generateAdapter = true)
data class SearchMetadata(
    val hash: String? = null,
    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
)

@JsonClass(generateAdapter = true)
data class SearchQuery(
    val q: String,
    val indexUid: String = "images_v6",
    val facets: List<String> = listOf("type", "user.username"),
    val attributesToHighlight: List<String> = emptyList(),
    val highlightPreTag: String = "__ais-highlight__",
    val highlightPostTag: String = "__/ais-highlight__",
    val limit: Int = 20,
    val offset: Int = 0,
    val filter: List<String>? = null,
    val sort: List<String>? = null,
)

@JsonClass(generateAdapter = true) data class SearchRequest(val queries: List<SearchQuery>)

@JsonClass(generateAdapter = true)
data class SearchResponseItem(
    val indexUid: String?,
    val hits: List<CivitaiSearchResult>?,
    val query: String?,
    val processingTimeMs: Int?,
    val limit: Int?,
    val offset: Int?,
    val estimatedTotalHits: Int?,
)

@JsonClass(generateAdapter = true) data class SearchResponse(val results: List<SearchResponseItem>)
