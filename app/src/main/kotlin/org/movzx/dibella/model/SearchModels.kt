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
    val nsfwLevel: Int,
    val type: String?,
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
