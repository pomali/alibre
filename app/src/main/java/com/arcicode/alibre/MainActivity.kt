package com.arcicode.alibre // Updated package

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.arcicode.alibre.R // Updated import
import com.arcicode.alibre.databinding.ActivityMainBinding // Updated import
import com.arcicode.alibre.utils.PermissionUtils // Updated import

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionUtils.hasReadExternalStoragePermission(this)) {
            PermissionUtils.requestReadExternalStoragePermission(this)
        } else {
            initializeApp()
        }
    }

    private fun initializeApp() {
        Toast.makeText(this, "Permissions Granted. Initializing App.", Toast.LENGTH_SHORT).show()

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.READ_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                initializeApp()
            } else {
                // Permission denied
                Toast.makeText(this, "Read storage permission is required to access ebooks.", Toast.LENGTH_LONG).show()
                // Optionally, disable functionality or guide user to settings
            }
        }
    }
}

