package com.arcicode.alibre.db // Updated package

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arcicode.alibre.model.LibraryFolder // Updated import

@Dao
interface LibraryFolderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if path already exists
    suspend fun addLibraryFolder(folder: LibraryFolder): Long

    @Delete
    suspend fun removeLibraryFolder(folder: LibraryFolder)

    @Query("SELECT * FROM library_folders ORDER BY path ASC")
    fun getAllLibraryFolders(): LiveData<List<LibraryFolder>>

    @Query("SELECT * FROM library_folders ORDER BY path ASC")
    suspend fun getAllLibraryFoldersSync(): List<LibraryFolder>

    @Query("SELECT * FROM library_folders WHERE path = :path LIMIT 1")
    suspend fun getLibraryFolderByPath(path: String): LibraryFolder?
}

