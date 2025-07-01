package com.arcicode.alibre.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arcicode.alibre.R
import com.arcicode.alibre.model.Book
import java.text.SimpleDateFormat
import java.util.*

class BooksAdapter(
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, BooksAdapter.BookViewHolder>(BookDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view, onBookClick)
    }
    
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class BookViewHolder(
        itemView: View,
        private val onBookClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val titleTextView: TextView = itemView.findViewById(R.id.textView_book_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.textView_book_author)
        private val formatTextView: TextView = itemView.findViewById(R.id.textView_book_format)
        private val lastOpenedTextView: TextView = itemView.findViewById(R.id.textView_last_opened)
        
        fun bind(book: Book) {
            titleTextView.text = book.title
            authorTextView.text = book.author ?: "Unknown Author"
            formatTextView.text = book.format
            
            // Format last opened timestamp
            if (book.lastOpenedTimestamp > 0) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                lastOpenedTextView.text = "Last opened: ${dateFormat.format(Date(book.lastOpenedTimestamp))}"
                lastOpenedTextView.visibility = View.VISIBLE
            } else {
                lastOpenedTextView.text = "Never opened"
                lastOpenedTextView.visibility = View.VISIBLE
            }
            
            itemView.setOnClickListener {
                onBookClick(book)
            }
        }
    }
    
    private class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
}
