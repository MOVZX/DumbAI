package org.movzx.dumbai.data

import androidx.room.*
import org.movzx.dumbai.model.FavoriteImage
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteImageDao {
    @Query("SELECT * FROM favorite_images ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteImage>>

    @Query("SELECT * FROM favorite_images ORDER BY timestamp DESC")
    suspend fun getAllFavoritesSync(): List<FavoriteImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(image: FavoriteImage)

    @Delete
    suspend fun deleteFavorite(image: FavoriteImage)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_images WHERE id = :id)")
    fun isFavorite(id: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_images WHERE id = :id)")
    suspend fun isFavoriteDirect(id: Long): Boolean

    @Query("SELECT id FROM favorite_images")
    fun getAllFavoriteIds(): Flow<List<Long>>
}

@Dao
interface FeedDao {
    @Query("SELECT * FROM cached_feed_images WHERE feedType = :type ORDER BY `order` ASC")
    suspend fun getCachedFeed(type: String): List<org.movzx.dumbai.model.CachedFeedImage>

    @Query("DELETE FROM cached_feed_images WHERE feedType = :type")
    suspend fun clearFeed(type: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(images: List<org.movzx.dumbai.model.CachedFeedImage>)
}
