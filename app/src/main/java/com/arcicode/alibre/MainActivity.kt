package com.arcicode.alibre // Updated package

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.arcicode.alibre.R // Updated import
import com.arcicode.alibre.databinding.ActivityMainBinding // Updated import

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // No storage permissions needed when using Storage Access Framework (SAF)
        initializeApp()
    }

    private fun initializeApp() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupActionBarWithNavController(navController)

        // The start destination is already set in nav_graph.xml, so LibraryFragment will load.
        // No need to manually add fragment if (savedInstanceState == null) for the initial fragment.
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

