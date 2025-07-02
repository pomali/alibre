package com.arcicode.alibre.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.arcicode.alibre.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileScanner {
    
    private const val TAG = "FileScanner"
    
    // Supported ebook formats
    private val SUPPORTED_FORMATS = listOf(
        "pdf", "txt", "epub", "html", "htm", "mobi", "azw", "azw3", "fb2", "rtf"
    )
    
    /**
     * Scans a folder URI for ebook files and returns a list of Book objects
     */
    suspend fun scanFolderForBooks(context: Context, folderUri: Uri): List<Book> = withContext(Dispatchers.IO) {
        val books = mutableListOf<Book>()
        
        Log.d(TAG, "Starting scan for folder: $folderUri")
        
        try {
            scanDirectoryRecursively(context, folderUri, books)
            Log.d(TAG, "Scan completed. Found ${books.size} books")
        } catch (e: Exception) {
            Log.e(TAG, "Error during folder scan", e)
            e.printStackTrace()
        }
        
        return@withContext books
    }
    
    private suspend fun scanDirectoryRecursively(context: Context, directoryUri: Uri, books: MutableList<Book>) {
        val contentResolver = context.contentResolver
        
        Log.d(TAG, "Scanning directory: $directoryUri")
        
        try {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(directoryUri)
            Log.d(TAG, "Tree document ID: $treeDocumentId")
            
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri, 
                treeDocumentId
            )
            
            Log.d(TAG, "Built children URI: $childrenUri")
            
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
            
            if (cursor == null) {
                Log.w(TAG, "Cursor is null for directory: $directoryUri")
                return
            }
            
            Log.d(TAG, "Cursor has ${cursor.count} entries")
            
            cursor.use { c ->
                val documentIdIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val displayNameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeTypeIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                
                if (documentIdIndex == -1 || displayNameIndex == -1 || mimeTypeIndex == -1) {
                    Log.e(TAG, "One or more required columns not found in cursor")
                    return
                }
                
                while (c.moveToNext()) {
                    try {
                        val documentId = c.getString(documentIdIndex)
                        val displayName = c.getString(displayNameIndex)
                        val mimeType = c.getString(mimeTypeIndex)
                        
                        if (documentId == null || displayName == null) {
                            Log.w(TAG, "Skipping item with null documentId or displayName")
                            continue
                        }
                        
                        Log.d(TAG, "Found item: $displayName (type: $mimeType)")
                        
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)
                        
                        // Check if it's a directory
                        if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                            Log.d(TAG, "Scanning subdirectory: $displayName")
                            // Recursively scan subdirectories
                            scanDirectoryRecursively(context, documentUri, books)
                        } else {
                            // Check if it's a supported ebook format
                            val fileExtension = getFileExtension(displayName)
                            Log.d(TAG, "File extension for $displayName: $fileExtension")
                            
                            if (fileExtension != null) {
                                val lowercaseExt = fileExtension.lowercase()
                                val isSupported = SUPPORTED_FORMATS.contains(lowercaseExt)
                                Log.d(TAG, "Extension '$lowercaseExt' is supported: $isSupported (supported formats: $SUPPORTED_FORMATS)")
                                
                                if (isSupported) {
                                    Log.d(TAG, "Adding supported ebook: $displayName")
                                    val book = createBookFromFile(documentUri.toString(), displayName, fileExtension)
                                    books.add(book)
                                } else {
                                    Log.d(TAG, "Skipping unsupported file: $displayName (extension: $lowercaseExt)")
                                }
                            } else {
                                Log.d(TAG, "Skipping file with no extension: $displayName")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing cursor item", e)
                        // Continue with next item instead of failing completely
                        continue
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scanning directory: $directoryUri - Permission may have been revoked", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: $directoryUri", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Tests if the given URI still has valid permissions
     */
    fun testUriPermissions(context: Context, uri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null
            )
            cursor?.use { true } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "URI permission test failed for $uri", e)
            false
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
        val extension = if (lastDotIndex != -1 && lastDotIndex < filename.length - 1) {
            filename.substring(lastDotIndex + 1)
        } else {
            null
        }
        
        Log.d(TAG, "Extracting extension from '$filename': '$extension'")
        return extension
    }
}
