package org.movzx.dibella.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.movzx.dibella.model.FavoriteImage

@Dao
interface FavoriteImageDao {
    @Query("SELECT * FROM favorite_images ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteImage>>

    @Query("SELECT * FROM favorite_images ORDER BY timestamp DESC")
    suspend fun getAllFavoritesSync(): List<FavoriteImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(image: FavoriteImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(images: List<FavoriteImage>)

    @Delete suspend fun deleteFavorite(image: FavoriteImage)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_images WHERE id = :id)")
    fun isFavorite(id: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_images WHERE id = :id)")
    suspend fun isFavoriteDirect(id: Long): Boolean

    @Query("SELECT id FROM favorite_images") fun getAllFavoriteIds(): Flow<List<Long>>

    @Query("SELECT * FROM favorite_images WHERE id = :id")
    suspend fun getFavorite(id: Long): FavoriteImage?

    @Query("SELECT * FROM favorite_images WHERE id = :id")
    fun getFavoriteFlow(id: Long): Flow<FavoriteImage?>
}
