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
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
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
    
    fun clearSuccessMessage() {
        _successMessage.value = null
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
                
                val folders = libraryFolderDao.getAllLibraryFoldersSync()
                Log.d(TAG, "Found ${folders.size} library folders to rescan")
                
                if (folders.isEmpty()) {
                    Log.w(TAG, "No library folders found to rescan")
                    _error.value = "No library folders found. Please use 'Add Folder' to add ebook folders to your library."
                    return@launch
                }
                
                val allBooks = mutableListOf<Book>()
                val context = getApplication<Application>()
                
                val failedFolders = mutableListOf<String>()
                
                for (folder in folders) {
                    try {
                        Log.d(TAG, "Rescanning folder: ${folder.path}")
                        val uri = Uri.parse(folder.path)
                        
                        // Test permissions first
                        if (!com.arcicode.alibre.utils.FileScanner.testUriPermissions(context, uri)) {
                            Log.e(TAG, "URI permissions test failed for folder: ${folder.path}")
                            failedFolders.add(folder.path)
                            continue
                        }
                        
                        val books = com.arcicode.alibre.utils.FileScanner.scanFolderForBooks(context, uri)
                        Log.d(TAG, "Found ${books.size} books in folder: ${folder.path}")
                        allBooks.addAll(books)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error rescanning folder: ${folder.path}", e)
                        failedFolders.add(folder.path)
                    }
                }
                
                // Handle failed folders
                if (failedFolders.isNotEmpty()) {
                    val failedCount = failedFolders.size
                    val totalCount = folders.size
                    Log.w(TAG, "Failed to access $failedCount out of $totalCount folders")
                    
                    if (failedCount == totalCount) {
                        // All folders failed
                        _error.value = "Unable to access any library folders. The app may have lost folder permissions. Please use 'Add Folder' to re-grant access to your library folders."
                        return@launch
                    } else {
                        // Some folders failed
                        _error.value = "Unable to access $failedCount out of $totalCount library folders. Use 'Add Folder' to re-grant access to inaccessible folders."
                    }
                }
                
                Log.d(TAG, "Total books found across all folders: ${allBooks.size}")
                
                // Clear existing books and add all found books
                Log.d(TAG, "Clearing existing books from database")
                bookDao.deleteAllBooks()
                
                Log.d(TAG, "Adding ${allBooks.size} books to database")
                allBooks.forEach { book ->
                    Log.d(TAG, "Inserting book: ${book.title} (${book.filePath})")
                    bookDao.insertBook(book)
                }
                
                Log.d(TAG, "Rescan completed. Total books found: ${allBooks.size}")
                
                // Provide appropriate success/completion message
                if (failedFolders.isEmpty()) {
                    // All folders scanned successfully
                    if (allBooks.isNotEmpty()) {
                        _successMessage.value = "Rescan completed! Found ${allBooks.size} book(s)"
                    } else {
                        _successMessage.value = "Rescan completed. No books found in library folders."
                    }
                } else {
                    // Some folders failed, but we still found some books
                    val successfulCount = folders.size - failedFolders.size
                    if (allBooks.isNotEmpty()) {
                        _successMessage.value = "Partial rescan completed! Found ${allBooks.size} book(s) from $successfulCount accessible folder(s)"
                    } else {
                        _successMessage.value = "Rescan completed from $successfulCount accessible folder(s). No books found."
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during library rescan", e)
                _error.value = "Failed to rescan library: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Check which library folders are currently accessible
     * Returns a pair of (accessible folders, inaccessible folders)
     */
    suspend fun checkFolderAccessibility(): Pair<List<LibraryFolder>, List<LibraryFolder>> {
        val allFolders = libraryFolderDao.getAllLibraryFoldersSync()
        val accessibleFolders = mutableListOf<LibraryFolder>()
        val inaccessibleFolders = mutableListOf<LibraryFolder>()
        val context = getApplication<Application>()
        
        for (folder in allFolders) {
            try {
                val uri = Uri.parse(folder.path)
                if (com.arcicode.alibre.utils.FileScanner.testUriPermissions(context, uri)) {
                    accessibleFolders.add(folder)
                } else {
                    inaccessibleFolders.add(folder)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking accessibility for folder: ${folder.path}", e)
                inaccessibleFolders.add(folder)
            }
        }
        
        return Pair(accessibleFolders, inaccessibleFolders)
    }
}
