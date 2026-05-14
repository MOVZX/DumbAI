package org.movzx.dibella.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.movzx.dibella.R
import org.movzx.dibella.data.BookmarkRepository
import org.movzx.dibella.model.Bookmark

data class BookmarkUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = true,
    val bookmarkCount: Int = 0,
)

@HiltViewModel
class BookmarkViewModel @Inject constructor(private val repository: BookmarkRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(BookmarkUiState())
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<Int>()
    val uiMessage = _uiMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getAllBookmarks().collect { bookmarks ->
                _uiState.update {
                    it.copy(
                        bookmarks = bookmarks,
                        isLoading = false,
                        bookmarkCount = bookmarks.size,
                    )
                }
            }
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
            sendMessage(R.string.msg_bookmark_deleted)
        }
    }

    fun updateBookmarkTitle(bookmark: Bookmark, newTitle: String) {
        viewModelScope.launch {
            repository.updateBookmark(bookmark.copy(title = newTitle))
            sendMessage(R.string.msg_bookmark_edited)
        }
    }

    private fun sendMessage(@StringRes resId: Int) {
        viewModelScope.launch { _uiMessage.emit(resId) }
    }
}
