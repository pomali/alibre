package com.arcicode.alibre.model // Updated package

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_folders")
data class LibraryFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String
)

