package com.example.alibre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.alibre.navigation.AppNavigator
import com.example.alibre.ui.theme.AlibreTheme
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.documentfile.provider.DocumentFile
import com.example.alibre.model.Book
import com.example.alibre.model.BookType
import com.example.alibre.model.getBookTypeFromFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val _selectedFolderUris = mutableStateListOf<Uri>()
    val selectedFolderUris: List<Uri> get() = _selectedFolderUris

    private val _books = mutableStateListOf<Book>()
    val books: List<Book> get() = _books

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val openDocumentTreeLauncher = registerForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Log.d("MainActivity", "Selected URI: $uri, Read permission granted.")
                    if (!_selectedFolderUris.contains(uri)) {
                        _selectedFolderUris.add(uri)
                        // Scanning will be triggered by LaunchedEffect observing _selectedFolderUris
                    }
                } catch (e: SecurityException) {
                    Log.e("MainActivity", "Failed to take persistable URI permission for $uri", e)
                    // Handle error: show a message to the user, etc.
                }
            } else {
                Log.d("MainActivity", "No URI selected")
            }
        }

        setContent {
            AlibreTheme {
                // Trigger scanning when selectedFolderUris changes
                LaunchedEffect(selectedFolderUris.toList()) { // Use toList() to ensure change detection
                    scanFoldersForBooks()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigator(
                        onAddFolderRequested = {
                            openDocumentTreeLauncher.launch(null)
                        },
                        selectedFolderUris = selectedFolderUris,
                        books = books // Pass the book list
                    )
                }
            }
        }
    }

    private suspend fun scanFoldersForBooks() {
        Log.d("MainActivity", "Scanning folders for books...")
        val newBookList = mutableListOf<Book>()
        withContext(Dispatchers.IO) { // Perform file operations off the main thread
            for (folderUri in _selectedFolderUris) {
                val parentDocumentFile = DocumentFile.fromTreeUri(applicationContext, folderUri)
                if (parentDocumentFile == null || !parentDocumentFile.isDirectory) {
                    Log.w("MainActivity", "Cannot access folder: $folderUri")
                    continue
                }

                val files = parentDocumentFile.listFiles()
                Log.d("MainActivity", "Folder: ${parentDocumentFile.name}, Files found: ${files.size}")

                for (file in files) {
                    if (file.isFile && file.name != null) {
                        val fileName = file.name ?: "Unknown File"
                        val bookType = getBookTypeFromFileName(fileName)
                        if (bookType != BookType.UNKNOWN) {
                            Log.d("MainActivity", "Found book: $fileName, Type: $bookType, Uri: ${file.uri}")
                            newBookList.add(
                                Book(
                                    uri = file.uri,
                                    title = fileName.substringBeforeLast('.'),
                                    type = bookType
                                )
                            )
                        }
                    }
                }
            }
        }
        // Update the list on the main thread
        withContext(Dispatchers.Main) {
            _books.clear()
            _books.addAll(newBookList.distinctBy { it.uri }) // Avoid duplicates if folders overlap or scanned multiple times
            Log.d("MainActivity", "Total books found: ${_books.size}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AlibreTheme {
        AppNavigator(onAddFolderRequested = {}, selectedFolderUris = emptyList(), books = emptyList())
    }
}
