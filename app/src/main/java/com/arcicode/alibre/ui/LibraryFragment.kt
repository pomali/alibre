package com.arcicode.alibre.ui // Updated package

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
// import android.provider.DocumentsContract // Not strictly needed for this version
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
// import androidx.navigation.fragment.findNavController // Not used yet
import com.arcicode.alibre.EBookReaderApplication // Updated import
// import com.arcicode.alibre.R // Not strictly needed for this version of file, will be used later
import com.arcicode.alibre.databinding.FragmentLibraryBinding // Updated import
import com.arcicode.alibre.db.LibraryFolderDao // Updated import
import com.arcicode.alibre.model.LibraryFolder // Updated import
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var libraryFolderDao: LibraryFolderDao

    private val openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val contentResolver = requireActivity().contentResolver
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    saveLibraryFolder(uri)
                } catch (e: SecurityException) {
                    Toast.makeText(requireContext(), "Failed to persist folder permissions.", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = requireActivity().application as EBookReaderApplication
        libraryFolderDao = application.database.libraryFolderDao()
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

        binding.buttonAddFolder.setOnClickListener {
            openFolderPicker()
        }

        // TODO: Setup RecyclerView, Observe ViewModel data for library folders and books
        // Example: binding.buttonNavigateToReader.setOnClickListener {
        //     findNavController().navigate(R.id.action_libraryFragment_to_readingFragment)
        // }
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        openDocumentTreeLauncher.launch(intent)
    }

    private fun saveLibraryFolder(uri: Uri) {
        val folderPath = uri.toString()
        lifecycleScope.launch {
            val existingFolder = libraryFolderDao.getLibraryFolderByPath(folderPath)
            if (existingFolder == null) {
                libraryFolderDao.addLibraryFolder(LibraryFolder(path = folderPath))
                Toast.makeText(requireContext(), "Folder added: $folderPath", Toast.LENGTH_SHORT).show()
                // TODO: Trigger scan for books in this folder
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

