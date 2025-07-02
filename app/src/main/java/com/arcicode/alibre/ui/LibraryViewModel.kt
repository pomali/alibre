package com.arcicode.alibre.ui

import android.app.Application
import android.net.Uri
import android.util.Log
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
    
    private val TAG = "LibraryViewModel"
    
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
                Log.d(TAG, "Adding ${books.size} books to database")
                
                books.forEach { book ->
                    Log.d(TAG, "Processing book: ${book.title} at ${book.filePath}")
                    // Check if book already exists by file path
                    val existingBook = bookDao.getBookByFilePath(book.filePath)
                    if (existingBook == null) {
                        Log.d(TAG, "Inserting new book: ${book.title}")
                        bookDao.insertBook(book)
                    } else {
                        Log.d(TAG, "Book already exists, skipping: ${book.title}")
                    }
                }
                Log.d(TAG, "Finished adding books")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add books", e)
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
    
    fun rescanAllLibraryFolders() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Starting rescan of all library folders")
                
                val folders = libraryFolderDao.getAllLibraryFolders().value ?: emptyList()
                Log.d(TAG, "Found ${folders.size} library folders to rescan")
                
                val allBooks = mutableListOf<Book>()
                val context = getApplication<Application>()
                
                for (folder in folders) {
                    try {
                        Log.d(TAG, "Rescanning folder: ${folder.path}")
                        val uri = Uri.parse(folder.path)
                        val books = com.arcicode.alibre.utils.FileScanner.scanFolderForBooks(context, uri)
                        Log.d(TAG, "Found ${books.size} books in folder: ${folder.path}")
                        allBooks.addAll(books)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error rescanning folder: ${folder.path}", e)
                        _error.value = "Error rescanning folder: ${e.message}"
                    }
                }
                
                // Clear existing books and add all found books
                bookDao.deleteAllBooks()
                allBooks.forEach { book ->
                    bookDao.insertBook(book)
                }
                
                Log.d(TAG, "Rescan completed. Total books found: ${allBooks.size}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during library rescan", e)
                _error.value = "Failed to rescan library: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
