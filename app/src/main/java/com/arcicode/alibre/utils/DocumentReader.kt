package com.arcicode.alibre.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object DocumentReader {
    
    private const val TAG = "DocumentReader"
    
    /**
     * Reads the content of a document based on its format
     */
    suspend fun readDocumentContent(context: Context, filePath: String, format: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(filePath)
            Log.d(TAG, "Reading document: $filePath, format: $format")
            
            when (format.lowercase()) {
                "txt" -> readTextFile(context, uri)
                "html", "htm" -> readHtmlFile(context, uri)
                "epub" -> readEpubFile(context, uri)
                "pdf" -> "PDF content cannot be displayed as text. Use PDF viewer."
                "mobi", "azw", "azw3" -> "This format requires specialized reader support."
                "fb2" -> readXmlBasedFile(context, uri)
                "rtf" -> readRtfFile(context, uri)
                else -> "Unsupported format: $format"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading document: $filePath", e)
            "Error reading document: ${e.message}"
        }
    }
    
    /**
     * Reads plain text files
     */
    private fun readTextFile(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                reader.readText()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading text file", e)
            null
        }
    }
    
    /**
     * Reads HTML files and extracts readable content
     */
    private fun readHtmlFile(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val htmlContent = BufferedReader(InputStreamReader(stream)).readText()
                
                // Parse HTML and extract text content
                val doc = Jsoup.parse(htmlContent)
                
                // Remove script and style elements
                doc.select("script, style").remove()
                
                // Get title if available
                val title = doc.title()
                val bodyText = doc.body()?.text() ?: doc.text()
                
                if (title.isNotEmpty()) {
                    "$title\n\n$bodyText"
                } else {
                    bodyText
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading HTML file", e)
            null
        }
    }
    
    /**
     * Basic EPUB reading - extracts text from HTML files inside EPUB
     * Note: This is a simplified implementation
     */
    private fun readEpubFile(context: Context, uri: Uri): String? {
        return try {
            // For now, return a placeholder message
            // Full EPUB support would require a specialized library like epublib
            "EPUB format detected. Basic text extraction not yet implemented.\n\n" +
                    "To read EPUB files, this app would need additional libraries for proper EPUB parsing."
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EPUB file", e)
            null
        }
    }
    
    /**
     * Reads XML-based formats like FB2
     */
    private fun readXmlBasedFile(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val xmlContent = BufferedReader(InputStreamReader(stream)).readText()
                
                // Parse XML and extract text content
                val doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser())
                doc.text()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading XML-based file", e)
            null
        }
    }
    
    /**
     * Basic RTF reading - strips RTF codes and extracts plain text
     */
    private fun readRtfFile(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val rtfContent = BufferedReader(InputStreamReader(stream)).readText()
                
                // Basic RTF to text conversion (removes control codes)
                stripRtfFormatting(rtfContent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading RTF file", e)
            null
        }
    }
    
    /**
     * Strips RTF formatting and extracts plain text
     */
    private fun stripRtfFormatting(rtfContent: String): String {
        var text = rtfContent
        
        // Remove RTF control codes (basic implementation)
        text = text.replace(Regex("\\\\[a-z]+\\d*\\s?"), "")
        text = text.replace(Regex("\\{"), "")
        text = text.replace(Regex("\\}"), "")
        text = text.replace(Regex("\\\\"), "")
        
        // Clean up extra whitespace
        text = text.replace(Regex("\\s+"), " ")
        text = text.trim()
        
        return text
    }
    
    /**
     * Checks if the format supports PDF viewing
     */
    fun supportsPdfViewing(format: String): Boolean {
        return format.lowercase() == "pdf"
    }
    
    /**
     * Checks if the format supports text-based reading
     */
    fun supportsTextReading(format: String): Boolean {
        return when (format.lowercase()) {
            "txt", "html", "htm", "fb2", "rtf" -> true
            else -> false
        }
    }
    
    /**
     * Gets a user-friendly error message for unsupported formats
     */
    fun getUnsupportedFormatMessage(format: String): String {
        return when (format.lowercase()) {
            "epub" -> "EPUB support requires additional libraries. Basic text extraction is limited."
            "mobi", "azw", "azw3" -> "Kindle formats require specialized reader libraries."
            "pdf" -> "PDF files should be viewed using the built-in PDF viewer."
            else -> "Format '$format' is not yet supported for text reading."
        }
    }
}
