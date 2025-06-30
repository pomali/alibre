package com.example.alibre.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.alibre.ui.theme.AlibreTheme

import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    encodedBookUri: String?, // Changed from bookPath to encodedBookUri
    onNavigateBack: () -> Unit
) {
    val bookUri = remember(encodedBookUri) {
        encodedBookUri?.let { Uri.parse(Uri.decode(it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bookUri?.lastPathSegment ?: "Reader") }, // Display filename or "Reader"
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Reader Screen")
            if (bookUri != null) {
                Text("Displaying URI: ${bookUri.toString()}") // Display the full URI for now
                // Actual book content rendering will happen in later steps
            } else {
                Text("No book selected or URI is invalid.")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    AlibreTheme {
        // Simulate an encoded URI string
        val sampleUri = Uri.parse("content://com.example/document/123.pdf")
        ReaderScreen(encodedBookUri = Uri.encode(sampleUri.toString()), onNavigateBack = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenNoBookPreview() {
    AlibreTheme {
        ReaderScreen(encodedBookUri = null, onNavigateBack = {})
    }
}
