package com.arcicode.alibre.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcicode.alibre.databinding.ItemLibraryFolderBinding
import com.arcicode.alibre.model.LibraryFolder

class LibraryFoldersAdapter(
    private val onRemoveFolder: (LibraryFolder) -> Unit
) : ListAdapter<LibraryFolder, LibraryFoldersAdapter.FolderViewHolder>(FolderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemLibraryFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(
        private val binding: ItemLibraryFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: LibraryFolder) {
            val uri = Uri.parse(folder.path)
            val displayName = getDisplayNameFromUri(uri)
            
            binding.textViewFolderName.text = displayName
            binding.textViewFolderPath.text = folder.path
            
            binding.buttonRemoveFolder.setOnClickListener {
                onRemoveFolder(folder)
            }
        }
        
        private fun getDisplayNameFromUri(uri: Uri): String {
            // Extract a human-readable name from the URI
            val segments = uri.pathSegments
            return if (segments.isNotEmpty()) {
                // Try to get the last meaningful segment
                val lastSegment = segments.lastOrNull { it.isNotBlank() }
                lastSegment?.let { segment ->
                    // Decode URL-encoded characters and make it more readable
                    Uri.decode(segment).replace("%3A", ":").replace("%2F", "/")
                } ?: "Unknown Folder"
            } else {
                "Root Folder"
            }
        }
    }

    class FolderDiffCallback : DiffUtil.ItemCallback<LibraryFolder>() {
        override fun areItemsTheSame(oldItem: LibraryFolder, newItem: LibraryFolder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LibraryFolder, newItem: LibraryFolder): Boolean {
            return oldItem == newItem
        }
    }
}
