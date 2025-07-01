package com.arcicode.alibre.ui // Updated package

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcicode.alibre.R
import com.arcicode.alibre.databinding.FragmentLibraryBinding
import com.arcicode.alibre.model.Book
import com.arcicode.alibre.model.LibraryFolder
import com.arcicode.alibre.utils.FileScanner
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LibraryViewModel
    private lateinit var booksAdapter: BooksAdapter
    private lateinit var foldersAdapter: LibraryFoldersAdapter

    private val openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val contentResolver = requireActivity().contentResolver
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    saveLibraryFolderAndScan(uri)
                } catch (e: SecurityException) {
                    Toast.makeText(requireContext(), "Failed to persist folder permissions.", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[LibraryViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.buttonAddFolder.setOnClickListener {
            openFolderPicker()
        }
    }

    private fun setupRecyclerView() {
        booksAdapter = BooksAdapter { book ->
            onBookClicked(book)
        }
        
        binding.recyclerViewBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = booksAdapter
        }
        
        foldersAdapter = LibraryFoldersAdapter { folder ->
            onRemoveFolderClicked(folder)
        }
        
        binding.recyclerViewFolders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = foldersAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.allBooks.observe(viewLifecycleOwner) { books ->
            booksAdapter.submitList(books)
            
            // Show/hide empty state
            if (books.isEmpty()) {
                binding.recyclerViewBooks.visibility = View.GONE
                binding.textViewEmptyState.visibility = View.VISIBLE
            } else {
                binding.recyclerViewBooks.visibility = View.VISIBLE
                binding.textViewEmptyState.visibility = View.GONE
            }
        }
        
        viewModel.allLibraryFolders.observe(viewLifecycleOwner) { folders ->
            foldersAdapter.submitList(folders)
            
            // Show/hide folders empty state
            if (folders.isEmpty()) {
                binding.recyclerViewFolders.visibility = View.GONE
                binding.textViewFoldersEmpty.visibility = View.VISIBLE
            } else {
                binding.recyclerViewFolders.visibility = View.VISIBLE
                binding.textViewFoldersEmpty.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun onBookClicked(book: Book) {
        // Update last opened timestamp
        viewModel.updateBookLastOpened(book)
        
        // Navigate to reading fragment
        val bundle = Bundle().apply {
            putString("bookPath", book.filePath)
            putLong("bookId", book.id)
        }
        findNavController().navigate(R.id.action_libraryFragment_to_readingFragment, bundle)
    }
    
    private fun onRemoveFolderClicked(folder: LibraryFolder) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Remove Folder")
            .setMessage("Are you sure you want to remove this folder from your library?\n\nThis will:\n• Remove the folder from your library\n• Remove all books from this folder from your library\n• NOT delete the actual files on your device")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeLibraryFolder(folder)
                Toast.makeText(requireContext(), "Folder and its books removed from library", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        openDocumentTreeLauncher.launch(intent)
    }

    private fun saveLibraryFolderAndScan(uri: Uri) {
        val folderPath = uri.toString()
        lifecycleScope.launch {
            val existingFolder = viewModel.getLibraryFolderByPath(folderPath)
            if (existingFolder == null) {
                // Add folder to database
                viewModel.addLibraryFolder(LibraryFolder(path = folderPath))
                
                // Show scanning message
                Toast.makeText(requireContext(), "Scanning folder for ebooks...", Toast.LENGTH_SHORT).show()
                
                // Scan folder for books
                try {
                    val books = FileScanner.scanFolderForBooks(requireContext(), uri)
                    if (books.isNotEmpty()) {
                        viewModel.addBooks(books)
                        Toast.makeText(requireContext(), "Found ${books.size} ebook(s)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No ebooks found in this folder", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error scanning folder: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(requireContext(), "Folder already in library.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

