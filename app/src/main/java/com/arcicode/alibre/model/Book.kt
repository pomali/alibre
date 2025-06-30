package com.arcicode.alibre.model // Updated package

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val filePath: String,
    val format: String, // e.g., "PDF", "EPUB", "TXT"
    val coverImagePath: String?,
    var lastReadPosition: String?, // Could be page number, character offset, or specific locator string
    var totalPages: Int?, // Or total units, depending on format
    var lastOpenedTimestamp: Long // Timestamp of when the book was last opened
)

