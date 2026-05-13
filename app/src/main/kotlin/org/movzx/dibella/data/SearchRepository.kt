package org.movzx.dibella.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import org.movzx.dibella.api.CivitaiSearchApi
import org.movzx.dibella.model.CivitaiSearchResult
import org.movzx.dibella.model.SearchQuery
import org.movzx.dibella.model.SearchRequest
import org.movzx.dibella.util.Logger

@Singleton
class SearchRepository
@Inject
constructor(
    private val civitaiSearchApi: CivitaiSearchApi,
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend fun search(
        query: String,
        type: String,
        sort: String,
        limit: Int = 200,
        offset: Int = 0,
        bearerToken: String,
    ): Pair<List<CivitaiSearchResult>, Int> {
        var attempt = 0

        while (attempt < 3) {
            try {
                Logger.d(
                    "Dibella_Search",
                    "Search Request | Query: $query, Type: $type, Sort: $sort, Limit: $limit, Offset: $offset",
                )

                val typeFilter =
                    when (type) {
                        "image" -> "\"type\"=\"image\""
                        "video" -> "\"type\"=\"video\""
                        else -> "\"type\"=\"image\" OR \"type\"=\"video\""
                    }

                val nsfwFilter = buildNsfwFilter()

                val sortArray =
                    when (sort) {
                        "Most Reactions" -> listOf("stats.reactionCountAllTime:desc")
                        "Most Comments" -> listOf("stats.commentCountAllTime:desc")
                        "Most Collected" -> listOf("stats.collectedCountAllTime:desc")
                        "Most Buzz" -> listOf("stats.tippedAmountCountAllTime:desc")
                        "Newest" -> listOf("createdAt:desc")
                        else -> null
                    }

                val searchQuery =
                    SearchQuery(
                        q = query,
                        limit = limit,
                        offset = offset,
                        filter = listOf(typeFilter, buildNsfwFilter()),
                        sort = sortArray,
                    )

                val request = SearchRequest(queries = listOf(searchQuery))
                val authorization = "Bearer $bearerToken"
                val response = civitaiSearchApi.search(request, authorization)
                val hits = response.results.firstOrNull()?.hits ?: emptyList()
                val totalHits = response.results.firstOrNull()?.estimatedTotalHits ?: 0

                Logger.d(
                    "Dibella_Search",
                    "Search Response | Hits: ${hits.size}, Total: $totalHits",
                )

                return hits to totalHits
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                attempt++

                Logger.e("Dibella_Search", "Search Error (Attempt $attempt): ${e.message}")

                if (attempt < 3) delay(2000L * attempt)
            }
        }

        throw Exception("Failed to search after $attempt attempts")
    }

    private fun buildNsfwFilter(): String {
        return "(poi != true OR user.username = MOVZX) AND (minor != true) AND (nsfwLevel=1 OR nsfwLevel=2 OR nsfwLevel=4 OR nsfwLevel=8 OR nsfwLevel=16)"
    }
}
