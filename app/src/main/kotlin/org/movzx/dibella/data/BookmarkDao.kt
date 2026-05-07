package org.movzx.dibella.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.movzx.dibella.model.Bookmark

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    suspend fun getAllBookmarksSync(): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBookmark(bookmark: Bookmark)

    @Update suspend fun updateBookmark(bookmark: Bookmark)

    @Delete suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks") suspend fun clearAll()
}
