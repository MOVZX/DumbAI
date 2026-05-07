package org.movzx.dibella.data

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import org.movzx.dibella.model.*
import org.movzx.dibella.util.CivitaiUrlBuilder

@Singleton
class BackupRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: UserPreferencesRepository,
    private val favoritesRepository: FavoritesRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val feedCacheDao: FeedCacheDao,
    private val moshi: Moshi,
) {
    suspend fun exportData(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val favorites =
                    favoritesRepository.getAllFavoritesSync().map { fav ->
                        FavoriteImageBackup(
                            id = fav.id,
                            url = CivitaiUrlBuilder.compressUrl(fav.url),
                            nsfw = fav.nsfw,
                            type = fav.type,
                            timestamp = fav.timestamp,
                        )
                    }

                val bookmarks =
                    bookmarkRepository.getAllBookmarksSync().map { bm ->
                        BookmarkBackup(
                            id = bm.id,
                            title = bm.title,
                            type = bm.type,
                            sort = bm.sort,
                            period = bm.period,
                            nsfw = bm.nsfw,
                            cursor = bm.cursor,
                            tags = bm.tags,
                            timestamp = bm.timestamp,
                        )
                    }

                val settings = repository.getCurrentSettings()

                val feedItems =
                    feedCacheDao.getAllFeedItemsSync().map { item ->
                        FeedItemBackup(
                            id = item.id,
                            url = CivitaiUrlBuilder.compressUrl(item.url),
                            width = item.width,
                            height = item.height,
                            nsfw = item.nsfw,
                            type = item.type,
                            feedType = item.feedType,
                            orderIndex = item.orderIndex,
                        )
                    }

                val backup =
                    AppBackup(
                        version = 1,
                        settings = settings,
                        favorites = favorites,
                        bookmarks = bookmarks,
                        feedItems = feedItems,
                    )

                val adapter = moshi.adapter(AppBackup::class.java)

                context.contentResolver.openOutputStream(uri)?.let { os ->
                    os.sink().buffer().use { sink -> adapter.toJson(sink, backup) }
                }

                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun importData(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val source = inputStream.source().buffer()
                    val adapter = moshi.adapter(AppBackup::class.java)
                    val backup = adapter.fromJson(source) ?: return@withContext false

                    backup.settings?.let { repository.importSettings(it) }

                    val favoriteImages =
                        backup.favorites.map { back ->
                            FavoriteImage(
                                id = back.id,
                                url = CivitaiUrlBuilder.expandUrl(back.url, back.type),
                                width = null,
                                height = null,
                                nsfw = back.nsfw,
                                type = back.type,
                                timestamp = back.timestamp,
                                isSynced = false,
                            )
                        }

                    favoritesRepository.importFavorites(favoriteImages)

                    backup.bookmarks.forEach { bm ->
                        bookmarkRepository.addBookmark(
                            Bookmark(
                                title = bm.title,
                                type = bm.type,
                                sort = bm.sort,
                                period = bm.period,
                                nsfw = bm.nsfw,
                                cursor = bm.cursor,
                                tags = bm.tags,
                                timestamp = bm.timestamp,
                            )
                        )
                    }

                    if (backup.feedItems.isNotEmpty()) {
                        val feedItems =
                            backup.feedItems.map { back ->
                                FeedItemCache(
                                    id = back.id,
                                    url = CivitaiUrlBuilder.expandUrl(back.url, back.type),
                                    width = back.width,
                                    height = back.height,
                                    nsfw = back.nsfw,
                                    type = back.type,
                                    feedType = back.feedType,
                                    orderIndex = back.orderIndex,
                                )
                            }
                        feedCacheDao.insertFeed(feedItems)
                    }

                    true
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
}
