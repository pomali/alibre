package com.arcicode.alibre.ui // Updated package

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// import com.arcicode.alibre.R // Not used yet
import com.arcicode.alibre.databinding.FragmentReadingBinding // Updated import

class ReadingFragment : Fragment() {

    private var _binding: FragmentReadingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Setup document display (TextView, WebView, PDFView etc.)
        // TODO: Load book content based on arguments
        // Example: val bookPath = arguments?.getString("bookPath")
        // binding.textViewBookTitle.text = bookPath ?: "No book path"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

