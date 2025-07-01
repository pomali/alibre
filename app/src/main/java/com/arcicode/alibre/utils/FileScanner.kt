package com.arcicode.alibre.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.arcicode.alibre.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileScanner {
    
    // Supported ebook formats
    private val SUPPORTED_FORMATS = listOf(
        "pdf", "txt", "epub", "html", "htm", "mobi", "azw", "azw3", "fb2", "rtf"
    )
    
    /**
     * Scans a folder URI for ebook files and returns a list of Book objects
     */
    suspend fun scanFolderForBooks(context: Context, folderUri: Uri): List<Book> = withContext(Dispatchers.IO) {
        val books = mutableListOf<Book>()
        
        try {
            scanDirectoryRecursively(context, folderUri, books)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext books
    }
    
    private suspend fun scanDirectoryRecursively(context: Context, directoryUri: Uri, books: MutableList<Book>) {
        val contentResolver = context.contentResolver
        
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri, 
                DocumentsContract.getTreeDocumentId(directoryUri)
            )
            
            val cursor: Cursor? = contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null,
                null,
                null
            )
            
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val documentId = c.getString(0)
                    val displayName = c.getString(1)
                    val mimeType = c.getString(2)
                    // val size = c.getLong(3) // Not currently used
                    
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)
                    
                    // Check if it's a directory
                    if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                        // Recursively scan subdirectories
                        scanDirectoryRecursively(context, documentUri, books)
                    } else {
                        // Check if it's a supported ebook format
                        val fileExtension = getFileExtension(displayName)
                        if (fileExtension != null && SUPPORTED_FORMATS.contains(fileExtension.lowercase())) {
                            val book = createBookFromFile(documentUri.toString(), displayName, fileExtension)
                            books.add(book)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createBookFromFile(filePath: String, fileName: String, format: String): Book {
        // Extract title from filename (remove extension)
        val title = fileName.substringBeforeLast(".")
        
        // Try to extract author from filename if it follows common patterns
        // Common patterns: "Author - Title", "Title by Author", "Author_Title"
        val author = extractAuthorFromFilename(fileName)
        
        return Book(
            title = title,
            author = author,
            filePath = filePath,
            format = format.uppercase(),
            coverImagePath = null,
            lastReadPosition = null,
            totalPages = null,
            lastOpenedTimestamp = 0L
        )
    }
    
    private fun extractAuthorFromFilename(filename: String): String? {
        val nameWithoutExtension = filename.substringBeforeLast(".")
        
        // Pattern: "Author - Title"
        if (nameWithoutExtension.contains(" - ")) {
            val parts = nameWithoutExtension.split(" - ", limit = 2)
            if (parts.size == 2) {
                return parts[0].trim()
            }
        }
        
        // Pattern: "Title by Author"
        if (nameWithoutExtension.contains(" by ")) {
            val parts = nameWithoutExtension.split(" by ", limit = 2)
            if (parts.size == 2) {
                return parts[1].trim()
            }
        }
        
        // Pattern: "Author_Title"
        if (nameWithoutExtension.contains("_")) {
            val parts = nameWithoutExtension.split("_", limit = 2)
            if (parts.size == 2) {
                return parts[0].trim()
            }
        }
        
        return null // Could not extract author
    }
    
    private fun getFileExtension(filename: String): String? {
        val lastDotIndex = filename.lastIndexOf('.')
        return if (lastDotIndex != -1 && lastDotIndex < filename.length - 1) {
            filename.substring(lastDotIndex + 1)
        } else {
            null
        }
    }
}
