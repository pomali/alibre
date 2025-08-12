package com.arcicode.alibre.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arcicode.alibre.EBookReaderApplication
import com.arcicode.alibre.db.BookDao
import com.arcicode.alibre.model.Book
import com.arcicode.alibre.utils.DocumentReader
import kotlinx.coroutines.launch

class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "ReadingViewModel"
    
    private val bookDao: BookDao
    
    private val _book = MutableLiveData<Book?>()
    val book: LiveData<Book?> = _book
    
    private val _content = MutableLiveData<String?>()
    val content: LiveData<String?> = _content
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _scrollPosition = MutableLiveData<Int>()
    val scrollPosition: LiveData<Int> = _scrollPosition
    
    init {
        val database = (application as EBookReaderApplication).database
        bookDao = database.bookDao()
    }
    
    /**
     * Loads a book and its content
     */
    fun loadBook(bookId: Long, bookPath: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d(TAG, "Loading book with ID: $bookId, path: $bookPath")
                
                // Try to get book from database first
                val book = if (bookId > 0) {
                    bookDao.getBookByIdSync(bookId)
                } else {
                    // Fallback: find book by path
                    bookPath?.let { path ->
                        bookDao.getBookByFilePath(path)
                    }
                }
                
                if (book != null) {
                    _book.value = book
                    loadBookContent(book)
                    
                    // Update last opened timestamp
                    updateLastOpened(book)
                } else {
                    _error.value = "Book not found in library"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading book", e)
                _error.value = "Error loading book: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Loads the content of the book
     */
    private suspend fun loadBookContent(book: Book) {
        try {
            Log.d(TAG, "Loading content for book: ${book.title}, format: ${book.format}")
            
            when {
                DocumentReader.supportsPdfViewing(book.format) -> {
                    _content.value = "PDF_VIEWER_MODE"
                }
                DocumentReader.supportsTextReading(book.format) -> {
                    val content = DocumentReader.readDocumentContent(
                        getApplication(),
                        book.filePath,
                        book.format
                    )
                    _content.value = content ?: "Failed to load document content"
                }
                else -> {
                    _content.value = DocumentReader.getUnsupportedFormatMessage(book.format)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading book content", e)
            _content.value = "Error loading content: ${e.message}"
        }
    }
    
    /**
     * Updates the last opened timestamp for the book
     */
    private suspend fun updateLastOpened(book: Book) {
        try {
            val updatedBook = book.copy(lastOpenedTimestamp = System.currentTimeMillis())
            bookDao.updateBook(updatedBook)
            _book.value = updatedBook
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last opened timestamp", e)
        }
    }
    
    /**
     * Saves the current reading position
     */
    fun saveReadingPosition(position: Int) {
        _scrollPosition.value = position
        
        _book.value?.let { book ->
            viewModelScope.launch {
                try {
                    val updatedBook = book.copy(
                        lastReadPosition = position.toString(),
                        lastOpenedTimestamp = System.currentTimeMillis()
                    )
                    bookDao.updateBook(updatedBook)
                    _book.value = updatedBook
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving reading position", e)
                }
            }
        }
    }
    
    /**
     * Gets the last reading position
     */
    fun getLastReadingPosition(): Int {
        return _book.value?.lastReadPosition?.toIntOrNull() ?: 0
    }
    
    /**
     * Clears any error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Reloads the current book content
     */
    fun reloadContent() {
        _book.value?.let { book ->
            viewModelScope.launch {
                _isLoading.value = true
                loadBookContent(book)
                _isLoading.value = false
            }
        }
    }
}
