package com.arcicode.alibre.db // Updated package

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arcicode.alibre.model.Book // Updated import
import com.arcicode.alibre.model.LibraryFolder // Updated import

@Database(entities = [Book::class, LibraryFolder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun libraryFolderDao(): LibraryFolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ebook_reader_database" // Database name string, can remain the same or change
                )
                // Wipes and rebuilds instead of migrating if no Migration object.
                // Migration is not part of this first step.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

