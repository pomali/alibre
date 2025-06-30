package com.example.alibre.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alibre.ui.screens.LibraryScreen
import com.example.alibre.ui.screens.ReaderScreen

object Routes {
    const val LIBRARY = "library"
    const val READER = "reader/{bookPath}" // Example: pass book path as argument
    fun readerWithArg(bookPath: String) = "reader/$bookPath"
}

import com.example.alibre.model.Book

@Composable
fun AppNavigator(
    onAddFolderRequested: () -> Unit,
    selectedFolderUris: List<android.net.Uri>,
    books: List<Book> // Add this parameter
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onNavigateToReader = { bookUri -> // Modified to accept book URI
                    navController.navigate(Routes.readerWithArg(Uri.encode(bookUri.toString())))
                },
                onAddFolder = onAddFolderRequested,
                folderUris = selectedFolderUris, // Still pass this for now, maybe remove later
                books = books // Pass the book list
            )
        }
        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookPath = backStackEntry.arguments?.getString("bookPath")
            ReaderScreen(
                bookPath = bookPath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
