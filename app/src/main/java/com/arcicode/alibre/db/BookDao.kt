package com.arcicode.alibre.db // Updated package

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arcicode.alibre.model.Book // Updated import

@Dao
interface BookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookById(bookId: Long): LiveData<Book?>

    @Query("SELECT * FROM books ORDER BY lastOpenedTimestamp DESC")
    fun getAllBooks(): LiveData<List<Book>>

    @Query("SELECT * FROM books WHERE filePath = :filePath LIMIT 1")
    suspend fun getBookByFilePath(filePath: String): Book?

    @Query("SELECT * FROM books WHERE title LIKE :query OR author LIKE :query ORDER BY title ASC")
    fun searchBooks(query: String): LiveData<List<Book>>

    @Query("DELETE FROM books WHERE filePath NOT IN (:validFilePaths)")
    suspend fun deleteBooksNotInPathList(validFilePaths: List<String>)

    @Query("SELECT * FROM books WHERE filePath LIKE :folderPath || '%'")
    fun getBooksByFolderPath(folderPath: String): LiveData<List<Book>>

    @Query("SELECT filePath FROM books")
    suspend fun getAllBookFilePaths(): List<String>
}

