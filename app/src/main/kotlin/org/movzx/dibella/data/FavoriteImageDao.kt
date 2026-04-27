package org.movzx.dibella.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.movzx.dibella.model.FavoriteImage
import org.movzx.dibella.util.Logger

@Dao
interface FavoriteImageDao {
    @Query("SELECT * FROM favorite_images ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteImage>>

    @Query("SELECT * FROM favorite_images ORDER BY timestamp DESC")
    suspend fun _getAllFavoritesSync(): List<FavoriteImage>

    suspend fun getAllFavoritesSync(): List<FavoriteImage> {
        val favorites = _getAllFavoritesSync()

        Logger.d("Dibella_DB", "DAO: Fetched ${favorites.size} favorites (Sync)")

        return favorites
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertFavorite(image: FavoriteImage)

    @Transaction
    suspend fun insertFavorite(image: FavoriteImage) {
        Logger.d("Dibella_DB", "[${image.id}] DAO: Inserting/Updating favorite")

        _insertFavorite(image)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertFavorites(images: List<FavoriteImage>)

    @Transaction
    suspend fun insertFavorites(images: List<FavoriteImage>) {
        Logger.d("Dibella_DB", "DAO: Bulk inserting ${images.size} favorites")

        _insertFavorites(images)
    }

    @Delete suspend fun _deleteFavorite(image: FavoriteImage)

    @Transaction
    suspend fun deleteFavorite(image: FavoriteImage) {
        Logger.d("Dibella_DB", "[${image.id}] DAO: Deleting favorite record")

        _deleteFavorite(image)
    }

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
