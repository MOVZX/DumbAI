package org.movzx.dibella.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import org.movzx.dibella.api.CivitaiApi
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FeedItemCache
import org.movzx.dibella.util.Logger

@Singleton
class FeedRepository
@Inject
constructor(
    private val civitaiApi: CivitaiApi,
    private val feedCacheDao: FeedCacheDao,
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend fun getCachedFeed(type: String): List<CivitaiImage> {
        return feedCacheDao.getFeed(type).map { it.toCivitaiImage() }
    }

    suspend fun clearCache(type: String) {
        feedCacheDao.clearFeed(type)
    }

    suspend fun fetchImages(
        type: String,
        nsfw: String,
        sort: String,
        period: String,
        tagIds: String?,
        limit: Int,
        cursor: String?,
        isNew: Boolean,
        startOrderIndex: Int = 0,
    ): Pair<List<CivitaiImage>, String?> {
        var attempt = 0
        var currentCursor = cursor

        while (attempt < 3) {
            try {
                Logger.d(
                    "Dibella_Net",
                    "API Request | Type: $type, Limit: $limit, Cursor: $currentCursor",
                )

                val response =
                    civitaiApi.getImages(
                        limit = limit,
                        nsfw = nsfw,
                        sort = sort,
                        period = period,
                        type = type,
                        tags = tagIds,
                        cursor = currentCursor,
                    )

                val items = response.items.distinctBy { it.id }
                val nextCursor = response.metadata.nextCursor?.substringBefore('|')

                val cacheItems = items.mapIndexed { index, image ->
                    FeedItemCache.fromCivitaiImage(image, type, startOrderIndex + index)
                }

                if (isNew) feedCacheDao.replaceFeed(type, cacheItems)
                else feedCacheDao.insertOrUpdateFeed(cacheItems)

                return items to nextCursor
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                attempt++

                Logger.e("Dibella_Net", "API Error (Attempt $attempt): ${e.message}")

                val newCursor = currentCursor?.toLongOrNull()?.let { (it + 1).toString() }

                if (newCursor != null) {
                    currentCursor = newCursor

                    Logger.d("Dibella_Cache", "Cursor recovery: incremented to $newCursor")
                }

                if (attempt < 3) delay((2000L * attempt).coerceAtMost(10000L))
            }
        }

        throw Exception("Failed to fetch images after $attempt attempts")
    }
}
