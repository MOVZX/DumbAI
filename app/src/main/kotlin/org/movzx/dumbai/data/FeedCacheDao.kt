package org.movzx.dumbai.data

import androidx.room.*
import org.movzx.dumbai.model.FeedItemCache

@Dao
interface FeedCacheDao {
    @Query("SELECT * FROM feed_cache WHERE feedType = :feedType ORDER BY orderIndex ASC")
    suspend fun getFeed(feedType: String): List<FeedItemCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(items: List<FeedItemCache>)

    @Query("DELETE FROM feed_cache WHERE feedType = :feedType")
    suspend fun clearFeed(feedType: String)

    @Transaction
    suspend fun replaceFeed(feedType: String, items: List<FeedItemCache>) {
        clearFeed(feedType)
        insertFeed(items)
    }
}
