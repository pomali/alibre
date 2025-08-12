package com.arcicode.alibre.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.arcicode.alibre.databinding.FragmentReadingBinding

class ReadingFragment : Fragment() {

    private val TAG = "ReadingFragment"

    private var _binding: FragmentReadingBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ReadingViewModel
    private val args: ReadingFragmentArgs by navArgs()
    
    private var currentScrollPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ReadingViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupWebView()
        
        // Load the book
        val bookId = args.bookId
        val bookPath = args.bookPath
        
        Log.d(TAG, "Loading book - ID: $bookId, Path: $bookPath")
        viewModel.loadBook(bookId, bookPath)
    }
    
    private fun setupObservers() {
        viewModel.book.observe(viewLifecycleOwner) { book ->
            book?.let {
                binding.textViewBookTitle.text = it.title
                
                // Restore last reading position if available
                val lastPosition = viewModel.getLastReadingPosition()
                if (lastPosition > 0) {
                    currentScrollPosition = lastPosition
                }
            }
        }
        
        viewModel.content.observe(viewLifecycleOwner) { content ->
            content?.let {
                displayContent(it)
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarReading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
        
        viewModel.scrollPosition.observe(viewLifecycleOwner) { position ->
            // Update scroll position if needed
        }
    }
    
    private fun setupWebView() {
        binding.webView.webViewClient = WebViewClient()
        binding.webView.settings.apply {
            javaScriptEnabled = false // Keep disabled for security
            builtInZoomControls = true
            displayZoomControls = false
        }
    }
    
    private fun displayContent(content: String) {
        hideAllViews()
        
        when (content) {
            "PDF_VIEWER_MODE" -> {
                displayPdfPlaceholder()
            }
            else -> {
                displayTextContent(content)
            }
        }
    }
    
    private fun displayTextContent(content: String) {
        binding.textViewBookContent.text = content
        binding.scrollViewContent.visibility = View.VISIBLE
        
        // Restore scroll position after content is set
        binding.scrollViewContent.post {
            binding.scrollViewContent.scrollTo(0, currentScrollPosition)
        }
        
        // Set up scroll position tracking
        binding.scrollViewContent.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            currentScrollPosition = scrollY
            // Save position periodically (every 500px scroll)
            if (scrollY % 500 == 0) {
                viewModel.saveReadingPosition(scrollY)
            }
        }
    }
    
    private fun displayPdfPlaceholder() {
        binding.textViewBookContent.text = "PDF viewer is not yet implemented.\n\nThis is a PDF file that would normally be displayed using a PDF viewer library."
        binding.scrollViewContent.visibility = View.VISIBLE
    }
    
    private fun displayHtmlContent(content: String) {
        binding.webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
        binding.webView.visibility = View.VISIBLE
    }
    
    private fun hideAllViews() {
        binding.scrollViewContent.visibility = View.GONE
        binding.webView.visibility = View.GONE
        binding.textViewError.visibility = View.GONE
    }
    
    private fun showError(error: String) {
        hideAllViews()
        binding.textViewError.text = error
        binding.textViewError.visibility = View.VISIBLE
        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
    }
    
    override fun onPause() {
        super.onPause()
        // Save current reading position when leaving the fragment
        viewModel.saveReadingPosition(currentScrollPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

