package org.movzx.dibella.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import org.movzx.dibella.model.Bookmark

@Singleton
class BookmarkRepository @Inject constructor(private val bookmarkDao: BookmarkDao) {
    fun getAllBookmarks(): Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    suspend fun getAllBookmarksSync(): List<Bookmark> = bookmarkDao.getAllBookmarksSync()

    suspend fun addBookmark(bookmark: Bookmark) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(bookmark)
    }
}
