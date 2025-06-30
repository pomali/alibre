package com.example.alibre.model

import android.net.Uri

enum class BookType {
    TXT, PDF, EPUB, HTML, UNKNOWN
}

data class Book(
    val uri: Uri,
    val title: String,
    val type: BookType,
    val lastReadPosition: String? = null, // Could be page number, character offset, etc.
    val totalPages: Int? = null, // Or some other progress metric
    val author: String? = null, // Optional: for future use
    val coverImageUri: Uri? = null // Optional: for future use
)

fun getBookTypeFromFileName(fileName: String): BookType {
    return when {
        fileName.endsWith(".txt", ignoreCase = true) -> BookType.TXT
        fileName.endsWith(".pdf", ignoreCase = true) -> BookType.PDF
        fileName.endsWith(".epub", ignoreCase = true) -> BookType.EPUB
        fileName.endsWith(".html", ignoreCase = true) || fileName.endsWith(".htm", ignoreCase = true) -> BookType.HTML
        else -> BookType.UNKNOWN
    }
}
