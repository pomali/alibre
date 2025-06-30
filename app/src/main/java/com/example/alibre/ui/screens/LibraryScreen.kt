package com.example.alibre.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.alibre.ui.theme.AlibreTheme

import android.net.Uri
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.ui.unit.dp
import com.example.alibre.model.Book
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToReader: (Uri) -> Unit, // Changed to accept Book URI
    onAddFolder: () -> Unit,
    folderUris: List<Uri>, // Kept for now, can be removed if not needed for display
    books: List<Book>      // Add this parameter
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Alibre Library") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp), // Adjusted padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onAddFolder) {
                Text("Add Folder")
            }
            // Removed the generic "Go to Reader (Test)" button

            Spacer(modifier = Modifier.height(16.dp))

            if (books.isEmpty()) {
                Text(
                    text = if (folderUris.isEmpty()) "Click 'Add Folder' to select a directory." else "No supported books found in selected folders. Add more folders or check file types (.txt, .pdf, .epub, .html).",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(books, key = { it.uri }) { book ->
                        ListItem(
                            headlineContent = { Text(book.title, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { Text("Type: ${book.type}") },
                            modifier = Modifier.clickable { onNavigateToReader(book.uri) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview_WithBooks() {
    AlibreTheme {
        val sampleBooks = listOf(
            Book(Uri.parse("file:///example.pdf"), "Example PDF Book", com.example.alibre.model.BookType.PDF),
            Book(Uri.parse("file:///example.epub"), "Another EPub Adventure", com.example.alibre.model.BookType.EPUB),
            Book(Uri.parse("file:///example.txt"), "Simple Text File", com.example.alibre.model.BookType.TXT)
        )
        LibraryScreen(
            onNavigateToReader = {},
            onAddFolder = {},
            folderUris = listOf(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments")),
            books = sampleBooks
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview_NoBooks() {
    AlibreTheme {
        LibraryScreen(
            onNavigateToReader = {},
            onAddFolder = {},
            folderUris = listOf(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments")),
            books = emptyList()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview_NoFolders() {
    AlibreTheme {
        LibraryScreen(
            onNavigateToReader = {},
            onAddFolder = {},
            folderUris = emptyList(),
            books = emptyList()
        )
    }
}
