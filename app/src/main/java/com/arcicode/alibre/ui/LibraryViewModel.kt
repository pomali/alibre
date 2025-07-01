package com.arcicode.alibre.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arcicode.alibre.EBookReaderApplication
import com.arcicode.alibre.db.BookDao
import com.arcicode.alibre.db.LibraryFolderDao
import com.arcicode.alibre.model.Book
import com.arcicode.alibre.model.LibraryFolder
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bookDao: BookDao
    private val libraryFolderDao: LibraryFolderDao
    
    val allBooks: LiveData<List<Book>>
    val allLibraryFolders: LiveData<List<LibraryFolder>>
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        val database = (application as EBookReaderApplication).database
        bookDao = database.bookDao()
        libraryFolderDao = database.libraryFolderDao()
        
        allBooks = bookDao.getAllBooks()
        allLibraryFolders = libraryFolderDao.getAllLibraryFolders()
    }
    
    fun addLibraryFolder(folder: LibraryFolder) {
        viewModelScope.launch {
            try {
                libraryFolderDao.addLibraryFolder(folder)
            } catch (e: Exception) {
                _error.value = "Failed to add library folder: ${e.message}"
            }
        }
    }
    
    fun addBooks(books: List<Book>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                books.forEach { book ->
                    // Check if book already exists by file path
                    val existingBook = bookDao.getBookByFilePath(book.filePath)
                    if (existingBook == null) {
                        bookDao.insertBook(book)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to add books: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateBookLastOpened(book: Book) {
        viewModelScope.launch {
            try {
                val updatedBook = book.copy(lastOpenedTimestamp = System.currentTimeMillis())
                bookDao.updateBook(updatedBook)
            } catch (e: Exception) {
                _error.value = "Failed to update book: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    suspend fun getLibraryFolderByPath(path: String): LibraryFolder? {
        return libraryFolderDao.getLibraryFolderByPath(path)
    }
    
    fun removeLibraryFolder(folder: LibraryFolder) {
        viewModelScope.launch {
            try {
                libraryFolderDao.removeLibraryFolder(folder)
                // Also remove books from this folder
                bookDao.deleteBooksByFolderPath(folder.path)
            } catch (e: Exception) {
                _error.value = "Failed to remove library folder: ${e.message}"
            }
        }
    }
}
