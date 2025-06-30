package com.arcicode.alibre // Updated package

import android.app.Application
import com.arcicode.alibre.db.AppDatabase // Updated import

class EBookReaderApplication : Application() {

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // Other application-wide initializations can go here
    }
}

